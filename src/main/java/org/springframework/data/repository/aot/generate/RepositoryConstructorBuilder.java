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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata.ConstructorArgument;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.util.Assert;

/**
 * Builder for AOT Repository Constructors.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class RepositoryConstructorBuilder implements AotRepositoryConstructorBuilder {

	@SuppressWarnings("NullAway")
	private final String beanFactory = AotRepositoryBeanDefinitionPropertiesDecorator.RESERVED_TYPES
			.get(ResolvableType.forClass(BeanFactory.class));

	@SuppressWarnings("NullAway")
	private final String fragmentCreationContext = AotRepositoryBeanDefinitionPropertiesDecorator.RESERVED_TYPES
			.get(ResolvableType.forClass(RepositoryFactoryBeanSupport.FragmentCreationContext.class));

	private final AotRepositoryFragmentMetadata metadata;
	private final Set<String> parametersAdded = new LinkedHashSet<>();
	private final Map<String, String> localVariables = new LinkedHashMap<>();
	private final VariableNameFactory variableNameFactory;

	// add super call with all parameters added
	private ConstructorCustomizer customizer = (builder) -> {
		builder.addStatement("super(%s%s)".formatted( //
				"$L".repeat(parametersAdded.isEmpty() ? 0 : 1), //
				", $L".repeat(Math.max(0, parametersAdded.size() - 1))), parametersAdded.toArray());
	};

	RepositoryConstructorBuilder(AotRepositoryFragmentMetadata metadata) {
		this.metadata = metadata;
		this.variableNameFactory = new LocalVariableNameFactory(
				AotRepositoryBeanDefinitionPropertiesDecorator.RESERVED_TYPES.values());
	}

	@Override
	public void addParameter(String parameterName, ResolvableType type,
			Consumer<ConstructorParameterCustomizer> customizer) {

		this.parametersAdded.add(parameterName);

		Supplier<ConstructorArgument> constructorArgumentSupplier = () -> {

			ConstructorParameterContext context = new ConstructorParameterContext(this::localVariable, parameterName, type);
			customizer.accept(context);

			return new ConstructorArgument(parameterName, type, context.bindToField, context.getRequiredParameterOrigin());
		};

		this.metadata.addConstructorArgument(parameterName, type, constructorArgumentSupplier);
	}

	/**
	 * Context to customize a constructor parameter.
	 */
	class ConstructorParameterContext implements ConstructorParameterCustomizer {

		private final VariableNameFactory variableFactory;
		private final String parameterName;
		private final TypeName typeName;

		boolean bindToField;
		@Nullable ParameterOrigin block;

		ConstructorParameterContext(VariableNameFactory variableFactory, String parameterName,
				ResolvableType resolvableType) {

			this.variableFactory = variableFactory;
			this.parameterName = parameterName;
			this.typeName = AotRepositoryFragmentMetadata.typeNameOf(resolvableType);

			if (resolvableType.isAssignableFrom(BeanFactory.class)) {
				origin(FragmentParameterContext::getBeanFactory);
			} else if (resolvableType.isAssignableFrom(RepositoryFactoryBeanSupport.FragmentCreationContext.class)) {
				origin(FragmentParameterContext::getFragmentCreationContext);
			} else {
				origin(new RuntimeBeanReference(resolvableType.toClass()));
			}
		}

		@Override
		public ConstructorParameterCustomizer bindToField(boolean bindToField) {
			this.bindToField = bindToField;
			return this;
		}

		@Override
		public void origin(BeanReference reference) {

			origin(ctx -> {

				CodeBlock.Builder builder = CodeBlock.builder();
				String variableName = ctx.localVariable(parameterName);

				if (reference instanceof RuntimeBeanReference rbr && rbr.getBeanType() != null) {

					if (rbr.getBeanName().equals(rbr.getBeanType().getName())) {
						builder.addStatement("$1T $2L = $3L.getBean($4T.class)", typeName, variableName, ctx.beanFactory(),
								rbr.getBeanType());
					} else {
						builder.addStatement("$1T $2L = $3L.getBean($4S, $5T.class)", typeName, variableName, ctx.beanFactory(),
								rbr.getBeanName(), rbr.getBeanType());
					}
				} else {
					builder.addStatement("$1T $2L = ($1T) $3L.getBean($4S)", typeName, variableName, ctx.beanFactory(),
							reference.getBeanName());
				}

				return ParameterOrigin.of(variableName, builder.build());
			});
		}

		@Override
		public void origin(Function<FragmentParameterContext, ParameterOrigin> originFunction) {

			FragmentParameterContext ctx = new FragmentParameterContext() {

				@Override
				public String beanFactory() {
					return beanFactory;
				}

				@Override
				public String fragmentCreationContext() {
					return fragmentCreationContext;
				}

				@Override
				public String localVariable(String variableName) {
					return variableFactory.generateName(variableName);
				}
			};

			this.block = originFunction.apply(ctx);

			Assert.state(block != null, "Resulting ParameterOriginBlock must not be null");
		}

		public ParameterOrigin getRequiredParameterOrigin() {

			Assert.state(block != null, "ParameterOriginBlock must not be null");

			return block;
		}
	}

	/**
	 * Add constructor customizer. Customizer is invoked after adding constructor arguments and before assigning
	 * constructor arguments to fields.
	 *
	 * @param customizer the customizer with direct access to the {@link MethodSpec.Builder constructor builder}.
	 */
	@Override
	public void customize(ConstructorCustomizer customizer) {

		Assert.notNull(customizer, "ConstructorCustomizer must not be null");
		this.customizer = customizer;
	}

	/**
	 * Obtain a naming-clash free variant for the given logical variable name within the local method context. Returns the
	 * target variable name when called multiple times with the same {@code variableName}.
	 *
	 * @param variableName the logical variable name.
	 * @return the variable name used in the generated code.
	 */
	public String localVariable(String variableName) {
		return localVariables.computeIfAbsent(variableName, variableNameFactory::generateName);
	}

	public MethodSpec buildConstructor() {

		MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

		for (Entry<String, ConstructorArgument> parameter : this.metadata.getConstructorArguments().entrySet()) {
			builder.addParameter(parameter.getValue().typeName(), parameter.getKey());
		}

		CodeBlock.Builder customCtorCode = CodeBlock.builder();
		customizer.customize(customCtorCode);
		if(!customCtorCode.isEmpty()) {
			builder.addCode(customCtorCode.build());
		}

		for (Entry<String, ConstructorArgument> parameter : this.metadata.getConstructorArguments().entrySet()) {
			if (parameter.getValue().isBoundToField()) {
				builder.addStatement("this.$N = $N", parameter.getKey(), parameter.getKey());
			}
		}

		return builder.build();
	}

	public void dispose() {

		for (String parameterName : this.parametersAdded) {
			ConstructorArgument removedArgument = this.metadata.getConstructorArguments().remove(parameterName);
			if (removedArgument != null && removedArgument.isBoundToField()) {
				this.metadata.getFields().remove(parameterName);
			}
		}
	}
}
