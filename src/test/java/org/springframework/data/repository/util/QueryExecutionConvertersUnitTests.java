/*
 * Copyright 2014-2016 the original author or authors.
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
package org.springframework.data.repository.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;
import scala.Option;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.SpringVersion;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.util.Version;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.common.base.Optional;

/**
 * Unit tests for {@link QueryExecutionConverters}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class QueryExecutionConvertersUnitTests {

	private static final Version SPRING_VERSION = Version.parse(SpringVersion.getVersion());
	private static final Version FOUR_DOT_TWO = new Version(4, 2);

	DefaultConversionService conversionService;

	@Before
	public void setUp() {

		this.conversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(conversionService);
	}

	/**
	 * @see DATACMNS-714
	 */
	@Test
	public void registersWrapperTypes() {

		assertThat(QueryExecutionConverters.supports(Optional.class), is(true));
		assertThat(QueryExecutionConverters.supports(java.util.Optional.class), is(true));
		assertThat(QueryExecutionConverters.supports(Future.class), is(true));
		assertThat(QueryExecutionConverters.supports(ListenableFuture.class), is(true));
		assertThat(QueryExecutionConverters.supports(Option.class), is(true));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void registersReactiveWrapperTypes() {

		assertThat(QueryExecutionConverters.supports(Publisher.class), is(true));
		assertThat(QueryExecutionConverters.supports(Mono.class), is(true));
		assertThat(QueryExecutionConverters.supports(Flux.class), is(true));
		assertThat(QueryExecutionConverters.supports(Single.class), is(true));
		assertThat(QueryExecutionConverters.supports(Completable.class), is(true));
		assertThat(QueryExecutionConverters.supports(Observable.class), is(true));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void registersUnwrapperTypes() {

		assertThat(QueryExecutionConverters.supportsUnwrapping(Optional.class), is(true));
		assertThat(QueryExecutionConverters.supportsUnwrapping(java.util.Optional.class), is(true));
		assertThat(QueryExecutionConverters.supportsUnwrapping(Future.class), is(true));
		assertThat(QueryExecutionConverters.supportsUnwrapping(ListenableFuture.class), is(true));
		assertThat(QueryExecutionConverters.supportsUnwrapping(Option.class), is(true));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void doesNotRegisterReactiveUnwrapperTypes() {

		assertThat(QueryExecutionConverters.supportsUnwrapping(Publisher.class), is(false));
		assertThat(QueryExecutionConverters.supportsUnwrapping(Mono.class), is(false));
		assertThat(QueryExecutionConverters.supportsUnwrapping(Flux.class), is(false));
		assertThat(QueryExecutionConverters.supportsUnwrapping(Single.class), is(false));
		assertThat(QueryExecutionConverters.supportsUnwrapping(Completable.class), is(false));
		assertThat(QueryExecutionConverters.supportsUnwrapping(Observable.class), is(false));
	}

	/**
	 * @see DATACMNS-714
	 */
	@Test
	public void registersCompletableFutureAsWrapperTypeOnSpring42OrBetter() {

		assumeThat(SPRING_VERSION.isGreaterThanOrEqualTo(FOUR_DOT_TWO), is(true));

		assertThat(QueryExecutionConverters.supports(CompletableFuture.class), is(true));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void turnsNullIntoGuavaOptional() {

		Optional<Object> optional = conversionService.convert(new NullableWrapper(null), Optional.class);
		assertThat(optional, is(Optional.<Object> absent()));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void turnsNullIntoJdk8Optional() {

		java.util.Optional<Object> optional = conversionService.convert(new NullableWrapper(null),
				java.util.Optional.class);
		assertThat(optional, is(java.util.Optional.<Object> empty()));
	}

	/**
	 * @see DATACMNS-714
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void turnsNullIntoCompletableFutureForNull() throws Exception {

		CompletableFuture<Object> result = conversionService.convert(new NullableWrapper(null), CompletableFuture.class);

		assertThat(result, is(notNullValue()));
		assertThat(result.isDone(), is(true));
		assertThat(result.get(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-768
	 */
	@Test
	public void unwrapsJdk8Optional() {
		assertThat(QueryExecutionConverters.unwrap(java.util.Optional.of("Foo")), is((Object) "Foo"));
	}

	/**
	 * @see DATACMNS-768
	 */
	@Test
	public void unwrapsGuava8Optional() {
		assertThat(QueryExecutionConverters.unwrap(Optional.of("Foo")), is((Object) "Foo"));
	}

	/**
	 * @see DATACMNS-768
	 */
	@Test
	public void unwrapsNullToNull() {
		assertThat(QueryExecutionConverters.unwrap(null), is(nullValue()));
	}

	/**
	 * @see DATACMNS-768
	 */
	@Test
	public void unwrapsNonWrapperTypeToItself() {
		assertThat(QueryExecutionConverters.unwrap("Foo"), is((Object) "Foo"));
	}

	/**
	 * @see DATACMNS-795
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void turnsNullIntoScalaOptionEmpty() {

		assertThat((Option<Object>) conversionService.convert(new NullableWrapper(null), Option.class),
				is(Option.<Object> empty()));
	}

	/**
	 * @see DATACMNS-795
	 */
	@Test
	public void unwrapsScalaOption() {
		assertThat(QueryExecutionConverters.unwrap(Option.apply("foo")), is((Object) "foo"));
	}

	/**
	 * @see DATACMNS-874
	 */
	@Test
	public void unwrapsEmptyScalaOption() {
		assertThat(QueryExecutionConverters.unwrap(Option.empty()), is((Object) null));
	}
}
