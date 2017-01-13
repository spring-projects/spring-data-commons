/*
 * Copyright 2013-2017 the original author or authors.
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
package org.springframework.data.mapping.model;

import static java.lang.annotation.ElementType.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AnnotationBasedPersistentProperty}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class AnnotationBasedPersistentPropertyUnitTests<P extends AnnotationBasedPersistentProperty<P>> {

	BasicPersistentEntity<Object, SamplePersistentProperty> entity;
	SampleMappingContext context;

	@Before
	public void setUp() {

		context = new SampleMappingContext();
		entity = context.getPersistentEntity(Sample.class);
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

		SamplePersistentProperty property = entity.getPersistentProperty("meta");

		// Assert direct annotations are cached on construction
		Map<Class<? extends Annotation>, Annotation> cache = getAnnotationCache(property);
		assertThat(cache.containsKey(MyAnnotationAsMeta.class), is(true));
		assertThat(cache.containsKey(MyAnnotation.class), is(false));

		// Assert meta annotation is found and cached
		MyAnnotation annotation = property.findAnnotation(MyAnnotation.class);
		assertThat(annotation, is(notNullValue()));
		assertThat(cache.containsKey(MyAnnotation.class), is(true));
	}

	@Test // DATACMNS-282
	public void discoversAmbiguousMappingUsingDirectAnnotationsOnAccessors() {

		try {
			context.getPersistentEntity(InvalidSample.class);
			fail("Expected MappingException!");
		} catch (MappingException o_O) {
			assertThat(context.hasPersistentEntityFor(InvalidSample.class), is(false));
		}
	}

	@Test // DATACMNS-243
	public void defaultsToFieldAccess() {
		assertThat(getProperty(FieldAccess.class, "name").usePropertyAccess(), is(false));
	}

	@Test // DATACMNS-243
	public void usesAccessTypeDeclaredOnTypeAsDefault() {
		assertThat(getProperty(PropertyAccess.class, "firstname").usePropertyAccess(), is(true));
	}

	@Test // DATACMNS-243
	public void propertyAnnotationOverridesTypeConfiguration() {
		assertThat(getProperty(PropertyAccess.class, "lastname").usePropertyAccess(), is(false));
	}

	@Test // DATACMNS-243
	public void fieldAnnotationOverridesTypeConfiguration() {
		assertThat(getProperty(PropertyAccess.class, "emailAddress").usePropertyAccess(), is(false));
	}

	@Test // DATACMNS-243
	public void doesNotRejectSameAnnotationIfItsEqualOnBothFieldAndAccessor() {
		context.getPersistentEntity(AnotherInvalidSample.class);
	}

	@Test // DATACMNS-534
	public void treatsNoAnnotationCorrectly() {
		assertThat(getProperty(ClassWithReadOnlyProperties.class, "noAnnotations").isWritable(), is(true));
	}

	@Test // DATACMNS-534
	public void treatsTransientAsNotExisting() {
		assertThat(getProperty(ClassWithReadOnlyProperties.class, "transientProperty"), nullValue());
	}

	@Test // DATACMNS-534
	public void treatsReadOnlyAsNonWritable() {
		assertThat(getProperty(ClassWithReadOnlyProperties.class, "readOnlyProperty").isWritable(), is(false));
	}

	@Test // DATACMNS-534
	public void considersPropertyWithReadOnlyMetaAnnotationReadOnly() {
		assertThat(getProperty(ClassWithReadOnlyProperties.class, "customReadOnlyProperty").isWritable(), is(false));
	}

	@Test // DATACMNS-556
	public void doesNotRejectNonSpringDataAnnotationsUsedOnBothFieldAndAccessor() {
		getProperty(TypeWithCustomAnnotationsOnBothFieldAndAccessor.class, "field");
	}

	@Test // DATACMNS-677
	@SuppressWarnings("unchecked")
	public void cachesNonPresenceOfAnnotationOnField() {

		SamplePersistentProperty property = getProperty(Sample.class, "getterWithoutField");

		assertThat(property.findAnnotation(MyAnnotation.class), is(nullValue()));

		Map<Class<?>, ?> field = (Map<Class<?>, ?>) ReflectionTestUtils.getField(property, "annotationCache");

		assertThat(field.containsKey(MyAnnotation.class), is(true));
		assertThat(field.get(MyAnnotation.class), is(nullValue()));
	}

	@Test // DATACMNS-825
	public void composedAnnotationWithAliasForGetCachedCorrectly() {

		SamplePersistentProperty property = entity.getPersistentProperty("metaAliased");

		// Assert direct annotations are cached on construction
		Map<Class<? extends Annotation>, Annotation> cache = getAnnotationCache(property);
		assertThat(cache.containsKey(MyComposedAnnotationUsingAliasFor.class), is(true));
		assertThat(cache.containsKey(MyAnnotation.class), is(false));

		// Assert meta annotation is found and cached
		MyAnnotation annotation = property.findAnnotation(MyAnnotation.class);
		assertThat(annotation, is(notNullValue()));
		assertThat(cache.containsKey(MyAnnotation.class), is(true));
	}

	@Test // DATACMNS-825
	public void composedAnnotationWithAliasShouldHaveSynthesizedAttributeValues() {

		SamplePersistentProperty property = entity.getPersistentProperty("metaAliased");

		MyAnnotation annotation = property.findAnnotation(MyAnnotation.class);
		assertThat(AnnotationUtils.getValue(annotation), is((Object) "spring"));
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Annotation>, Annotation> getAnnotationCache(SamplePersistentProperty property) {
		return (Map<Class<? extends Annotation>, Annotation>) ReflectionTestUtils.getField(property, "annotationCache");
	}

	private <A extends Annotation> A assertAnnotationPresent(Class<A> annotationType,
			AnnotationBasedPersistentProperty<?> property) {

		A annotation = property.findAnnotation(annotationType);
		assertThat(annotation, is(notNullValue()));
		return annotation;
	}

	private SamplePersistentProperty getProperty(Class<?> type, String name) {
		return context.getPersistentEntity(type).getPersistentProperty(name);
	}

	static class Sample {

		@MyId String id;

		@MyAnnotation String field;
		String getter;
		String setter;
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
	public static @interface MyAnnotation {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = { FIELD, METHOD })
	@MyAnnotation
	public static @interface MyAnnotationAsMeta {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = { FIELD, METHOD })
	@MyAnnotation
	public static @interface MyComposedAnnotationUsingAliasFor {

		@AliasFor(annotation = MyAnnotation.class, attribute = "value")
		String name() default "spring";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = { FIELD, METHOD, ANNOTATION_TYPE })
	@Id
	public static @interface MyId {
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
	static @interface CustomReadOnly {

	}
}
