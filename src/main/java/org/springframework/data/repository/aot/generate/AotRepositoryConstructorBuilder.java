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

import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanReference;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;

/**
 * Builder for AOT Repository Constructors.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
public interface AotRepositoryConstructorBuilder {

	/**
	 * Add constructor parameter and create a field storing its value.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 */
	default void addParameter(String parameterName, Class<?> type) {
		addParameter(parameterName, ResolvableType.forClass(type));
	}

	/**
	 * Add constructor parameter and create a field storing its value.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 */
	default void addParameter(String parameterName, ResolvableType type) {
		addParameter(parameterName, type, true);
	}

	/**
	 * Add constructor parameter.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 * @param bindToField whether to create a field for the parameter and assign its value to the field.
	 */
	default void addParameter(String parameterName, Class<?> type, boolean bindToField) {
		addParameter(parameterName, ResolvableType.forClass(type), bindToField);
	}

	/**
	 * Add constructor parameter.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 * @param bindToField whether to create a field for the parameter and assign its value to the field.
	 */
	default void addParameter(String parameterName, ResolvableType type, boolean bindToField) {
		addParameter(parameterName, type, c -> c.bindToField(bindToField));
	}

	/**
	 * Add constructor parameter.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 * @param parameterCustomizer customizer for the parameter.
	 */
	default void addParameter(String parameterName, Class<?> type,
			Consumer<ConstructorParameterCustomizer> parameterCustomizer) {
		addParameter(parameterName, ResolvableType.forClass(type), parameterCustomizer);
	}

	/**
	 * Add constructor parameter.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 * @param parameterCustomizer customizer for the parameter.
	 */
	void addParameter(String parameterName, ResolvableType type,
			Consumer<ConstructorParameterCustomizer> parameterCustomizer);

	/**
	 * Add constructor body customizer. The customizer is invoked after adding constructor arguments and before assigning
	 * constructor arguments to fields.
	 *
	 * @param customizer the customizer with direct access to the {@link CodeBlock.Builder constructor builder}.
	 */
	void customize(ConstructorCustomizer customizer);

	/**
	 * Customizer for the AOT repository constructor.
	 */
	interface ConstructorCustomizer {

		/**
		 * Customize the constructor.
		 *
		 * @param builder the constructor builder to be customized.
		 */
		void customize(CodeBlock.Builder builder);

	}

	/**
	 * Customizer for a AOT repository constructor parameter.
	 */
	interface ConstructorParameterCustomizer {

		/**
		 * Bind the constructor parameter to a field of the same type using the original parameter name.
		 *
		 * @return {@code this} for method chaining.
		 */
		default ConstructorParameterCustomizer bindToField() {
			return bindToField(true);
		}

		/**
		 * Bind the constructor parameter to a field of the same type using the original parameter name.
		 *
		 * @return {@code this} for method chaining.
		 */
		ConstructorParameterCustomizer bindToField(boolean bindToField);

		/**
		 * Use the given {@link BeanReference} to define the constructor parameter origin. Bean references can be by name,
		 * by type or by type and name. Using a bean reference renders a lookup to a local variable using the parameter name
		 * as guidance for the local variable name
		 *
		 * @see FragmentParameterContext#localVariable(String)
		 */
		void origin(BeanReference reference);

		/**
		 * Use the given {@link BeanReference} to define the constructor parameter origin. Bean references can be by name,
		 * by type or by type and name.
		 */
		void origin(Function<FragmentParameterContext, ParameterOrigin> originFunction);

	}

	/**
	 * Context to obtain a constructor parameter value when declaring the constructor parameter origin.
	 */
	interface FragmentParameterContext {

		/**
		 * @return variable name of the {@link org.springframework.beans.factory.BeanFactory}.
		 */
		String beanFactory();

		/**
		 * @return parameter origin to obtain the {@link org.springframework.beans.factory.BeanFactory}.
		 */
		default ParameterOrigin getBeanFactory() {
			return ParameterOrigin.ofReference(beanFactory());
		}

		/**
		 * @return variable name of the
		 *         {@link org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport.FragmentCreationContext}.
		 */
		String fragmentCreationContext();

		/**
		 * @return parameter origin to obtain the fragment creation context.
		 */
		default ParameterOrigin getFragmentCreationContext() {
			return ParameterOrigin.ofReference(fragmentCreationContext());
		}

		/**
		 * Obtain a naming-clash free variant for the given logical variable name within the local method context. Returns
		 * the target variable name when called multiple times with the same {@code variableName}.
		 *
		 * @param variableName the logical variable name.
		 * @return the variable name used in the generated code.
		 */
		String localVariable(String variableName);

	}

	/**
	 * Interface describing the origin of a constructor parameter. The parameter value can be obtained either from a
	 * {@link #getReference() reference} variable, a {@link #getCodeBlock() code block} or a combination of both.
	 *
	 * @author Mark Paluch
	 * @since 4.0
	 */
	interface ParameterOrigin {

		/**
		 * Construct a {@link ParameterOrigin} from the given {@link CodeBlock} and reference name.
		 *
		 * @param reference the reference name to obtain the parameter value from.
		 * @param codeBlock the code block that is required to set up the parameter value.
		 * @return a {@link ParameterOrigin} from the given {@link CodeBlock} and reference name.
		 */
		static ParameterOrigin of(String reference, CodeBlock codeBlock) {

			Assert.hasText(reference, "Parameter reference must not be empty");

			return new DefaultParameterOrigin(reference, codeBlock);
		}

		/**
		 * Construct a {@link ParameterOrigin} from the given {@link CodeBlock}.
		 *
		 * @param codeBlock the code block that produces the parameter value.
		 * @return a {@link ParameterOrigin} from the given {@link CodeBlock}.
		 */
		static ParameterOrigin of(CodeBlock codeBlock) {

			Assert.notNull(codeBlock, "CodeBlock reference must not be empty");

			return new DefaultParameterOrigin(null, codeBlock);
		}

		/**
		 * Construct a {@link ParameterOrigin} from the given reference name.
		 *
		 * @param reference the reference name of the variable to obtain the parameter value from.
		 * @return a {@link ParameterOrigin} from the given reference name.
		 */
		static ParameterOrigin ofReference(String reference) {

			Assert.hasText(reference, "Parameter reference must not be empty");

			return of(reference, CodeBlock.builder().build());
		}

		/**
		 * Obtain the reference name to obtain the parameter value from. Can be {@literal null} if the parameter value is
		 * solely obtained from the {@link #getCodeBlock() code block}.
		 *
		 * @return name of the reference or {@literal null} if absent.
		 */
		@Nullable
		String getReference();

		/**
		 * Obtain the code block to obtain the parameter value from. Never {@literal null}, can be empty.
		 */
		CodeBlock getCodeBlock();

	}
}
