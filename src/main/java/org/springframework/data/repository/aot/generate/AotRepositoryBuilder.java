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
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.Generated;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata.ConstructorArgument;
import org.springframework.data.repository.aot.generate.json.JSONException;
import org.springframework.data.repository.aot.generate.json.JSONObject;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;

/**
 * Builder for AOT repository fragments.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class AotRepositoryBuilder {

	private final RepositoryInformation repositoryInformation;
	private final ProjectionFactory projectionFactory;
	private final AotRepositoryFragmentMetadata generationMetadata;

	private @Nullable Consumer<AotRepositoryConstructorBuilder> constructorCustomizer;
	private @Nullable BiFunction<Method, RepositoryInformation, MethodContributor<? extends QueryMethod>> methodContributorFunction;
	private ClassCustomizer customizer;

	private AotRepositoryBuilder(RepositoryInformation repositoryInformation, ProjectionFactory projectionFactory) {

		this.repositoryInformation = repositoryInformation;
		this.projectionFactory = projectionFactory;

		this.generationMetadata = new AotRepositoryFragmentMetadata(className());
		this.generationMetadata.addField(FieldSpec
				.builder(TypeName.get(Log.class), "logger", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
				.initializer("$T.getLog($T.class)", TypeName.get(LogFactory.class), this.generationMetadata.getTargetTypeName())
				.build());

		this.customizer = (info, metadata, builder) -> {};
	}

	public static <M extends QueryMethod> AotRepositoryBuilder forRepository(RepositoryInformation repositoryInformation,
			ProjectionFactory projectionFactory) {
		return new AotRepositoryBuilder(repositoryInformation, projectionFactory);
	}

	public AotRepositoryBuilder withConstructorCustomizer(
			Consumer<AotRepositoryConstructorBuilder> constructorCustomizer) {

		this.constructorCustomizer = constructorCustomizer;
		return this;
	}

	public AotRepositoryBuilder withQueryMethodContributor(
			BiFunction<Method, RepositoryInformation, MethodContributor<? extends QueryMethod>> methodContributorFunction) {
		this.methodContributorFunction = methodContributorFunction;
		return this;
	}

	public AotRepositoryBuilder withClassCustomizer(ClassCustomizer classCustomizer) {

		this.customizer = classCustomizer;
		return this;
	}

	public AotBundle build() {

		// start creating the type
		TypeSpec.Builder builder = TypeSpec.classBuilder(this.generationMetadata.getTargetTypeName()) //
				.addModifiers(Modifier.PUBLIC) //
				.addAnnotation(Generated.class) //
				.addJavadoc("AOT generated repository implementation for {@link $T}.\n",
						repositoryInformation.getRepositoryInterface());

		// create the constructor
		AotRepositoryConstructorBuilder constructorBuilder = new AotRepositoryConstructorBuilder(repositoryInformation,
				generationMetadata);
		if (constructorCustomizer != null) {
			constructorCustomizer.accept(constructorBuilder);
		}

		builder.addMethod(constructorBuilder.buildConstructor());

		List<AotRepositoryMethod> methodMetadata = new ArrayList<>();
		AotRepositoryMetadata.RepositoryType repositoryType = repositoryInformation.isReactiveRepository()
				? AotRepositoryMetadata.RepositoryType.REACTIVE
				: AotRepositoryMetadata.RepositoryType.IMPERATIVE;

		RepositoryComposition repositoryComposition = repositoryInformation.getRepositoryComposition();

		Arrays.stream(repositoryInformation.getRepositoryInterface().getMethods())
				.sorted(Comparator.<Method, String> comparing(it -> {
					return it.getDeclaringClass().getName();
				}).thenComparing(Method::getName).thenComparing(Method::getParameterCount).thenComparing(Method::toString))
				.forEach(method -> {
					contributeMethod(method, repositoryComposition, methodMetadata, builder);
				});

		// write fields at the end so we make sure to capture things added by methods
		generationMetadata.getFields().values().forEach(builder::addField);

		// finally customize the file itself
		this.customizer.customize(repositoryInformation, generationMetadata, builder);
		JavaFile javaFile = JavaFile.builder(packageName(), builder.build()).build();

		// TODO: module identifier
		AotRepositoryMetadata metadata = new AotRepositoryMetadata(repositoryInformation.getRepositoryInterface().getName(),
				"", repositoryType, methodMetadata);

		try {
			return new AotBundle(javaFile, metadata.toJson());
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}

	private void contributeMethod(Method method, RepositoryComposition repositoryComposition,
			List<AotRepositoryMethod> methodMetadata, TypeSpec.Builder builder) {

		if (repositoryInformation.isCustomMethod(method) || repositoryInformation.isBaseClassMethod(method)) {

			RepositoryFragment<?> fragment = repositoryComposition.findFragment(method);

			if (fragment != null) {
				methodMetadata.add(getFragmentMetadata(method, fragment));
			}
			return;
		}

		if (method.isBridge() || method.isDefault() || java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
			return;
		}

		if (repositoryInformation.isQueryMethod(method) && methodContributorFunction != null) {

			MethodContributor<? extends QueryMethod> contributor = methodContributorFunction.apply(method,
					repositoryInformation);

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
		String implementation = fragment.getImplementation().map(it -> it.getClass().getName()).orElse(null);

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
			autowireFields.put(entry.getKey(), entry.getValue().getTypeName());
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
	 * Customizer interface to customize the AOT repository fragment class after it has been defined.
	 */
	public interface ClassCustomizer {

		/**
		 * Apply customization ot the AOT repository fragment class after it has been defined..
		 *
		 * @param information
		 * @param metadata
		 * @param builder
		 */
		void customize(RepositoryInformation information, AotRepositoryFragmentMetadata metadata, TypeSpec.Builder builder);

	}

	record AotBundle(JavaFile javaFile, JSONObject metadata) {
	}

}
