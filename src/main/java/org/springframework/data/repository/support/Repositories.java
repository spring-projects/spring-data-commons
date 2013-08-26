/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;

/**
 * Wrapper class to access repository instances obtained from a {@link ListableBeanFactory}.
 * 
 * @author Oliver Gierke
 */
public class Repositories implements Iterable<Class<?>> {

	static final Repositories NONE = new Repositories();

	private final Map<Class<?>, RepositoryFactoryInformation<Object, Serializable>> domainClassToBeanName = new HashMap<Class<?>, RepositoryFactoryInformation<Object, Serializable>>();
	private final Map<RepositoryFactoryInformation<Object, Serializable>, String> repositories = new HashMap<RepositoryFactoryInformation<Object, Serializable>, String>();

	private final BeanFactory beanFactory;
	private final Set<String> repositoryFactoryBeanNames = new HashSet<String>();

	/**
	 * Constructor to create the {@link #NONE} instance.
	 */
	private Repositories() {
		this.beanFactory = null;
	}

	/**
	 * Creates a new {@link Repositories} instance by looking up the repository instances and meta information from the
	 * given {@link ListableBeanFactory}.
	 * 
	 * @param factory must not be {@literal null}.
	 */
	public Repositories(ListableBeanFactory factory) {

		Assert.notNull(factory);
		this.beanFactory = factory;

		String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory,
				RepositoryFactoryInformation.class, false, false);
		this.repositoryFactoryBeanNames.addAll(Arrays.asList(beanNamesForType));
	}

	/**
	 * Returns whether we have a repository instance registered to manage instances of the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public boolean hasRepositoryFor(Class<?> domainClass) {
		lookupRepositoryFactoryInformationFor(domainClass);
		return domainClassToBeanName.containsKey(domainClass);
	}

	/**
	 * Returns the repository managing the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T, S extends Serializable> CrudRepository<T, S> getRepositoryFor(Class<?> domainClass) {

		RepositoryFactoryInformation<Object, Serializable> information = getRepoInfoFor(domainClass);

		if (information == null) {
			return null;
		}

		return (CrudRepository<T, S>) beanFactory.getBean(repositories.get(information));
	}

	/**
	 * Returns the {@link EntityInformation} for the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T, S extends Serializable> EntityInformation<T, S> getEntityInformationFor(Class<?> domainClass) {

		RepositoryFactoryInformation<Object, Serializable> information = getRepoInfoFor(domainClass);
		return information == null ? null : (EntityInformation<T, S>) information.getEntityInformation();
	}

	/**
	 * Returns the {@link EntityInformation} for the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link EntityInformation} for the given domain class or {@literal null} if no repository registered for
	 *         this domain class.
	 */
	public RepositoryInformation getRepositoryInformationFor(Class<?> domainClass) {

		RepositoryFactoryInformation<Object, Serializable> information = getRepoInfoFor(domainClass);
		return information == null ? null : information.getRepositoryInformation();
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

		RepositoryFactoryInformation<Object, Serializable> information = getRepoInfoFor(domainClass);
		return information == null ? null : information.getPersistentEntity();
	}

	/**
	 * Returns the {@link QueryMethod}s contained in the repository managing the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public List<QueryMethod> getQueryMethodsFor(Class<?> domainClass) {

		RepositoryFactoryInformation<Object, Serializable> information = getRepoInfoFor(domainClass);
		return information == null ? Collections.<QueryMethod> emptyList() : information.getQueryMethods();
	}

	private RepositoryFactoryInformation<Object, Serializable> getRepoInfoFor(Class<?> domainClass) {

		Assert.notNull(domainClass);

		// Create defensive copy of the keys to allow threads to potentially add values while iterating over them
		Set<RepositoryFactoryInformation<Object, Serializable>> keys = Collections.unmodifiableSet(repositories.keySet());

		for (RepositoryFactoryInformation<Object, Serializable> information : keys) {
			if (domainClass.equals(information.getEntityInformation().getJavaType())) {
				return information;
			}
		}

		return lookupRepositoryFactoryInformationFor(domainClass);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Class<?>> iterator() {
		lookupRepositoryFactoryInformationFor(null);
		return domainClassToBeanName.keySet().iterator();
	}

	/**
	 * Looks up the {@link RepositoryFactoryInformation} for a given domain type. Will inspect the {@link BeanFactory} for
	 * beans implementing {@link RepositoryFactoryInformation} and cache the domain class to repository bean name mappings
	 * for further lookups. If a {@link RepositoryFactoryInformation} for the given domain type is found we interrupt the
	 * lookup proces to prevent beans from being looked up early.
	 * 
	 * @param domainType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private RepositoryFactoryInformation<Object, Serializable> lookupRepositoryFactoryInformationFor(Class<?> domainType) {

		if (domainClassToBeanName.containsKey(domainType)) {
			return domainClassToBeanName.get(domainType);
		}

		for (String repositoryFactoryName : repositoryFactoryBeanNames) {

			RepositoryFactoryInformation<Object, Serializable> information = beanFactory.getBean(repositoryFactoryName,
					RepositoryFactoryInformation.class);

			RepositoryInformation info = information.getRepositoryInformation();
			Class<?> repositoryInterface = info.getRepositoryInterface();

			if (!CrudRepository.class.isAssignableFrom(repositoryInterface)) {
				continue;
			}

			repositories.put(information, BeanFactoryUtils.transformedBeanName(repositoryFactoryName));
			domainClassToBeanName.put(info.getDomainType(), information);

			if (info.getDomainType().equals(domainType)) {
				return information;
			}
		}

		return null;
	}
}
