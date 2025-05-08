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

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.Generated;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata.ConstructorArgument;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.FieldSpec;
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

	private AotRepositoryBuilder(RepositoryInformation repositoryInformation, String moduleName,
			ProjectionFactory projectionFactory) {

		this.repositoryInformation = repositoryInformation;
		this.moduleName = moduleName;
		this.projectionFactory = projectionFactory;

		this.generationMetadata = new AotRepositoryFragmentMetadata(className());
		this.generationMetadata.addField(FieldSpec
				.builder(TypeName.get(Log.class), "logger", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
				.initializer("$T.getLog($T.class)", TypeName.get(LogFactory.class), this.generationMetadata.getTargetTypeName())
				.build());

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

	public AotBundle build() {

		List<AotRepositoryMethod> methodMetadata = new ArrayList<>();
		RepositoryComposition repositoryComposition = repositoryInformation.getRepositoryComposition();

		// start creating the type
		TypeSpec.Builder builder = TypeSpec.classBuilder(this.generationMetadata.getTargetTypeName()) //
				.addModifiers(Modifier.PUBLIC) //
				.addAnnotation(Generated.class) //
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

	private MethodSpec buildConstructor() {

		RepositoryConstructorBuilder constructorBuilder = new RepositoryConstructorBuilder(
				generationMetadata);

		if (constructorCustomizer != null) {
			constructorCustomizer.accept(constructorBuilder);
		}

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

	private ClassName className() {
		return new ClassNameGenerator(ClassName.get(packageName(), typeName())).generateClassName("Aot", null);
	}

	private String packageName() {
		return repositoryInformation.getRepositoryInterface().getPackageName();
	}

	private String typeName() {
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
