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

import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeName;

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
	void addParameter(String parameterName, Class<?> type);

	/**
	 * Add constructor parameter and create a field storing its value.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 */
	default void addParameter(String parameterName, TypeName type) {
		addParameter(parameterName, type, true);
	}

	/**
	 * Add constructor parameter.
	 *
	 * @param parameterName name of the parameter.
	 * @param type parameter type.
	 * @param createField whether to create a field for the parameter and assign its value to the field.
	 */
	void addParameter(String parameterName, TypeName type, boolean createField);

	/**
	 * Add constructor customizer. Customizer is invoked after adding constructor arguments and before assigning
	 * constructor arguments to fields.
	 *
	 * @param customizer the customizer with direct access to the {@link MethodSpec.Builder constructor builder}.
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
		void customize(MethodSpec.Builder builder);

	}

}
