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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 */
public class AotRepositoryMethodBuilder {

	private final AotRepositoryMethodGenerationContext context;

	private RepositoryMethodCustomizer customizer = (context, body) -> {};

	public AotRepositoryMethodBuilder(AotRepositoryMethodGenerationContext context) {

		this.context = context;
		initReturnType(context.getMethod(), context.getRepositoryInformation());
		initParameters(context.getMethod(), context.getRepositoryInformation());
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
		addParameter(ParameterSpec.builder(type, parameterName).build());
	}

	public void addParameter(ParameterSpec parameter) {
		this.context.addParameter(parameter);
	}

	public void setReturnType(@Nullable TypeName returnType, @Nullable TypeName actualReturnType) {
		this.context.getTargetMethodMetadata().setReturnType(returnType);
		this.context.getTargetMethodMetadata().setActualReturnType(actualReturnType);
	}

	public AotRepositoryMethodBuilder customize(RepositoryMethodCustomizer customizer) {
		this.customizer = customizer;
		return this;
	}

	MethodSpec buildMethod() {

		MethodSpec.Builder builder = MethodSpec.methodBuilder(context.getMethod().getName()).addModifiers(Modifier.PUBLIC);
		if (!context.returnsVoid()) {
			builder.returns(context.getReturnType());
		}
		builder.addJavadoc("AOT generated implementation of {@link $T#$L($L)}.", context.getMethod().getDeclaringClass(),
				context.getMethod().getName(),
				StringUtils.collectionToCommaDelimitedString(context.getTargetMethodMetadata().getMethodArguments().values().stream()
						.map(it -> it.type.toString()).collect(Collectors.toList())));
		context.getTargetMethodMetadata().getMethodArguments().forEach((name, spec) -> builder.addParameter(spec));
		customizer.customize(context, builder);
		return builder.build();
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
				index++;
			}

		}
	}

	private void initReturnType(Method method, RepositoryInformation repositoryInformation) {

		ResolvableType returnType = ResolvableType.forMethodReturnType(method,
				repositoryInformation.getRepositoryInterface());

		TypeName returnTypeName = TypeName.get(returnType.resolve());
		TypeName actualReturnTypeName = null;
		if (returnType.hasGenerics()) {
			Class<?>[] generics = returnType.resolveGenerics();
			returnTypeName = ParameterizedTypeName.get(returnType.resolve(), generics);

			if (generics.length == 1) {
				actualReturnTypeName = TypeName.get(generics[0]);
			}
		}

		setReturnType(returnTypeName, actualReturnTypeName);
	}

	public interface RepositoryMethodCustomizer {
		void customize(AotRepositoryMethodGenerationContext context, MethodSpec.Builder builder);
	}
}
