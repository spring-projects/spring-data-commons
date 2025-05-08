/*
 * Copyright 2024 the original author or authors.
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

import java.util.Map.Entry;

import javax.lang.model.element.Modifier;

import org.springframework.core.ResolvableType;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata.ConstructorArgument;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.util.Assert;

/**
 * Builder for AOT Repository Constructors.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class RepositoryConstructorBuilder implements AotRepositoryConstructorBuilder {

	private final AotRepositoryFragmentMetadata metadata;

	private ConstructorCustomizer customizer = (builder) -> {};

	RepositoryConstructorBuilder(AotRepositoryFragmentMetadata metadata) {
		this.metadata = metadata;
	}

	/**
	 * Add constructor parameter and create a field storing its value.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 */
	@Override
	public void addParameter(String parameterName, Class<?> type) {

		ResolvableType resolvableType = ResolvableType.forClass(type);
		if (!resolvableType.hasGenerics() || !resolvableType.hasResolvableGenerics()) {
			addParameter(parameterName, TypeName.get(type));
			return;
		}

		addParameter(parameterName, ParameterizedTypeName.get(type, resolvableType.resolveGenerics()));
	}

	/**
	 * Add constructor parameter and create a field storing its value.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 */
	@Override
	public void addParameter(String parameterName, TypeName type) {
		addParameter(parameterName, type, true);
	}

	/**
	 * Add constructor parameter.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 * @param createField whether to create a field for the parameter and assign its value to the field.
	 */
	@Override
	public void addParameter(String parameterName, TypeName type, boolean createField) {

		this.metadata.addConstructorArgument(parameterName, type, createField ? parameterName : null);

		if (createField) {
			this.metadata.addField(parameterName, type, Modifier.PRIVATE, Modifier.FINAL);
		}
	}

	/**
	 * Add constructor customizer. Customizer is invoked after adding constructor arguments and before assigning
	 * constructor arguments to fields.
	 *
	 * @param customizer the customizer with direct access to the {@link MethodSpec.Builder constructor builder}.
	 */
	@Override
	public void customize(ConstructorCustomizer customizer) {

		Assert.notNull(customizer, "ConstructorCustomizer must not be null");
		this.customizer = customizer;
	}

	public MethodSpec buildConstructor() {

		MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

		for (Entry<String, ConstructorArgument> parameter : this.metadata.getConstructorArguments().entrySet()) {
			builder.addParameter(parameter.getValue().typeName(), parameter.getKey());
		}

		customizer.customize(builder);

		for (Entry<String, ConstructorArgument> parameter : this.metadata.getConstructorArguments().entrySet()) {
			if (parameter.getValue().isForLocalField()) {
				builder.addStatement("this.$N = $N", parameter.getKey(), parameter.getKey());
			}
		}

		return builder.build();
	}

}
