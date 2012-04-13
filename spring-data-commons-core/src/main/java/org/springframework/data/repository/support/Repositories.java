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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.util.Assert;

/**
 * Wrapper class to access repository instances obtained from a {@link ListableBeanFactory}.
 * 
 * @author Oliver Gierke
 */
public class Repositories implements
		Iterable<Entry<EntityInformation<Object, Serializable>, CrudRepository<Object, Serializable>>> {

	static final Repositories NONE = new Repositories();

	private final Map<EntityInformation<Object, Serializable>, CrudRepository<Object, Serializable>> repositories = new HashMap<EntityInformation<Object, Serializable>, CrudRepository<Object, Serializable>>();

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

		for (RepositoryFactoryInformation entry : providers) {

			EntityInformation<Object, Serializable> metadata = entry.getEntityInformation();
			Class repositoryInterface = entry.getRepositoryInterface();

			if (CrudRepository.class.isAssignableFrom(repositoryInterface)) {
				Class<CrudRepository<Object, Serializable>> objectType = repositoryInterface;
				CrudRepository<Object, Serializable> repository = BeanFactoryUtils.beanOfTypeIncludingAncestors(factory,
						objectType);

				this.repositories.put(metadata, repository);
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
		return repositories.containsKey(getEntityInformationFor(domainClass));
	}

	/**
	 * Returns the repository managing the given domain class.
	 * 
	 * @param domainClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T, S extends Serializable> CrudRepository<T, S> getRepositoryFor(Class<?> domainClass) {
		return (CrudRepository<T, S>) repositories.get(getEntityInformationFor(domainClass));
	}

	/**
	 * Returns the repository for the given {@link EntityInformation}.
	 * 
	 * @param entityInformation
	 * @return the repository for the given {@link EntityInformation}.
	 */
	@SuppressWarnings("unchecked")
	public <T, S extends Serializable> CrudRepository<T, S> getRepositoryFor(EntityInformation<T, S> entityInformation) {
		return (CrudRepository<T, S>) repositories.get(entityInformation);
	}

	/**
	 * Returns the {@link EntityInformation} for the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link EntityInformation} for the given domain class or {@literal null} if no repository registered for
	 *         this domain class.
	 */
	@SuppressWarnings("unchecked")
	public <T, S extends Serializable> EntityInformation<T, S> getEntityInformationFor(Class<?> domainClass) {

		for (EntityInformation<?, Serializable> information : repositories.keySet()) {
			if (domainClass.equals(information.getJavaType())) {
				return (EntityInformation<T, S>) information;
			}
		}

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Entry<EntityInformation<Object, Serializable>, CrudRepository<Object, Serializable>>> iterator() {
		return repositories.entrySet().iterator();
	}
}
