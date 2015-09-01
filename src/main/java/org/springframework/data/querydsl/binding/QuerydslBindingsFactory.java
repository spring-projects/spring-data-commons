/*
 * Copyright 2015 the original author or authors.
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

import com.mysema.query.types.EntityPath;

/**
 * @author Oliver Gierke
 */
public class QuerydslBindingsFactory implements ApplicationContextAware {

	private static final String INVALID_DOMAIN_TYPE = "Unable to find Querydsl root type for detected domain type %s! User @%s's root attribute to define the domain type manually!";

	private final EntityPathResolver entityPathResolver;
	private final Map<TypeInformation<?>, EntityPath<?>> entityPaths;

	private AutowireCapableBeanFactory beanFactory;
	private Repositories repositories;

	/**
	 * @param entityPathResolver must not be {@literal null}.
	 */
	public QuerydslBindingsFactory(EntityPathResolver entityPathResolver) {

		Assert.notNull(entityPathResolver, "EntityPathResolver must not be null!");

		this.entityPathResolver = entityPathResolver;
		this.entityPaths = new ConcurrentReferenceHashMap<TypeInformation<?>, EntityPath<?>>();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
		this.repositories = new Repositories(applicationContext);
	}

	/**
	 * Returns the {@link EntityPathResolver} used by the factory.
	 * 
	 * @return the entityPathResolver
	 */
	public EntityPathResolver getEntityPathResolver() {
		return entityPathResolver;
	}

	public QuerydslBindings createBindingsFor(Class<? extends QuerydslBinderCustomizer> customizer,
			TypeInformation<?> domainType) {

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

		EntityPath<?> path = entityPaths.get(candidate);

		if (path != null) {
			return path;
		}

		Class<?> type = candidate.getType();

		try {
			path = entityPathResolver.createPath(type);
		} catch (IllegalArgumentException o_O) {
			throw new IllegalStateException(
					String.format(INVALID_DOMAIN_TYPE, candidate.getType(), QuerydslPredicate.class.getSimpleName()), o_O);
		}

		entityPaths.put(candidate, path);
		return path;
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
			Class<? extends QuerydslBinderCustomizer> customizer, Class<?> domainType) {

		if (customizer != null && !QuerydslBinderCustomizer.class.equals(customizer)) {
			return createQuerydslBinderCustomizer(customizer);
		}

		if (repositories != null && repositories.hasRepositoryFor(domainType)) {

			Object repository = repositories.getRepositoryFor(domainType);

			if (repository instanceof QuerydslBinderCustomizer) {
				return (QuerydslBinderCustomizer<EntityPath<?>>) repository;
			}
		}

		return NoOpCustomizer.INSTANCE;
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

	private static enum NoOpCustomizer implements QuerydslBinderCustomizer<EntityPath<?>> {

		INSTANCE;

		@Override
		public void customize(QuerydslBindings bindings, EntityPath<?> root) {}
	}
}
