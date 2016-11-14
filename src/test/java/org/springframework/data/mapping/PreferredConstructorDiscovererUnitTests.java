/*
 * Copyright 2011-2016 by the original author(s).
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
 */
public class PreferredConstructorDiscovererUnitTests<P extends PersistentProperty<P>> {

	@Test
	public void findsNoArgConstructorForClassWithoutExplicitConstructor() {

		PreferredConstructorDiscoverer<EntityWithoutConstructor, P> discoverer = new PreferredConstructorDiscoverer<>(
				EntityWithoutConstructor.class);

		assertThat(discoverer.getConstructor()).hasValueSatisfying(constructor -> {

			assertThat(constructor).isNotNull();
			assertThat(constructor.isNoArgConstructor()).isTrue();
			assertThat(constructor.isExplicitlyAnnotated()).isFalse();
		});
	}

	@Test
	public void findsNoArgConstructorForClassWithMultipleConstructorsAndNoArgOne() {

		PreferredConstructorDiscoverer<ClassWithEmptyConstructor, P> discoverer = new PreferredConstructorDiscoverer<>(
				ClassWithEmptyConstructor.class);

		assertThat(discoverer.getConstructor()).hasValueSatisfying(constructor -> {

			assertThat(constructor).isNotNull();
			assertThat(constructor.isNoArgConstructor()).isTrue();
			assertThat(constructor.isExplicitlyAnnotated()).isFalse();
		});
	}

	@Test
	public void doesNotThrowExceptionForMultipleConstructorsAndNoNoArgConstructorWithoutAnnotation() {

		PreferredConstructorDiscoverer<ClassWithMultipleConstructorsWithoutEmptyOne, P> discoverer = new PreferredConstructorDiscoverer<>(
				ClassWithMultipleConstructorsWithoutEmptyOne.class);

		assertThat(discoverer.getConstructor()).isNotPresent();
	}

	@Test
	public void usesConstructorWithAnnotationOverEveryOther() {

		PreferredConstructorDiscoverer<ClassWithMultipleConstructorsAndAnnotation, P> discoverer = new PreferredConstructorDiscoverer<>(
				ClassWithMultipleConstructorsAndAnnotation.class);

		assertThat(discoverer.getConstructor()).hasValueSatisfying(constructor -> {

			assertThat(constructor).isNotNull();
			assertThat(constructor.isNoArgConstructor()).isFalse();
			assertThat(constructor.isExplicitlyAnnotated()).isTrue();

			assertThat(constructor.hasParameters()).isTrue();

			Iterator<Parameter<Object, P>> parameters = constructor.getParameters().iterator();

			Parameter<?, P> parameter = parameters.next();
			assertThat(parameter.getType().getType()).isEqualTo(Long.class);
			assertThat(parameters.hasNext()).isFalse();
		});
	}

	/**
	 * @see DATACMNS-134
	 */
	@Test
	public void discoversInnerClassConstructorCorrectly() {

		PersistentEntity<Inner, P> entity = new BasicPersistentEntity<Inner, P>(ClassTypeInformation.from(Inner.class));
		PreferredConstructorDiscoverer<Inner, P> discoverer = new PreferredConstructorDiscoverer<Inner, P>(entity);

		assertThat(discoverer.getConstructor()).hasValueSatisfying(constructor -> {

			Parameter<?, P> parameter = constructor.getParameters().iterator().next();
			assertThat(constructor.isEnclosingClassParameter(parameter)).isTrue();
		});
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
