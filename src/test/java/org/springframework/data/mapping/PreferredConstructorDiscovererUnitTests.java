/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mapping.PreferredConstructorDiscovererUnitTests.Outer.Inner;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link PreferredConstructorDiscoverer}.
 *
 * @author Oliver Gierke
 * @author Roman Rodov
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Pavel Anisimov
 */
class PreferredConstructorDiscovererUnitTests<P extends PersistentProperty<P>> {

	@Test // DATACMNS-1126
	void findsNoArgConstructorForClassWithoutExplicitConstructor() {

		var constructor = PreferredConstructorDiscoverer.discover(EntityWithoutConstructor.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.isNoArgConstructor()).isTrue();
		assertThat(constructor.isExplicitlyAnnotated()).isFalse();
	}

	@Test // DATACMNS-1126
	void findsNoArgConstructorForClassWithMultipleConstructorsAndNoArgOne() {

		var constructor = PreferredConstructorDiscoverer.discover(ClassWithEmptyConstructor.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.isNoArgConstructor()).isTrue();
		assertThat(constructor.isExplicitlyAnnotated()).isFalse();
	}

	@Test // DATACMNS-1126
	void doesNotThrowExceptionForMultipleConstructorsAndNoNoArgConstructorWithoutAnnotation() {

		var constructor = PreferredConstructorDiscoverer.discover(ClassWithMultipleConstructorsWithoutEmptyOne.class);

		assertThat(constructor).isNull();
	}

	@Test // DATACMNS-1126
	void usesConstructorWithAnnotationOverEveryOther() {

		PreferredConstructor<ClassWithMultipleConstructorsAndAnnotation, P> constructor = PreferredConstructorDiscoverer
				.discover(ClassWithMultipleConstructorsAndAnnotation.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.isNoArgConstructor()).isFalse();
		assertThat(constructor.isExplicitlyAnnotated()).isTrue();

		assertThat(constructor.hasParameters()).isTrue();

		Iterator<Parameter<Object, P>> parameters = constructor.getParameters().iterator();

		Parameter<?, P> parameter = parameters.next();
		assertThat(parameter.getType().getType()).isEqualTo(Long.class);
		assertThat(parameters.hasNext()).isFalse();
	}

	@Test // DATACMNS-134, DATACMNS-1126
	void discoversInnerClassConstructorCorrectly() {

		PersistentEntity<Inner, P> entity = new BasicPersistentEntity<>(TypeInformation.of(Inner.class));

		var constructor = PreferredConstructorDiscoverer.discover(entity);

		assertThat(constructor).isNotNull();

		Parameter<?, P> parameter = constructor.getParameters().iterator().next();
		assertThat(constructor.isParentParameter(parameter)).isTrue();
	}

	@Test // DATACMNS-1082, DATACMNS-1126
	void skipsSyntheticConstructor() {

		PersistentEntity<SyntheticConstructor, P> entity = new BasicPersistentEntity<>(
				TypeInformation.of(SyntheticConstructor.class));

		var constructor = PreferredConstructorDiscoverer.discover(entity);
		assertThat(constructor).isNotNull();

		var annotation = constructor.getConstructor().getAnnotation(PersistenceConstructor.class);
		assertThat(annotation).isNotNull();
		assertThat(constructor.getConstructor().isSynthetic()).isFalse();
	}

	@Test // GH-2313
	void capturesEnclosingTypeParameterOfNonStaticInnerClass() {

		var constructor = PreferredConstructorDiscoverer.discover(NonStaticWithGenericTypeArgUsedInCtor.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.getParameters()).hasSize(2);
		assertThat(constructor.getParameters().get(0).getName()).isEqualTo("this$0");
		assertThat(constructor.getParameters().get(1).getName()).isEqualTo("value");
	}

	@Test // GH-2313
	void capturesSuperClassEnclosingTypeParameterOfNonStaticInnerClass() {

		var constructor = PreferredConstructorDiscoverer.discover(NonStaticInnerWithGenericArgUsedInCtor.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.getParameters()).hasSize(2);
		assertThat(constructor.getParameters().get(0).getName()).isEqualTo("this$0");
		assertThat(constructor.getParameters().get(1).getName()).isEqualTo("value");
	}

	@Test // GH-2332
	void detectsMetaAnnotatedValueAnnotation() {

		var constructor = PreferredConstructorDiscoverer.discover(ClassWithMetaAnnotatedParameter.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.getParameters().get(0).getSpelExpression()).isEqualTo("${hello-world}");
		assertThat(constructor.getParameters().get(0).getAnnotations()).isNotNull();
	}

	@Test // GH-2332
	void detectsCanonicalRecordConstructorWhenRecordHasSingleArgConstructor() {

		var constructor = PreferredConstructorDiscoverer.discover(RecordWithSingleArgConstructor.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.getParameters()).hasSize(2);
		assertThat(constructor.getParameters().get(0).getRawType()).isEqualTo(Long.class);
		assertThat(constructor.getParameters().get(1).getRawType()).isEqualTo(String.class);
	}

	@Test // GH-2332
	void detectsCanonicalRecordConstructorWhenRecordHasNoArgConstructor() {

		var constructor = PreferredConstructorDiscoverer.discover(RecordWithNoArgConstructor.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.getParameters()).hasSize(2);
		assertThat(constructor.getParameters().get(0).getRawType()).isEqualTo(Long.class);
		assertThat(constructor.getParameters().get(1).getRawType()).isEqualTo(String.class);
	}

	@Test // GH-2332
	void detectsAnnotatedRecordConstructor() {

		var constructor = PreferredConstructorDiscoverer.discover(RecordWithPersistenceCreator.class);

		assertThat(constructor).isNotNull();
		assertThat(constructor.getParameters()).hasSize(1);
		assertThat(constructor.getParameters().get(0).getRawType()).isEqualTo(String.class);
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

	private static class EntityWithoutConstructor {

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

	static class GenericTypeArgUsedInCtor<T> {

		GenericTypeArgUsedInCtor(T value) {}
	}

	class NonStaticWithGenericTypeArgUsedInCtor<T> {

		protected NonStaticWithGenericTypeArgUsedInCtor(T value) {}
	}

	class NonStaticInnerWithGenericArgUsedInCtor<T> extends GenericTypeArgUsedInCtor<T> {

		public NonStaticInnerWithGenericArgUsedInCtor(T value) {
			super(value);
		}
	}

	static class ClassWithMetaAnnotatedParameter {

		ClassWithMetaAnnotatedParameter(@MyValue String value) {}
	}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	@Value("${hello-world}")
	@interface MyValue {
	}

	public record RecordWithSingleArgConstructor(Long id, String name) {

		public RecordWithSingleArgConstructor(String name) {
			this(null, name);
		}
	}

	public record RecordWithNoArgConstructor(Long id, String name) {

		public RecordWithNoArgConstructor(String name) {
			this(null, null);
		}
	}

	public record RecordWithPersistenceCreator(Long id, String name) {

		@PersistenceCreator
		public RecordWithPersistenceCreator(String name) {
			this(null, name);
		}
	}
}
