/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.type.classreading;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.data.type.MethodsMetadata;

/**
 * Extension to {@link MetadataReader} for accessing class metadata and method metadata as read by an ASM
 * {@link org.springframework.asm.ClassReader}.
 *
 * @author Mark Paluch
 * @since 2.1
 */
public interface MethodsMetadataReader extends MetadataReader {

	/**
	 * @return the {@link MethodsMetadata} for methods in the class file.
	 */
	MethodsMetadata getMethodsMetadata();
}
