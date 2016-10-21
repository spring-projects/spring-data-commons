/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import lombok.Getter;
import lombok.Value;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.EventPublishingRepositoryProxyPostProcessor.EventPublishingMethod;
import org.springframework.data.repository.core.support.EventPublishingRepositoryProxyPostProcessor.EventPublishingMethodInterceptor;

/**
 * Unit tests for {@link EventPublishingRepositoryProxyPostProcessor} and contained classes.
 * 
 * @author Oliver Gierke
 * @soundtrack Henrik Freischlader Trio - Nobody Else To Blame (Openness)
 */
@RunWith(MockitoJUnitRunner.class)
public class EventPublishingRepositoryProxyPostProcessorUnitTests {

	@Mock ApplicationEventPublisher publisher;
	@Mock MethodInvocation invocation;

	/**
	 * @see DATACMNS-928
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullAggregateTypes() {
		EventPublishingMethod.of(null);
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void publishingEventsForNullIsNoOp() {
		EventPublishingMethod.of(OneEvent.class).publishEventsFrom(null, publisher);
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void exposesEventsExposedByEntityToPublisher() {

		SomeEvent first = new SomeEvent();
		SomeEvent second = new SomeEvent();
		MultipleEvents entity = MultipleEvents.of(Arrays.asList(first, second));

		EventPublishingMethod.of(MultipleEvents.class).publishEventsFrom(entity, publisher);

		verify(publisher).publishEvent(eq(first));
		verify(publisher).publishEvent(eq(second));
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void exposesSingleEventByEntityToPublisher() {

		SomeEvent event = new SomeEvent();
		OneEvent entity = OneEvent.of(event);

		EventPublishingMethod.of(OneEvent.class).publishEventsFrom(entity, publisher);

		verify(publisher, times(1)).publishEvent(event);
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void doesNotExposeNullEvent() {

		OneEvent entity = OneEvent.of(null);

		EventPublishingMethod.of(OneEvent.class).publishEventsFrom(entity, publisher);

		verify(publisher, times(0)).publishEvent(any());
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void doesNotCreatePublishingMethodIfNoAnnotationDetected() {
		assertThat(EventPublishingMethod.of(Object.class), is(nullValue()));
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void interceptsSaveMethod() throws Throwable {

		Method saveMethod = SampleRepository.class.getMethod("save", Object.class);
		doReturn(saveMethod).when(invocation).getMethod();

		SomeEvent event = new SomeEvent();
		MultipleEvents sample = MultipleEvents.of(Arrays.asList(event));
		doReturn(new Object[] { sample }).when(invocation).getArguments();

		new EventPublishingMethodInterceptor(saveMethod, EventPublishingMethod.of(MultipleEvents.class), publisher)
				.invoke(invocation);

		verify(publisher).publishEvent(event);
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void doesNotInterceptNonSaveMethod() throws Throwable {

		Method saveMethod = SampleRepository.class.getMethod("save", Object.class);
		doReturn(SampleRepository.class.getMethod("findOne", Serializable.class)).when(invocation).getMethod();

		new EventPublishingMethodInterceptor(saveMethod, EventPublishingMethod.of(MultipleEvents.class), publisher)
				.invoke(invocation);

		verify(publisher, never()).publishEvent(any());
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void registersAdviceIfDomainTypeExposesEvents() {

		RepositoryInformation information = new DummyRepositoryInformation(SampleRepository.class);
		RepositoryProxyPostProcessor processor = new EventPublishingRepositoryProxyPostProcessor(publisher);

		ProxyFactory factory = mock(ProxyFactory.class);

		processor.postProcess(factory, information);

		verify(factory).addAdvice(any(EventPublishingMethodInterceptor.class));
	}

	/**
	 * @see DATACMNS-928
	 */
	@Test
	public void doesNotAddAdviceIfDomainTypeDoesNotExposeEvents() {

		RepositoryInformation information = new DummyRepositoryInformation(CrudRepository.class);
		RepositoryProxyPostProcessor processor = new EventPublishingRepositoryProxyPostProcessor(publisher);

		ProxyFactory factory = mock(ProxyFactory.class);

		processor.postProcess(factory, information);

		verify(factory, never()).addAdvice(any(Advice.class));
	}

	@Value(staticConstructor = "of")
	static class MultipleEvents {
		@Getter(onMethod = @__(@DomainEvents)) Collection<? extends Object> events;
	}

	@Value(staticConstructor = "of")
	static class OneEvent {
		@Getter(onMethod = @__(@DomainEvents)) Object event;
	}

	@Value
	static class SomeEvent {
		UUID id = UUID.randomUUID();
	}

	interface SampleRepository extends CrudRepository<MultipleEvents, Long> {}
}
