/*
 * Copyright 2015-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.querydsl.binding;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.querydsl.core.types.EntityPath;

/**
 * Factory to create {@link QuerydslBindings} using an {@link EntityPathResolver}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.11
 */
public class QuerydslBindingsFactory implements ApplicationContextAware {

	private static final String INVALID_DOMAIN_TYPE = "Unable to find Querydsl root type for detected domain type %s; User @%s's root attribute to define the domain type manually";

	private final EntityPathResolver entityPathResolver;
	private final Map<TypeInformation<?>, EntityPath<?>> entityPaths;

	private @Nullable AutowireCapableBeanFactory beanFactory;
	private @Nullable Repositories repositories;
	private QuerydslBinderCustomizer<EntityPath<?>> defaultCustomizer;

	/**
	 * Creates a new {@link QuerydslBindingsFactory} using the given {@link EntityPathResolver}.
	 *
	 * @param entityPathResolver must not be {@literal null}.
	 */
	public QuerydslBindingsFactory(EntityPathResolver entityPathResolver) {

		Assert.notNull(entityPathResolver, "EntityPathResolver must not be null");

		this.entityPathResolver = entityPathResolver;
		this.entityPaths = new ConcurrentHashMap<>();
		this.defaultCustomizer = NoOpCustomizer.INSTANCE;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
		this.repositories = new Repositories(applicationContext);
		this.defaultCustomizer = findDefaultCustomizer();
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
		return doCreateBindingsFor(domainType, null);
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
		return doCreateBindingsFor(domainType, customizer);
	}

	private QuerydslBindings doCreateBindingsFor(TypeInformation<?> domainType,
			@Nullable Class<? extends QuerydslBinderCustomizer<?>> customizer) {

		Assert.notNull(domainType, "Domain type must not be null");

		EntityPath<?> path = verifyEntityPathPresent(domainType);

		QuerydslBindings bindings = new QuerydslBindings();
		defaultCustomizer.customize(bindings, path);
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
	 * Obtains registered {@link QuerydslBinderCustomizerDefaults} instances from the
	 * {@link org.springframework.beans.factory.BeanFactory}.
	 *
	 * @return
	 */
	private QuerydslBinderCustomizer<EntityPath<?>> findDefaultCustomizer() {
		return beanFactory != null ? getDefaultQuerydslBinderCustomizer(beanFactory) : NoOpCustomizer.INSTANCE;
	}

	private QuerydslBinderCustomizer<EntityPath<?>> getDefaultQuerydslBinderCustomizer(
			AutowireCapableBeanFactory beanFactory) {

		List<QuerydslBinderCustomizerDefaults> customizers = beanFactory
				.getBeanProvider(QuerydslBinderCustomizerDefaults.class).stream().toList();

		return (bindings, root) -> {
			for (QuerydslBinderCustomizerDefaults querydslBinderCustomizerDefaults : customizers) {
				querydslBinderCustomizerDefaults.customize(bindings, root);
			}
		};
	}

	/**
	 * Obtains the {@link QuerydslBinderCustomizer} for the given domain type. Will inspect the given annotation for a
	 * dedicated configured one or consider the domain type's repository.
	 *
	 * @param customizer
	 * @param domainType
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private QuerydslBinderCustomizer<EntityPath<?>> findCustomizerForDomainType(
			@Nullable Class<? extends QuerydslBinderCustomizer> customizer, Class<?> domainType) {

		if (customizer == null || QuerydslBinderCustomizer.class.equals(customizer)) {

			if (repositories == null) {
				return NoOpCustomizer.INSTANCE;
			}

			return repositories.getRepositoryFor(domainType) //
					.map(it -> it instanceof QuerydslBinderCustomizer ? (QuerydslBinderCustomizer<EntityPath<?>>) it : null)
					.orElse(NoOpCustomizer.INSTANCE);
		}

		return createQuerydslBinderCustomizer(customizer);
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

		if (beanFactory == null) {
			return BeanUtils.instantiateClass(type);
		}

		try {
			return beanFactory.getBean(type);
		} catch (NoSuchBeanDefinitionException e) {
			return beanFactory.createBean(type);
		}
	}

	private enum NoOpCustomizer implements QuerydslBinderCustomizer<EntityPath<?>> {

		INSTANCE;

		@Override
		public void customize(QuerydslBindings bindings, EntityPath<?> root) {}
	}
}
