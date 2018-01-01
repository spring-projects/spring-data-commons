/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.querydsl.binding;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.querydsl.core.types.EntityPath;

/**
 * Factory to create {@link QuerydslBindings} using an {@link EntityPathResolver}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.11
 */
public class QuerydslBindingsFactory implements ApplicationContextAware {

	private static final String INVALID_DOMAIN_TYPE = "Unable to find Querydsl root type for detected domain type %s! User @%s's root attribute to define the domain type manually!";

	private final EntityPathResolver entityPathResolver;
	private final Map<TypeInformation<?>, EntityPath<?>> entityPaths;

	private Optional<AutowireCapableBeanFactory> beanFactory;
	private Optional<Repositories> repositories;

	/**
	 * Creates a new {@link QuerydslBindingsFactory} using the given {@link EntityPathResolver}.
	 *
	 * @param entityPathResolver must not be {@literal null}.
	 */
	public QuerydslBindingsFactory(EntityPathResolver entityPathResolver) {

		Assert.notNull(entityPathResolver, "EntityPathResolver must not be null!");

		this.entityPathResolver = entityPathResolver;
		this.entityPaths = new ConcurrentReferenceHashMap<>();
		this.beanFactory = Optional.empty();
		this.repositories = Optional.empty();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.beanFactory = Optional.of(applicationContext.getAutowireCapableBeanFactory());
		this.repositories = Optional.of(new Repositories(applicationContext));
	}

	/**
	 * Returns the {@link EntityPathResolver} used by the factory.
	 *
	 * @return the entityPathResolver
	 */
	public EntityPathResolver getEntityPathResolver() {
		return entityPathResolver;
	}

	/**
	 * Creates the {@link QuerydslBindings} to be used using for the given domain type. A {@link QuerydslBinderCustomizer}
	 * will be auto-detected.
	 *
	 * @param domainType must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public QuerydslBindings createBindingsFor(TypeInformation<?> domainType) {
		return createBindingsFor(domainType, Optional.empty());
	}

	/**
	 * Creates the {@link QuerydslBindings} to be used using for the given domain type and a pre-defined
	 * {@link QuerydslBinderCustomizer}.
	 *
	 * @param domainType must not be {@literal null}.
	 * @param customizer the {@link QuerydslBinderCustomizer} to use, must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public QuerydslBindings createBindingsFor(TypeInformation<?> domainType,
			Class<? extends QuerydslBinderCustomizer<?>> customizer) {
		return createBindingsFor(domainType, Optional.of(customizer));
	}

	/**
	 * Creates the {@link QuerydslBindings} to be used using for the given domain type and a pre-defined
	 * {@link QuerydslBinderCustomizer}. If no customizer is given, auto-detection will be applied.
	 *
	 * @param domainType must not be {@literal null}.
	 * @param customizer the {@link QuerydslBinderCustomizer} to use. If an empty {@link Optional} is given customizer
	 *          detection for the given domain type will be applied.
	 * @return
	 */
	private QuerydslBindings createBindingsFor(TypeInformation<?> domainType,
			Optional<Class<? extends QuerydslBinderCustomizer<?>>> customizer) {

		Assert.notNull(customizer, "Customizer must not be null!");
		Assert.notNull(domainType, "Domain type must not be null!");

		EntityPath<?> path = verifyEntityPathPresent(domainType);

		QuerydslBindings bindings = new QuerydslBindings();
		findCustomizerForDomainType(customizer, domainType.getType()).customize(bindings, path);

		return bindings;
	}

	/**
	 * Tries to detect a Querydsl query type for the given domain type candidate via the configured
	 * {@link EntityPathResolver}.
	 *
	 * @param candidate must not be {@literal null}.
	 * @throws IllegalStateException to indicate the query type can't be found and manual configuration is necessary.
	 */
	private EntityPath<?> verifyEntityPathPresent(TypeInformation<?> candidate) {

		return entityPaths.computeIfAbsent(candidate, key -> {

			try {
				return entityPathResolver.createPath(key.getType());
			} catch (IllegalArgumentException o_O) {
				throw new IllegalStateException(
						String.format(INVALID_DOMAIN_TYPE, key.getType(), QuerydslPredicate.class.getSimpleName()), o_O);
			}
		});
	}

	/**
	 * Obtains the {@link QuerydslBinderCustomizer} for the given domain type. Will inspect the given annotation for a
	 * dedicatedly configured one or consider the domain types's repository.
	 *
	 * @param annotation
	 * @param domainType
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private QuerydslBinderCustomizer<EntityPath<?>> findCustomizerForDomainType(
			Optional<? extends Class<? extends QuerydslBinderCustomizer>> customizer, Class<?> domainType) {

		return customizer//
				.filter(it -> !QuerydslBinderCustomizer.class.equals(it))//
				.map(this::createQuerydslBinderCustomizer)
				.orElseGet(() -> repositories.flatMap(it -> it.getRepositoryFor(domainType))//
						.map(it -> it instanceof QuerydslBinderCustomizer ? (QuerydslBinderCustomizer<EntityPath<?>>) it : null)//
						.orElse(NoOpCustomizer.INSTANCE));
	}

	/**
	 * Obtains a {@link QuerydslBinderCustomizer} for the given type. Will try to obtain a bean from the
	 * {@link org.springframework.beans.factory.BeanFactory} first or fall back to create a fresh instance through the
	 * {@link org.springframework.beans.factory.BeanFactory} or finally falling back to a plain instantiation if no
	 * {@link org.springframework.beans.factory.BeanFactory} is present.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private QuerydslBinderCustomizer<EntityPath<?>> createQuerydslBinderCustomizer(
			Class<? extends QuerydslBinderCustomizer> type) {

		return beanFactory.map(it -> {

			try {
				return it.getBean(type);
			} catch (NoSuchBeanDefinitionException e) {
				return it.createBean(type);
			}
		}).orElseGet(() -> BeanUtils.instantiateClass(type));
	}

	private static enum NoOpCustomizer implements QuerydslBinderCustomizer<EntityPath<?>> {

		INSTANCE;

		@Override
		public void customize(QuerydslBindings bindings, EntityPath<?> root) {}
	}
}
