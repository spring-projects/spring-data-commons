/*
 * Copyright 2025 the original author or authors.
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

import org.springframework.javapoet.TypeSpec;

/**
 * Builder for AOT repository fragment classes.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface AotRepositoryClassBuilder {

	/**
	 * Add a class customizer. Customizer is invoked after building the class.
	 *
	 * @param customizer the customizer with direct access to the {@link TypeSpec.Builder type builder}.
	 */
	void customize(ClassCustomizer customizer);

	/**
	 * Customizer interface to customize the AOT repository fragment class after it has been defined.
	 */
	interface ClassCustomizer {

		/**
		 * Apply customization ot the AOT repository fragment class after it has been defined.
		 *
		 * @param builder the class builder to be customized.
		 */
		void customize(TypeSpec.Builder builder);

	}

}
