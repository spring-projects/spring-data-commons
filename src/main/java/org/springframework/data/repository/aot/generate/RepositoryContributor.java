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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.repository.aot.generate.AotRepositoryBuilder.TargetAotRepositoryImplementationMetadata;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;

/**
 * @author Christoph Strobl
 */
public class RepositoryContributor implements AotCodeContributor {

	private static final Log logger = LogFactory.getLog(RepositoryContributor.class);

	private AotRepositoryContext repositoryContext;

	public RepositoryContributor(AotRepositoryContext repositoryContext) {
		this.repositoryContext = repositoryContext;
	}

	@Override
	public void contribute(GenerationContext generationContext) {

		//TODO: do we need - generationContext.withName("spring-data");
		RepositoryInformation repositoryInformation = repositoryContext.getRepositoryInformation();

		AotRepositoryBuilder builder = AotRepositoryBuilder.forRepository(repositoryInformation);
		builder.withFileCustomizer(this::customizeFile);
		builder.withConstructorCustomizer(this::customizeConstructor);
		builder.withDerivedMethodFunction(this::contributeRepositoryMethod);

		JavaFile file = builder.javaFile();
		String typeName = "%s.%s".formatted(file.packageName, file.typeSpec.name);

		if (logger.isTraceEnabled()) {
			logger.trace("""
					------ AOT Generated Repository: %s ------
					%s
					-------------------
					""".formatted(typeName, file));
		}

		// generate the file itself
		generationContext.getGeneratedFiles().addSourceFile(file);

		// generate native runtime hints - needed cause we're using the repository proxy
		generationContext.getRuntimeHints().reflection().registerType(TypeReference.of(typeName), MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);

		// register it in spring.factories
		String registration = "%s=%s".formatted(repositoryInformation.getRepositoryInterface().getName(), typeName);
		generationContext.getGeneratedFiles().addResourceFile("META-INF/spring.factories", registration);
	}

	/**
	 * Customization Hook for Store implementations
	 */
	protected void customizeConstructor(AotRepositoryConstructorBuilder constructorBuilder) {

	}

	protected void customizeFile(RepositoryInformation information, TargetAotRepositoryImplementationMetadata metadata,
			TypeSpec.Builder builder) {

	}

	protected AotRepositoryMethodBuilder contributeRepositoryMethod(AotRepositoryMethodGenerationContext context) {
		return null;
	}
}
