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

import java.util.List;
import java.util.Map.Entry;

import javax.lang.model.element.Modifier;

import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;

/**
 * @author Christoph Strobl
 */
public class AotRepositoryConstructorBuilder {

	private final RepositoryInformation repositoryInformation;
	private final AotRepositoryImplementationMetadata metadata;

	private ConstructorCustomizer customizer = (info, builder) -> {};

	AotRepositoryConstructorBuilder(RepositoryInformation repositoryInformation,
			AotRepositoryImplementationMetadata metadata) {

		this.repositoryInformation = repositoryInformation;
		this.metadata = metadata;
	}

	public void addParameter(String parameterName, Class<?> type) {

		ResolvableType resolvableType = ResolvableType.forClass(type);
		if (!resolvableType.hasGenerics() || !resolvableType.hasResolvableGenerics()) {
			addParameter(parameterName, TypeName.get(type));
			return;
		}
		addParameter(parameterName, ParameterizedTypeName.get(type, resolvableType.resolveGenerics()));
	}

	public void addParameter(String parameterName, TypeName type) {

		this.metadata.addConstructorArgument(parameterName, type);
		this.metadata.addField(parameterName, type, Modifier.PRIVATE, Modifier.FINAL);
	}

	public void customize(ConstructorCustomizer customizer) {
		this.customizer = customizer;
	}

	MethodSpec buildConstructor() {

		MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
		for (Entry<String, TypeName> parameter : this.metadata.getConstructorArguments().entrySet()) {
			builder.addParameter(parameter.getValue(), parameter.getKey()).addStatement("this.$N = $N", parameter.getKey(),
					parameter.getKey());
		}
		customizer.customize(repositoryInformation, builder);
		return builder.build();
	}

	private static TypeName getDefaultStoreRepositoryImplementationType(RepositoryInformation repositoryInformation) {

		ResolvableType resolvableType = ResolvableType.forClass(repositoryInformation.getRepositoryBaseClass());
		if (resolvableType.hasGenerics()) {
			List<Class<?>> generics = List.of();
			if (resolvableType.getGenerics().length == 2) { // TODO: Find some other way to resolve generics
				generics = List.of(repositoryInformation.getDomainType(), repositoryInformation.getIdType());
			}
			return ParameterizedTypeName.get(repositoryInformation.getRepositoryBaseClass(), generics.toArray(Class[]::new));
		}
		return TypeName.get(repositoryInformation.getRepositoryBaseClass());
	}

	public interface ConstructorCustomizer {

		void customize(RepositoryInformation repositoryInformation, MethodSpec.Builder builder);
	}
}
