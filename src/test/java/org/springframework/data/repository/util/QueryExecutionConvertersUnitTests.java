/*
 * Copyright 2014-2015 the original author or authors.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.SpringVersion;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.util.Version;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.common.base.Optional;

/**
 * Unit tests for {@link QueryExecutionConverters}.
 * 
 * @author Oliver Gierke
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
}
