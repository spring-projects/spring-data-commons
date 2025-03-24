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

import static org.assertj.core.api.Assertions.*;

import example.UserRepository;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.data.aot.CodeContributionAssert;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 */
class RepositoryContributorUnitTests {

	@Test
	void testCompile() {

		DummyModuleAotRepositoryContext aotContext = new DummyModuleAotRepositoryContext(UserRepository.class, null);
		RepositoryContributor repositoryContributor = new RepositoryContributor(aotContext) {

			@Override
			protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method,
					RepositoryInformation repositoryInformation) {

				return MethodContributor.forQueryMethod(new QueryMethod(method, repositoryInformation, getProjectionFactory()))
						.contribute(context -> {

							CodeBlock.Builder builder = CodeBlock.builder();
							if (!ClassUtils.isVoidType(method.getReturnType())) {
								builder.addStatement("return null");
							}

							return builder.build();
						});
			}
		};

		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);
		repositoryContributor.contribute(generationContext);
		generationContext.writeGeneratedContent();

		String expectedTypeName = "example.UserRepositoryImpl__Aot";

		TestCompiler.forSystem().with(generationContext).compile(compiled -> {
			assertThat(compiled.getAllCompiledClasses()).map(Class::getName).contains(expectedTypeName);
		});

		new CodeContributionAssert(generationContext).contributesReflectionFor(expectedTypeName);
	}

}
