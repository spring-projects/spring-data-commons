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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.aot.generate.AotRepositoryBuilder.GenerationMetadata;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;

/**
 * @author Christoph Strobl
 */
public class AotRepositoryDerivedMethodBuilder extends AotRepositoryMethodBuilder {

	public AotRepositoryDerivedMethodBuilder(Method method, RepositoryInformation repositoryInformation,
			GenerationMetadata metadata) {

		super(method, repositoryInformation, metadata);

		initReturnType(method, repositoryInformation);
		initParameters(method, repositoryInformation);
	}

	private void initParameters(Method method, RepositoryInformation repositoryInformation) {

		ResolvableType repositoryInterface = ResolvableType.forClass(repositoryInformation.getRepositoryInterface());
		if (method.getParameterCount() > 0) {
			int index = 0;
			for (Parameter parameter : method.getParameters()) {

				ResolvableType resolvableParameterType = ResolvableType.forMethodParameter(new MethodParameter(method, index),
						repositoryInterface);

				TypeName parameterType = TypeName.get(resolvableParameterType.resolve());
				if (resolvableParameterType.hasGenerics()) {
					parameterType = ParameterizedTypeName.get(resolvableParameterType.resolve(),
							resolvableParameterType.resolveGenerics());
				}
				addParameter(parameter.getName(), parameterType);
			}
		}
	}

	private void initReturnType(Method method, RepositoryInformation repositoryInformation) {

		ResolvableType returnType = ResolvableType.forMethodReturnType(method,
				repositoryInformation.getRepositoryInterface());

		TypeName returnTypeName = TypeName.get(returnType.resolve());
		if (returnType.hasGenerics()) {
			returnTypeName = ParameterizedTypeName.get(returnType.resolve(), returnType.resolveGenerics());
		}

		setReturnType(returnTypeName);
	}
}
