/*
 * Copyright 2012 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
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
	private final Map<RepositoryFactoryInformation<Object, Serializable>, CrudRepository<Object, Serializable>> repositories = new HashMap<RepositoryFactoryInformation<Object, Serializable>, CrudRepository<Object, Serializable>>();

	/**
	 * Constructor to create the {@link #NONE} instance.
	 */
	private Repositories() {

	}

	/**
	 * Creates a new {@link Repositories} instance by looking up the repository instances and meta information from the
	 * given {@link ListableBeanFactory}.
	 * 
	 * @param factory must not be {@literal null}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Repositories(ListableBeanFactory factory) {

		Assert.notNull(factory);

		Collection<RepositoryFactoryInformation> providers = BeanFactoryUtils.beansOfTypeIncludingAncestors(factory,
				RepositoryFactoryInformation.class).values();

		for (RepositoryFactoryInformation<Object, Serializable> info : providers) {

			RepositoryInformation information = info.getRepositoryInformation();
			Class repositoryInterface = information.getRepositoryInterface();

			if (CrudRepository.class.isAssignableFrom(repositoryInterface)) {
				Class<CrudRepository<Object, Serializable>> objectType = repositoryInterface;
				CrudRepository<Object, Serializable> repository = BeanFactoryUtils.beanOfTypeIncludingAncestors(factory,
						objectType);

				this.domainClassToBeanName.put(information.getDomainType(), info);
				this.repositories.put(info, repository);
			}
		}
	}

	/**
	 * Returns whether we have a repository instance registered to manage instances of the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public boolean hasRepositoryFor(Class<?> domainClass) {
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
		return (CrudRepository<T, S>) repositories.get(domainClassToBeanName.get(domainClass));
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

		for (RepositoryFactoryInformation<Object, Serializable> information : repositories.keySet()) {
			if (domainClass.equals(information.getEntityInformation().getJavaType())) {
				return information;
			}
		}

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Class<?>> iterator() {
		return domainClassToBeanName.keySet().iterator();
	}
}
