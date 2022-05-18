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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.data.util.Lazy;

/**
 * Types managed by a Spring Data implementation. Used to predefine a set of know entities that might need processing
 * during the Spring container, Spring Data Repository initialization phase.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @see java.lang.FunctionalInterface
 * @since 3.0
 */
@FunctionalInterface
public interface ManagedTypes {

	/**
	 * Factory method used to construct a new instance of {@link ManagedTypes} containing no {@link Class types}.
	 *
	 * @return an empty {@link ManagedTypes} instance.
	 * @see java.util.Collections#emptySet()
	 * @see #of(Iterable)
	 */
	static ManagedTypes empty() {
		return of(Collections.emptySet());
	}

	/**
	 * Factory method used to construct {@link ManagedTypes} from the given, required {@link Iterable} of {@link Class
	 * types}.
	 *
	 * @param types {@link Iterable} of {@link Class types} used to initialize the {@link ManagedTypes}; must not be
	 *          {@literal null}.
	 * @return new instance of {@link ManagedTypes} initialized the given, required {@link Iterable} of {@link Class
	 *         types}.
	 * @see java.lang.Iterable
	 * @see #of(Stream)
	 * @see #of(Supplier)
	 */
	static ManagedTypes of(Iterable<? extends Class<?>> types) {
		return types::forEach;
	}

	/**
	 * Factory method used to construct {@link ManagedTypes} from the given, required {@link Stream} of {@link Class
	 * types}.
	 *
	 * @param types {@link Stream} of {@link Class types} used to initialize the {@link ManagedTypes}; must not be
	 *          {@literal null}.
	 * @return new instance of {@link ManagedTypes} initialized the given, required {@link Stream} of {@link Class types}.
	 * @see java.util.stream.Stream
	 * @see #of(Iterable)
	 * @see #of(Supplier)
	 */
	static ManagedTypes of(Stream<? extends Class<?>> types) {
		return types::forEach;
	}

	/**
	 * Factory method used to construct {@link ManagedTypes} from the given, required {@link Supplier} of an
	 * {@link Iterable} of {@link Class types}.
	 *
	 * @param dataProvider {@link Supplier} of an {@link Iterable} of {@link Class types} used to lazily initialize the
	 *          {@link ManagedTypes}; must not be {@literal null}.
	 * @return new instance of {@link ManagedTypes} initialized the given, required {@link Supplier} of an
	 *         {@link Iterable} of {@link Class types}.
	 * @see java.util.function.Supplier
	 * @see java.lang.Iterable
	 * @see #of(Iterable)
	 * @see #of(Stream)
	 */
	static ManagedTypes of(Supplier<Iterable<Class<?>>> dataProvider) {

		return new ManagedTypes() {

			final Lazy<Iterable<Class<?>>> lazyProvider = Lazy.of(dataProvider);

			@Override
			public void forEach(Consumer<Class<?>> action) {
				lazyProvider.get().forEach(action);
			}
		};
	}

	/**
	 * Applies the given {@link Consumer action} to each of the {@link Class types} contained in this {@link ManagedTypes}
	 * instance.
	 *
	 * @param action {@link Consumer} defining the action to perform on the {@link Class types} contained in this
	 *          {@link ManagedTypes} instance; must not be {@literal null}.
	 * @see java.util.function.Consumer
	 */
	void forEach(Consumer<Class<?>> action);

	/**
	 * Returns all the {@link ManagedTypes} in a {@link List}.
	 *
	 * @return these {@link ManagedTypes} in a {@link List}; never {@literal null}.
	 * @see java.util.List
	 */
	default List<Class<?>> toList() {

		List<Class<?>> list = new ArrayList<>(100);
		forEach(list::add);
		return list;
	}
}
