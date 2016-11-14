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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class LazyUnitTests {

	@Mock Supplier<String> supplier;
	@Mock Supplier<Optional<String>> optionalSupplier;

	@Test
	public void invokesSupplierOnFirstAccess() {

		doReturn("foo").when(supplier).get();

		Lazy<String> lazy = Lazy.of(supplier);

		assertThat(lazy.get()).isEqualTo("foo");
		assertThat(lazy.get()).isEqualTo("foo");

		verify(supplier, times(1)).get();
	}

	@Test
	public void createsLazyOptional() {

		doReturn(Optional.empty()).when(optionalSupplier).get();

		assertThat(Lazy.of(optionalSupplier).get()).isEmpty();
	}
}
