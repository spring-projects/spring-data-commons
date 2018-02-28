/*
 * Copyright 2018 the original author or authors.
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

import static java.util.stream.Collectors.*;
import static org.springframework.core.CollectionFactory.*;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.ResultPostProcessor;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Invoker for {@link ResourceProcessor} implementations that will invoke them based on type matches
 *
 * @author Oliver Gierke
 */
class ResultPostProcessorInvoker {

	static final ResultPostProcessorInvoker NONE = new ResultPostProcessorInvoker(Object.class, Collections.emptyList());

	private final List<PostProcessorAdapter> processors;

	/**
	 * Creates a new {@link ResultPostProcessorInvoker} for the given aggregate reference type and {@link Collection} of
	 * post-processors.
	 * 
	 * @param aggregateType must not be {@literal null}.
	 * @param processors must not be {@literal null}.
	 */
	public ResultPostProcessorInvoker(Class<?> aggregateType,
			Collection<? extends ResultPostProcessor.ByType<?>> processors) {

		Assert.notNull(aggregateType, "Aggregate type must not be null!");
		Assert.notNull(processors, "Collection of AggregatePostProcessor must not be null!");

		this.processors = processors.stream() //
				.map(it -> ResultPostProcessor.ForAggregate.class.isInstance(it) //
						? PostProcessorAdapter.withReference((ResultPostProcessor.ForAggregate) it, aggregateType) //
						: PostProcessorAdapter.fromGeneric(it)) //
				.collect(Collectors.toList());
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public Object postProcess(@Nullable Object source) {

		if (processors.isEmpty()) {
			return source;
		}

		if (source == null) {
			return invokeProcessors(source);
		} else if (Collection.class.isInstance(source)) {
			return invokeProcessors(Collection.class.cast(source));
		} else if (Optional.class.isInstance(source)) {
			return Optional.class.cast(source).map(this::invokeProcessors);
		} else if (Stream.class.isInstance(source)) {
			return Stream.class.cast(source).map(this::invokeProcessors);
		} else if (ReactiveWrapperConverters.supports(source.getClass())) {
			return ReactiveWrapperConverters.map(source, this::invokeProcessors);
		}

		return invokeProcessors(source);
	}

	private Collection<?> invokeProcessors(Collection<?> source) {

		return source.stream() //
				.map(this::invokeProcessors) //
				.collect(toCollection(() -> createApproximateCollection(source, source.size())));
	}

	@Nullable
	private Object invokeProcessors(@Nullable Object source) {

		return processors.stream() //
				.filter(it -> it.canHandle(source)) //
				.reduce(source, (t, u) -> u.invoke(t), (l, r) -> r);
	}

	/**
	 * Simple wrapper around a {@link ResultPostProcessor} and an accompanying type for which the post processor is
	 * invoked.
	 *
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	private static class PostProcessorAdapter {

		private static final String UNRESOLVABLE_DOMAIN_TYPE = "Can't determine domain object type from %s. Make sure you define as standalone class, not as Lambda!";

		private final @NonNull Class<?> resolvedType;
		private final @NonNull ResultPostProcessor.ByType<Object> processor;

		/**
		 * Creates a new {@link PostProcessorAdapter} inspecting generics information of the given instance to find out for
		 * which result objects to invoke the given processor.
		 * 
		 * @param processor must not be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public static PostProcessorAdapter fromGeneric(ResultPostProcessor.ByType<? extends Object> processor) {

			Class<?> resolve = ResolvableType //
					.forClass(ResultPostProcessor.ByType.class, processor.getClass()) //
					.getGeneric(0) //
					.resolve();

			if (resolve == null) {
				throw new IllegalArgumentException(String.format(UNRESOLVABLE_DOMAIN_TYPE, processor.getClass()));
			}

			return new PostProcessorAdapter(resolve, (ResultPostProcessor.ByType<Object>) processor);
		}

		/**
		 * Creates a new {@link PostProcessorAdapter} with a given reference type.
		 * 
		 * @param processor must not be {@literal null}.
		 * @param reference must not be {@literal null}.
		 * @return
		 */
		public static PostProcessorAdapter withReference(ResultPostProcessor.ByType<Object> processor, Class<?> reference) {
			return new PostProcessorAdapter(reference, processor);
		}

		@Nullable
		public Object invoke(@Nullable Object source) {
			return processor.postProcess(source);
		}

		public boolean canHandle(@Nullable Object source) {
			return resolvedType.isInstance(source);
		}
	}
}
