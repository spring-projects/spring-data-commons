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

import java.io.IOException;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.springframework.core.type.MethodMetadata;
import org.springframework.data.type.MethodsMetadata;

/**
 * Unit tests for {@link DefaultMethodsMetadataReader}.
 *
 * @author Mark Paluch
 */
class DefaultMethodsMetadataReaderUnitTests {

	@Test // DATACMNS-1206
	void shouldReadClassMethods() throws IOException {

		MethodsMetadata metadata = getMethodsMetadata(Foo.class);

		assertThat(metadata.getMethods()).hasSize(3);

		Iterator<MethodMetadata> iterator = metadata.getMethods().iterator();

		assertThat(iterator.next().getMethodName()).isEqualTo("one");
		assertThat(iterator.next().getMethodName()).isEqualTo("two");
		assertThat(iterator.next().getMethodName()).isEqualTo("three");
	}

	@Test // DATACMNS-1206
	void shouldReadInterfaceMethods() throws IOException {

		MethodsMetadata metadata = getMethodsMetadata(Baz.class);

		assertThat(metadata.getMethods()).hasSize(3);

		Iterator<MethodMetadata> iterator = metadata.getMethods().iterator();

		assertThat(iterator.next().getMethodName()).isEqualTo("one");
		assertThat(iterator.next().getMethodName()).isEqualTo("two");
		assertThat(iterator.next().getMethodName()).isEqualTo("three");
	}

	@Test // DATACMNS-1206
	void shouldMetadata() throws IOException {

		MethodsMetadataReaderFactory factory = new MethodsMetadataReaderFactory();
		MethodsMetadataReader metadataReader = factory.getMetadataReader(getClass().getName());

		assertThat(metadataReader.getClassMetadata()).isNotNull();
		assertThat(metadataReader.getAnnotationMetadata()).isNotNull();
	}

	@Test // DATACMNS-1206
	void shouldReturnMethodMetadataByName() throws IOException {

		MethodsMetadata metadata = getMethodsMetadata(Foo.class);

		assertThat(metadata.getMethods()).hasSize(3);

		assertThat(metadata.getMethods("one")).extracting(MethodMetadata::getMethodName).contains("one");
		assertThat(metadata.getMethods("foo")).isEmpty();
	}

	private static MethodsMetadata getMethodsMetadata(Class<?> classToIntrospect) throws IOException {

		MethodsMetadataReaderFactory factory = new MethodsMetadataReaderFactory();
		MethodsMetadataReader metadataReader = factory.getMetadataReader(classToIntrospect.getName());
		return metadataReader.getMethodsMetadata();
	}

	// Create a scenario with a cyclic dependency to mix up methods reported by class.getDeclaredMethods()
	// That's not exactly deterministic because it depends on when the compiler sees the classes.
	abstract class Foo {

		abstract void one(Foo b);

		abstract void two(Bar b);

		abstract void three(Foo b);
	}

	interface Baz {

		void one(Foo b);

		void two(Bar b);

		void three(Baz b);
	}

	abstract class Bar {

		abstract void dependOnFoo(Foo f);

		abstract void dependOnBaz(Baz f);
	}
}
