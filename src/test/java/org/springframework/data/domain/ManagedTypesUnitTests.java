/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
class ManagedTypesUnitTests {

	@Mock Consumer<Class<?>> action;

	@Test // GH-2634
	void emptyNeverCallsAction() {

		ManagedTypes.empty().forEach(action);
		verify(action, never()).accept(any());
	}

	@Test // GH-2634
	void supplierBasedManagedTypesAreEvaluatedLazily() {

		Supplier<Iterable<Class<?>>> typesSupplier = spy(new Supplier<Iterable<Class<?>>>() {
			@Override
			public Iterable<Class<?>> get() {
				return Collections.singleton(Object.class);
			}
		});

		ManagedTypes managedTypes = ManagedTypes.of(typesSupplier);

		managedTypes.forEach(action); // 1st invocation
		verify(action).accept(any());
		verify(typesSupplier).get();

		managedTypes.forEach(action); // 2nd invocation
		verify(action, times(2)).accept(any());
		verify(typesSupplier, times(1)).get();
	}

	@Test // GH-2634
	void toListOnEmptyReturnsEmptyList() {
		assertThat(ManagedTypes.empty().toList()).isEmpty();
	}

	@Test // GH-2634
	void toListContainsEntriesInOrder() {
		assertThat(ManagedTypes.of(Arrays.asList(Object.class, List.class)).toList()).containsExactly(Object.class,
				List.class);
	}
}
