/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.repository.aot.generate;

import java.util.Set;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link VariableNameFactory} implementation keeping track of defined names resolving name clashes using internal
 * counter appending {@code _%d} to a suggested name in case of a clash.
 *
 * @author Christoph Strobl
 * @since 4.0
 */
class LocalVariableNameFactory implements VariableNameFactory {

	private final MultiValueMap<String, String> variables;

	static LocalVariableNameFactory forMethod(MethodMetadata methodMetadata) {
		return of(methodMetadata.getMethodArguments().keySet());
	}

	static LocalVariableNameFactory empty() {
		return of(Set.of());
	}

	static LocalVariableNameFactory of(Set<String> variables) {
		return new LocalVariableNameFactory(variables);
	}

	LocalVariableNameFactory(Iterable<String> predefinedVariableNames) {

		variables = new LinkedMultiValueMap<>();
		for (String parameterName : predefinedVariableNames) {
			variables.add(parameterName, parameterName);
		}
	}

	@Override
	public String generateName(String suggestedName) {

		if (!variables.containsKey(suggestedName)) {
			variables.add(suggestedName, suggestedName);
			return suggestedName;
		}

		String targetName = suggestTargetName(suggestedName);
		variables.add(suggestedName, targetName);
		variables.add(targetName, targetName);
		return targetName;
	}

	String suggestTargetName(String suggested) {
		return suggestTargetName(suggested, 1);
	}

	String suggestTargetName(String suggested, int counter) {

		String targetName = "%s_%s".formatted(suggested, counter);
		if (!variables.containsKey(targetName)) {
			return targetName;
		}
		return suggestTargetName(suggested, counter + 1);
	}
}
