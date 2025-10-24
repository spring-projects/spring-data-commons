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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.GeneratedTypeReference;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryCreator.AotBundle;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.StringUtils;

/**
 * Contributor for AOT repository fragments.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
public class RepositoryContributor {

	private static final Log logger = LogFactory.getLog(RepositoryContributor.class);
	private static final Log jsonLogger = LogFactory.getLog(RepositoryContributor.class.getName() + ".json");
	private static final String FEATURE_NAME = "AotRepository";

	private final AotRepositoryContext repositoryContext;
	private final AotRepositoryCreator creator;
	private @Nullable TypeReference contributedTypeName;

	/**
	 * Create a new {@code RepositoryContributor} for the given {@link AotRepositoryContext}.
	 *
	 * @param repositoryContext context providing details about the repository to be generated.
	 */
	public RepositoryContributor(AotRepositoryContext repositoryContext) {

		this.repositoryContext = repositoryContext;
		this.creator = AotRepositoryCreator.forRepository(repositoryContext.getRepositoryInformation(),
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
	@Nullable
	TypeReference getContributedTypeName() {
		return this.contributedTypeName;
	}

	/**
	 * @return the associated {@link AotRepositoryFragmentMetadata}.
	 */
	AotRepositoryFragmentMetadata getAotFragmentMetadata() {
		return creator.getRepositoryMetadata();
	}

	/**
	 * Contribute the AOT repository fragment to the given {@link GenerationContext}. This method will prepare the
	 * metadata, generate the source code and write it to the {@link GenerationContext}.
	 *
	 * @param generationContext must not be {@literal null}.
	 */
	public final void contribute(GenerationContext generationContext) {

		// prepare and collect metadata
		creator.customizeClass(this::customizeClass) //
				.customizeConstructor(this::customizeConstructor) //
				.contributeMethods(this::contributeQueryMethod); //

		if (logger.isDebugEnabled()) {
			logger.debug("Contributing %s AOT repository implementation for '%s'".formatted(repositoryContext.getModuleName(),
					repositoryContext.getRepositoryInformation().getRepositoryInterface().getName()));
		}

		// obtain the generated type and its target name.
		// Writing the source is triggered by DefaultGenerationContext#writeGeneratedContent() at a later stage
		GeneratedClass generatedClass = generationContext.getGeneratedClasses().getOrAddForFeatureComponent(FEATURE_NAME,
				creator.getClassName(), targetTypeSpec -> {

					// write out the content
					AotBundle aotBundle = creator.create(targetTypeSpec);

					String repositoryJson = repositoryContext.isGeneratedRepositoriesMetadataEnabled()
							? generateJsonMetadata(aotBundle)
							: null;

					if (logger.isTraceEnabled()) {

						TypeSpec typeSpec = targetTypeSpec.build();
						JavaFile javaFile = JavaFile.builder(creator.packageName(), typeSpec).build();

						logger.trace("""

								%s
								""".formatted(formatTraceMessage("Generated Repository", typeSpec.name(),
								prefixWithLineNumbers(javaFile.toString()).trim())));
					}

					if (jsonLogger.isTraceEnabled()) {

						if (StringUtils.hasText(repositoryJson)) {

							jsonLogger.trace("""

									%s
									""".formatted(
									formatTraceMessage("Repository.json", aotBundle.repositoryJsonFileName(), repositoryJson)));
						}
					}

					if (StringUtils.hasText(repositoryJson)) {
						generationContext.getGeneratedFiles().handleFile(Kind.RESOURCE, aotBundle.repositoryJsonFileName(),
								fileHandler -> {
									if (!fileHandler.exists()) {
										fileHandler.create(() -> new ByteArrayInputStream(repositoryJson.getBytes(StandardCharsets.UTF_8)));
									}
								});
					}
				});

		// generate native runtime hints

		// make sure to capture the target file name
		this.contributedTypeName = GeneratedTypeReference.of(generatedClass.getName());

		generationContext.getRuntimeHints().reflection().registerType(this.contributedTypeName,
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
	}

	/**
	 * Format a trace message with a title, label, and content using ascii art style borders.
	 *
	 * @param title title of the block (e.g. "Generated Source").
	 * @param label label that follows the title. Will be truncated if too long.
	 * @param content the actual content to be displayed.
	 * @return
	 */
	public static String formatTraceMessage(String title, String label, String content) {

		int remainingLength = 64 - title.length();
		String header = ("= %s: %-" + remainingLength + "." + remainingLength + "s =").formatted(title,
				formatMaxLength(label, remainingLength - 1));

		return """
				======================================================================
				%s
				======================================================================
				%s
				======================================================================
				""".formatted(header, content);
	}

	private static String formatMaxLength(String name, int length) {
		return name.length() > length ? "…" + name.substring(name.length() - length) : name;
	}

	/**
	 * Format the given contents by prefixing each line with its line number in a block comment.
	 *
	 * @param contents
	 * @return
	 */
	public static String prefixWithLineNumbers(String contents) {

		List<String> lines = contents.lines().toList();

		int decimals = (int) Math.log10(Math.abs(lines.size())) + 1;
		StringBuilder builder = new StringBuilder();

		int lineNumber = 1;
		for (String s : lines) {

			String formattedLineNumber = String.format("/* %-" + decimals + "d */\t", lineNumber);

			builder.append(formattedLineNumber).append(s).append(System.lineSeparator());

			lineNumber++;
		}

		return builder.toString();
	}

	private String generateJsonMetadata(AotBundle aotBundle) {

		String repositoryJson = "";

		if (repositoryContext.isGeneratedRepositoriesMetadataEnabled()) {
			try {
				repositoryJson = aotBundle.metadata().get().toJson().toString(2);
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		return repositoryJson;
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
