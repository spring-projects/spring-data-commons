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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.generate.Generated;
import org.springframework.aot.generate.GeneratedTypeReference;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata.ConstructorArgument;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;

/**
 * Builder for AOT repository fragments.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class AotRepositoryBuilder {

	private static final Log logger = LogFactory.getLog(AotRepositoryBuilder.class);

	private final RepositoryInformation repositoryInformation;
	private final String moduleName;
	private final ProjectionFactory projectionFactory;
	private final AotRepositoryFragmentMetadata generationMetadata;

	private @Nullable Consumer<AotRepositoryConstructorBuilder> constructorCustomizer;
	private @Nullable MethodContributorFactory methodContributorFactory;
	private Consumer<AotRepositoryClassBuilder> classCustomizer;
	private @Nullable TypeReference targetClassName;
	private RepositoryConstructorBuilder constructorBuilder;

	private AotRepositoryBuilder(RepositoryInformation repositoryInformation, String moduleName,
			ProjectionFactory projectionFactory) {

		this.repositoryInformation = repositoryInformation;
		this.moduleName = moduleName;
		this.projectionFactory = projectionFactory;

		this.generationMetadata = new AotRepositoryFragmentMetadata();
		this.classCustomizer = (builder) -> {};
		this.constructorBuilder = new RepositoryConstructorBuilder(generationMetadata);
	}

	/**
	 * Create a new {@code AotRepositoryBuilder} for the given {@link RepositoryInformation}.
	 *
	 * @param information must not be {@literal null}.
	 * @param moduleName must not be {@literal null}.
	 * @param projectionFactory must not be {@literal null}.
	 * @return
	 */
	public static AotRepositoryBuilder forRepository(RepositoryInformation information, String moduleName,
			ProjectionFactory projectionFactory) {
		return new AotRepositoryBuilder(information, moduleName, projectionFactory);
	}

	/**
	 * Configure a {@link AotRepositoryConstructorBuilder} customizer.
	 *
	 * @param classCustomizer must not be {@literal null}.
	 * @return {@code this}.
	 */
	public AotRepositoryBuilder withClassCustomizer(Consumer<AotRepositoryClassBuilder> classCustomizer) {

		this.classCustomizer = classCustomizer;
		return this;
	}

	/**
	 * Configure a {@link AotRepositoryConstructorBuilder} customizer.
	 *
	 * @param constructorCustomizer must not be {@literal null}.
	 * @return {@code this}.
	 */
	public AotRepositoryBuilder withConstructorCustomizer(
			Consumer<AotRepositoryConstructorBuilder> constructorCustomizer) {

		this.constructorCustomizer = constructorCustomizer;
		return this;
	}

	/**
	 * Configure a {@link MethodContributor} factory.
	 *
	 * @param methodContributorFactory must not be {@literal null}.
	 * @return {@code this}.
	 */
	public AotRepositoryBuilder withQueryMethodContributor(MethodContributorFactory methodContributorFactory) {

		this.methodContributorFactory = methodContributorFactory;
		return this;
	}

	public AotRepositoryBuilder prepare(@Nullable ClassName targetClassName) {
		if (targetClassName == null) {
			withTargetClassName(null);
		} else {
			withTargetClassName(GeneratedTypeReference.of(targetClassName));
		}
		if (constructorCustomizer != null) {
			constructorCustomizer.accept(constructorBuilder);
		}
		return this;
	}

	public AotBundle build(TypeSpec.Builder builder) {

		List<AotRepositoryMethod> methodMetadata = new ArrayList<>();
		RepositoryComposition repositoryComposition = repositoryInformation.getRepositoryComposition();

		builder.addModifiers(Modifier.PUBLIC) //
				.addJavadoc("AOT generated $L repository implementation for {@link $T}.\n", moduleName,
						repositoryInformation.getRepositoryInterface());

		// create the constructor
		builder.addMethod(buildConstructor());

		Arrays.stream(repositoryInformation.getRepositoryInterface().getMethods())
				.sorted(Comparator.<Method, String> comparing(it -> {
					return it.getDeclaringClass().getName();
				}).thenComparing(Method::getName).thenComparing(Method::getParameterCount).thenComparing(Method::toString))
				.forEach(method -> {
					try {
						contributeMethod(method, repositoryComposition, methodMetadata, builder);
					} catch (RuntimeException e) {
						if (logger.isErrorEnabled()) {
							logger.error("Failed to contribute Repository method [%s.%s]"
									.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName()), e);
						}
					}
				});

		// write fields at the end so we make sure to capture things added by methods
		generationMetadata.getFields().values().forEach(builder::addField);

		// finally customize the file itself
		this.classCustomizer.accept(customizer -> {

			Assert.notNull(customizer, "ClassCustomizer must not be null");
			customizer.customize(builder);
		});

		JavaFile javaFile = JavaFile.builder(packageName(), builder.build()).build();
		AotRepositoryMetadata metadata = getAotRepositoryMetadata(methodMetadata);

		return new AotBundle(javaFile, metadata);
	}

	public AotBundle build() {

		ClassName className = ClassName
				.bestGuess((targetClassName != null ? targetClassName : intendedTargetClassName()).getCanonicalName());
		return build(TypeSpec.classBuilder(className).addAnnotation(Generated.class));
	}

	public TypeReference intendedTargetClassName() {
		return TypeReference.of("%s.%s".formatted(packageName(), typeName()));
	}

	public @Nullable TypeReference actualTargetClassName() {

		if (targetClassName == null) {
			return null;
		}
		return targetClassName;
	}

	AotRepositoryBuilder withTargetClassName(@Nullable TypeReference targetClassName) {
		this.targetClassName = targetClassName;
		return this;
	}

	private MethodSpec buildConstructor() {
		return constructorBuilder.buildConstructor();
	}

	private AotRepositoryMetadata getAotRepositoryMetadata(List<AotRepositoryMethod> methodMetadata) {

		AotRepositoryMetadata.RepositoryType repositoryType = repositoryInformation.isReactiveRepository()
				? AotRepositoryMetadata.RepositoryType.REACTIVE
				: AotRepositoryMetadata.RepositoryType.IMPERATIVE;

		String jsonModuleName = moduleName != null ? moduleName.replaceAll("Reactive", "").trim() : null;

		return new AotRepositoryMetadata(repositoryInformation.getRepositoryInterface().getName(), jsonModuleName,
				repositoryType, methodMetadata);
	}

	private void contributeMethod(Method method, RepositoryComposition repositoryComposition,
			List<AotRepositoryMethod> methodMetadata, TypeSpec.Builder builder) {

		if (repositoryInformation.isCustomMethod(method)
				|| (repositoryInformation.isBaseClassMethod(method) && !repositoryInformation.isQueryMethod(method))) {

			RepositoryFragment<?> fragment = repositoryComposition.findFragment(method);

			if (fragment != null) {
				methodMetadata.add(getFragmentMetadata(method, fragment));
				return;
			}
		}

		if (method.isBridge() || method.isDefault() || java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
			return;
		}

		if (repositoryInformation.isQueryMethod(method) && methodContributorFactory != null) {

			MethodContributor<? extends QueryMethod> contributor = methodContributorFactory.create(method);

			if (contributor != null) {

				if (contributor.contributesMethodSpec() && !repositoryInformation.isReactiveRepository()) {

					AotQueryMethodGenerationContext context = new AotQueryMethodGenerationContext(repositoryInformation, method,
							contributor.getQueryMethod(), generationMetadata);

					builder.addMethod(contributor.contribute(context));
				}

				methodMetadata
						.add(new AotRepositoryMethod(method.getName(), method.toGenericString(), contributor.getMetadata(), null));
			}
		}
	}

	private AotRepositoryMethod getFragmentMetadata(Method method, RepositoryFragment<?> fragment) {

		String signature = fragment.getSignatureContributor().getName();
		String implementation = fragment.getImplementationClass().map(Class::getName).orElse(null);
		AotFragmentTarget fragmentTarget = new AotFragmentTarget(signature, implementation);

		return new AotRepositoryMethod(method.getName(), method.toGenericString(), null, fragmentTarget);
	}

	public AotRepositoryFragmentMetadata getGenerationMetadata() {
		return generationMetadata;
	}

	public String packageName() {
		return repositoryInformation.getRepositoryInterface().getPackageName();
	}

	public String typeName() {
		return "%sImpl".formatted(repositoryInformation.getRepositoryInterface().getSimpleName());
	}

	public Map<String, TypeName> getAutowireFields() {
		Map<String, TypeName> autowireFields = new LinkedHashMap<>(generationMetadata.getConstructorArguments().size());
		for (Map.Entry<String, ConstructorArgument> entry : generationMetadata.getConstructorArguments().entrySet()) {
			autowireFields.put(entry.getKey(), entry.getValue().typeName());
		}
		return autowireFields;
	}

	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	public ProjectionFactory getProjectionFactory() {
		return projectionFactory;
	}

	/**
	 * Customizer interface to customize the AOT repository fragment constructor through
	 * {@link AotRepositoryConstructorBuilder}.
	 */
	public interface ConstructorCustomizer {

		/**
		 * Apply customization ot the AOT repository fragment constructor.
		 *
		 * @param constructorBuilder the builder to be customized.
		 */
		void customize(AotRepositoryConstructorBuilder constructorBuilder);

	}

	/**
	 * Factory interface to conditionally create {@link MethodContributor} instances. An implementation may decide whether
	 * to return a {@link MethodContributor} or {@literal null}, if no method (code or metadata) should be contributed.
	 */
	public interface MethodContributorFactory {

		/**
		 * Apply customization ot the AOT repository fragment constructor.
		 *
		 * @param method the method to be contributed.
		 * @return the {@link MethodContributor} to be used. Can be {@literal null} if the method and method metadata should
		 *         not be contributed.
		 */
		@Nullable
		MethodContributor<? extends QueryMethod> create(Method method);

	}

	record AotBundle(JavaFile javaFile, AotRepositoryMetadata metadata) {
	}

}
