/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.repository.query;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.repository.query.spi.Function;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import lombok.Data;

/**
 * {@link MultiValueMap} like data structure to keep lists of
 * {@link org.springframework.data.repository.query.spi.Function}s indexed by name and argument list length, where the
 * value lists are actually unique with respect to the signature.
 *
 * @author Jens Schauder
 * @since 2.0
 */
class FunctionsMap {

	private final MultiValueMap<NameAndArgumentCount, Function> functions = CollectionUtils.toMultiValueMap(new HashMap<>());

	void addAll(Map<String, Function> newFunctions) {

		newFunctions.forEach((n, f) -> {
			NameAndArgumentCount k = new NameAndArgumentCount(n, f.getParameterCount());
			List<Function> currentElements = get(k);
			if (!contains(currentElements, f)) {
				functions.add(k, f);
			}
		});
	}

	void addAll(MultiValueMap<NameAndArgumentCount, Function> newFunctions) {

		newFunctions.forEach((k, list) -> {
			List<Function> currentElements = get(k);
			list.forEach(f -> {
				if (!contains(currentElements, f)) {
					functions.add(k, f);
				}
			});
		});
	}

	List<Function> get(NameAndArgumentCount key) {
		return functions.getOrDefault(key, Collections.emptyList());
	}

	/**
	 * Gets the function that best matches the parameters given.
	 *
	 * The {@code name} must match, and the {@code argumentTypes} must be compatible with parameter list of the function.
	 * In order to resolve ambiguity it checks for a method with exactly matching parameter list.
	 *
	 * @param name the name of the method
	 * @param argumentTypes types of arguments that the method must be able to accept
	 * @return a {@code Function} if a unique on gets found. {@code Optional.empty} if none matches.
	 *    Throws {@link IllegalStateException} if multiple functions match the parameters.
	 */
	Optional<Function> get(String name, List<TypeDescriptor> argumentTypes) {

		Stream<Function> candidates = get(new NameAndArgumentCount(name, argumentTypes.size()))
				.stream() //
				.filter(f -> f.supports(argumentTypes));
		return bestMatch(candidates.collect(Collectors.toList()), argumentTypes);
	}

	private static boolean contains(List<Function> elements, Function f) {
		return elements.stream().anyMatch(f::isSignatureEqual);
	}

	private static Optional<Function> bestMatch(List<Function> candidates, List<TypeDescriptor> argumentTypes) {

		if (candidates.isEmpty()) {
			return Optional.empty();
		}
		if (candidates.size() == 1) {
			return Optional.of(candidates.get(0));
		}

		Optional<Function> exactMatch = candidates.stream().filter(f -> f.supportsExact(argumentTypes)).findFirst();
		if (exactMatch.isPresent()) {
			return exactMatch;
		} else {
			throw new IllegalStateException("There are multiple matching methods.");
		}
	}

	@Data
	static class NameAndArgumentCount {
		private final String name;
		private final int count;

		static NameAndArgumentCount of(Method m) {
			return new NameAndArgumentCount(m.getName(), m.getParameterCount());
		}
	}
}
