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

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.util.AnnotationDetectionMethodCallback;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RepositoryProxyPostProcessor} to register a {@link MethodInterceptor} to intercept the
 * {@link CrudRepository#save(Object)} method and publish events potentially exposed via a method annotated with
 * {@link DomainEvents}. If no such method can be detected on the aggregate root, no interceptor is added. Additionally,
 * the aggregate root can expose a method annotated with {@link AfterDomainEventPublication}. If present, the method will be
 * invoked after all events have been published.
 * 
 * @author Oliver Gierke
 * @since 1.13
 * @soundtrack Henrik Freischlader Trio - Master Plan (Openness)
 */
@RequiredArgsConstructor
public class EventPublishingRepositoryProxyPostProcessor implements RepositoryProxyPostProcessor {

	private final ApplicationEventPublisher publisher;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryProxyPostProcessor#postProcess(org.springframework.aop.framework.ProxyFactory, org.springframework.data.repository.core.RepositoryInformation)
	 */
	@Override
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {

		EventPublishingMethod method = EventPublishingMethod.of(repositoryInformation.getDomainType());

		if (method == null) {
			return;
		}

		factory.addAdvice(new EventPublishingMethodInterceptor(repositoryInformation.getCrudMethods().getSaveMethod(),
				method, publisher));
	}

	@RequiredArgsConstructor
	static class EventPublishingMethodInterceptor implements MethodInterceptor {

		private final Method saveMethod;
		private final EventPublishingMethod eventMethod;
		private final ApplicationEventPublisher publisher;

		/* 
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			if (!invocation.getMethod().equals(saveMethod)) {
				return invocation.proceed();
			}

			for (Object argument : invocation.getArguments()) {
				eventMethod.publishEventsFrom(argument, publisher);
			}

			return invocation.proceed();
		}
	}

	@RequiredArgsConstructor
	static class EventPublishingMethod {

		private static Map<Class<?>, EventPublishingMethod> CACHE = new ConcurrentReferenceHashMap<Class<?>, EventPublishingMethod>();
		private static EventPublishingMethod NONE = new EventPublishingMethod(null, null);

		private final Method publishingMethod;
		private final Method clearingMethod;

		/**
		 * Creates an {@link EventPublishingMethod} for the given type.
		 * 
		 * @param type must not be {@literal null}.
		 * @return an {@link EventPublishingMethod} for the given type or {@literal null} in case the given type does not
		 *         expose an event publishing method.
		 */
		public static EventPublishingMethod of(Class<?> type) {

			Assert.notNull(type, "Type must not be null!");

			EventPublishingMethod eventPublishingMethod = CACHE.get(type);

			if (eventPublishingMethod != null) {
				return eventPublishingMethod.orNull();
			}

			AnnotationDetectionMethodCallback<DomainEvents> publishing = new AnnotationDetectionMethodCallback<DomainEvents>(
					DomainEvents.class);
			ReflectionUtils.doWithMethods(type, publishing);

			// TODO: Lazify this as the inspection might not be needed if the publishing callback didn't find an annotation in
			// the first place

			AnnotationDetectionMethodCallback<AfterDomainEventPublication> clearing = new AnnotationDetectionMethodCallback<AfterDomainEventPublication>(
					AfterDomainEventPublication.class);
			ReflectionUtils.doWithMethods(type, clearing);

			EventPublishingMethod result = from(publishing, clearing);

			CACHE.put(type, result);

			return result.orNull();
		}

		/**
		 * Publishes all events in the given aggregate root using the given {@link ApplicationEventPublisher}.
		 * 
		 * @param object can be {@literal null}.
		 * @param publisher must not be {@literal null}.
		 */
		public void publishEventsFrom(Object object, ApplicationEventPublisher publisher) {

			if (object == null) {
				return;
			}

			for (Object event : asCollection(ReflectionUtils.invokeMethod(publishingMethod, object))) {
				publisher.publishEvent(event);
			}

			if (clearingMethod != null) {
				ReflectionUtils.invokeMethod(clearingMethod, object);
			}
		}

		/**
		 * Returns the current {@link EventPublishingMethod} or {@literal null} if it's the default value.
		 * 
		 * @return
		 */
		private EventPublishingMethod orNull() {
			return this == EventPublishingMethod.NONE ? null : this;
		}

		/**
		 * Creates a new {@link EventPublishingMethod} using the given pre-populated
		 * {@link AnnotationDetectionMethodCallback} looking up an optional clearing method from the given callback.
		 * 
		 * @param publishing must not be {@literal null}.
		 * @param clearing must not be {@literal null}.
		 * @return
		 */
		private static EventPublishingMethod from(AnnotationDetectionMethodCallback<?> publishing,
				AnnotationDetectionMethodCallback<?> clearing) {

			if (!publishing.hasFoundAnnotation()) {
				return EventPublishingMethod.NONE;
			}

			Method eventMethod = publishing.getMethod();
			ReflectionUtils.makeAccessible(eventMethod);

			return new EventPublishingMethod(eventMethod, getClearingMethod(clearing));
		}

		/**
		 * Returns the {@link Method} supposed to be invoked for event clearing or {@literal null} if none is found.
		 * 
		 * @param clearing must not be {@literal null}.
		 * @return
		 */
		private static Method getClearingMethod(AnnotationDetectionMethodCallback<?> clearing) {

			if (!clearing.hasFoundAnnotation()) {
				return null;
			}

			Method method = clearing.getMethod();
			ReflectionUtils.makeAccessible(method);

			return method;
		}

		/**
		 * Returns the given source object as collection, i.e. collections are returned as is, objects are turned into a
		 * one-element collection, {@literal null} will become an empty collection.
		 * 
		 * @param source can be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		private static Collection<Object> asCollection(Object source) {

			if (source == null) {
				return Collections.emptyList();
			}

			if (Collection.class.isInstance(source)) {
				return (Collection<Object>) source;
			}

			return Arrays.asList(source);
		}
	}
}
