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
public class AotRepositoryFragmentMetadata {

	private final ClassName className;
	private final Map<String, FieldSpec> fields = new HashMap<>(3);
	private final Map<String, ConstructorArgument> constructorArguments = new LinkedHashMap<>(3);

	public AotRepositoryFragmentMetadata(ClassName className) {
		this.className = className;
	}

	/**
	 * Lookup a field name by exact type. Returns the first field that matches the type or {@literal null} if no field
	 * with that type was found.
	 *
	 * @param type
	 * @return
	 */
	@Nullable
	public String fieldNameOf(Class<?> type) {

		TypeName lookup = TypeName.get(type).withoutAnnotations();
		for (Entry<String, FieldSpec> field : fields.entrySet()) {
			if (field.getValue().type().withoutAnnotations().equals(lookup)) {
				return field.getKey();
			}
		}

		return null;
	}

	public ClassName getTargetTypeName() {
		return className;
	}

	/**
	 * Add a field to the repository fragment.
	 *
	 * @param fieldName name of the field to add. Must be unique.
	 * @param type field type.
	 * @param modifiers modifiers for the field, e.g. {@link Modifier#PRIVATE}, {@link Modifier#FINAL}, etc.
	 */
	public void addField(String fieldName, TypeName type, Modifier... modifiers) {
		fields.put(fieldName, FieldSpec.builder(type, fieldName, modifiers).build());
	}

	/**
	 * Add a field to the repository fragment.
	 *
	 * @param fieldSpec the field specification to add.
	 */
	public void addField(FieldSpec fieldSpec) {
		fields.put(fieldSpec.name(), fieldSpec);
	}

	/**
	 * Returns the fields of the repository fragment.
	 *
	 * @return the fields of the repository fragment.
	 */
	public Map<String, FieldSpec> getFields() {
		return fields;
	}

	/**
	 * Add a constructor argument to the repository fragment.
	 *
	 * @param parameterName name of the constructor parameter to add. Must be unique.
	 * @param type type of the constructor parameter.
	 * @param fieldName name of the field to bind the constructor parameter to, or {@literal null} if no field should be
	 *          created.
	 */
	public void addConstructorArgument(String parameterName, TypeName type, @Nullable String fieldName) {

		this.constructorArguments.put(parameterName, new ConstructorArgument(parameterName, type, fieldName));

		if (fieldName != null) {
			addField(parameterName, type, Modifier.PRIVATE, Modifier.FINAL);
		}
	}

	/**
	 * Returns the constructor arguments of the repository fragment.
	 *
	 * @return the constructor arguments of the repository fragment.
	 */
	public Map<String, ConstructorArgument> getConstructorArguments() {
		return constructorArguments;
	}

	/**
	 * Constructor argument metadata.
	 *
	 * @param parameterName
	 * @param typeName
	 * @param fieldName
	 */
	public record ConstructorArgument(String parameterName, TypeName typeName, @Nullable String fieldName) {

		@Deprecated(forRemoval = true)
		boolean isForLocalField() {
			return isBoundToField();
		}

		boolean isBoundToField() {
			return fieldName != null;
		}

	}
}
