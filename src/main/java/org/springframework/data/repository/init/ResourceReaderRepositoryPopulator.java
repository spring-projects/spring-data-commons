/*
 * Copyright 2012-2022 the original author or authors.
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
package org.springframework.data.repository.init;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link RepositoryPopulator} using a {@link ResourceReader} to read objects from the configured {@link Resource}s.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.4
 */
public class ResourceReaderRepositoryPopulator implements RepositoryPopulator, ApplicationEventPublisherAware {

	private static final Log logger = LogFactory.getLog(ResourceReaderRepositoryPopulator.class);

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

		Assert.notNull(reader, "Reader must not be null");

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
		Assert.hasText(location, "Location must not be null");
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

		Assert.notNull(repositories, "Repositories must not be null");

		AggregatePersisterFactory persisterFactory = new AggregatePersisterFactory(repositories);

		for (Resource resource : resources) {

			logger.info(String.format("Reading resource: %s", resource));

			Object result = readObjectFrom(resource);

			if (result instanceof Collection) {
				for (Object element : (Collection<?>) result) {
					if (element != null) {
						persist(element, persisterFactory);
					} else {
						logger.info("Skipping null element found in unmarshal result");
					}
				}
			} else {
				persist(result, persisterFactory);
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
	 * @param persisterFactory must not be {@literal null}.
	 */
	private void persist(Object object, AggregatePersisterFactory persisterFactory) {

		AggregatePersister persister = persisterFactory.getPersisterFor(object.getClass());
		logger.debug(String.format("Persisting %s using repository %s", object, persister));
		persister.save(object);
	}

	/**
	 * Factory to create {@link AggregatePersister} instances.
	 */
	static class AggregatePersisterFactory {

		private final Map<Class<?>, AggregatePersister> persisters = new HashMap<>();
		private final Repositories repositories;

		public AggregatePersisterFactory(Repositories repositories) {
			this.repositories = repositories;
		}

		/**
		 * Obtain a {@link AggregatePersister}.
		 *
		 * @param domainType
		 * @return
		 */
		public AggregatePersister getPersisterFor(Class<?> domainType) {
			return persisters.computeIfAbsent(domainType, this::createPersisterFor);
		}

		private AggregatePersister createPersisterFor(Class<?> domainType) {

			RepositoryInformation repositoryInformation = repositories.getRequiredRepositoryInformation(domainType);
			Object repository = repositories.getRepositoryFor(domainType).orElseThrow(
					() -> new IllegalStateException(String.format("No repository found for domain type: %s", domainType)));

			if (repositoryInformation.isReactiveRepository()) {

				return repository instanceof ReactiveCrudRepository //
						? new ReactiveCrudRepositoryPersister(repository) //
						: new ReflectiveReactivePersister(repositoryInformation, repository);
			}

			if (repository instanceof CrudRepository) {
				return new CrudRepositoryPersister(repository);
			}

			return new ReflectivePersister(repositoryInformation, repository);
		}
	}

	/**
	 * Interface defining a save method to persist an object within a repository.
	 */
	interface AggregatePersister {

		/**
		 * Saves the {@code object} in an appropriate repository.
		 *
		 * @param object
		 */
		void save(Object object);
	}

	/**
	 * Reflection variant of a {@link AggregatePersister}.
	 */
	private static class ReflectivePersister implements AggregatePersister {

		private final CrudMethods methods;
		private final Object repository;

		public ReflectivePersister(RepositoryMetadata metadata, Object repository) {

			this.methods = metadata.getCrudMethods();
			this.repository = repository;
		}

		@Override
		public void save(Object object) {
			doPersist(object);
		}

		Object doPersist(Object object) {
			Method method = methods.getSaveMethod()//
					.orElseThrow(() -> new IllegalStateException("Repository doesn't have a save-method declared"));

			return ReflectionUtils.invokeMethod(method, repository, object);
		}

		@Override
		public String toString() {
			return repository.toString();
		}
	}

	/**
	 * Reactive extension to save objects in a reactive repository.
	 */
	private static class ReflectiveReactivePersister extends ReflectivePersister {

		public ReflectiveReactivePersister(RepositoryMetadata metadata, Object repository) {
			super(metadata, repository);
		}

		@Override
		public void save(Object object) {

			Object wrapper = doPersist(object);

			Publisher<?> publisher = ReactiveWrapperConverters.toWrapper(wrapper, Publisher.class);

			if (!(publisher instanceof Mono)) {
				publisher = Flux.from(publisher).collectList();
			}

			Mono.from(publisher).block();
		}
	}

	/**
	 * {@link AggregatePersister} to operate with {@link CrudRepository}.
	 */
	private static class CrudRepositoryPersister implements AggregatePersister {

		private final CrudRepository<Object, Object> repository;

		@SuppressWarnings("unchecked")
		public CrudRepositoryPersister(Object repository) {

			Assert.isInstanceOf(CrudRepository.class, repository);
			this.repository = (CrudRepository<Object, Object>) repository;
		}

		@Override
		public void save(Object object) {
			repository.save(object);
		}

		@Override
		public String toString() {
			return repository.toString();
		}
	}

	/**
	 * {@link AggregatePersister} to operate with {@link ReactiveCrudRepository}.
	 */
	private static class ReactiveCrudRepositoryPersister implements AggregatePersister {

		private final ReactiveCrudRepository<Object, Object> repository;

		@SuppressWarnings("unchecked")
		public ReactiveCrudRepositoryPersister(Object repository) {

			Assert.isInstanceOf(ReactiveCrudRepository.class, repository);
			this.repository = (ReactiveCrudRepository<Object, Object>) repository;
		}

		@Override
		public void save(Object object) {
			repository.save(object).block();
		}

		@Override
		public String toString() {
			return repository.toString();
		}
	}
}
