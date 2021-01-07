/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.repository.util;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.repository.util.QueryExecutionConverters.*;

import io.vavr.collection.Seq;
import io.vavr.control.Try;
import io.vavr.control.Try.Failure;
import lombok.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;
import scala.Option;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.common.base.Optional;

/**
 * Unit tests for {@link QueryExecutionConverters}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Maciek Opała
 */
class QueryExecutionConvertersUnitTests {

	DefaultConversionService conversionService;

	@BeforeEach
	void setUp() {

		this.conversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(conversionService);
	}

	@Test // DATACMNS-714
	void registersWrapperTypes() {

		assertThat(QueryExecutionConverters.supports(Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(java.util.Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Future.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(ListenableFuture.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Option.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(io.vavr.control.Option.class)).isTrue();
	}

	@Test // DATACMNS-836
	void registersUnwrapperTypes() {

		assertThat(QueryExecutionConverters.supportsUnwrapping(Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(java.util.Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Future.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(ListenableFuture.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Option.class)).isTrue();
	}

	@Test // DATACMNS-836
	void doesNotRegisterReactiveUnwrapperTypes() {

		assertThat(QueryExecutionConverters.supportsUnwrapping(Publisher.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Mono.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Flux.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Single.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Completable.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Observable.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Single.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Maybe.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Completable.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Flowable.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Observable.class)).isFalse();
	}

	@Test // DATACMNS-714
	void registersCompletableFutureAsWrapperTypeOnSpring42OrBetter() {
		assertThat(QueryExecutionConverters.supports(CompletableFuture.class)).isTrue();
	}

	@Test // DATACMNS-483
	void turnsNullIntoGuavaOptional() {
		assertThat(conversionService.convert(new NullableWrapper(null), Optional.class)).isEqualTo(Optional.absent());
	}

	@Test // DATACMNS-483
	@SuppressWarnings("unchecked")
	void turnsNullIntoJdk8Optional() {
		assertThat(conversionService.convert(new NullableWrapper(null), java.util.Optional.class)).isEmpty();
	}

	@Test // DATACMNS-714
	@SuppressWarnings("unchecked")
	void turnsNullIntoCompletableFutureForNull() throws Exception {

		CompletableFuture<Object> result = conversionService.convert(new NullableWrapper(null), CompletableFuture.class);

		assertThat(result).isNotNull();
		assertThat(result.isDone()).isTrue();
		assertThat(result.get()).isNull();
	}

	@Test // DATACMNS-768
	void unwrapsJdk8Optional() {
		assertThat(QueryExecutionConverters.unwrap(java.util.Optional.of("Foo"))).isEqualTo("Foo");
	}

	@Test // DATACMNS-768
	void unwrapsGuava8Optional() {
		assertThat(QueryExecutionConverters.unwrap(Optional.of("Foo"))).isEqualTo("Foo");
	}

	@Test // DATACMNS-768
	void unwrapsNullToNull() {
		assertThat(QueryExecutionConverters.unwrap(null)).isNull();
	}

	@Test // DATACMNS-768
	void unwrapsNonWrapperTypeToItself() {
		assertThat(QueryExecutionConverters.unwrap("Foo")).isEqualTo("Foo");
	}

	@Test // DATACMNS-795
	void turnsNullIntoScalaOptionEmpty() {
		assertThat(conversionService.convert(new NullableWrapper(null), Option.class)).isEqualTo(Option.empty());
	}

	@Test // DATACMNS-795
	void unwrapsScalaOption() {
		assertThat(QueryExecutionConverters.unwrap(Option.apply("foo"))).isEqualTo("foo");
	}

	@Test // DATACMNS-874
	void unwrapsEmptyScalaOption() {
		assertThat(QueryExecutionConverters.unwrap(Option.empty())).isNull();
	}

	@Test // DATACMNS-1005
	void registersAllowedPageabletypes() {

		Set<Class<?>> allowedPageableTypes = QueryExecutionConverters.getAllowedPageableTypes();
		assertThat(allowedPageableTypes).contains(Page.class, Slice.class, List.class, Seq.class);
	}

	@Test // DATACMNS-1065
	void unwrapsEmptyVavrOption() {
		assertThat(QueryExecutionConverters.unwrap(io.vavr.control.Option.none())).isNull();
	}

	@Test // DATACMNS-1065
	void unwrapsVavrOption() {
		assertThat(QueryExecutionConverters.unwrap(io.vavr.control.Option.of("string"))).isEqualTo("string");
	}

	@Test // DATACMNS-1065
	void conversListToVavr() {

		assertThat(conversionService.canConvert(List.class, io.vavr.collection.Traversable.class)).isTrue();
		assertThat(conversionService.canConvert(List.class, io.vavr.collection.List.class)).isTrue();
		assertThat(conversionService.canConvert(List.class, io.vavr.collection.Set.class)).isTrue();
		assertThat(conversionService.canConvert(List.class, io.vavr.collection.Map.class)).isFalse();

		List<Integer> integers = Arrays.asList(1, 2, 3);

		io.vavr.collection.Traversable<?> result = conversionService.convert(integers,
				io.vavr.collection.Traversable.class);

		assertThat(result).isInstanceOf(io.vavr.collection.List.class);
	}

	@Test // DATACMNS-1065
	void convertsSetToVavr() {

		assertThat(conversionService.canConvert(Set.class, io.vavr.collection.Traversable.class)).isTrue();
		assertThat(conversionService.canConvert(Set.class, io.vavr.collection.Set.class)).isTrue();
		assertThat(conversionService.canConvert(Set.class, io.vavr.collection.List.class)).isTrue();
		assertThat(conversionService.canConvert(Set.class, io.vavr.collection.Map.class)).isFalse();

		Set<Integer> integers = Collections.singleton(1);

		io.vavr.collection.Traversable<?> result = conversionService.convert(integers,
				io.vavr.collection.Traversable.class);

		assertThat(result).isInstanceOf(io.vavr.collection.Set.class);
	}

	@Test // DATACMNS-1065
	void convertsMapToVavr() {

		assertThat(conversionService.canConvert(Map.class, io.vavr.collection.Traversable.class)).isTrue();
		assertThat(conversionService.canConvert(Map.class, io.vavr.collection.Map.class)).isTrue();
		assertThat(conversionService.canConvert(Map.class, io.vavr.collection.Set.class)).isFalse();
		assertThat(conversionService.canConvert(Map.class, io.vavr.collection.List.class)).isFalse();

		Map<String, String> map = Collections.singletonMap("key", "value");

		io.vavr.collection.Traversable<?> result = conversionService.convert(map, io.vavr.collection.Traversable.class);

		assertThat(result).isInstanceOf(io.vavr.collection.Map.class);
	}

	@Test // DATACMNS-1065
	void unwrapsVavrCollectionsToJavaOnes() {

		assertThat(unwrap(io.vavr.collection.List.of(1, 2, 3))).isInstanceOf(List.class);
		assertThat(unwrap(io.vavr.collection.LinkedHashSet.of(1, 2, 3))).isInstanceOf(Set.class);
		assertThat(unwrap(io.vavr.collection.LinkedHashMap.of("key", "value"))).isInstanceOf(Map.class);
	}

	@Test // DATACMNS-1065
	void vavrSeqIsASupportedPageableType() {

		Set<Class<?>> allowedPageableTypes = QueryExecutionConverters.getAllowedPageableTypes();
		assertThat(allowedPageableTypes).contains(io.vavr.collection.Seq.class);
	}

	@Test // DATAJPA-1258
	void convertsJavaListsToVavrSet() {

		List<String> source = Collections.singletonList("foo");

		assertThat(conversionService.convert(source, io.vavr.collection.Set.class)) //
				.isInstanceOf(io.vavr.collection.Set.class);
	}

	@Test // DATACMNS-1299
	void unwrapsPages() throws Exception {

		Method method = Sample.class.getMethod("pages");
		TypeInformation<Object> returnType = ClassTypeInformation.fromReturnTypeOf(method);

		assertThat(QueryExecutionConverters.unwrapWrapperTypes(returnType).getType())
				.isEqualTo(String.class);
	}

	@Test // DATACMNS-983
	void exposesExecutionAdapterForJavaslangTry() throws Throwable {

		Object result = getExecutionAdapter(Try.class).apply(() -> {
			throw new IOException("Some message!");
		});

		assertThat(result).isInstanceOf(Failure.class);
	}

	@Test // DATACMNS-983
	void unwrapsDomainTypeFromJavaslangTryWrapper() throws Exception {

		for (String methodName : Arrays.asList("tryMethod", "tryForSeqMethod")) {

			Method method = Sample.class.getMethod(methodName);

			TypeInformation<?> type = QueryExecutionConverters
					.unwrapWrapperTypes(ClassTypeInformation.fromReturnTypeOf(method));

			assertThat(type.getType()).isEqualTo(Sample.class);
		}
	}

	@Test // DATACMNS-983
	void exposesExecutionAdapterForVavrTry() throws Throwable {

		Object result = getExecutionAdapter(io.vavr.control.Try.class).apply(() -> {
			throw new IOException("Some message!");
		});

		assertThat(result).isInstanceOf(io.vavr.control.Try.Failure.class);
	}

	@Test // DATACMNS-983
	void unwrapsDomainTypeFromVavrTryWrapper() throws Exception {

		for (String methodName : Arrays.asList("tryMethod", "tryForSeqMethod")) {

			Method method = Sample.class.getMethod(methodName);

			TypeInformation<?> type = QueryExecutionConverters
					.unwrapWrapperTypes(ClassTypeInformation.fromReturnTypeOf(method));

			assertThat(type.getType()).isEqualTo(Sample.class);
		}
	}

	@Test // DATACMNS-1430
	void returnsStreamableForIterable() throws Exception {

		assertThat(conversionService.canConvert(Iterable.class, Streamable.class)).isTrue();
		assertThat(conversionService.convert(Arrays.asList("foo"), Streamable.class)).containsExactly("foo");
	}

	@Test // DATACMNS-1430
	void convertsToStreamableWrapper() throws Exception {

		assertThat(conversionService.canConvert(Iterable.class, StreamableWrapper.class)).isTrue();
		assertThat(conversionService.convert(Arrays.asList("foo"), StreamableWrapper.class).getStreamable()) //
				.containsExactly("foo");
	}

	@Test // DATACMNS-1430
	void convertsToStreamableWrapperImplementingStreamable() throws Exception {

		assertThat(conversionService.canConvert(Iterable.class, CustomStreamableWrapper.class)).isTrue();
		assertThat(conversionService.convert(Arrays.asList("foo"), CustomStreamableWrapper.class)) //
				.containsExactly("foo");
	}

	@Test // DATACMNS-1484
	void doesNotConvertCollectionToStreamableIfReturnTypeIsIterable() {

		List<String> source = Arrays.asList("1", "2");

		assertThat(conversionService.convert(source, Iterable.class)).isSameAs(source);

	}

	interface Sample {

		Page<String> pages();

		Try<Sample> tryMethod();

		Try<Seq<Sample>> tryForSeqMethod();

		io.vavr.control.Try<Sample> vavrTryMethod();

		io.vavr.control.Try<io.vavr.collection.Seq<Sample>> vavrTryForSeqMethod();
	}

	// DATACMNS-1430

	@Value(staticConstructor = "of")
	static class StreamableWrapper {
		Streamable<String> streamable;
	}

	@Value
	static class CustomStreamableWrapper<T> implements Streamable<T> {

		Streamable<T> source;

		@Override
		public Iterator<T> iterator() {
			return source.iterator();
		}
	}
}
