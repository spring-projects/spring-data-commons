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
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.javapoet.CodeBlock;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Unit test to simulate the actual lifecycle and component interactions during AOT processing.
 *
 * @author Christoph Strobl
 */
class AotRepositoryConfigurationUnitTests {

	@Test
	void testAotCompilationLifecycle() {

		AotRepositoryContext repositoryContext = mock(AotRepositoryContext.class);
		when(repositoryContext.getModuleName()).thenReturn("Commons");
		RepositoryInformation repositoryInformation = mock(RepositoryInformation.class);

		when(repositoryContext.getRepositoryInformation()).thenReturn(repositoryInformation);
		when(repositoryInformation.getRepositoryInterface()).thenReturn((Class) example.UserRepository.class);
		when(repositoryInformation.getRepositoryComposition())
				.thenReturn(RepositoryComposition.of(RepositoryFragment.structural(example.UserRepository.class)));

		RepositoryContributor contributor = new RepositoryContributor(repositoryContext) {
			@Override
			protected void customizeConstructor(AotRepositoryConstructorBuilder builder) {
				builder.addParameter("txTemplate", TransactionTemplate.class);
			}
		};

		// contribute metadata but do not write files at this point
		TestGenerationContext generationContext = new TestGenerationContext(example.UserRepository.class);
		contributor.contribute(generationContext);

		// make sure we capture the contributed type name already
		TypeReference contributedTypeName = contributor.getContributedTypeName();
		assertThat(contributedTypeName).isNotNull();

		// required constructor arguments need to be present at this point
		Map<String, ResolvableType> requiredArgs = new LinkedHashMap<>(
				contributor.getAotFragmentMetadata().getAutowireFields());
		assertThat(requiredArgs).hasSize(1);

		// decorator kicks in and enhanced the BeanDefinition. No files written so far.
		AotRepositoryBeanDefinitionPropertiesDecorator decorator = new AotRepositoryBeanDefinitionPropertiesDecorator(
				() -> CodeBlock.builder().build(), contributor);
		CodeBlock beanDefinitionInitialization = decorator.decorate();

		assertThat(beanDefinitionInitialization.toString()).contains("TransactionTemplate txTemplate = ") //
				.containsSubsequence("new ", contributedTypeName.getSimpleName(), "(txTemplate)");
		assertThat(generationContext.getGeneratedFiles().getGeneratedFiles(Kind.SOURCE)).isEmpty();

		// when everything is in place, generated files are written
		generationContext.writeGeneratedContent();

		// make sure write operation for generated content did not change constructor nor type name
		assertThat(contributor.getContributedTypeName()).isEqualTo(contributedTypeName);
		assertThat(contributor.getAotFragmentMetadata().getAutowireFields()).containsExactlyEntriesOf(requiredArgs);

		// file is actually present now
		assertThat(generationContext.getGeneratedFiles().getGeneratedFiles(Kind.SOURCE))
				.containsKey("example/UserRepositoryImpl__AotRepository.java");
	}

}
