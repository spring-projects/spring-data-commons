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
package org.springframework.data.repository.init;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.Assert;

/**
 * A {@link RepositoryPopulator} using a {@link ResourceReader} to read objects from the configured {@link Resource}
 * s.
 * 
 * @author Oliver Gierke
 * @since 1.4
 */
public class ResourceReaderRepositoryPopulator implements RepositoryPopulator {

	private static final Log LOG = LogFactory.getLog(ResourceReaderRepositoryPopulator.class);

	private final ResourcePatternResolver resolver;
	private final ResourceReader reader;
	private final ClassLoader classLoader;

	private Collection<Resource> resources;

	/**
	 * Creates a new {@link ResourceReaderRepositoryPopulator} using the given {@link ResourceReader}.
	 * 
	 * @param reader must not be {@literal null}.
	 */
	public ResourceReaderRepositoryPopulator(ResourceReader reader) {
		this(reader, null);
	}

	/**
	 * Createsa a new {@link ResourceReaderRepositoryPopulator} using the given {@link ResourceReader} and
	 * {@link ClassLoader}.
	 * 
	 * @param reader must not be {@literal null}.
	 * @param classLoader
	 */
	public ResourceReaderRepositoryPopulator(ResourceReader resourceReader, ClassLoader classLoader) {

		Assert.notNull(resourceReader);

		this.reader = resourceReader;
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
		Assert.hasText(location);
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
	 * @see org.springframework.data.repository.init.RepositoryPopulator#initialize()
	 */
	public void populate(Repositories repositories) {

		for (Resource resource : resources) {

			Object result = readObjectFrom(resource);

			if (result instanceof Collection) {
				for (Object element : (Collection<?>) result) {
					if (element != null) {
						persist(element, repositories);
					} else {
						LOG.info("Skipping null element found in unmarshal result!");
					}
				}
			} else {
				persist(result, repositories);
			}
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
	 * @param repositories must not be {@literal null}.
	 */
	private void persist(Object object, Repositories repositories) {
		CrudRepository<Object, Serializable> repository = repositories.getRepositoryFor(object.getClass());
		repository.save(object);
	}
}
