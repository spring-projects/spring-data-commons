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
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedTypeReference;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryCreator.AotBundle;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.ClassName;

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

	private final AotRepositoryCreator creator;
	private @Nullable TypeReference contributedTypeName;

	/**
	 * Create a new {@code RepositoryContributor} for the given {@link AotRepositoryContext}.
	 *
	 * @param repositoryContext
	 */
	public RepositoryContributor(AotRepositoryContext repositoryContext) {

		creator = AotRepositoryCreator.forRepository(repositoryContext.getRepositoryInformation(),
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
		return creator.getProjectionFactory();
	}

	/**
	 * @return the used {@link RepositoryInformation}.
	 */
	protected RepositoryInformation getRepositoryInformation() {
		return creator.getRepositoryInformation();
	}

	/**
	 * @return the {@link TypeReference} of the contributed type. Can be {@literal null} until
	 *         {@link #contribute(GenerationContext)} is called to obtain the actual {@link GeneratedClass} instance.
	 */
	public @Nullable TypeReference getContributedTypeName() {
		return this.contributedTypeName;
	}

	/**
	 * Get the required constructor arguments for the to be generated repository implementation. Types will be obtained by
	 * type from {@link org.springframework.beans.factory.BeanFactory} upon initialization of the generated fragment
	 * during application startup.
	 * <p>
	 * Can be overridden if required. Needs to match arguments of generated repository implementation.
	 * 
	 * @return key/value pairs of required argument required to instantiate the generated fragment.
	 */
	// TODO: should we switch from ResolvableType to some custom value object to cover qualifiers?
	public java.util.Map<String, ResolvableType> requiredArgs() {
		return Collections.unmodifiableMap(creator.getAutowireFields());
	}

	public final void contribute(GenerationContext generationContext) {

		// prepare and collect metadata
		creator.customizeClass(this::customizeClass) //
				.customizeConstructor(this::customizeConstructor) //
				.resolveQueryMethods(this::contributeQueryMethod); //

		// obtain the generated type and its target name.
		// Writing the source is triggered by DefaultGenerationContext#writeGeneratedContent() at a later stage
		GeneratedClass generatedClass = generationContext.getGeneratedClasses().getOrAddForFeatureComponent(FEATURE_NAME,
				ClassName.bestGuess(creator.repositoryImplementationTypeName()), targetTypeSpec -> {

					// write out the content
					AotBundle aotBundle = creator.create(targetTypeSpec);
					String repositoryJson;
					try {
						repositoryJson = aotBundle.metadata().get().toJson().toString(2);
					} catch (JSONException e) {
						throw new RuntimeException(e);

					}
					if (logger.isTraceEnabled()) {

						logger.trace("""
								------ AOT Repository.json: %s ------
								%s
								-------------------
								""".formatted(aotBundle.repositoryJsonFileName(), repositoryJson));

						logger.trace("""
								------ AOT Generated Repository: %s ------
								%s
								-------------------
								""".formatted(null, aotBundle.javaFile()));
					}

					generationContext.getGeneratedFiles().addResourceFile(aotBundle.repositoryJsonFileName(), repositoryJson);

					// generate native runtime hints - needed cause we're using the repository proxy
					generationContext.getRuntimeHints().reflection().registerType(aotBundle.generatedRepositoryTypeName(),
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
				});

		// make sure to capture the target file name
		this.contributedTypeName = GeneratedTypeReference.of(generatedClass.getName());
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
