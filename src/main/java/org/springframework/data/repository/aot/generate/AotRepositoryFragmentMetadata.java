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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.Modifier;

import org.jspecify.annotations.Nullable;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.TypeName;

/**
 * Metadata for a repository fragment.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class AotRepositoryFragmentMetadata {

	private final ClassName className;
	private final Map<String, FieldSpec> fields = new HashMap<>(3);
	private final Map<String, ConstructorArgument> constructorArguments = new LinkedHashMap<>(3);

	public AotRepositoryFragmentMetadata(ClassName className) {
		this.className = className;
	}

	@Nullable
	public String fieldNameOf(Class<?> type) {

		TypeName lookup = TypeName.get(type).withoutAnnotations();
		for (Entry<String, FieldSpec> field : fields.entrySet()) {
			if (field.getValue().type.withoutAnnotations().equals(lookup)) {
				return field.getKey();
			}
		}

		return null;
	}

	public ClassName getTargetTypeName() {
		return className;
	}

	public void addField(String fieldName, TypeName type, Modifier... modifiers) {
		fields.put(fieldName, FieldSpec.builder(type, fieldName, modifiers).build());
	}

	public void addField(FieldSpec fieldSpec) {
		fields.put(fieldSpec.name, fieldSpec);
	}

	public Map<String, FieldSpec> getFields() {
		return fields;
	}

	public Map<String, ConstructorArgument> getConstructorArguments() {
		return constructorArguments;
	}

	public void addConstructorArgument(String parameterName, TypeName type, @Nullable String fieldName) {
		this.constructorArguments.put(parameterName, new ConstructorArgument(parameterName, type, fieldName));
	}

	public record ConstructorArgument(String parameterName, TypeName typeName, @Nullable String fieldName) {

		boolean isForLocalField() {
			return fieldName != null;
		}

	}
}
