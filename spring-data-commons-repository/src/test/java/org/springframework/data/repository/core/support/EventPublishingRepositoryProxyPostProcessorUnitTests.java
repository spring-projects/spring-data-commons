/*
 * Copyright 2016-2025 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.EventPublishingRepositoryProxyPostProcessor.EventPublishingMethod;
import org.springframework.data.repository.core.support.EventPublishingRepositoryProxyPostProcessor.EventPublishingMethodInterceptor;

/**
 * Unit tests for {@link EventPublishingRepositoryProxyPostProcessor} and contained classes.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Yuki Yoshida
 * @author Réda Housni Alaoui
 * @soundtrack Henrik Freischlader Trio - Nobody Else To Blame (Openness)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventPublishingRepositoryProxyPostProcessorUnitTests {

	@Mock ApplicationEventPublisher publisher;
	@Mock MethodInvocation invocation;

	@Test // DATACMNS-928
	void rejectsNullAggregateTypes() {
		assertThatIllegalArgumentException().isThrownBy(() -> EventPublishingMethod.of(null));
	}

	@Test // DATACMNS-928
	void exposesEventsExposedByEntityToPublisher() {

		var first = new SomeEvent();
		var second = new SomeEvent();
		var entity = MultipleEvents.of(Arrays.asList(first, second));

		EventPublishingMethod.of(MultipleEvents.class).publishEventsFrom(List.of(entity), publisher);

		verify(publisher).publishEvent(eq(first));
		verify(publisher).publishEvent(eq(second));
	}

	@Test // DATACMNS-928
	void exposesSingleEventByEntityToPublisher() {

		var event = new SomeEvent();
		var entity = OneEvent.of(event);

		EventPublishingMethod.of(OneEvent.class).publishEventsFrom(List.of(entity), publisher);

		verify(publisher, times(1)).publishEvent(event);
	}

	@Test // DATACMNS-928
	void doesNotExposeNullEvent() {

		var entity = OneEvent.of(null);

		EventPublishingMethod.of(OneEvent.class).publishEventsFrom(List.of(entity), publisher);

		verify(publisher, times(0)).publishEvent(any());
	}

	@Test // DATACMNS-928
	void doesNotCreatePublishingMethodIfNoAnnotationDetected() {
		assertThat(EventPublishingMethod.of(Object.class)).isNull();
	}

	@Test // DATACMNS-928
	void interceptsSaveMethod() throws Throwable {

		var event = new SomeEvent();
		var sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("save", Object.class), sample);

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(event);
	}

	@Test // DATACMNS-1663
	void interceptsDeleteMethod() throws Throwable {
		var event = new SomeEvent();
		var sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("delete", Object.class), sample);

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(event);
	}

	@Test // DATACMNS-928
	void doesNotInterceptNonSaveMethod() throws Throwable {

		doReturn(SampleRepository.class.getMethod("findById", Object.class)).when(invocation).getMethod();

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher, never()).publishEvent(any());
	}

	@Test // DATACMNS-1663
	void doesNotInterceptDeleteByIdMethod() throws Throwable {

		doReturn(SampleRepository.class.getMethod("deleteById", Object.class)).when(invocation).getMethod();

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher, never()).publishEvent(any());
	}

	@Test // DATACMNS-928
	void registersAdviceIfDomainTypeExposesEvents() {

		RepositoryInformation information = new DummyRepositoryInformation(SampleRepository.class);
		RepositoryProxyPostProcessor processor = new EventPublishingRepositoryProxyPostProcessor(publisher);

		var factory = mock(ProxyFactory.class);

		processor.postProcess(factory, information);

		verify(factory).addAdvice(any(EventPublishingMethodInterceptor.class));
	}

	@Test // DATACMNS-928
	void doesNotAddAdviceIfDomainTypeDoesNotExposeEvents() {

		RepositoryInformation information = new DummyRepositoryInformation(CrudRepository.class);
		RepositoryProxyPostProcessor processor = new EventPublishingRepositoryProxyPostProcessor(publisher);

		var factory = mock(ProxyFactory.class);

		processor.postProcess(factory, information);

		verify(factory, never()).addAdvice(any(Advice.class));
	}

	@Test // DATACMNS-928
	void publishesEventsForCallToSaveWithIterable() throws Throwable {

		var event = new SomeEvent();
		var sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("saveAll", Iterable.class), List.of(sample));

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(any(SomeEvent.class));
	}

	@Test // DATACMNS-1663
	void publishesEventsForCallToDeleteWithIterable() throws Throwable {

		var event = new SomeEvent();
		var sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("deleteAll", Iterable.class), List.of(sample));

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(any(SomeEvent.class));
	}

	@Test // GH-2448
	void publishesEventsForCallToDeleteInBatchWithIterable() throws Throwable {

		var event = new SomeEvent();
		var sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("deleteInBatch", Iterable.class), List.of(sample));

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(any(SomeEvent.class));
	}

	@Test // GH-2448
	void publishesEventsForCallToDeleteAllInBatchWithIterable() throws Throwable {

		var event = new SomeEvent();
		var sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("deleteAllInBatch", Iterable.class), List.of(sample));

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(any(SomeEvent.class));
	}

	@Test // DATACMNS-975
	void publishesEventsAfterSaveInvocation() throws Throwable {

		doThrow(new IllegalStateException()).when(invocation).proceed();

		try {
			EventPublishingMethodInterceptor//
					.of(EventPublishingMethod.of(OneEvent.class), publisher)//
					.invoke(invocation);
		} catch (IllegalStateException o_O) {
			verify(publisher, never()).publishEvent(any(SomeEvent.class));
		}
	}

	@Test // DATACMNS-1113
	void invokesEventsForMethodsThatStartsWithSave() throws Throwable {

		var event = new SomeEvent();
		var sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("saveAndFlush", MultipleEvents.class), sample);

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(event);
	}

	@Test // DATACMNS-1067
	void clearsEventsEvenIfNoneWereExposedToPublish() {

		var entity = spy(EventsWithClearing.of(Collections.emptyList()));

		EventPublishingMethod.of(EventsWithClearing.class).publishEventsFrom(List.of(entity), publisher);

		verify(entity, times(1)).clearDomainEvents();
	}

	@Test // DATACMNS-1067
	void clearsEventsIfThereWereSomeToBePublished() {

		var entity = spy(EventsWithClearing.of(Collections.singletonList(new SomeEvent())));

		EventPublishingMethod.of(EventsWithClearing.class).publishEventsFrom(List.of(entity), publisher);

		verify(entity, times(1)).clearDomainEvents();
	}

	@Test // DATACMNS-1067
	void clearsEventsForOperationOnMutlipleAggregates() {

		var firstEntity = spy(EventsWithClearing.of(Collections.emptyList()));
		var secondEntity = spy(EventsWithClearing.of(Collections.singletonList(new SomeEvent())));

		Collection<EventsWithClearing> entities = Arrays.asList(firstEntity, secondEntity);

		EventPublishingMethod.of(EventsWithClearing.class).publishEventsFrom(entities, publisher);

		verify(firstEntity, times(1)).clearDomainEvents();
		verify(secondEntity, times(1)).clearDomainEvents();
	}

	@Test // DATACMNS-1163
	void publishesEventFromParameter() throws Throwable {

		var event = new Object();
		var parameter = MultipleEvents.of(Collections.singleton(event));
		var returnValue = MultipleEvents.of(Collections.emptySet());

		var method = SampleRepository.class.getMethod("save", Object.class);
		mockInvocation(invocation, method, parameter, returnValue);

		EventPublishingMethodInterceptor.of(EventPublishingMethod.of(MultipleEvents.class), publisher).invoke(invocation);

		verify(publisher, times(1)).publishEvent(event);
	}

	@Test // GH-2448
	void doesNotEmitEventsFromPrimitiveValue() throws Throwable {

		var method = SampleRepository.class.getMethod("delete", Object.class);
		mockInvocation(invocation, method, "foo", MultipleEvents.of(Collections.emptySet()));

		EventPublishingMethodInterceptor.of(EventPublishingMethod.of(MultipleEvents.class), publisher).invoke(invocation);

		verify(publisher, never()).publishEvent(any());
	}

	@Test // GH-3116
	void rejectsEventAddedDuringProcessing() throws Throwable {

		var originalEvent = new SomeEvent();
		var eventToBeAdded = new SomeEvent();

		var events = new ArrayList<Object>();
		events.add(originalEvent);

		var aggregate = MultipleEvents.of(events);

		doAnswer(invocation -> {

			events.add(eventToBeAdded);
			return null;

		}).when(publisher).publishEvent(any(Object.class));

		var method = EventPublishingMethod.of(MultipleEvents.class);

		assertThatIllegalStateException()
				.isThrownBy(() -> method.publishEventsFrom(List.of(aggregate), publisher))
				.withMessageContaining(eventToBeAdded.toString())
				.withMessageNotContaining(originalEvent.toString());
	}

	private static void mockInvocation(MethodInvocation invocation, Method method, Object parameterAndReturnValue)
			throws Throwable {

		mockInvocation(invocation, method, parameterAndReturnValue, parameterAndReturnValue);
	}

	private static void mockInvocation(MethodInvocation invocation, Method method, Object parameter, Object returnValue)
			throws Throwable {

		doReturn(method).when(invocation).getMethod();
		doReturn(new Object[] { parameter }).when(invocation).getArguments();
		doReturn(returnValue).when(invocation).proceed();
	}

	static final class MultipleEvents {
		private final Collection<? extends Object> events;

		private MultipleEvents(Collection<? extends Object> events) {
			this.events = events;
		}

		public static MultipleEvents of(Collection<? extends Object> events) {
			return new MultipleEvents(events);
		}

		@DomainEvents
		public Collection<?> getEvents() {
			return this.events;
		}
	}

	static class EventsWithClearing {
		final Collection<? extends Object> events;

		private EventsWithClearing(Collection<? extends Object> events) {
			this.events = events;
		}

		public static EventsWithClearing of(Collection<? extends Object> events) {
			return new EventsWithClearing(events);
		}

		@AfterDomainEventPublication
		void clearDomainEvents() {}

		@DomainEvents
		public Collection<?> getEvents() {
			return this.events;
		}
	}

	private static final class OneEvent {
		private final Object event;

		private OneEvent(Object event) {
			this.event = event;
		}

		public static OneEvent of(Object event) {
			return new OneEvent(event);
		}

		@DomainEvents
		public Object getEvent() {
			return this.event;
		}
	}

	private static class SomeEvent {
		final UUID id = UUID.randomUUID();
	}

	interface SampleRepository extends CrudRepository<MultipleEvents, Long> {

		MultipleEvents saveAndFlush(MultipleEvents events);

		MultipleEvents delete(String name);

		MultipleEvents deleteAllInBatch(Iterable<MultipleEvents> events);

		MultipleEvents deleteInBatch(Iterable<MultipleEvents> events);
	}
}
