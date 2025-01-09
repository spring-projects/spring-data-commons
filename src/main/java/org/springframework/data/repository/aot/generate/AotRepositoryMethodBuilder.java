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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.FieldSpec;
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

	private final Method method;
	private final RepositoryInformation repositoryInformation;
	private final MethodGenerationMetadata metadata;

	private RepositoryMethodCustomizer customizer = (info, md, builder) -> {};

	public AotRepositoryMethodBuilder(Method method, RepositoryInformation repositoryInformation,
			AotRepositoryBuilder.GenerationMetadata metadata) {

		this.method = method;
		this.repositoryInformation = repositoryInformation;
		this.metadata = new MethodGenerationMetadata(metadata, method);
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
		this.metadata.methodArguments.put(parameter.name, parameter);
	}

	public void setReturnType(@Nullable TypeName returnType, @Nullable TypeName actualReturnType) {
		this.metadata.returnType = returnType;
		this.metadata.actualReturnType = actualReturnType;
	}

	public void customize(RepositoryMethodCustomizer customizer) {
		this.customizer = customizer;
	}

	MethodSpec buildMethod() {

		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getName()).addModifiers(Modifier.PUBLIC);
		if (!metadata.returnsVoid()) {
			builder.returns(metadata.getReturnType());
		}
		builder.addJavadoc("AOT generated implementation of {@link $T#$L($L)}.", method.getDeclaringClass(),
				method.getName(), StringUtils.collectionToCommaDelimitedString(
						metadata.methodArguments.values().stream().map(it -> it.type.toString()).collect(Collectors.toList())));
		metadata.methodArguments.forEach((name, spec) -> builder.addParameter(spec));
		customizer.customize(repositoryInformation, metadata, builder);
		return builder.build();
	}

	public interface RepositoryMethodCustomizer {

		void customize(RepositoryInformation repositoryInformation, MethodGenerationMetadata metadata,
				MethodSpec.Builder builder);
	}

	public static class MethodGenerationMetadata {

		private final AotRepositoryBuilder.GenerationMetadata generationMetadata;
		private final Method repositoryMethod;
		private final Map<String, ParameterSpec> methodArguments;
		@Nullable public TypeName actualReturnType;
		@Nullable private TypeName returnType;

		public MethodGenerationMetadata(AotRepositoryBuilder.GenerationMetadata generationMetadata,
				Method repositoryMethod) {
			this.generationMetadata = generationMetadata;
			this.repositoryMethod = repositoryMethod;
			this.methodArguments = new LinkedHashMap<>();
		}

		public Method getRepositoryMethod() {
			return repositoryMethod;
		}

		@Nullable
		public String getParameterNameOf(Class<?> type) {
			for (Entry<String, ParameterSpec> entry : methodArguments.entrySet()) {
				if (entry.getValue().type.equals(TypeName.get(type))) {
					return entry.getKey();
				}
			}
			return null;
		}

		public boolean returnsVoid() {
			return repositoryMethod.getReturnType().equals(Void.TYPE);
		}

		@Nullable
		public TypeName getReturnType() {
			return returnType;
		}

		@Nullable
		public TypeName getActualReturnType() {
			return actualReturnType;
		}

		@Nullable
		public String getSortParameterName() {
			return getParameterNameOf(Sort.class);
		}

		@Nullable
		public String getPageableParameterName() {
			return getParameterNameOf(Pageable.class);
		}

		@Nullable
		public String getLimitParameterName() {
			return getParameterNameOf(Limit.class);
		}

		public void addParameter(ParameterSpec parameterSpec) {
			this.methodArguments.put(parameterSpec.name, parameterSpec);
		}

		@Nullable
		public String fieldNameOf(Class<?> type) {
			return generationMetadata.fieldNameOf(type);
		}

		public boolean hasField(String fieldName) {
			return generationMetadata.hasField(fieldName);
		}

		public void addField(String fieldName, TypeName type, Modifier... modifiers) {
			generationMetadata.addField(fieldName, type, modifiers);
		}

		public void addField(FieldSpec fieldSpec) {
			generationMetadata.addField(fieldSpec);
		}

		public Map<String, FieldSpec> getFields() {
			return generationMetadata.getFields();
		}
	}
}
