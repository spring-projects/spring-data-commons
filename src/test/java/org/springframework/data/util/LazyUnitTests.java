/*
 * Copyright 2016-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link Lazy}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class LazyUnitTests {

	@Mock Supplier<String> supplier;
	@Mock Supplier<Optional<String>> optionalSupplier;

	@Test
	public void createsLazyOptional() {

		doReturn(Optional.empty()).when(optionalSupplier).get();

		assertThat(Lazy.of(optionalSupplier).get()).isEmpty();
	}

	@Test
	public void createsLazyFromValue() {

		Object value = new Object();

		assertThat(Lazy.of(value).get()).isEqualTo(value);
	}

	@Test
	public void returnsLastValueInChain() {

		Object reference = new Object();

		Object foo = Lazy.of(() -> null) //
				.or(() -> null) //
				.or(() -> reference) //
				.get();

		assertThat(foo).isEqualTo(reference);
	}

	@Test
	public void returnsCachedInstanceOnMultipleAccesses() {

		Lazy<Object> lazy = Lazy.of(() -> new Object());

		assertThat(lazy.get()).isSameAs(lazy.get());
	}

	@Test
	public void rejectsNullValueLookup() {

		assertThatExceptionOfType(IllegalStateException.class) //
				.isThrownBy(() -> Lazy.of(() -> null).get());
	}

	@Test
	public void allowsNullableValueLookupViaOptional() {
		assertThat(Lazy.of(() -> null).getOptional()).isEmpty();
	}

	@Test
	public void ignoresElseIfValuePresent() {

		Object first = new Object();
		Object second = new Object();

		Lazy<Object> nonEmpty = Lazy.of(() -> first);

		assertThat(nonEmpty.orElse(second)).isEqualTo(first);
		assertThat(nonEmpty.or(second).get()).isEqualTo(first);
		assertThat(nonEmpty.or(() -> second).get()).isEqualTo(first);
	}

	@Test
	public void returnsElseValue() {

		Object reference = new Object();

		Lazy<Object> empty = Lazy.of(() -> null);

		assertThat(empty.orElse(reference)).isEqualTo(reference);
		assertThat(empty.or(reference).get()).isEqualTo(reference);
		assertThat(empty.or(() -> reference).get()).isEqualTo(reference);
	}
}
