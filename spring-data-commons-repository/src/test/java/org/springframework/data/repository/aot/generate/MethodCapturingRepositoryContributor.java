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

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.assertj.core.api.MapAssert;
import org.jspecify.annotations.Nullable;

import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Christoph Strobl
 */
public class MethodCapturingRepositoryContributor extends RepositoryContributor {

	MultiValueMap<String, Method> capturedInvocations;

	public MethodCapturingRepositoryContributor(AotRepositoryContext repositoryContext) {
		super(repositoryContext);
		this.capturedInvocations = new LinkedMultiValueMap<>(3);
	}

	@Override
	protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method) {
		capturedInvocations.add(method.getName(), method);
		return null;
	}

	public void verifyContributionFor(String methodName) {
		assertThat(capturedInvocations).containsKey(methodName);
	}

	public MapAssert<String, List<Method>> verifyContributedMethods() {
		return assertThat(capturedInvocations);
	}
}
