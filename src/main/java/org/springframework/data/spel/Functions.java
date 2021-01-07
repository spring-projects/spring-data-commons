/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.spel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.spel.spi.Function;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link MultiValueMap} like data structure to keep lists of
 * {@link org.springframework.data.repository.query.spi.Function}s indexed by name and argument list length, where the
 * value lists are actually unique with respect to the signature.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 * @since 2.1
 */
class Functions {

	private static final String MESSAGE_TEMPLATE = "There are multiple matching methods of name '%s' for parameter types (%s), but no "
			+ "exact match. Make sure to provide only one matching overload or one with exactly those types.";

	private final MultiValueMap<String, Function> functions = new LinkedMultiValueMap<>();

	void addAll(Map<String, Function> newFunctions) {

		newFunctions.forEach((n, f) -> {

			List<Function> currentElements = get(n);

			if (!contains(currentElements, f)) {
				functions.add(n, f);
			}
		});
	}

	void addAll(MultiValueMap<String, Function> newFunctions) {

		newFunctions.forEach((k, list) -> {

			List<Function> currentElements = get(k);

			list.stream() //
					.filter(f -> !contains(currentElements, f)) //
					.forEach(f -> functions.add(k, f));
		});
	}

	List<Function> get(String name) {
		return functions.getOrDefault(name, Collections.emptyList());
	}

	/**
	 * Gets the function that best matches the parameters given. The {@code name} must match, and the
	 * {@code argumentTypes} must be compatible with parameter list of the function. In order to resolve ambiguity it
	 * checks for a method with exactly matching parameter list.
	 *
	 * @param name the name of the method
	 * @param argumentTypes types of arguments that the method must be able to accept
	 * @return a {@code Function} if a unique on gets found. {@code Optional.empty} if none matches. Throws
	 *         {@link IllegalStateException} if multiple functions match the parameters.
	 */
	Optional<Function> get(String name, List<TypeDescriptor> argumentTypes) {

		Stream<Function> candidates = get(name).stream() //
				.filter(f -> f.supports(argumentTypes));

		List<Function> collect = candidates.collect(Collectors.toList());

		return bestMatch(collect, argumentTypes);
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

		if (!exactMatch.isPresent()) {
			throw new IllegalStateException(createErrorMessage(candidates, argumentTypes));
		}

		return exactMatch;
	}

	private static String createErrorMessage(List<Function> candidates, List<TypeDescriptor> argumentTypes) {

		String argumentTypeString = argumentTypes.stream()//
				.map(TypeDescriptor::getName)//
				.collect(Collectors.joining(","));

		return String.format(MESSAGE_TEMPLATE, candidates.get(0).getName(), argumentTypeString);
	}
}
