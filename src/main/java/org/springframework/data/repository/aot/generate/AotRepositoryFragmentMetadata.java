/*
 * Copyright 2025-present the original author or authors.
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.data.javapoet.TypeNames;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;

/**
 * Metadata for a repository fragment.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class AotRepositoryFragmentMetadata {

	private final Map<String, LocalField> fields = new HashMap<>(3);
	private final Map<String, ConstructorArgument> constructorArguments = new LinkedHashMap<>(3);
	private final Map<String, LocalMethod> methods = new LinkedHashMap<>();
	private final Map<String, DelegateMethod> delegateMethods = new LinkedHashMap<>();

	/**
	 * Lookup a field name by exact type. Returns the first field that matches the type or {@literal null} if no field
	 * with that type was found.
	 *
	 * @param type
	 * @return
	 */
	@Nullable
	public String fieldNameOf(Class<?> type) {

		ResolvableType lookup = ResolvableType.forClass(type);
		for (Entry<String, LocalField> field : fields.entrySet()) {
			if (field.getValue().fieldType().getType().equals(lookup.getType())) {
				return field.getKey();
			}
		}

		return null;
	}

	/**
	 * Add a field to the repository fragment.
	 *
	 * @param fieldName name of the field to add. Must be unique.
	 * @param type field type.
	 * @param modifiers modifiers for the field, e.g. {@link Modifier#PRIVATE}, {@link Modifier#FINAL}, etc.
	 */
	public void addField(String fieldName, ResolvableType type, Modifier... modifiers) {
		fields.putIfAbsent(fieldName, new LocalField(fieldName, type, modifiers));
	}

	/**
	 * Returns the fields of the repository fragment.
	 *
	 * @return the fields of the repository fragment.
	 */
	public Map<String, LocalField> getFields() {
		return fields;
	}

	/**
	 * Add a constructor argument to the repository fragment.
	 *
	 * @param parameterName name of the constructor parameter to add. Must be unique.
	 * @param type type of the constructor parameter.
	 * @param argumentSupplier supplier to create the constructor argument.
	 */
	public void addConstructorArgument(String parameterName, ResolvableType type,
			Supplier<ConstructorArgument> argumentSupplier) {

		this.constructorArguments.computeIfAbsent(parameterName, it -> {

			ConstructorArgument constructorArgument = argumentSupplier.get();

			if (constructorArgument.isBoundToField()) {
				addField(parameterName, type, Modifier.PRIVATE, Modifier.FINAL);
			}

			return constructorArgument;
		});
	}

	public void addRepositoryMethod(Method source, MethodContributor<? extends QueryMethod> methodContributor) {
		this.methods.putIfAbsent(source.toGenericString(), new LocalMethod(source, methodContributor));
	}

	public void addDelegateMethod(Method source, MethodContributor<? extends QueryMethod> methodContributor) {
		this.delegateMethods.putIfAbsent(source.toGenericString(), new DelegateMethod(source, null, methodContributor));
	}

	public void addDelegateMethod(Method source, RepositoryFragment<?> fragment) {
		this.delegateMethods.putIfAbsent(source.toGenericString(), new DelegateMethod(source, fragment, null));
	}

	/**
	 * Returns the constructor arguments of the repository fragment.
	 *
	 * @return the constructor arguments of the repository fragment.
	 */
	public Map<String, ConstructorArgument> getConstructorArguments() {
		return constructorArguments;
	}

	Map<String, ResolvableType> getAutowireFields() {

		Map<String, ResolvableType> autowireFields = new LinkedHashMap<>(getConstructorArguments().size());
		for (Map.Entry<String, ConstructorArgument> entry : getConstructorArguments().entrySet()) {
			autowireFields.put(entry.getKey(), entry.getValue().parameterType());
		}
		return autowireFields;
	}

	public Map<String, LocalMethod> getMethods() {
		return methods;
	}

	public Map<String, DelegateMethod> getDelegateMethods() {
		return delegateMethods;
	}

	static TypeName typeNameOf(ResolvableType type) {
        return TypeNames.resolvedTypeName(type);
    }

    public record ConstructorArgument(String parameterName, ResolvableType parameterType, boolean bindToField,
			AotRepositoryConstructorBuilder.ParameterOrigin parameterOrigin) {

		boolean isBoundToField() {
			return bindToField;
		}

		TypeName typeName() {
			return typeNameOf(parameterType());
		}

	}

	public record LocalField(String fieldName, ResolvableType fieldType, Modifier... modifiers) {

		TypeName typeName() {
			return typeNameOf(fieldType());
		}
	}

	public record LocalMethod(Method source, MethodContributor<? extends QueryMethod> methodContributor) {

	}

	public record DelegateMethod(Method source, @Nullable RepositoryFragment<?> fragment,
			@Nullable MethodContributor<? extends QueryMethod> methodContributor) {

	}

}
