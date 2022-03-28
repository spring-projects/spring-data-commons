/*
 * Copyright 2022. the original author or authors.
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
package org.springframework.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.data.util.Lazy;

/**
 * Types managed by a Spring Data implementation. Used to predefine a set of know entities that might need processing
 * during container/repository initialization phase.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
public interface ManagedTypes {

	void forEach(Consumer<Class<?>> action);

	default List<Class<?>> toList() {

		List<Class<?>> tmp = new ArrayList<>(100);
		forEach(tmp::add);
		return tmp;
	}

	static ManagedTypes of(Iterable<? extends Class<?>> types) {
		return types::forEach;
	}

	static ManagedTypes of(Stream<? extends Class<?>> types) {
		return types::forEach;
	}

	static ManagedTypes o(Supplier<Iterable<? extends Class<?>>> dataProvider) {

		return new ManagedTypes() {

			Lazy<Iterable<? extends Class<?>>> lazyProvider = Lazy.of(dataProvider);

			@Override
			public void forEach(Consumer<Class<?>> action) {
				lazyProvider.get().forEach(action);
			}
		};
	}
}
