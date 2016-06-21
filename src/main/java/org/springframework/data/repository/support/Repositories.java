/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.repository.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Wrapper class to access repository instances obtained from a {@link ListableBeanFactory}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Thomas Eizinger
 */
public class Repositories implements Iterable<Class<?>> {

	static final Repositories NONE = new Repositories();

	private static final RepositoryFactoryInformation<Object, Serializable> EMPTY_REPOSITORY_FACTORY_INFO = EmptyRepositoryFactoryInformation.INSTANCE;
	private static final String DOMAIN_TYPE_MUST_NOT_BE_NULL = "Domain type must not be null!";

	private final Optional<BeanFactory> beanFactory;
	private final Map<Class<?>, String> repositoryBeanNames;
	private final Map<Class<?>, RepositoryFactoryInformation<Object, Serializable>> repositoryFactoryInfos;

	/**
	 * Constructor to create the {@link #NONE} instance.
	 */
	private Repositories() {

		this.beanFactory = Optional.empty();
		this.repositoryBeanNames = Collections.emptyMap();
		this.repositoryFactoryInfos = Collections.emptyMap();
	}

	/**
	 * Creates a new {@link Repositories} instance by looking up the repository instances and meta information from the
	 * given {@link ListableBeanFactory}.
	 * 
	 * @param factory must not be {@literal null}.
	 */
	public Repositories(ListableBeanFactory factory) {

		Assert.notNull(factory);

		this.beanFactory = Optional.of(factory);
		this.repositoryFactoryInfos = new HashMap<Class<?>, RepositoryFactoryInformation<Object, Serializable>>();
		this.repositoryBeanNames = new HashMap<Class<?>, String>();

		populateRepositoryFactoryInformation(factory);
	}

	private void populateRepositoryFactoryInformation(ListableBeanFactory factory) {

		for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, RepositoryFactoryInformation.class,
				false, false)) {
			cacheRepositoryFactory(name);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private synchronized void cacheRepositoryFactory(String name) {

		RepositoryFactoryInformation repositoryFactoryInformation = beanFactory.get().getBean(name,
				RepositoryFactoryInformation.class);
		Class<?> domainType = ClassUtils
				.getUserClass(repositoryFactoryInformation.getRepositoryInformation().getDomainType());

		RepositoryInformation information = repositoryFactoryInformation.getRepositoryInformation();
		Set<Class<?>> alternativeDomainTypes = information.getAlternativeDomainTypes();
		String beanName = BeanFactoryUtils.transformedBeanName(name);

		Set<Class<?>> typesToRegister = new HashSet<Class<?>>(alternativeDomainTypes.size() + 1);
		typesToRegister.add(domainType);
		typesToRegister.addAll(alternativeDomainTypes);

		for (Class<?> type : typesToRegister) {
			this.repositoryFactoryInfos.put(type, repositoryFactoryInformation);
			this.repositoryBeanNames.put(type, beanName);
		}
	}

	/**
	 * Returns whether we have a repository instance registered to manage instances of the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public boolean hasRepositoryFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		return repositoryFactoryInfos.containsKey(domainClass);
	}

	/**
	 * Returns the repository managing the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public Optional<Object> getRepositoryFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		Optional<String> repositoryBeanName = Optional.ofNullable(repositoryBeanNames.get(domainClass));
		return beanFactory.flatMap(it -> repositoryBeanName.map(name -> it.getBean(name)));
	}

	/**
	 * Returns the {@link RepositoryFactoryInformation} for the given domain class. The given <code>code</code> is
	 * converted to the actual user class if necessary, @see ClassUtils#getUserClass.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link RepositoryFactoryInformation} for the given domain class or {@literal null} if no repository
	 *         registered for this domain class.
	 */
	private RepositoryFactoryInformation<Object, Serializable> getRepositoryFactoryInfoFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		Class<?> userType = ClassUtils.getUserClass(domainClass);
		RepositoryFactoryInformation<Object, Serializable> repositoryInfo = repositoryFactoryInfos.get(userType);

		if (repositoryInfo != null) {
			return repositoryInfo;
		}

		if (!userType.equals(Object.class)) {
			return getRepositoryFactoryInfoFor(userType.getSuperclass());
		}

		return EMPTY_REPOSITORY_FACTORY_INFO;
	}

	/**
	 * Returns the {@link EntityInformation} for the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T, S extends Serializable> EntityInformation<T, S> getEntityInformationFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		return (EntityInformation<T, S>) getRepositoryFactoryInfoFor(domainClass).getEntityInformation();
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link RepositoryInformation} for the given domain class or {@literal Optional#empty()} if no
	 *         repository registered for this domain class.
	 */
	public Optional<RepositoryInformation> getRepositoryInformationFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		RepositoryFactoryInformation<Object, Serializable> information = getRepositoryFactoryInfoFor(domainClass);
		return information == EMPTY_REPOSITORY_FACTORY_INFO ? Optional.empty()
				: Optional.of(information.getRepositoryInformation());
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given repository interface.
	 * 
	 * @param repositoryInterface must not be {@literal null}.
	 * @return the {@link RepositoryInformation} for the given repository interface or {@literal null} there's no
	 *         repository instance registered for the given interface.
	 * @since 1.12
	 */
	public Optional<RepositoryInformation> getRepositoryInformation(Class<?> repositoryInterface) {

		return repositoryFactoryInfos.values().stream()//
				.map(RepositoryFactoryInformation::getRepositoryInformation)//
				.filter(information -> information.getRepositoryInterface().equals(repositoryInterface))//
				.findFirst();
	}

	/**
	 * Returns the {@link PersistentEntity} for the given domain class. Might return {@literal null} in case the module
	 * storing the given domain class does not support the mapping subsystem.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link PersistentEntity} for the given domain class or {@literal null} if no repository is registered
	 *         for the domain class or the repository is not backed by a {@link MappingContext} implementation.
	 */
	public PersistentEntity<?, ?> getPersistentEntity(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		return getRepositoryFactoryInfoFor(domainClass).getPersistentEntity();
	}

	/**
	 * Returns the {@link QueryMethod}s contained in the repository managing the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public List<QueryMethod> getQueryMethodsFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		return getRepositoryFactoryInfoFor(domainClass).getQueryMethods();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Class<?>> iterator() {
		return repositoryFactoryInfos.keySet().iterator();
	}

	/**
	 * Null-object to avoid nasty {@literal null} checks in cache lookups.
	 * 
	 * @author Thomas Darimont
	 */
	private static enum EmptyRepositoryFactoryInformation implements RepositoryFactoryInformation<Object, Serializable> {

		INSTANCE;

		@Override
		public EntityInformation<Object, Serializable> getEntityInformation() {
			return null;
		}

		@Override
		public RepositoryInformation getRepositoryInformation() {
			return null;
		}

		@Override
		public PersistentEntity<?, ?> getPersistentEntity() {
			return null;
		}

		@Override
		public List<QueryMethod> getQueryMethods() {
			return Collections.<QueryMethod>emptyList();
		}
	}
}
