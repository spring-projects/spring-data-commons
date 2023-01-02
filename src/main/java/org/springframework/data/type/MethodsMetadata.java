/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.type;

import java.util.Set;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.data.type.classreading.MethodsMetadataReader;

/**
 * Interface that defines abstract metadata of a specific class, in a form that does not require that class to be loaded
 * yet.
 *
 * @author Mark Paluch
 * @since 2.1
 * @see MethodMetadata
 * @see ClassMetadata
 * @see MethodsMetadataReader#getMethodsMetadata()
 * @deprecated since 3.0, use {@link MetadataReader} directly to obtain {@link AnnotationMetadata#getDeclaredMethods()
 *             declared methods} directly.
 */
@Deprecated
public interface MethodsMetadata extends ClassMetadata {

	/**
	 * Return all methods.
	 *
	 * @return the methods declared in the class ordered as found in the class file. Order does not necessarily reflect
	 *         the declaration order in the source file.
	 */
	Set<MethodMetadata> getMethods();

	/**
	 * Return all methods matching method {@code name}.
	 *
	 * @param name name of the method, must not be {@literal null} or empty.
	 * @return the methods matching method {@code name } declared in the class ordered as found in the class file. Order
	 *         does not necessarily reflect the declaration order in the source file.
	 */
	Set<MethodMetadata> getMethods(String name);
}
