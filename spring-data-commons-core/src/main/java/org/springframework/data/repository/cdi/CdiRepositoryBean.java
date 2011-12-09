/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.repository.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * Base class for {@link Bean} wrappers.
 * 
 * @author Dirk Mahler
 * @author Oliver Gierke
 */
public abstract class CdiRepositoryBean<T> implements Bean<T> {

	private static final Log LOG = LogFactory.getLog(CdiRepositoryBean.class);

	private final Set<Annotation> qualifiers;
	private final Class<T> repositoryType;
	private final BeanManager beanManager;

	/**
	 * Creates a new {@link CdiRepositoryBean}.
	 * 
	 * @param qualifiers must not be {@literal null}.
	 * @param repositoryType has to be an interface must not be {@literal null}.
	 */
	public CdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager) {

		Assert.notNull(qualifiers);
		Assert.notNull(beanManager);
		Assert.notNull(repositoryType);
		Assert.isTrue(repositoryType.isInterface());

		this.qualifiers = qualifiers;
		this.repositoryType = repositoryType;
		this.beanManager = beanManager;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getTypes()
	 */
	@SuppressWarnings("rawtypes")
	public Set<Type> getTypes() {

		Set<Class> interfaces = new HashSet<Class>();
		interfaces.add(repositoryType);
		interfaces.addAll(Arrays.asList(repositoryType.getInterfaces()));

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Declaring types '%s' for repository '%s'.", interfaces.toString(),
					repositoryType.getName()));
		}

		return new HashSet<Type>(interfaces);
	}
	
	/**
	 * Returns an instance of an {@link EntityManager}.
	 * 
	 * @param beanManager The BeanManager.
	 * @param bean The bean representing an EntityManager.
	 * @return The EntityManager instance.
	 */
	@SuppressWarnings("unchecked")
	protected <S> S getDependencyInstance(Bean<S> bean, Class<S> type) {
		CreationalContext<S> creationalContext = beanManager.createCreationalContext(bean);
		return (S) beanManager.getReference(bean, type, creationalContext);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.context.spi.Contextual#create(javax.enterprise.context.spi.CreationalContext)
	 */
	public final T create(CreationalContext<T> creationalContext) {

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Creating bean instance for repository type '%s'.", repositoryType.getName()));
		}
		return create(creationalContext, repositoryType);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.context.spi.Contextual#destroy(java.lang.Object, javax.enterprise.context.spi.CreationalContext)
	 */
	public void destroy(T instance, CreationalContext<T> creationalContext) {

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Destroying bean instance %s for repository type '%s'.", instance.toString(),
					repositoryType.getName()));
		}

		creationalContext.release();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getQualifiers()
	 */
	public Set<Annotation> getQualifiers() {
		return qualifiers;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getName()
	 */
	public String getName() {
		return repositoryType.getName();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getStereotypes()
	 */
	public Set<Class<? extends Annotation>> getStereotypes() {

		Set<Class<? extends Annotation>> stereotypes = new HashSet<Class<? extends Annotation>>();

		for (Annotation annotation : repositoryType.getAnnotations()) {
			Class<? extends Annotation> annotationType = annotation.annotationType();
			if (annotationType.isAnnotationPresent(Stereotype.class)) {
				stereotypes.add(annotationType);
			}
		}

		return stereotypes;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getBeanClass()
	 */
	public Class<?> getBeanClass() {
		return repositoryType;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#isAlternative()
	 */
	public boolean isAlternative() {
		return repositoryType.isAnnotationPresent(Alternative.class);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#isNullable()
	 */
	public boolean isNullable() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getInjectionPoints()
	 */
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	/**
	 * @param creationalContext
	 * @param repositoryType
	 * @return
	 */
	protected abstract T create(CreationalContext<T> creationalContext, Class<T> repositoryType);

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("JpaRepositoryBean: type='%s', qualifiers=%s", repositoryType.getName(), qualifiers.toString());
	}
}
