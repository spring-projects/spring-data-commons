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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import scala.Option;

import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.common.base.Optional;

/**
 * Unit tests for {@link NullableWrapperConverters}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Maciek Opała
 */
class NullableWrapperConvertersUnitTests {

	DefaultConversionService conversionService;

	@BeforeEach
	void setUp() {

		this.conversionService = new DefaultConversionService();
		NullableWrapperConverters.registerConvertersIn(conversionService);
	}

	@Test // DATACMNS-714, DATACMNS-1762
	void registersWrapperTypes() {

		assertThat(NullableWrapperConverters.supports(Optional.class)).isTrue();
		assertThat(NullableWrapperConverters.supports(java.util.Optional.class)).isTrue();
		assertThat(NullableWrapperConverters.supports(Future.class)).isFalse();
		assertThat(NullableWrapperConverters.supports(ListenableFuture.class)).isFalse();
		assertThat(NullableWrapperConverters.supports(Option.class)).isTrue();
		assertThat(NullableWrapperConverters.supports(io.vavr.control.Option.class)).isTrue();
	}

	@Test // DATACMNS-836, DATACMNS-1762
	void registersUnwrapperTypes() {

		assertThat(NullableWrapperConverters.supportsUnwrapping(Optional.class)).isTrue();
		assertThat(NullableWrapperConverters.supportsUnwrapping(java.util.Optional.class)).isTrue();
		assertThat(NullableWrapperConverters.supportsUnwrapping(Future.class)).isFalse();
		assertThat(NullableWrapperConverters.supportsUnwrapping(ListenableFuture.class)).isFalse();
		assertThat(NullableWrapperConverters.supportsUnwrapping(Option.class)).isTrue();
	}

	@Test // DATACMNS-483, DATACMNS-1762
	void turnsNullIntoGuavaOptional() {
		assertThat(conversionService.convert(new NullableWrapper(null), Optional.class)).isEqualTo(Optional.absent());
	}

	@Test // DATACMNS-483, DATACMNS-1762
	@SuppressWarnings("unchecked")
	void turnsNullIntoJdk8Optional() {
		assertThat(conversionService.convert(new NullableWrapper(null), java.util.Optional.class)).isEmpty();
	}

	@Test // DATACMNS-768, DATACMNS-1762
	void unwrapsJdk8Optional() {
		assertThat(NullableWrapperConverters.unwrap(java.util.Optional.of("Foo"))).isEqualTo("Foo");
	}

	@Test // DATACMNS-768, DATACMNS-1762
	void unwrapsGuava8Optional() {
		assertThat(NullableWrapperConverters.unwrap(Optional.of("Foo"))).isEqualTo("Foo");
	}

	@Test // DATACMNS-768, DATACMNS-1762
	void unwrapsNullToNull() {
		assertThat(NullableWrapperConverters.unwrap(null)).isNull();
	}

	@Test // DATACMNS-768, DATACMNS-1762
	void unwrapsNonWrapperTypeToItself() {
		assertThat(NullableWrapperConverters.unwrap("Foo")).isEqualTo("Foo");
	}

	@Test // DATACMNS-795, DATACMNS-1762
	void turnsNullIntoScalaOptionEmpty() {
		assertThat(conversionService.convert(new NullableWrapper(null), Option.class)).isEqualTo(Option.empty());
	}

	@Test // DATACMNS-795, DATACMNS-1762
	void unwrapsScalaOption() {
		assertThat(NullableWrapperConverters.unwrap(Option.apply("foo"))).isEqualTo("foo");
	}

	@Test // DATACMNS-874, DATACMNS-1762
	void unwrapsEmptyScalaOption() {
		assertThat(NullableWrapperConverters.unwrap(Option.empty())).isNull();
	}

	@Test // DATACMNS-1065, DATACMNS-1762
	void unwrapsEmptyVavrOption() {
		assertThat(NullableWrapperConverters.unwrap(io.vavr.control.Option.none())).isNull();
	}

	@Test // DATACMNS-1065, DATACMNS-1762
	void unwrapsVavrOption() {
		assertThat(NullableWrapperConverters.unwrap(io.vavr.control.Option.of("string"))).isEqualTo("string");
	}

}
