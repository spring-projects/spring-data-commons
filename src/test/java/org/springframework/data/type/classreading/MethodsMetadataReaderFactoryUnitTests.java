/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.type.classreading;

import static org.assertj.core.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;

/**
 * Unit tests for {@link MethodsMetadataReaderFactory}.
 *
 * @author Mark Paluch
 */
class MethodsMetadataReaderFactoryUnitTests {

	@Test // DATACMNS-1206
	void shouldReadFromDefaultClassLoader() throws IOException {

		MethodsMetadataReaderFactory factory = new MethodsMetadataReaderFactory();
		MethodsMetadataReader reader = factory.getMetadataReader(getClass().getName());

		assertThat(reader).isNotNull();
	}

	@Test // DATACMNS-1206
	void shouldReadFromClassLoader() throws IOException {

		MethodsMetadataReaderFactory factory = new MethodsMetadataReaderFactory(getClass().getClassLoader());
		MethodsMetadataReader reader = factory.getMetadataReader(getClass().getName());

		assertThat(reader).isNotNull();
	}

	@Test // DATACMNS-1206
	void shouldNotFindClass() {

		MethodsMetadataReaderFactory factory = new MethodsMetadataReaderFactory(new URLClassLoader(new URL[0], null));

		assertThatThrownBy(() -> factory.getMetadataReader(getClass().getName())).isInstanceOf(FileNotFoundException.class);
	}

	@Test // DATACMNS-1206
	void shouldReadFromResourceLoader() throws IOException {

		MethodsMetadataReaderFactory factory = new MethodsMetadataReaderFactory(new DefaultResourceLoader());
		MethodsMetadataReader reader = factory.getMetadataReader(getClass().getName());

		assertThat(reader).isNotNull();
	}
}
