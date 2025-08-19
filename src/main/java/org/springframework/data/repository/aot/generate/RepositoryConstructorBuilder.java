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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.springframework.core.ResolvableType;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata.ConstructorArgument;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
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
	private final Set<String> parametersAdded = new HashSet<>();

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
		addParameter(parameterName, ResolvableType.forClass(type));
	}

	/**
	 * Add constructor parameter and create a field storing its value.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 */
	@Override
	public void addParameter(String parameterName, ResolvableType type) {
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
	public void addParameter(String parameterName, ResolvableType type, boolean createField) {
		this.parametersAdded.add(parameterName);
		this.metadata.addConstructorArgument(parameterName, type, createField ? parameterName : null);
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

		CodeBlock.Builder customCtorCode = CodeBlock.builder();
		customizer.customize(customCtorCode);
		if(!customCtorCode.isEmpty()) {
			builder.addCode(customCtorCode.build());
		}

		for (Entry<String, ConstructorArgument> parameter : this.metadata.getConstructorArguments().entrySet()) {
			if (parameter.getValue().isBoundToField()) {
				builder.addStatement("this.$N = $N", parameter.getKey(), parameter.getKey());
			}
		}

		return builder.build();
	}

	public void dispose() {

		for (String parameterName : this.parametersAdded) {
			ConstructorArgument removedArgument = this.metadata.getConstructorArguments().remove(parameterName);
			if (removedArgument != null && removedArgument.isBoundToField()) {
				this.metadata.getFields().remove(parameterName);
			}
		}
	}
}
