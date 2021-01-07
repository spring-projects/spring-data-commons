/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.mapping.model;

import static java.lang.annotation.ElementType.*;
import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AnnotationBasedPersistentProperty}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class AnnotationBasedPersistentPropertyUnitTests<P extends AnnotationBasedPersistentProperty<P>> {

	BasicPersistentEntity<Object, SamplePersistentProperty> entity;
	SampleMappingContext context;

	@BeforeEach
	public void setUp() {

		context = new SampleMappingContext();
		entity = context.getRequiredPersistentEntity(Sample.class);
	}

	@Test // DATACMNS-269
	public void discoversAnnotationOnField() {
		assertAnnotationPresent(MyAnnotation.class, entity.getPersistentProperty("field"));
	}

	@Test // DATACMNS-269
	public void discoversAnnotationOnGetters() {
		assertAnnotationPresent(MyAnnotation.class, entity.getPersistentProperty("getter"));
	}

	@Test // DATACMNS-269
	public void discoversAnnotationOnSetters() {
		assertAnnotationPresent(MyAnnotation.class, entity.getPersistentProperty("setter"));
	}

	@Test // DATACMNS-269
	public void findsMetaAnnotation() {

		assertAnnotationPresent(MyId.class, entity.getPersistentProperty("id"));
		assertAnnotationPresent(Id.class, entity.getPersistentProperty("id"));
	}

	@Test // DATACMNS-282
	public void populatesAnnotationCacheWithDirectAnnotationsOnCreation() {

		assertThat(entity.getPersistentProperty("meta")).satisfies(property -> {

			// Assert direct annotations are cached on construction
			Map<Class<? extends Annotation>, Annotation> cache = getAnnotationCache(property);
			assertThat(cache.containsKey(MyAnnotationAsMeta.class)).isTrue();
			assertThat(cache.containsKey(MyAnnotation.class)).isFalse();

			// Assert meta annotation is found and cached
			assertThat(property.findAnnotation(MyAnnotation.class))
					.satisfies(annotation -> assertThat(cache.containsKey(MyAnnotation.class)).isTrue());
		});
	}

	@Test // DATACMNS-282
	public void discoversAmbiguousMappingUsingDirectAnnotationsOnAccessors() {

		try {
			context.getPersistentEntity(InvalidSample.class);
			fail("Expected MappingException!");
		} catch (MappingException o_O) {
			assertThat(context.hasPersistentEntityFor(InvalidSample.class)).isFalse();
		}
	}

	@Test // DATACMNS-243
	public void defaultsToFieldAccess() {

		assertThat(getProperty(FieldAccess.class, "name")).satisfies(it -> assertThat(it.usePropertyAccess()).isFalse());
	}

	@Test // DATACMNS-243
	public void usesAccessTypeDeclaredOnTypeAsDefault() {

		assertThat(getProperty(PropertyAccess.class, "firstname"))
				.satisfies(it -> assertThat(it.usePropertyAccess()).isTrue());
	}

	@Test // DATACMNS-243
	public void propertyAnnotationOverridesTypeConfiguration() {

		assertThat(getProperty(PropertyAccess.class, "lastname"))
				.satisfies(it -> assertThat(it.usePropertyAccess()).isFalse());
	}

	@Test // DATACMNS-243
	public void fieldAnnotationOverridesTypeConfiguration() {

		assertThat(getProperty(PropertyAccess.class, "emailAddress"))
				.satisfies(it -> assertThat(it.usePropertyAccess()).isFalse());
	}

	@Test // DATACMNS-243
	public void doesNotRejectSameAnnotationIfItsEqualOnBothFieldAndAccessor() {
		context.getPersistentEntity(AnotherInvalidSample.class);
	}

	@Test // DATACMNS-534
	public void treatsNoAnnotationCorrectly() {

		assertThat(getProperty(ClassWithReadOnlyProperties.class, "noAnnotations"))
				.satisfies(it -> assertThat(it.isWritable()).isTrue());
	}

	@Test // DATACMNS-534
	public void treatsTransientAsNotExisting() {
		assertThat(getProperty(ClassWithReadOnlyProperties.class, "transientProperty")).isNull();
	}

	@Test // DATACMNS-534
	public void treatsReadOnlyAsNonWritable() {

		assertThat(getProperty(ClassWithReadOnlyProperties.class, "readOnlyProperty"))
				.satisfies(it -> assertThat(it.isWritable()).isFalse());
	}

	@Test // DATACMNS-534
	public void considersPropertyWithReadOnlyMetaAnnotationReadOnly() {

		assertThat(getProperty(ClassWithReadOnlyProperties.class, "customReadOnlyProperty"))
				.satisfies(it -> assertThat(it.isWritable()).isFalse());
	}

	@Test // DATACMNS-556
	public void doesNotRejectNonSpringDataAnnotationsUsedOnBothFieldAndAccessor() {
		getProperty(TypeWithCustomAnnotationsOnBothFieldAndAccessor.class, "field");
	}

	@Test // DATACMNS-677
	@SuppressWarnings("unchecked")
	public void cachesNonPresenceOfAnnotationOnField() {

		SamplePersistentProperty property = getProperty(Sample.class, "getterWithoutField");

		assertThat(property).satisfies(it -> {

			assertThat(it.findAnnotation(MyAnnotation.class)).isNull();

			Map<Class<?>, ?> field = (Map<Class<?>, ?>) ReflectionTestUtils.getField(it, "annotationCache");

			assertThat(field.containsKey(MyAnnotation.class)).isTrue();
			assertThat(field.get(MyAnnotation.class)).isEqualTo(Optional.empty());
		});
	}

	@Test // DATACMNS-825
	public void composedAnnotationWithAliasForGetCachedCorrectly() {

		assertThat(entity.getPersistentProperty("metaAliased")).satisfies(property -> {

			// Assert direct annotations are cached on construction
			Map<Class<? extends Annotation>, Annotation> cache = getAnnotationCache(property);
			assertThat(cache.containsKey(MyComposedAnnotationUsingAliasFor.class)).isTrue();
			assertThat(cache.containsKey(MyAnnotation.class)).isFalse();

			// Assert meta annotation is found and cached
			assertThat(property.findAnnotation(MyAnnotation.class))
					.satisfies(it -> assertThat(cache.containsKey(MyAnnotation.class)).isTrue());
		});
	}

	@Test // DATACMNS-825
	public void composedAnnotationWithAliasShouldHaveSynthesizedAttributeValues() {

		assertThat(entity.getPersistentProperty("metaAliased"))
				.satisfies(property -> assertThat(property.findAnnotation(MyAnnotation.class))
						.satisfies(annotation -> assertThat(AnnotationUtils.getValue(annotation)).isEqualTo("spring")));
	}

	@Test // DATACMNS-867
	public void revisedAnnotationWithAliasShouldHaveSynthesizedAttributeValues() {

		assertThat(entity.getPersistentProperty("setter")).satisfies(
				property -> assertThat(property.findAnnotation(RevisedAnnnotationWithAliasFor.class)).satisfies(annotation -> {
					assertThat(annotation.name()).isEqualTo("my-value");
					assertThat(annotation.value()).isEqualTo("my-value");
				}));
	}

	@Test // DATACMNS-1141
	public void getRequiredAnnotationReturnsAnnotation() {

		PersistentProperty property = getProperty(Sample.class, "id");

		assertThat(property.getRequiredAnnotation(Id.class)).isNotNull();
	}

	@Test // DATACMNS-1141
	public void getRequiredAnnotationThrowsException() {

		PersistentProperty property = getProperty(Sample.class, "id");

		assertThatThrownBy(() -> property.getRequiredAnnotation(Transient.class)).isInstanceOf(IllegalStateException.class);
	}

	@Test // DATACMNS-1318
	public void detectsUltimateAssociationTargetClass() {

		Stream.of("toSample", "toSample2", "sample", "withoutAnnotation").forEach(it -> {
			assertThat(getProperty(WithReferences.class, it).getAssociationTargetType()).isEqualTo(Sample.class);
		});
	}

	@Test // DATACMNS-1359
	public void missingRequiredGetterThrowsException() {

		SamplePersistentProperty property = getProperty(Sample.class, "field");

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> property.getRequiredGetter()) //
				.withMessageContaining("field") //
				.withMessageContaining(Sample.class.getName());
	}

	@Test // DATACMNS-1359
	public void missingRequiredSetterThrowsException() {

		SamplePersistentProperty property = getProperty(Sample.class, "field");

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> property.getRequiredSetter()) //
				.withMessageContaining("field") //
				.withMessageContaining(Sample.class.getName());
	}

	@Test // DATACMNS-1359
	public void missingRequiredWitherThrowsException() {

		SamplePersistentProperty property = getProperty(Sample.class, "field");

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> property.getRequiredWither()) //
				.withMessageContaining("field") //
				.withMessageContaining(Sample.class.getName());
	}

	@Test
	public void missingRequiredFieldThrowsException() {

		SamplePersistentProperty property = getProperty(NoField.class, "firstname");

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> property.getRequiredField()) //
				.withMessageContaining("firstname") //
				.withMessageContaining(NoField.class.getName());
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Annotation>, Annotation> getAnnotationCache(SamplePersistentProperty property) {
		return (Map<Class<? extends Annotation>, Annotation>) ReflectionTestUtils.getField(property, "annotationCache");
	}

	private <A extends Annotation> A assertAnnotationPresent(Class<A> annotationType,
			AnnotationBasedPersistentProperty<?> property) {

		A annotation = property.findAnnotation(annotationType);

		assertThat(annotation).isNotNull();

		return annotation;
	}

	private SamplePersistentProperty getProperty(Class<?> type, String name) {
		return context.getRequiredPersistentEntity(type).getPersistentProperty(name);
	}

	static class Sample {

		@MyId String id;

		@MyAnnotation String field;
		String getter;
		@RevisedAnnnotationWithAliasFor(value = "my-value") String setter;
		String doubleMapping;

		@MyAnnotationAsMeta String meta;

		@MyComposedAnnotationUsingAliasFor String metaAliased;

		@MyAnnotation
		public String getGetter() {
			return getter;
		}

		@MyAnnotation
		public void setSetter(String setter) {
			this.setter = setter;
		}

		@MyAnnotation
		public String getDoubleMapping() {
			return doubleMapping;
		}

		@MyAnnotation
		public void setDoubleMapping(String doubleMapping) {
			this.doubleMapping = doubleMapping;
		}

		@AccessType(Type.PROPERTY)
		public Object getGetterWithoutField() {
			return null;
		}

		public void setGetterWithoutField(Object object) {}
	}

	static class InvalidSample {

		String meta;

		@MyAnnotation
		public String getMeta() {
			return meta;
		}

		@MyAnnotation("foo")
		public void setMeta(String meta) {
			this.meta = meta;
		}
	}

	static class AnotherInvalidSample {

		@MyAnnotation String property;

		@MyAnnotation
		public String getProperty() {
			return property;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = { FIELD, METHOD, ANNOTATION_TYPE })
	public @interface MyAnnotation {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = { FIELD, METHOD })
	@MyAnnotation
	public @interface MyAnnotationAsMeta {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = { FIELD, METHOD })
	@MyAnnotation
	public @interface MyComposedAnnotationUsingAliasFor {

		@AliasFor(annotation = MyAnnotation.class, attribute = "value")
		String name() default "spring";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = { FIELD, METHOD })
	@interface RevisedAnnnotationWithAliasFor {

		@AliasFor("value")
		String name() default "";

		@AliasFor("name")
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = { FIELD, METHOD, ANNOTATION_TYPE })
	@Id
	public @interface MyId {
	}

	static class FieldAccess {
		String name;
	}

	@AccessType(Type.PROPERTY)
	static class PropertyAccess {

		String firstname, lastname;
		@AccessType(Type.FIELD) String emailAddress;

		public String getFirstname() {
			return firstname;
		}

		@AccessType(Type.FIELD)
		public String getLastname() {
			return lastname;
		}
	}

	static class ClassWithReadOnlyProperties {

		String noAnnotations;

		@Transient String transientProperty;

		@ReadOnlyProperty String readOnlyProperty;

		@CustomReadOnly String customReadOnlyProperty;
	}

	static class TypeWithCustomAnnotationsOnBothFieldAndAccessor {

		@Nullable String field;

		@Nullable
		public String getField() {
			return field;
		}
	}

	@ReadOnlyProperty
	@Retention(RetentionPolicy.RUNTIME)
	@Target(FIELD)
	@interface CustomReadOnly {

	}

	static class WithReferences {

		@Reference(to = Sample.class) String toSample;
		@Reference(Sample.class) String toSample2;
		@Reference Sample sample;
		Sample withoutAnnotation;
	}

	interface NoField {

		String getFirstname();
	}
}
