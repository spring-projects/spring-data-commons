/*
 * Copyright 2016-present the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

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
 * {@link RepositoryProxyPostProcessor} to register a {@link MethodInterceptor} to intercept
 * {@link CrudRepository#save(Object)} and {@link CrudRepository#delete(Object)} methods and publish events potentially
 * exposed via a method annotated with {@link DomainEvents}. If no such method can be detected on the aggregate root, no
 * interceptor is added. Additionally, the aggregate root can expose a method annotated with
 * {@link AfterDomainEventPublication}. If present, the method will be invoked after all events have been published.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Yuki Yoshida
 * @author Réda Housni Alaoui
 * @since 1.13
 * @soundtrack Henrik Freischlader Trio - Master Plan (Openness)
 */
public class EventPublishingRepositoryProxyPostProcessor implements RepositoryProxyPostProcessor {

	private final ApplicationEventPublisher publisher;

	/**
	 * Creates a new {@link EventPublishingRepositoryProxyPostProcessor} for the given {@link ApplicationEventPublisher}.
	 *
	 * @param publisher must not be {@literal null}.
	 */
	public EventPublishingRepositoryProxyPostProcessor(ApplicationEventPublisher publisher) {

		Assert.notNull(publisher, "Object must not be null");

		this.publisher = publisher;
	}

	@Override
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {

		EventPublishingMethod method = EventPublishingMethod.of(repositoryInformation.getDomainType());

		if (method == null) {
			return;
		}

		factory.addAdvice(new EventPublishingMethodInterceptor(method, publisher));
	}

	/**
	 * {@link MethodInterceptor} to publish events exposed an aggregate on calls to a save or delete method on the
	 * repository.
	 *
	 * @author Oliver Gierke
	 * @since 1.13
	 */
	static class EventPublishingMethodInterceptor implements MethodInterceptor {

		private final EventPublishingMethod eventMethod;
		private final ApplicationEventPublisher publisher;

		private EventPublishingMethodInterceptor(EventPublishingMethod eventMethod, ApplicationEventPublisher publisher) {

			this.eventMethod = eventMethod;
			this.publisher = publisher;
		}

		public static EventPublishingMethodInterceptor of(EventPublishingMethod eventMethod,
				ApplicationEventPublisher publisher) {
			return new EventPublishingMethodInterceptor(eventMethod, publisher);
		}

		@Override
		public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {

			Object result = invocation.proceed();

			if (!isEventPublishingMethod(invocation.getMethod())) {
				return result;
			}

			Iterable<?> arguments = asIterable(invocation.getArguments()[0], invocation.getMethod());

			eventMethod.publishEventsFrom(arguments, publisher);

			return result;
		}
	}

	private static boolean isEventPublishingMethod(Method method) {
		return method.getParameterCount() == 1 //
				&& (isSaveMethod(method.getName()) || isDeleteMethod(method.getName()));
	}

	private static boolean isSaveMethod(String methodName) {
		return methodName.startsWith("save");
	}

	private static boolean isDeleteMethod(String methodName) {
		return methodName.equals("delete") || methodName.equals("deleteAll") || methodName.equals("deleteInBatch")
				|| methodName.equals("deleteAllInBatch");
	}

	/**
	 * Abstraction of a method on the aggregate root that exposes the events to publish.
	 *
	 * @author Oliver Gierke
	 * @since 1.13
	 */
	static class EventPublishingMethod {

		private static final Map<Class<?>, EventPublishingMethod> cache = new ConcurrentReferenceHashMap<>();
		private static final @SuppressWarnings("null") EventPublishingMethod NONE = new EventPublishingMethod(Object.class,
				null,
				null);
		private static final String ILLEGAL_MODIFICATION = "Aggregate's events were modified during event publication. "
				+ "Make sure event listeners obtain a fresh instance of the aggregate before adding further events. "
				+ "Additional events found: %s.";

		private final Class<?> type;
		private final @Nullable Method publishingMethod;
		private final @Nullable Method clearingMethod;

		EventPublishingMethod(Class<?> type, @Nullable Method publishingMethod, @Nullable Method clearingMethod) {

			this.type = type;
			this.publishingMethod = publishingMethod;
			this.clearingMethod = clearingMethod;
		}

