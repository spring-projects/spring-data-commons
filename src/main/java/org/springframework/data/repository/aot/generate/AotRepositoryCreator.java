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
import org.springframework.core.ResolvableType;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata.ConstructorArgument;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.Lazy;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;

/**
 * Builder style creator for AOT repository fragments.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class AotRepositoryCreator {

	private static final Log logger = LogFactory.getLog(AotRepositoryCreator.class);

	private final RepositoryInformation repositoryInformation;
	private final String moduleName;
	private final ProjectionFactory projectionFactory;
	private final AotRepositoryFragmentMetadata generationMetadata;

	private @Nullable RepositoryConstructorBuilder constructorBuilder;
	private Consumer<AotRepositoryClassBuilder> classCustomizer;

	private AotRepositoryCreator(RepositoryInformation repositoryInformation, String moduleName,
			ProjectionFactory projectionFactory) {

		this.repositoryInformation = repositoryInformation;
		this.moduleName = moduleName;
		this.projectionFactory = projectionFactory;

		this.generationMetadata = new AotRepositoryFragmentMetadata();
		this.classCustomizer = (builder) -> {};
	}

	/**
	 * Create a new {@code AotRepositoryBuilder} for the given {@link RepositoryInformation}.
	 *
	 * @param information must not be {@literal null}.
	 * @param moduleName must not be {@literal null}.
	 * @param projectionFactory must not be {@literal null}.
	 * @return
	 */
	static AotRepositoryCreator forRepository(RepositoryInformation information, String moduleName,
			ProjectionFactory projectionFactory) {
		return new AotRepositoryCreator(information, moduleName, projectionFactory);
	}

	/**
	 * Configure a {@link AotRepositoryConstructorBuilder} customizer.
	 *
	 * @param classCustomizer must not be {@literal null}.
	 * @return {@code this}.
	 */
	AotRepositoryCreator customizeClass(Consumer<AotRepositoryClassBuilder> classCustomizer) {
		this.classCustomizer = classCustomizer;
		return this;
	}

	/**
	 * Configure a {@link AotRepositoryConstructorBuilder} customizer.
	 *
	 * @param constructorCustomizer must not be {@literal null}.
	 * @return {@code this}.
	 */
	@SuppressWarnings("NullAway")
	AotRepositoryCreator customizeConstructor(Consumer<AotRepositoryConstructorBuilder> constructorCustomizer) {

		if (constructorBuilder != null) {
			constructorBuilder.dispose();
		}

		RepositoryConstructorBuilder constructorBuilder = new RepositoryConstructorBuilder(generationMetadata);
		constructorCustomizer.accept(constructorBuilder);
		this.constructorBuilder = constructorBuilder;
		return this;
	}

	AotRepositoryCreator resolveQueryMethods() {
		return resolveQueryMethods(new MethodContributorFactory() {
			@Override
			public @Nullable MethodContributor<? extends QueryMethod> create(Method method) {
				return null;
			}
		});
	}

	/**
	 * Configure a {@link MethodContributor} factory.
	 *
	 * @param methodContributorFactory must not be {@literal null}.
	 * @return {@code this}.
	 */
	AotRepositoryCreator resolveQueryMethods(@Nullable MethodContributorFactory methodContributorFactory) {

		Arrays.stream(repositoryInformation.getRepositoryInterface().getMethods())
				.sorted(Comparator.<Method, String> comparing(it -> {
					return it.getDeclaringClass().getName();
				}).thenComparing(Method::getName).thenComparing(Method::getParameterCount).thenComparing(Method::toString))
				.forEach(method -> {

					RepositoryComposition repositoryComposition = repositoryInformation.getRepositoryComposition();
					try {
						resolveQueryMethod(method, methodContributorFactory, repositoryComposition, generationMetadata);
					} catch (RuntimeException e) {
						if (logger.isErrorEnabled()) {
							logger.error("Failed to contribute Repository method [%s.%s]"
									.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName()), e);
						}
					}
				});
		return this;
	}

	AotBundle create() {
		return create(repositoryImplementationTypeName());
	}

	AotBundle create(String targetTypeName) {
		return create(TypeSpec.classBuilder(ClassName.bestGuess(targetTypeName)).addAnnotation(Generated.class));
	}

	AotBundle create(TypeSpec.Builder builder) {

		List<AotRepositoryMethod> methodMetadata = new ArrayList<>();

		builder.addModifiers(Modifier.PUBLIC) //
				.addJavadoc("AOT generated $L repository implementation for {@link $T}.\n", moduleName,
						repositoryInformation.getRepositoryInterface());

		// create the constructor
		builder.addMethod(buildConstructor());

		generationMetadata.getMethods().values().forEach(localMethod -> {

			MethodContributor<? extends QueryMethod> methodContributor = localMethod.methodContributor();
			AotQueryMethodGenerationContext context = new AotQueryMethodGenerationContext(repositoryInformation,
					localMethod.source(), methodContributor.getQueryMethod(), generationMetadata);

			MethodSpec methodSpec = methodContributor.contribute(context);
			if (methodSpec != null) {
				builder.addMethod(methodSpec);
			}

			// TODO: decouple json from method building and get rid of methodMetadata here?
			methodMetadata.add(new AotRepositoryMethod(localMethod.source().getName(), localMethod.source().toGenericString(),
					methodContributor.getMetadata(), null));
		});

		generationMetadata.getDelegateMethods().values().forEach(delegateMethod -> {

			String signature = delegateMethod.fragment() != null
					? delegateMethod.fragment().getSignatureContributor().getName()
					: delegateMethod.source().getDeclaringClass().getName();
			String implementation = delegateMethod.fragment() != null
					? delegateMethod.fragment().getImplementationClass().map(Class::getName).orElse(null)
					: null;
			QueryMetadata query = delegateMethod.methodContributor() != null
					? delegateMethod.methodContributor().getMetadata()
					: null;

			methodMetadata.add(new AotRepositoryMethod(delegateMethod.source().getName(), signature, query,
					new AotFragmentTarget(signature, implementation)));
		});

		// write fields at the end so we make sure to capture things added by methods
		generationMetadata.getFields().values().stream()
				.map(field -> FieldSpec.builder(field.fieldType().getType(), field.fieldName(), field.modifiers()).build())
				.forEach(builder::addField);

		// finally customize the file itself
		this.classCustomizer.accept(customizer -> {

			Assert.notNull(customizer, "ClassCustomizer must not be null");
			customizer.customize(builder);
		});

		return new AotBundle(repositoryInformation.getRepositoryInterface(),
				Lazy.of(() -> JavaFile.builder(packageName(), builder.build()).build()),
				Lazy.of(() -> getAotRepositoryMetadata(methodMetadata)));
	}

	String repositoryImplementationTypeName() {
		return "%s.%s".formatted(packageName(), typeName());
	}

	private MethodSpec buildConstructor() {
		return constructorBuilder != null ? constructorBuilder.buildConstructor()
				: MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
	}

	private AotRepositoryMetadata getAotRepositoryMetadata(List<AotRepositoryMethod> methodMetadata) {

		AotRepositoryMetadata.RepositoryType repositoryType = repositoryInformation.isReactiveRepository()
				? AotRepositoryMetadata.RepositoryType.REACTIVE
				: AotRepositoryMetadata.RepositoryType.IMPERATIVE;

		String jsonModuleName = moduleName != null ? moduleName.replaceAll("Reactive", "").trim() : null;

		return new AotRepositoryMetadata(repositoryInformation.getRepositoryInterface().getName(), jsonModuleName,
				repositoryType, methodMetadata);
	}

	private void resolveQueryMethod(Method method, @Nullable MethodContributorFactory contributorFactory,
			RepositoryComposition repositoryComposition, AotRepositoryFragmentMetadata metadata) {

		if (repositoryInformation.isCustomMethod(method)
				|| (repositoryInformation.isBaseClassMethod(method) && !repositoryInformation.isQueryMethod(method))) {

			RepositoryFragment<?> fragment = repositoryComposition.findFragment(method);

			if (fragment != null) {
				metadata.addDelegateMethod(method, fragment);
				return;
			}
		}

		if (method.isBridge() || method.isDefault() || java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
			return;
		}

		if (repositoryInformation.isQueryMethod(method) && contributorFactory != null) {

			MethodContributor<? extends QueryMethod> contributor = contributorFactory.create(method);

			if (contributor != null) {

				if (contributor.contributesMethodSpec() && !repositoryInformation.isReactiveRepository()) {
					metadata.addRepositoryMethod(method, contributor);
				} else {
					metadata.addDelegateMethod(method, contributor);
				}
			}
		}
	}

	public String packageName() {
		return repositoryInformation.getRepositoryInterface().getPackageName();
	}

	public String typeName() {
		return "%sImpl".formatted(repositoryInformation.getRepositoryInterface().getSimpleName());
	}

	public Map<String, ResolvableType> getAutowireFields() {

		Map<String, ResolvableType> autowireFields = new LinkedHashMap<>(
				generationMetadata.getConstructorArguments().size());
		for (Map.Entry<String, ConstructorArgument> entry : generationMetadata.getConstructorArguments().entrySet()) {
			autowireFields.put(entry.getKey(), entry.getValue().parameterType());
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

	record AotBundle(Class<?> sourceRepository, Lazy<JavaFile> javaFile, Lazy<AotRepositoryMetadata> metadata) {

		String repositoryJsonFileName() {
			return sourceRepository.getName().replace('.', '/') + ".json";
		}

		TypeReference generatedRepositoryTypeName() {
			JavaFile file = javaFile.get();
			return GeneratedTypeReference.of(ClassName.get(file.packageName(), file.typeSpec().name()));
		}

		String generatedCode() {
			return javaFile().get().toString();
		}

		String generatedMetadata() {
			return metadata().get().toJson().toString(2);
		}
	}

}
