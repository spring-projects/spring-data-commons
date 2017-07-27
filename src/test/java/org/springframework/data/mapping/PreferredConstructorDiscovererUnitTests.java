/*
 * Copyright 2011-2017 by the original author(s).
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

import static org.assertj.core.api.Assertions.*;

import java.util.Iterator;

import org.junit.Test;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.PreferredConstructorDiscovererUnitTests.Outer.Inner;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit tests for {@link PreferredConstructorDiscoverer}.
 *
 * @author Oliver Gierke
 * @author Roman Rodov
 * @author Mark Paluch
 */
public class PreferredConstructorDiscovererUnitTests<P extends PersistentProperty<P>> {

	@Test // DATACMNS-1126
	public void findsNoArgConstructorForClassWithoutExplicitConstructor() {

		assertThat(PreferredConstructorDiscoverer.discover(EntityWithoutConstructor.class)).satisfies(constructor -> {

			assertThat(constructor).isNotNull();
			assertThat(constructor.isNoArgConstructor()).isTrue();
			assertThat(constructor.isExplicitlyAnnotated()).isFalse();
		});
	}

	@Test // DATACMNS-1126
	public void findsNoArgConstructorForClassWithMultipleConstructorsAndNoArgOne() {

		assertThat(PreferredConstructorDiscoverer.discover(ClassWithEmptyConstructor.class)).satisfies(constructor -> {

			assertThat(constructor).isNotNull();
			assertThat(constructor.isNoArgConstructor()).isTrue();
			assertThat(constructor.isExplicitlyAnnotated()).isFalse();
		});
	}

	@Test // DATACMNS-1126
	public void doesNotThrowExceptionForMultipleConstructorsAndNoNoArgConstructorWithoutAnnotation() {

		assertThat(PreferredConstructorDiscoverer.discover(ClassWithMultipleConstructorsWithoutEmptyOne.class)).isNull();
	}

	@Test // DATACMNS-1126
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void usesConstructorWithAnnotationOverEveryOther() {


		assertThat(PreferredConstructorDiscoverer.discover(ClassWithMultipleConstructorsAndAnnotation.class))
				.satisfies(constructor -> {

			assertThat(constructor).isNotNull();
			assertThat(constructor.isNoArgConstructor()).isFalse();
			assertThat(constructor.isExplicitlyAnnotated()).isTrue();

			assertThat(constructor.hasParameters()).isTrue();

					Iterator<Parameter<Object, P>> parameters = (Iterator) constructor.getParameters().iterator();

			Parameter<?, P> parameter = parameters.next();
			assertThat(parameter.getType().getType()).isEqualTo(Long.class);
			assertThat(parameters.hasNext()).isFalse();
		});
	}

	@Test // DATACMNS-134, DATACMNS-1126
	public void discoversInnerClassConstructorCorrectly() {

		PersistentEntity<Inner, P> entity = new BasicPersistentEntity<>(ClassTypeInformation.from(Inner.class));

		assertThat(PreferredConstructorDiscoverer.discover(entity)).satisfies(constructor -> {

			Parameter<?, P> parameter = constructor.getParameters().iterator().next();
			assertThat(constructor.isEnclosingClassParameter(parameter)).isTrue();
		});
	}

	@Test // DATACMNS-1082, DATACMNS-1126
	public void skipsSyntheticConstructor() {

		PersistentEntity<SyntheticConstructor, P> entity = new BasicPersistentEntity<>(
				ClassTypeInformation.from(SyntheticConstructor.class));

		assertThat(PreferredConstructorDiscoverer.discover(entity)).satisfies(constructor -> {

			PersistenceConstructor annotation = constructor.getConstructor().getAnnotation(PersistenceConstructor.class);
			assertThat(annotation).isNotNull();
			assertThat(constructor.getConstructor().isSynthetic()).isFalse();
		});
	}

	static class SyntheticConstructor {
		@PersistenceConstructor
		private SyntheticConstructor(String x) {}

		class InnerSynthetic {
			// Compiler will generate a synthetic constructor since
			// SyntheticConstructor() is private.
			InnerSynthetic() {
				new SyntheticConstructor("");
			}
		}
	}

	static class EntityWithoutConstructor {

	}

	static class ClassWithEmptyConstructor {

		public ClassWithEmptyConstructor() {}
	}

	static class ClassWithMultipleConstructorsAndEmptyOne {

		public ClassWithMultipleConstructorsAndEmptyOne(String value) {}

		public ClassWithMultipleConstructorsAndEmptyOne() {}
	}

	static class ClassWithMultipleConstructorsWithoutEmptyOne {

		public ClassWithMultipleConstructorsWithoutEmptyOne(String value) {}

		public ClassWithMultipleConstructorsWithoutEmptyOne(Long value) {}
	}

	static class ClassWithMultipleConstructorsAndAnnotation {

		public ClassWithMultipleConstructorsAndAnnotation() {}

		public ClassWithMultipleConstructorsAndAnnotation(String value) {}

		@PersistenceConstructor
		public ClassWithMultipleConstructorsAndAnnotation(Long value) {}
	}

	static class Outer {

		class Inner {

		}
	}
}
