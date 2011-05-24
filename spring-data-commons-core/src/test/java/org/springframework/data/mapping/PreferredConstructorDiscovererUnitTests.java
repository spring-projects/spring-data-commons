/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mapping.model.PreferredConstructor;
import org.springframework.data.mapping.model.PreferredConstructor.Parameter;


/**
 * Unit tests for {@link PreferredConstructorDiscoverer}.
 *
 * @author Oliver Gierke
 */
public class PreferredConstructorDiscovererUnitTests {

	@Test
	public void findsNoArgConstructorForClassWithoutExplicitConstructor() {

		PreferredConstructorDiscoverer<EntityWithoutConstructor> discoverer =
				new PreferredConstructorDiscoverer<EntityWithoutConstructor>(
						EntityWithoutConstructor.class);
		PreferredConstructor<EntityWithoutConstructor> constructor =
				discoverer.getConstructor();

		assertThat(constructor, is(notNullValue()));
		assertThat(constructor.isNoArgConstructor(), is(true));
		assertThat(constructor.isExplicitlyAnnotated(), is(false));
	}


	@Test
	public void findsNoArgConstructorForClassWithMultipleConstructorsAndNoArgOne() {

		PreferredConstructorDiscoverer<ClassWithEmptyConstructor> discoverer =
				new PreferredConstructorDiscoverer<ClassWithEmptyConstructor>(
						ClassWithEmptyConstructor.class);
		PreferredConstructor<ClassWithEmptyConstructor> constructor =
				discoverer.getConstructor();

		assertThat(constructor, is(notNullValue()));
		assertThat(constructor.isNoArgConstructor(), is(true));
		assertThat(constructor.isExplicitlyAnnotated(), is(false));
	}


	@Test(expected = IllegalArgumentException.class)
	public void throwsExceptionForMultipleConstructorsAndNoNoArgConstructorWithoutAnnotation() {

		new PreferredConstructorDiscoverer<ClassWithMultipleConstructorsWithoutEmptyOne>(
				ClassWithMultipleConstructorsWithoutEmptyOne.class);
	}

	@Test
	public void usesConstructorWithAnnotationOverEveryOther() {

		PreferredConstructorDiscoverer<ClassWithMultipleConstructorsAndAnnotation> discoverer =
				new PreferredConstructorDiscoverer<ClassWithMultipleConstructorsAndAnnotation>(
						ClassWithMultipleConstructorsAndAnnotation.class);
		PreferredConstructor<ClassWithMultipleConstructorsAndAnnotation> constructor =
				discoverer.getConstructor();

		assertThat(constructor, is(notNullValue()));
		assertThat(constructor.isNoArgConstructor(), is(false));
		assertThat(constructor.isExplicitlyAnnotated(), is(true));

		assertThat(constructor.hasParameters(), is(true));
		Iterator<Parameter<?>> parameters = constructor.getParameters().iterator();

		Parameter<?> parameter = parameters.next();
		assertThat(parameter.getType().getType(), typeCompatibleWith(Long.class));
		assertThat(parameters.hasNext(), is(false));
	}

	static class EntityWithoutConstructor {

	}

	static class ClassWithEmptyConstructor {

		public ClassWithEmptyConstructor() {
		}
	}

	static class ClassWithMultipleConstructorsAndEmptyOne {

		public ClassWithMultipleConstructorsAndEmptyOne(String value) {
		}


		public ClassWithMultipleConstructorsAndEmptyOne() {
		}
	}

	static class ClassWithMultipleConstructorsWithoutEmptyOne {

		public ClassWithMultipleConstructorsWithoutEmptyOne(String value) {
		}

		public ClassWithMultipleConstructorsWithoutEmptyOne(Long value) {
		}
	}

	static class ClassWithMultipleConstructorsAndAnnotation {

		public ClassWithMultipleConstructorsAndAnnotation() {
		}

		public ClassWithMultipleConstructorsAndAnnotation(String value) {
		}

		@PersistenceConstructor
		public ClassWithMultipleConstructorsAndAnnotation(Long value) {
		}
	}
}
