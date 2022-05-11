/*
 * Copyright 2022 the original author or authors.
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Mono;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Unit tests for {@link ResourceReaderRepositoryPopulator} using reactive repositories.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@SpringJUnitConfig(classes = ReactiveResourceReaderRepositoryPopulatorUnitTests.ReactiveSampleConfiguration.class)
class ReactiveResourceReaderRepositoryPopulatorUnitTests {

	@Autowired ReactivePersonRepository personRepository;
	@Autowired ReactiveContactRepository contactRepository;
	@Autowired RxJavaUserRepository userRepository;
	@Autowired Repositories repositories;

	ApplicationEventPublisher publisher;
	ResourceReader reader;
	Resource resource;

	@BeforeEach
	void setUp() {

		this.reader = mock(ResourceReader.class);
		this.publisher = mock(ApplicationEventPublisher.class);
		this.resource = mock(Resource.class);
	}

	@Test // GH-2558
	void storesSingleUsingReactiveRepositoryObjectCorrectly() throws Exception {

		ReactivePerson reference = new ReactivePerson();
		when(personRepository.save(reference)).thenReturn(Mono.just(reference));
		setUpReferenceAndInitialize(reference);

		verify(personRepository).save(reference);
	}

	@Test // GH-2558
	void storesSingleUsingSimpleReactiveRepositoryObjectCorrectly() throws Exception {

		ReactiveContact reference = new ReactiveContact();
		when(contactRepository.save(reference)).thenReturn(Mono.just(reference));
		setUpReferenceAndInitialize(reference);

		verify(contactRepository).save(reference);
	}

	@Test // GH-2558
	void storesSingleUsingRxJavaRepositoryObjectCorrectly() throws Exception {

		ReactiveUser reference = new ReactiveUser();
		when(userRepository.save(reference)).thenReturn(Single.just(reference));
		setUpReferenceAndInitialize(reference);

		verify(userRepository).save(reference);
	}

	@Test
	void emitsRepositoriesPopulatedEventIfPublisherConfigured() throws Exception {

		ReactivePerson reference = new ReactivePerson();
		when(personRepository.save(reference)).thenReturn(Mono.just(reference));
		RepositoryPopulator populator = setUpReferenceAndInitialize(reference, publisher);

		ApplicationEvent event = new RepositoriesPopulatedEvent(populator, repositories);
		verify(publisher, times(1)).publishEvent(event);
	}

	private RepositoryPopulator setUpReferenceAndInitialize(Object reference, ApplicationEventPublisher publisher)
			throws Exception {

		when(reader.readFrom(any(), any())).thenReturn(reference);

		ResourceReaderRepositoryPopulator populator = new ResourceReaderRepositoryPopulator(reader);
		populator.setResources(resource);
		populator.setApplicationEventPublisher(publisher);
		populator.populate(repositories);

		return populator;
	}

	private RepositoryPopulator setUpReferenceAndInitialize(Object reference) throws Exception {
		return setUpReferenceAndInitialize(reference, null);
	}

	@Configuration
	static class ReactiveSampleConfiguration {

		@Autowired ApplicationContext context;

		@Bean
		Repositories repositories() {
			return new Repositories(context);
		}

		@Bean
		ReactivePersonRepository personRepository() {
			return mock(ReactivePersonRepository.class);
		}

		@Bean
		RepositoryFactoryBeanSupport<Repository<ReactivePerson, Object>, ReactivePerson, Object> personRepositoryFactory(
				ReactivePersonRepository personRepository) {

			DummyRepositoryFactoryBean<Repository<ReactivePerson, Object>, ReactivePerson, Object> factoryBean = new DummyRepositoryFactoryBean<>(
					ReactivePersonRepository.class);
			factoryBean.setCustomImplementation(personRepository);
			return factoryBean;
		}

		@Bean
		ReactiveContactRepository contactRepository() {
			return mock(ReactiveContactRepository.class);
		}

		@Bean
		RepositoryFactoryBeanSupport<Repository<ReactiveContact, Object>, ReactiveContact, Object> contactRepositoryFactory(
				ReactiveContactRepository contactRepository) {

			DummyRepositoryFactoryBean<Repository<ReactiveContact, Object>, ReactiveContact, Object> factoryBean = new DummyRepositoryFactoryBean<>(
					ReactiveContactRepository.class);
			factoryBean.setCustomImplementation(contactRepository);
			return factoryBean;
		}

		@Bean
		RxJavaUserRepository userRepository() {
			return mock(RxJavaUserRepository.class);
		}

		@Bean
		RepositoryFactoryBeanSupport<Repository<ReactiveUser, Object>, ReactiveUser, Object> userRepositoryFactory(
				RxJavaUserRepository userRepository) {

			DummyRepositoryFactoryBean<Repository<ReactiveUser, Object>, ReactiveUser, Object> factoryBean = new DummyRepositoryFactoryBean<>(
					RxJavaUserRepository.class);
			factoryBean.setCustomImplementation(userRepository);
			return factoryBean;
		}
	}

	static class ReactivePerson {

	}

	static class ReactiveContact {

	}

	static class ReactiveUser {

	}

	interface ReactivePersonRepository extends ReactiveCrudRepository<ReactivePerson, Object> {

	}

	interface ReactiveContactRepository extends Repository<ReactiveContact, Object> {

		Mono<ReactiveContact> save(ReactiveContact contact);

	}

	interface RxJavaUserRepository extends RxJava3CrudRepository<ReactiveUser, Object> {

	}
}
