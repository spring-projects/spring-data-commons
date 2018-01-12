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

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.type.MethodsMetadata;
import org.springframework.lang.Nullable;

/**
 * Extension of {@link SimpleMetadataReaderFactory} that reads {@link MethodsMetadata}, creating a new ASM
 * {@link MethodsMetadataReader} for every request.
 *
 * @author Mark Paluch
 * @since 2.1
 */
public class MethodsMetadataReaderFactory extends SimpleMetadataReaderFactory {

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the default class loader.
	 */
	public MethodsMetadataReaderFactory() {}

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the given {@link ResourceLoader}.
	 *
	 * @param resourceLoader the Spring {@link ResourceLoader} to use (also determines the {@link ClassLoader} to use).
	 */
	public MethodsMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	/**
	 * Create a new {@link MethodsMetadataReaderFactory} for the given {@link ClassLoader}.
	 *
	 * @param classLoader the class loader to use.
	 */
	public MethodsMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		super(classLoader);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.type.classreading.SimpleMetadataReaderFactory#getMetadataReader(java.lang.String)
	 */
	@Override
	public MethodsMetadataReader getMetadataReader(String className) throws IOException {
		return (MethodsMetadataReader) super.getMetadataReader(className);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.type.classreading.SimpleMetadataReaderFactory#getMetadataReader(org.springframework.core.io.Resource)
	 */
	@Override
	public MethodsMetadataReader getMetadataReader(Resource resource) throws IOException {
		return new DefaultMethodsMetadataReader(resource, getResourceLoader().getClassLoader());
	}
}
