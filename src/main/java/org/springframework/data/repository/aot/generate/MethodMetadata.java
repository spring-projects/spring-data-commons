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

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.TypeName;

/**
 * Metadata about an AOT Repository method.
 *
 * @author Christoph Strobl
 */
class MethodMetadata {

	private final Map<String, ParameterSpec> methodArguments = new LinkedHashMap<>();
	private final ResolvableType actualReturnType;
	private final ResolvableType returnType;

	public MethodMetadata(RepositoryInformation repositoryInformation, Method method) {

		this.returnType = repositoryInformation.getReturnType(method).toResolvableType();
		this.actualReturnType = ResolvableType.forType(repositoryInformation.getReturnedDomainClass(method));
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

	public ResolvableType getReturnType() {
		return returnType;
	}

	public ResolvableType getActualReturnType() {
		return actualReturnType;
	}

	public void addParameter(ParameterSpec parameterSpec) {
		this.methodArguments.put(parameterSpec.name, parameterSpec);
	}

	public Map<String, ParameterSpec> getMethodArguments() {
		return methodArguments;
	}

}
