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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.aot.generate.json.JSONException;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.StringUtils;

/**
 * Contributor for AOT repository fragments.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class RepositoryContributor {

	private static final Log logger = LogFactory.getLog(RepositoryContributor.class);

	private final AotRepositoryBuilder builder;

	public RepositoryContributor(AotRepositoryContext repositoryContext) {
		this.builder = AotRepositoryBuilder.forRepository(repositoryContext.getRepositoryInformation(),
				createProjectionFactory());
	}

	protected ProjectionFactory createProjectionFactory() {
		return new SpelAwareProxyProjectionFactory();
	}

	protected ProjectionFactory getProjectionFactory() {
		return builder.getProjectionFactory();
	}

	protected RepositoryInformation getRepositoryInformation() {
		return builder.getRepositoryInformation();
	}

	public String getContributedTypeName() {
		return builder.getGenerationMetadata().getTargetTypeName().toString();
	}

	public java.util.Map<String, TypeName> requiredArgs() {
		return builder.getAutowireFields();
	}

	public void contribute(GenerationContext generationContext) {

		// TODO: do we need - generationContext.withName("spring-data");

		builder.withClassCustomizer(this::customizeClass);
		builder.withConstructorCustomizer(this::customizeConstructor);
		builder.withQueryMethodContributor(this::contributeQueryMethod);

		AotRepositoryBuilder.AotBundle aotBundle = builder.build();

		Class<?> repositoryInterface = getRepositoryInformation().getRepositoryInterface();
		String repositoryJsonFileName = getRepositoryJsonFileName(repositoryInterface);

		JavaFile javaFile = aotBundle.javaFile();
		String typeName = "%s.%s".formatted(javaFile.packageName, javaFile.typeSpec.name);
		String repositoryJson;

		try {
			repositoryJson = aotBundle.metadata().toString(2);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("""
					------ AOT Repository.json: %s ------
					%s
					-------------------
					""".formatted(repositoryJsonFileName, repositoryJson));

			logger.trace("""
					------ AOT Generated Repository: %s ------
					%s
					-------------------
					""".formatted(typeName, javaFile));
		}

		// generate the files
		generationContext.getGeneratedFiles().addSourceFile(javaFile);
		generationContext.getGeneratedFiles().addResourceFile(repositoryJsonFileName, repositoryJson);

		// generate native runtime hints - needed cause we're using the repository proxy
		generationContext.getRuntimeHints().reflection().registerType(TypeReference.of(typeName),
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
	}

	private static String getRepositoryJsonFileName(Class<?> repositoryInterface) {

		String repositoryJsonName = repositoryInterface.getSimpleName() + ".json";
		String repositoryJsonPath = repositoryInterface.getPackageName().replace('.', '/');

		return StringUtils.hasText(repositoryJsonPath) ? repositoryJsonPath + "/" + repositoryJsonName : repositoryJsonName;
	}

	/**
	 * Customization hook for store implementations to customize class after building the entire class.
	 */
	protected void customizeClass(RepositoryInformation information, AotRepositoryFragmentMetadata metadata,
			TypeSpec.Builder builder) {

	}

	/**
	 * Customization hook for store implementations to customize the fragment constructor after building the constructor.
	 */
	protected void customizeConstructor(AotRepositoryConstructorBuilder constructorBuilder) {

	}

	/**
	 * Customization hook for store implementations to contribute a query method.
	 */
	protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method,
			RepositoryInformation repositoryInformation) {
		return null;
	}

}
