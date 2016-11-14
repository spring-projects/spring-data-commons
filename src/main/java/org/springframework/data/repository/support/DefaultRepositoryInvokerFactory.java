/*
 * Copyright 2013-2014 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link RepositoryInvokerFactory} to inspect the requested repository type and create a
 * matching {@link RepositoryInvoker} that suits the repository best. That means, the more concrete the base interface
 * of the repository is, the more concrete will the actual invoker become - which means it will favor concrete method
 * invocations over reflection ones.
 * 
 * @author Oliver Gierke
 * @since 1.10
 */
public class DefaultRepositoryInvokerFactory implements RepositoryInvokerFactory {

	private final Repositories repositories;
	private final ConversionService conversionService;
	private final Map<Class<?>, RepositoryInvoker> invokers;

	/**
	 * Creates a new {@link DefaultRepositoryInvokerFactory} for the given {@link Repositories}.
	 * 
	 * @param repositories must not be {@literal null}.
	 */
	public DefaultRepositoryInvokerFactory(Repositories repositories) {
		this(repositories, new DefaultFormattingConversionService());
	}

	/**
	 * Creates a new {@link DefaultRepositoryInvokerFactory} for the given {@link Repositories} and
	 * {@link ConversionService}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public DefaultRepositoryInvokerFactory(Repositories repositories, ConversionService conversionService) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.repositories = repositories;
		this.conversionService = conversionService;
		this.invokers = new HashMap<Class<?>, RepositoryInvoker>();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.invoke.RepositoryInvokerFactory#getInvokerFor(java.lang.Class)
	 */
	@Override
	public RepositoryInvoker getInvokerFor(Class<?> domainType) {
		return invokers.computeIfAbsent(domainType, type -> prepareInvokers(type));
	}

	/**
	 * Creates a {@link RepositoryInvoker} for the repository managing the given domain type.
	 * 
	 * @param domainType
	 * @return
	 */
	private RepositoryInvoker prepareInvokers(Class<?> domainType) {

		Object repository = repositories.getRepositoryFor(domainType);
		Assert.notNull(repository, String.format("No repository found for domain type: %s", domainType));
		RepositoryInformation information = repositories.getRepositoryInformationFor(domainType);

		return createInvoker(information, repository);
	}

	@SuppressWarnings("unchecked")
	protected RepositoryInvoker createInvoker(RepositoryInformation information, Object repository) {

		if (repository instanceof PagingAndSortingRepository) {
			return new PagingAndSortingRepositoryInvoker((PagingAndSortingRepository<Object, Serializable>) repository,
					information, conversionService);
		} else if (repository instanceof CrudRepository) {
			return new CrudRepositoryInvoker((CrudRepository<Object, Serializable>) repository, information,
					conversionService);
		} else {
			return new ReflectionRepositoryInvoker(repository, information, conversionService);
		}
	}
}