		/**
		 * Creates an {@link EventPublishingMethod} for the given type.
		 *
		 * @param type must not be {@literal null}.
		 * @return an {@link EventPublishingMethod} for the given type or {@literal null} in case the given type does not
		 *         expose an event publishing method.
		 */
		public static @Nullable EventPublishingMethod of(Class<?> type) {

			Assert.notNull(type, "Type must not be null");

			EventPublishingMethod eventPublishingMethod = cache.get(type);

			if (eventPublishingMethod != null) {
				return eventPublishingMethod.orNull();
			}

			EventPublishingMethod result = from(type, getDetector(type, DomainEvents.class),
					() -> getDetector(type, AfterDomainEventPublication.class));

			cache.put(type, result);

			return result.orNull();
		}

		/**
		 * Publishes all events in the given aggregate root using the given {@link ApplicationEventPublisher}.
		 *
		 * @param aggregates can be {@literal null}.
		 * @param publisher must not be {@literal null}.
		 */
		public void publishEventsFrom(@Nullable Iterable<?> aggregates, ApplicationEventPublisher publisher) {

			if (aggregates == null || publishingMethod == null) {
				return;
			}

			for (Object aggregateRoot : aggregates) {

				if (!type.isInstance(aggregateRoot)) {
					continue;
				}

				var events = asCollection(ReflectionUtils.invokeMethod(publishingMethod, aggregateRoot));

				for (Object event : events) {
					publisher.publishEvent(event);
				}

				var postPublication = asCollection(ReflectionUtils.invokeMethod(publishingMethod, aggregateRoot));

				if (events.size() != postPublication.size()) {

					postPublication.removeAll(events);

					throw new IllegalStateException(ILLEGAL_MODIFICATION.formatted(postPublication));
				}

				if (clearingMethod != null) {
					ReflectionUtils.invokeMethod(clearingMethod, aggregateRoot);
				}
			}
		}

		/**
		 * Returns the current {@link EventPublishingMethod} or {@literal null} if it's the default value.
		 *
		 * @return
		 */
		private @Nullable EventPublishingMethod orNull() {
			return this == EventPublishingMethod.NONE ? null : this;
		}

		private static <T extends Annotation> AnnotationDetectionMethodCallback<T> getDetector(Class<?> type,
				Class<T> annotation) {

			AnnotationDetectionMethodCallback<T> callback = new AnnotationDetectionMethodCallback<>(annotation);
			ReflectionUtils.doWithMethods(type, callback);

			return callback;
		}

		/**
		 * Creates a new {@link EventPublishingMethod} using the given pre-populated
		 * {@link AnnotationDetectionMethodCallback} looking up an optional clearing method from the given callback.
		 *
		 * @param publishing must not be {@literal null}.
		 * @param clearing must not be {@literal null}.
		 * @return
		 */
		private static EventPublishingMethod from(Class<?> type, AnnotationDetectionMethodCallback<?> publishing,
				Supplier<AnnotationDetectionMethodCallback<?>> clearing) {

			if (!publishing.hasFoundAnnotation()) {
				return EventPublishingMethod.NONE;
			}

			Method eventMethod = publishing.getRequiredMethod();
			ReflectionUtils.makeAccessible(eventMethod);

			return new EventPublishingMethod(type, eventMethod, getClearingMethod(clearing.get()));
		}

		/**
		 * Returns the {@link Method} supposed to be invoked for event clearing or {@literal null} if none is found.
		 *
		 * @param clearing must not be {@literal null}.
		 * @return
		 */
		private static @Nullable Method getClearingMethod(AnnotationDetectionMethodCallback<?> clearing) {

			if (!clearing.hasFoundAnnotation()) {
				return null;
			}

			Method method = clearing.getRequiredMethod();
			ReflectionUtils.makeAccessible(method);

			return method;
		}

	}

	/**
	 * Returns the given source object as collection, i.e. collections are returned as is, objects are turned into a
	 * one-element collection, {@literal null} will become an empty collection.
	 *
	 * @param source can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	private static Collection<Object> asCollection(@Nullable Object source) {

		if (source == null) {
			return Collections.emptyList();
		}

		if (source instanceof Collection) {
			return new ArrayList<>((Collection<Object>) source);
		}

		return Collections.singletonList(source);
	}

	/**
	 * Returns the given source object as {@link Iterable}.
	 *
	 * @param source can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	private static @Nullable Iterable<Object> asIterable(@Nullable Object source, @Nullable Method method) {

		return method != null && method.getName().startsWith("saveAll")
				? (Iterable<Object>) source
				: asCollection(source);
	}
}
