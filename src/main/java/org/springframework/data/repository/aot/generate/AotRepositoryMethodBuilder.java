/*
 * Copyright 2024-present the original author or authors.
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
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.springframework.data.javapoet.TypeNames;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.TypeVariableName;

/**
 * Builder for AOT repository query methods.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class AotRepositoryMethodBuilder {

	private final AotQueryMethodGenerationContext context;

	private Function<AotQueryMethodGenerationContext, CodeBlock> contribution = (context) -> CodeBlock.builder().build();
	private BiConsumer<AotQueryMethodGenerationContext, CodeBlock.Builder> customizer = (context, body) -> {};

	AotRepositoryMethodBuilder(AotQueryMethodGenerationContext context) {
		this.context = context;
	}

	/**
	 * Register a {@link org.springframework.data.repository.aot.generate.MethodContributor.RepositoryMethodContribution}
	 * for the repository interface that can contribute a query method implementation block.
	 *
	 * @param contribution
	 * @return
	 */
	public AotRepositoryMethodBuilder contribute(Function<AotQueryMethodGenerationContext, CodeBlock> contribution) {
		this.contribution = contribution;
		return this;
	}

	/**
	 * Register a query method customizer that is applied after a successful
	 * {@link org.springframework.data.repository.aot.generate.MethodContributor.RepositoryMethodContribution}.
	 *
	 * @param customizer
	 * @return
	 */
	public AotRepositoryMethodBuilder customize(
			BiConsumer<AotQueryMethodGenerationContext, CodeBlock.Builder> customizer) {
		this.customizer = customizer;
		return this;
	}

	/**
	 * Builds an AOT repository method if
	 * {@link org.springframework.data.repository.aot.generate.MethodContributor.RepositoryMethodContribution} can
	 * contribute a method.
	 *
	 * @return the {@link MethodSpec} or {@literal null}, if the method cannot be contributed.
	 */
	public MethodSpec buildMethod() {

		CodeBlock methodBody = contribution.apply(context);
		MethodSpec.Builder builder = initializeMethodBuilder();

		if (context.getExpressionMarker().isInUse()) {
			builder.addCode(context.getExpressionMarker().declaration());
		}

		builder.addCode(methodBody);

		CodeBlock.Builder customizerBlock = CodeBlock.builder();
		customizer.accept(context, customizerBlock);
		builder.addCode(customizerBlock.build());

		return builder.build();
	}

	private MethodSpec.Builder initializeMethodBuilder() {

		MethodSpec.Builder builder = MethodSpec.methodBuilder(context.getMethod().getName()).addModifiers(Modifier.PUBLIC);
		builder.returns(TypeNames.resolvedTypeName(context.getTargetMethodMetadata().getReturnType()));

		TypeVariable<Method>[] tvs = context.getMethod().getTypeParameters();
		for (TypeVariable<Method> tv : tvs) {
			builder.addTypeVariable(TypeVariableName.get(tv));
		}

		MethodMetadata methodMetadata = context.getTargetMethodMetadata();
		Map<String, ParameterSpec> methodArguments = methodMetadata.getMethodArguments();
		builder.addJavadoc("AOT generated implementation of {@link $T#$L}.", context.getMethod().getDeclaringClass(),
				ReflectionUtils.toString(context.getMethod()));

		methodArguments.forEach((name, spec) -> builder.addParameter(spec));

		return builder;
	}

}
