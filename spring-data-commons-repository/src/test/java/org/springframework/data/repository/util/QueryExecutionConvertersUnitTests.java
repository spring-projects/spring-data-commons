/*
 * Copyright 2014-2025 the original author or authors.
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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import io.vavr.control.Try.Failure;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import scala.Option;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.util.NullableWrapper;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;

import com.google.common.base.Optional;

/**
 * Unit tests for {@link QueryExecutionConverters}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Maciek Opała
 * @author Johannes Englmeier
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
		assertThat(QueryExecutionConverters.supports(CompletableFuture.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Option.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(io.vavr.control.Option.class)).isTrue();
	}

	@Test // DATACMNS-836
	void registersUnwrapperTypes() {

		assertThat(QueryExecutionConverters.supportsUnwrapping(Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(java.util.Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Future.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(CompletableFuture.class)).isTrue();
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

		var allowedPageableTypes = QueryExecutionConverters.getAllowedPageableTypes();
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

	@Test // DATACMNS-1299
	void unwrapsPages() throws Exception {

		var method = Sample.class.getMethod("pages");
		var returnType = TypeInformation.fromReturnTypeOf(method);

		assertThat(QueryExecutionConverters.unwrapWrapperTypes(returnType).getType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-983
	void exposesExecutionAdapterForJavaslangTry() throws Throwable {

		var result = getExecutionAdapter(Try.class).apply(() -> {
			throw new IOException("Some message");
		});

		assertThat(result).isInstanceOf(Failure.class);
	}

	@Test // DATACMNS-983
	void unwrapsDomainTypeFromJavaslangTryWrapper() throws Exception {

		for (var methodName : Arrays.asList("tryMethod", "tryForSeqMethod")) {

			var method = Sample.class.getMethod(methodName);
			var type = QueryExecutionConverters.unwrapWrapperTypes(TypeInformation.fromReturnTypeOf(method));

			assertThat(type.getType()).isEqualTo(Sample.class);
		}
	}

	@Test // DATACMNS-983
	void exposesExecutionAdapterForVavrTry() throws Throwable {

		var result = getExecutionAdapter(Try.class).apply(() -> {
			throw new IOException("Some message");
		});

		assertThat(result).isInstanceOf(Failure.class);
	}

	@Test // DATACMNS-983
	void unwrapsDomainTypeFromVavrTryWrapper() throws Exception {

		for (var methodName : Arrays.asList("tryMethod", "tryForSeqMethod")) {

			var method = Sample.class.getMethod(methodName);
			var type = QueryExecutionConverters.unwrapWrapperTypes(TypeInformation.fromReturnTypeOf(method));

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

		var source = Arrays.asList("1", "2");

		assertThat(conversionService.convert(source, Iterable.class)).isSameAs(source);

	}

	interface Sample {

		Page<String> pages();

		Try<Sample> tryMethod();

		Try<Seq<Sample>> tryForSeqMethod();

		Try<Sample> vavrTryMethod();

		Try<Seq<Sample>> vavrTryForSeqMethod();
	}

	// DATACMNS-1430

	static final class StreamableWrapper {
		private final Streamable<String> streamable;

		private StreamableWrapper(Streamable<String> streamable) {
			this.streamable = streamable;
		}

		public static StreamableWrapper of(Streamable<String> streamable) {
			return new StreamableWrapper(streamable);
		}

		public Streamable<String> getStreamable() {
			return this.streamable;
		}

	}

	static final class CustomStreamableWrapper<T> implements Streamable<T> {

		private final Streamable<T> source;

		public CustomStreamableWrapper(Streamable<T> source) {
			this.source = source;
		}

		@Override
		public Iterator<T> iterator() {
			return source.iterator();
		}

		public Streamable<T> source() {
			return source;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj == null || obj.getClass() != this.getClass())
				return false;
			var that = (CustomStreamableWrapper) obj;
			return Objects.equals(this.source, that.source);
		}

		@Override
		public int hashCode() {
			return Objects.hash(source);
		}

		@Override
		public String toString() {
			return "CustomStreamableWrapper[" + "source=" + source + ']';
		}
	}
}
