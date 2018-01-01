/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.repository.init;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link RepositoryPopulator} using a {@link ResourceReader} to read objects from the configured {@link Resource}s.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.4
 */
public class ResourceReaderRepositoryPopulator implements RepositoryPopulator, ApplicationEventPublisherAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceReaderRepositoryPopulator.class);

	private final ResourceReader reader;
	private final @Nullable ClassLoader classLoader;
	private final ResourcePatternResolver resolver;

	private @Nullable ApplicationEventPublisher publisher;
	private Collection<Resource> resources = Collections.emptySet();

	/**
	 * Creates a new {@link ResourceReaderRepositoryPopulator} using the given {@link ResourceReader}.
	 *
	 * @param reader must not be {@literal null}.
	 */
	public ResourceReaderRepositoryPopulator(ResourceReader reader) {
		this(reader, null);
	}

	/**
	 * Creates a new {@link ResourceReaderRepositoryPopulator} using the given {@link ResourceReader} and
	 * {@link ClassLoader}.
	 *
	 * @param reader must not be {@literal null}.
	 * @param classLoader can be {@literal null}.
	 */
	public ResourceReaderRepositoryPopulator(ResourceReader reader, @Nullable ClassLoader classLoader) {

		Assert.notNull(reader, "Reader must not be null!");

		this.reader = reader;
		this.classLoader = classLoader;
		this.resolver = classLoader == null ? new PathMatchingResourcePatternResolver()
				: new PathMatchingResourcePatternResolver(classLoader);
	}

	/**
	 * Configures the location of the {@link Resource}s to be used to initialize the repositories.
	 *
	 * @param location must not be {@literal null} or empty.
	 * @throws IOException
	 */
	public void setResourceLocation(String location) throws IOException {
		Assert.hasText(location, "Location must not be null!");
		setResources(resolver.getResources(location));
	}

	/**
	 * Configures the {@link Resource}s to be used to initialize the repositories.
	 *
	 * @param resources
	 */
	public void setResources(Resource... resources) {
		this.resources = Arrays.asList(resources);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.init.RepositoryPopulator#initialize()
	 */
	public void populate(Repositories repositories) {

		Assert.notNull(repositories, "Repositories must not be null!");

		RepositoryInvokerFactory invokerFactory = new DefaultRepositoryInvokerFactory(repositories);

		for (Resource resource : resources) {

			LOGGER.info(String.format("Reading resource: %s", resource));

			Object result = readObjectFrom(resource);

			if (result instanceof Collection) {
				for (Object element : (Collection<?>) result) {
					if (element != null) {
						persist(element, invokerFactory);
					} else {
						LOGGER.info("Skipping null element found in unmarshal result!");
					}
				}
			} else {
				persist(result, invokerFactory);
			}
		}

		if (publisher != null) {
			publisher.publishEvent(new RepositoriesPopulatedEvent(this, repositories));
		}
	}

	/**
	 * Reads the given resource into an {@link Object} using the configured {@link ResourceReader}.
	 *
	 * @param resource must not be {@literal null}.
	 * @return
	 */
	private Object readObjectFrom(Resource resource) {
		try {
			return reader.readFrom(resource, classLoader);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Persists the given {@link Object} using a suitable repository.
	 *
	 * @param object must not be {@literal null}.
	 * @param invokerFactory must not be {@literal null}.
	 */
	private void persist(Object object, RepositoryInvokerFactory invokerFactory) {

		RepositoryInvoker invoker = invokerFactory.getInvokerFor(object.getClass());
		LOGGER.debug(String.format("Persisting %s using repository %s", object, invoker));
		invoker.invokeSave(object);
	}
}
