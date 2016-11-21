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

import scala.Option;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.common.base.Optional;

/**
 * Unit tests for {@link QueryExecutionConverters}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class QueryExecutionConvertersUnitTests {

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
		assertThat(QueryExecutionConverters.supports(javaslang.control.Option.class), is(true));
	}

	/**
	 * @see DATACMNS-714
	 */
	@Test
	public void registersCompletableFutureAsWrapperTypeOnSpring42OrBetter() {
		assertThat(QueryExecutionConverters.supports(CompletableFuture.class), is(true));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void turnsNullIntoGuavaOptional() {

		Optional<Object> optional = conversionService.convert(new NullableWrapper(null), Optional.class);
		assertThat(optional, is(Optional.<Object>absent()));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void turnsNullIntoJdk8Optional() {

		java.util.Optional<Object> optional = conversionService.convert(new NullableWrapper(null),
				java.util.Optional.class);
		assertThat(optional, is(java.util.Optional.<Object>empty()));
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
				is(Option.<Object>empty()));
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

	/**
	 * @see DATACMNS-937
	 */
	@Test
	public void turnsNullIntoJavaslangOption() {
		assertThat(conversionService.convert(new NullableWrapper(null), javaslang.control.Option.class),
				is((Object) optionNone()));
	}

	/**
	 * @see DATACMNS-937
	 */
	@Test
	public void wrapsValueIntoJavaslangOption() {

		javaslang.control.Option<?> result = conversionService.convert(new NullableWrapper("string"),
				javaslang.control.Option.class);

		assertThat(result.isEmpty(), is(false));
		assertThat(result.get(), is((Object) "string"));
	}

	/**
	 * @see DATACMNS-937
	 */
	@Test
	public void unwrapsEmptyJavaslangOption() {
		assertThat(QueryExecutionConverters.unwrap(optionNone()), is(nullValue()));
	}

	/**
	 * @see DATACMNS-937
	 */
	@Test
	public void unwrapsJavaslangOption() {
		assertThat(QueryExecutionConverters.unwrap(option("string")), is((Object) "string"));
	}

	@SuppressWarnings("unchecked")
	private static javaslang.control.Option<Object> optionNone() {

		Method method = ReflectionUtils.findMethod(javaslang.control.Option.class, "none");
		return (javaslang.control.Option<Object>) ReflectionUtils.invokeMethod(method, null);
	}

	@SuppressWarnings("unchecked")
	private static <T> javaslang.control.Option<T> option(T source) {

		Method method = ReflectionUtils.findMethod(javaslang.control.Option.class, "of", Object.class);
		return (javaslang.control.Option<T>) ReflectionUtils.invokeMethod(method, null, source);
	}
}
