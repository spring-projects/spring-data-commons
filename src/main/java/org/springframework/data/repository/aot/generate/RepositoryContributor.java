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

		RepositoryInformation repositoryInformation = repositoryContext.getRepositoryInformation();

		AotRepositoryBuilder builder = AotRepositoryBuilder.forRepository(repositoryInformation);
		builder.withFileCustomizer(this::customizeFile);
		builder.withConstructorCustomizer(this::customizeConstructor);
		builder.withDerivedMethodCustomizer(this::customizeDerivedMethod);

		JavaFile file = builder.javaFile();

		if (logger.isTraceEnabled()) {
			logger.trace("""
					------ AOT Generated Repository: %s.%s ------
					%s
					-------------------
					""".formatted(file.packageName, file.typeSpec.name, file));
		}


		// generate the file itself
		generationContext.getGeneratedFiles().addSourceFile(file);

		// register it in spring.factories
		String registration = "%s=%s.%s".formatted(repositoryInformation.getRepositoryInterface().getName(), file.packageName, file.typeSpec.name);
		generationContext.getGeneratedFiles().addResourceFile("META-INF/spring.factories", registration);
	}

	/**
	 * Customization Hook for Store implementations
	 */
	protected void customizeConstructor(AotRepositoryConstructorBuilder constructorBuilder) {

	}

	protected void customizeFile(RepositoryInformation information, AotRepositoryBuilder.GenerationMetadata metadata,
			TypeSpec.Builder builder) {

	}

	protected void customizeDerivedMethod(AotRepositoryMethodBuilder methodBuilder) {

	}
}
