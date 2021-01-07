/*
 * Copyright 2016-2021 the original author or authors.
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
	void publishingEventsForNullIsNoOp() {
		EventPublishingMethod.of(OneEvent.class).publishEventsFrom(null, publisher);
	}

	@Test // DATACMNS-928
	void exposesEventsExposedByEntityToPublisher() {

		SomeEvent first = new SomeEvent();
		SomeEvent second = new SomeEvent();
		MultipleEvents entity = MultipleEvents.of(Arrays.asList(first, second));

		EventPublishingMethod.of(MultipleEvents.class).publishEventsFrom(entity, publisher);

		verify(publisher).publishEvent(eq(first));
		verify(publisher).publishEvent(eq(second));
	}

	@Test // DATACMNS-928
	void exposesSingleEventByEntityToPublisher() {

		SomeEvent event = new SomeEvent();
		OneEvent entity = OneEvent.of(event);

		EventPublishingMethod.of(OneEvent.class).publishEventsFrom(entity, publisher);

		verify(publisher, times(1)).publishEvent(event);
	}

	@Test // DATACMNS-928
	void doesNotExposeNullEvent() {

		OneEvent entity = OneEvent.of(null);

		EventPublishingMethod.of(OneEvent.class).publishEventsFrom(entity, publisher);

		verify(publisher, times(0)).publishEvent(any());
	}

	@Test // DATACMNS-928
	void doesNotCreatePublishingMethodIfNoAnnotationDetected() {
		assertThat(EventPublishingMethod.of(Object.class)).isNull();
	}

	@Test // DATACMNS-928
	void interceptsSaveMethod() throws Throwable {

		SomeEvent event = new SomeEvent();
		MultipleEvents sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("save", Object.class), sample);

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(event);
	}

	@Test // DATACMNS-1663
	public void interceptsDeleteMethod() throws Throwable {
		SomeEvent event = new SomeEvent();
		MultipleEvents sample = MultipleEvents.of(Collections.singletonList(event));
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
	public void doesNotInterceptDeleteByIdMethod() throws Throwable {

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

		ProxyFactory factory = mock(ProxyFactory.class);

		processor.postProcess(factory, information);

		verify(factory).addAdvice(any(EventPublishingMethodInterceptor.class));
	}

	@Test // DATACMNS-928
	void doesNotAddAdviceIfDomainTypeDoesNotExposeEvents() {

		RepositoryInformation information = new DummyRepositoryInformation(CrudRepository.class);
		RepositoryProxyPostProcessor processor = new EventPublishingRepositoryProxyPostProcessor(publisher);

		ProxyFactory factory = mock(ProxyFactory.class);

		processor.postProcess(factory, information);

		verify(factory, never()).addAdvice(any(Advice.class));
	}

	@Test // DATACMNS-928
	void publishesEventsForCallToSaveWithIterable() throws Throwable {

		SomeEvent event = new SomeEvent();
		MultipleEvents sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("saveAll", Iterable.class), sample);

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(any(SomeEvent.class));
	}

	@Test // DATACMNS-1663
	public void publishesEventsForCallToDeleteWithIterable() throws Throwable {

		SomeEvent event = new SomeEvent();
		MultipleEvents sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("deleteAll", Iterable.class), sample);

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

		SomeEvent event = new SomeEvent();
		MultipleEvents sample = MultipleEvents.of(Collections.singletonList(event));
		mockInvocation(invocation, SampleRepository.class.getMethod("saveAndFlush", MultipleEvents.class), sample);

		EventPublishingMethodInterceptor//
				.of(EventPublishingMethod.of(MultipleEvents.class), publisher)//
				.invoke(invocation);

		verify(publisher).publishEvent(event);
	}

	@Test // DATACMNS-1067
	void clearsEventsEvenIfNoneWereExposedToPublish() {

		EventsWithClearing entity = spy(EventsWithClearing.of(Collections.emptyList()));

		EventPublishingMethod.of(EventsWithClearing.class).publishEventsFrom(entity, publisher);

		verify(entity, times(1)).clearDomainEvents();
	}

	@Test // DATACMNS-1067
	void clearsEventsIfThereWereSomeToBePublished() {

		EventsWithClearing entity = spy(EventsWithClearing.of(Collections.singletonList(new SomeEvent())));

		EventPublishingMethod.of(EventsWithClearing.class).publishEventsFrom(entity, publisher);

		verify(entity, times(1)).clearDomainEvents();
	}

	@Test // DATACMNS-1067
	void clearsEventsForOperationOnMutlipleAggregates() {

		EventsWithClearing firstEntity = spy(EventsWithClearing.of(Collections.emptyList()));
		EventsWithClearing secondEntity = spy(EventsWithClearing.of(Collections.singletonList(new SomeEvent())));

		Collection<EventsWithClearing> entities = Arrays.asList(firstEntity, secondEntity);

		EventPublishingMethod.of(EventsWithClearing.class).publishEventsFrom(entities, publisher);

		verify(firstEntity, times(1)).clearDomainEvents();
		verify(secondEntity, times(1)).clearDomainEvents();
	}

	@Test // DATACMNS-1163
	void publishesEventFromParameter() throws Throwable {

		Object event = new Object();
		MultipleEvents parameter = MultipleEvents.of(Collections.singleton(event));
		MultipleEvents returnValue = MultipleEvents.of(Collections.emptySet());

		Method method = SampleRepository.class.getMethod("save", Object.class);
		mockInvocation(invocation, method, parameter, returnValue);

		EventPublishingMethodInterceptor.of(EventPublishingMethod.of(MultipleEvents.class), publisher).invoke(invocation);

		verify(publisher, times(1)).publishEvent(event);
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

	@Value(staticConstructor = "of")
	static class MultipleEvents {
		@Getter(onMethod = @__(@DomainEvents)) Collection<? extends Object> events;
	}

	@RequiredArgsConstructor(staticName = "of")
	static class EventsWithClearing {
		@Getter(onMethod = @__(@DomainEvents)) final Collection<? extends Object> events;

		@AfterDomainEventPublication
		void clearDomainEvents() {}
	}

	@Value(staticConstructor = "of")
	static class OneEvent {
		@Getter(onMethod = @__(@DomainEvents)) Object event;
	}

	@Value
	static class SomeEvent {
		UUID id = UUID.randomUUID();
	}

	interface SampleRepository extends CrudRepository<MultipleEvents, Long> {

		MultipleEvents saveAndFlush(MultipleEvents events);
	}
}
