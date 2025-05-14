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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import example.UserRepository;
import example.UserRepository.User;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.ParameterizedTypeName;

/**
 * @author Christoph Strobl
 */
class AotRepositoryMethodBuilderUnitTests {

	RepositoryInformation repositoryInformation;
	AotQueryMethodGenerationContext methodGenerationContext;

	@BeforeEach
	void beforeEach() {
		repositoryInformation = Mockito.mock(RepositoryInformation.class);
		methodGenerationContext = Mockito.mock(AotQueryMethodGenerationContext.class);

		when(methodGenerationContext.getRepositoryInformation()).thenReturn(repositoryInformation);
	}

	@Test // GH-3279
	void generatesMethodSkeletonBasedOnGenerationMetadata() throws NoSuchMethodException {

		Method method = UserRepository.class.getMethod("findByFirstname", String.class);
		when(methodGenerationContext.getMethod()).thenReturn(method);
		when(methodGenerationContext.getReturnType()).thenReturn(ResolvableType.forClass(User.class));
		doReturn(TypeInformation.of(User.class)).when(repositoryInformation).getReturnType(any());
		doReturn(TypeInformation.of(User.class)).when(repositoryInformation).getReturnedDomainTypeInformation(any());
		MethodMetadata methodMetadata = new MethodMetadata(repositoryInformation, method);
		methodMetadata.addParameter(ParameterSpec.builder(String.class, "firstname").build());
		when(methodGenerationContext.getTargetMethodMetadata()).thenReturn(methodMetadata);

		AotRepositoryMethodBuilder builder = new AotRepositoryMethodBuilder(methodGenerationContext);
		assertThat(builder.buildMethod().toString()) //
				.containsPattern("public .*User findByFirstname\\(.*String firstname\\)");
	}

	@Test // GH-3279
	void generatesMethodWithGenerics() throws NoSuchMethodException {

		Method method = UserRepository.class.getMethod("findByFirstnameIn", List.class);
		when(methodGenerationContext.getMethod()).thenReturn(method);
		when(methodGenerationContext.getReturnType())
				.thenReturn(ResolvableType.forClassWithGenerics(List.class, User.class));
		doReturn(TypeInformation.of(User.class)).when(repositoryInformation).getReturnType(any());
		doReturn(TypeInformation.of(User.class)).when(repositoryInformation).getReturnedDomainTypeInformation(any());
		MethodMetadata methodMetadata = new MethodMetadata(repositoryInformation, method);
		methodMetadata
				.addParameter(ParameterSpec.builder(ParameterizedTypeName.get(List.class, String.class), "firstnames").build());
		when(methodGenerationContext.getTargetMethodMetadata()).thenReturn(methodMetadata);

		AotRepositoryMethodBuilder builder = new AotRepositoryMethodBuilder(methodGenerationContext);
		assertThat(builder.buildMethod().toString()) //
				.containsPattern("public .*List<.*User> findByFirstnameIn\\(") //
				.containsPattern(".*List<.*String> firstnames\\)");
	}
}
