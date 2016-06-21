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
package org.springframework.data.util;

import lombok.EqualsAndHashCode;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

/**
 * @author Oliver Gierke
 */
@EqualsAndHashCode(callSuper = true)
public class OptionalAssert<T> extends org.assertj.core.api.OptionalAssert<T> {

	public OptionalAssert(Optional<T> actual) {
		super(actual);
	}

	public static <T> OptionalAssert<T> assertOptional(Optional<T> optional) {
		return new OptionalAssert<T>(optional);
	}

	public Optional<T> getActual() {
		return actual;
	}

	public <S> OptionalAssert<S> flatMap(Function<T, Optional<S>> function) {

		Assertions.assertThat(actual).isPresent();

		return assertOptional(actual.flatMap(function));
	}

	public <S> OptionalAssert<S> map(Function<T, S> function) {

		Assertions.assertThat(actual).isPresent();

		return assertOptional(actual.map(function));
	}

	public <S> AbstractObjectAssert<?, S> value(Function<T, S> function) {

		Assertions.assertThat(actual).isPresent();

		return Assertions.assertThat(actual.map(function).orElseThrow(() -> new IllegalStateException()));
	}

	public OptionalAssert<T> isEqualTo(OptionalAssert<?> other) {

		Assertions.assertThat(actual).isEqualTo(other.actual);

		return this;
	}

	public OptionalAssert<T> andAssert(Consumer<OptionalAssert<T>> consumer) {

		consumer.accept(this);

		return this;
	}
}
