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

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedTypeReference;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryBuilder.AotBundle;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.TypeName;

/**
 * Contributor for AOT repository fragments.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
public class RepositoryContributor {

	private static final Log logger = LogFactory.getLog(RepositoryContributor.class);
	private static final String FEATURE_NAME = "AotRepository";

	private final AotRepositoryBuilder builder;
	private @Nullable TypeReference contributedTypeName;

	/**
	 * Create a new {@code RepositoryContributor} for the given {@link AotRepositoryContext}.
	 *
	 * @param repositoryContext
	 */
	public RepositoryContributor(AotRepositoryContext repositoryContext) {

		builder = AotRepositoryBuilder.forRepository(repositoryContext.getRepositoryInformation(),
				repositoryContext.getModuleName(), createProjectionFactory());
	}

	/**
	 * @return a new {@link ProjectionFactory} to be used with the AOT repository builder. The actual instance should be
	 *         accessed through {@link #getProjectionFactory()}.
	 */
	protected ProjectionFactory createProjectionFactory() {
		return new SpelAwareProxyProjectionFactory();
	}

	/**
	 * @return the used {@link ProjectionFactory}.
	 */
	protected ProjectionFactory getProjectionFactory() {
		return builder.getProjectionFactory();
	}

	/**
	 * @return the used {@link RepositoryInformation}.
	 */
	protected RepositoryInformation getRepositoryInformation() {
		return builder.getRepositoryInformation();
	}

	public @Nullable TypeReference getContributedTypeName() {
		return this.contributedTypeName;
	}

	public java.util.Map<String, TypeName> requiredArgs() {
		return builder.getAutowireFields();
	}

	public void contribute(GenerationContext generationContext) {

		builder.withClassCustomizer(this::customizeClass) //
				.withConstructorCustomizer(this::customizeConstructor) //
				.withQueryMethodContributor(this::contributeQueryMethod); //

		GeneratedClass generatedClass = generationContext.getGeneratedClasses().getOrAddForFeatureComponent(FEATURE_NAME,
				builder.getClassName(), targetTypeSpec -> {

					// capture the actual type name early on so that we can use it in the constructor.
					builder.withClassName(targetTypeSpec.build().name());

					AotBundle aotBundle = builder.build(targetTypeSpec);
					Class<?> repositoryInterface = getRepositoryInformation().getRepositoryInterface();
					String repositoryJsonFileName = getRepositoryJsonFileName(repositoryInterface);
					String repositoryJson;
					try {
						repositoryJson = aotBundle.metadata().toJson().toString(2);
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
								""".formatted(null, aotBundle.javaFile()));
					}

					generationContext.getGeneratedFiles().addResourceFile(repositoryJsonFileName, repositoryJson);
				});

		this.contributedTypeName = GeneratedTypeReference.of(generatedClass.getName());

		// generate native runtime hints - needed cause we're using the repository proxy
		generationContext.getRuntimeHints().reflection().registerType(this.contributedTypeName,
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
	}

	private static String getRepositoryJsonFileName(Class<?> repositoryInterface) {
		return repositoryInterface.getName().replace('.', '/') + ".json";
	}

	/**
	 * Customization hook for store implementations to customize class after building the entire class.
	 */
	protected void customizeClass(AotRepositoryClassBuilder builder) {

	}

	/**
	 * Customization hook for store implementations to customize the fragment constructor after building the constructor.
	 */
	protected void customizeConstructor(AotRepositoryConstructorBuilder builder) {

	}

	/**
	 * Customization hook for store implementations to contribute a query method.
	 */
	protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method) {
		return null;
	}

}
