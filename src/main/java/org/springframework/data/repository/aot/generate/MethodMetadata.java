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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.TypeName;

/**
 * Metadata about an AOT Repository method.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class MethodMetadata {

	private final Map<String, ParameterSpec> methodArguments = new LinkedHashMap<>();
	private final Map<String, String> localVariables = new LinkedHashMap<>();
	private final ResolvableType actualReturnType;
	private final ResolvableType returnType;

	MethodMetadata(RepositoryInformation repositoryInformation, Method method) {

		this.returnType = repositoryInformation.getReturnType(method).toResolvableType();
		this.actualReturnType = repositoryInformation.getReturnedDomainTypeInformation(method).toResolvableType();
		this.initParameters(repositoryInformation, method, new DefaultParameterNameDiscoverer());
	}

	private void initParameters(RepositoryInformation repositoryInformation, Method method,
			ParameterNameDiscoverer nameDiscoverer) {

		ResolvableType repositoryInterface = ResolvableType.forClass(repositoryInformation.getRepositoryInterface());

		for (java.lang.reflect.Parameter parameter : method.getParameters()) {

			MethodParameter methodParameter = MethodParameter.forParameter(parameter);
			methodParameter.initParameterNameDiscovery(nameDiscoverer);
			ResolvableType resolvableParameterType = ResolvableType.forMethodParameter(methodParameter, repositoryInterface);

			TypeName parameterType = TypeName.get(resolvableParameterType.getType());

			addParameter(ParameterSpec.builder(parameterType, methodParameter.getParameterName()).build());
		}
	}

	ResolvableType getReturnType() {
		return returnType;
	}

	ResolvableType getActualReturnType() {
		return actualReturnType;
	}

	void addParameter(ParameterSpec parameterSpec) {
		this.methodArguments.put(parameterSpec.name, parameterSpec);
	}

	Map<String, ParameterSpec> getMethodArguments() {
		return methodArguments;
	}

	@Nullable
	String getParameterName(int position) {

		if (0 > position) {
			return null;
		}

		List<Entry<String, ParameterSpec>> entries = new ArrayList<>(methodArguments.entrySet());
		if (position < entries.size()) {
			return entries.get(position).getKey();
		}
		return null;
	}

	Map<String, String> getLocalVariables() {
		return localVariables;
	}

}
