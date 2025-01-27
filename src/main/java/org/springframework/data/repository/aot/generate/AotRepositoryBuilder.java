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

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.Generated;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * @author Christoph Strobl
 */
public class AotRepositoryBuilder {

	private final RepositoryInformation repositoryInformation;
	private final AotRepositoryImplementationMetadata generationMetadata;

	private Consumer<AotRepositoryConstructorBuilder> constructorBuilderCustomizer;
	private Function<AotRepositoryMethodGenerationContext, AotRepositoryMethodBuilder> methodContextFunction;
	private RepositoryCustomizer customizer;

	public static AotRepositoryBuilder forRepository(RepositoryInformation repositoryInformation) {
		return new AotRepositoryBuilder(repositoryInformation);
	}

	AotRepositoryBuilder(RepositoryInformation repositoryInformation) {

		this.repositoryInformation = repositoryInformation;
		this.generationMetadata = new AotRepositoryImplementationMetadata(className());
		this.generationMetadata.addField(FieldSpec
				.builder(TypeName.get(Log.class), "logger", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
				.initializer("$T.getLog($T.class)", TypeName.get(LogFactory.class), this.generationMetadata.getTargetTypeName())
				.build());

		this.customizer = (info, metadata, builder) -> {};
	}

	public JavaFile javaFile() {

		YearMonth creationDate = YearMonth.now(ZoneId.of("UTC"));

		// start creating the type
		TypeSpec.Builder builder = TypeSpec.classBuilder(this.generationMetadata.getTargetTypeName()) //
				.addModifiers(Modifier.PUBLIC) //
				.addAnnotation(Generated.class) //
				.addJavadoc("AOT generated repository implementation for {@link $T}.\n",
						repositoryInformation.getRepositoryInterface()) //
				.addJavadoc("\n") //
				.addJavadoc("@since $L/$L\n", creationDate.get(ChronoField.YEAR), creationDate.get(ChronoField.MONTH_OF_YEAR)) //
				.addJavadoc("@author $L", "Spring Data"); // TODO: does System.getProperty("user.name") make sense here?

		// TODO: we do not need that here
		// .addSuperinterface(repositoryInformation.getRepositoryInterface());

		// create the constructor
		AotRepositoryConstructorBuilder constructorBuilder = new AotRepositoryConstructorBuilder(repositoryInformation,
				generationMetadata);
		constructorBuilderCustomizer.accept(constructorBuilder);
		builder.addMethod(constructorBuilder.buildConstructor());

		// write methods
		// start with the derived ones
		ReflectionUtils.doWithMethods(repositoryInformation.getRepositoryInterface(), method -> {

			AotRepositoryMethodGenerationContext context = new AotRepositoryMethodGenerationContext(method,
					repositoryInformation, generationMetadata);
			AotRepositoryMethodBuilder methodBuilder = methodContextFunction.apply(context);
			if (methodBuilder != null) {
				builder.addMethod(methodBuilder.buildMethod());
			}

		}, it -> {

			/*
			the isBaseClassMethod(it) check seems to have some issues.
			need to hard code it here
			 */

			if (ReflectionUtils.findMethod(CrudRepository.class, it.getName(), it.getParameterTypes()) != null) {
				return false;
			}

			return !repositoryInformation.isBaseClassMethod(it) && !repositoryInformation.isCustomMethod(it)
					&& !it.isDefault();
		});

		// write fields at the end so we make sure to capture things added by methods
		generationMetadata.getFields().values().forEach(builder::addField);

		// finally customize the file itself
		this.customizer.customize(repositoryInformation, generationMetadata, builder);
		return JavaFile.builder(packageName(), builder.build()).build();
	}

	AotRepositoryBuilder withConstructorCustomizer(Consumer<AotRepositoryConstructorBuilder> constuctorBuilder) {

		this.constructorBuilderCustomizer = constuctorBuilder;
		return this;
	}

	AotRepositoryBuilder withDerivedMethodFunction(
			Function<AotRepositoryMethodGenerationContext, AotRepositoryMethodBuilder> methodContextFunction) {
		this.methodContextFunction = methodContextFunction;
		return this;
	}

	AotRepositoryBuilder withFileCustomizer(RepositoryCustomizer repositoryCustomizer) {

		this.customizer = repositoryCustomizer;
		return this;
	}

	AotRepositoryImplementationMetadata getGenerationMetadata() {
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

	public interface RepositoryCustomizer {

		void customize(RepositoryInformation repositoryInformation, AotRepositoryImplementationMetadata metadata,
				TypeSpec.Builder builder);
	}
}
