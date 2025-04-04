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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.Generated;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryInformation;
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

	private Consumer<AotRepositoryConstructorBuilder> constructorCustomizer;
	private BiFunction<Method, RepositoryInformation, MethodContributor<? extends QueryMethod>> methodContributorFunction;
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

	public JavaFile build() {

		// start creating the type
		TypeSpec.Builder builder = TypeSpec.classBuilder(this.generationMetadata.getTargetTypeName()) //
				.addModifiers(Modifier.PUBLIC) //
				.addAnnotation(Generated.class) //
				.addJavadoc("AOT generated repository implementation for {@link $T}.\n",
						repositoryInformation.getRepositoryInterface());

		// create the constructor
		AotRepositoryConstructorBuilder constructorBuilder = new AotRepositoryConstructorBuilder(repositoryInformation,
				generationMetadata);
		constructorCustomizer.accept(constructorBuilder);
		builder.addMethod(constructorBuilder.buildConstructor());

		Arrays.stream(repositoryInformation.getRepositoryInterface().getMethods())
				.sorted(Comparator.<Method, String> comparing(it -> {
					return it.getDeclaringClass().getName();
				}).thenComparing(Method::getName).thenComparing(Method::getParameterCount).thenComparing(Method::toString))
				.forEach(method -> {

					if (repositoryInformation.isCustomMethod(method)) {
						// TODO: fragment
						return;
					}

					if (repositoryInformation.isBaseClassMethod(method)) {
						// TODO: base
						return;
					}

					if (method.isBridge() || method.isDefault() || java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
						// TODO: report what we've skipped
						return;
					}

					if (repositoryInformation.isQueryMethod(method)) {

						MethodContributor<? extends QueryMethod> contributor = methodContributorFunction.apply(method,
								repositoryInformation);

						if (contributor != null) {

							AotQueryMethodGenerationContext context = new AotQueryMethodGenerationContext(repositoryInformation,
									method, contributor.getQueryMethod(), generationMetadata);

							builder.addMethod(contributor.contribute(context));
						}
					}
				});

		// write fields at the end so we make sure to capture things added by methods
		generationMetadata.getFields().values().forEach(builder::addField);

		// finally customize the file itself
		this.customizer.customize(repositoryInformation, generationMetadata, builder);
		return JavaFile.builder(packageName(), builder.build()).build();
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
		return generationMetadata.getConstructorArguments();
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
		void customize(RepositoryInformation information, AotRepositoryFragmentMetadata metadata,
				TypeSpec.Builder builder);
	}
}
