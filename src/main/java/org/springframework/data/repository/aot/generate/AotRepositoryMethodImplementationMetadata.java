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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 */
class AotRepositoryMethodImplementationMetadata {

	private final Map<String, ParameterSpec> methodArguments;
	@Nullable private TypeName actualReturnType;
	@Nullable private TypeName returnType;

	public AotRepositoryMethodImplementationMetadata() {
		this.methodArguments = new LinkedHashMap<>();
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

	@Nullable
	public TypeName getReturnType() {
		return returnType;
	}

	@Nullable
	public TypeName getActualReturnType() {
		return actualReturnType;
	}

	public void addParameter(ParameterSpec parameterSpec) {
		this.methodArguments.put(parameterSpec.name, parameterSpec);
	}

	Map<String, ParameterSpec> getMethodArguments() {
		return methodArguments;
	}

	void setActualReturnType(@Nullable TypeName actualReturnType) {
		this.actualReturnType = actualReturnType;
	}

	void setReturnType(@Nullable TypeName returnType) {
		this.returnType = returnType;
	}
}
