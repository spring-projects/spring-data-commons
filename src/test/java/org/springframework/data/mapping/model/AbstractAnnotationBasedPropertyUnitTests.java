/*
 * Copyright 2013 the original author or authors.
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
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Oliver Gierke
 */
public class AbstractAnnotationBasedPropertyUnitTests<P extends AnnotationBasedPersistentProperty<P>> {

	BasicPersistentEntity<Object, SamplePersistentProperty> entity;
	SampleMappingContext context;

	@Before
	public void setUp() {

		context = new SampleMappingContext();
		entity = context.getPersistentEntity(Sample.class);
	}

	@Test
	public void discoversAnnotationOnField() {
		assertAnnotationPresent(MyAnnotation.class, entity.getPersistentProperty("field"));
	}

	@Test
	public void discoversAnnotationOnGetters() {
		assertAnnotationPresent(MyAnnotation.class, entity.getPersistentProperty("getter"));
	}

	@Test
	public void discoversAnnotationOnSetters() {
		assertAnnotationPresent(MyAnnotation.class, entity.getPersistentProperty("setter"));
	}

	@Test
	public void prefersAnnotationOnMethodsToOverride() {
		MyAnnotation annotation = assertAnnotationPresent(MyAnnotation.class, entity.getPersistentProperty("override"));
		assertThat(annotation.value(), is("method"));
	}

	@Test
	public void findsMetaAnnotation() {

		assertAnnotationPresent(MyId.class, entity.getPersistentProperty("id"));
		assertAnnotationPresent(Id.class, entity.getPersistentProperty("id"));
	}

	/**
	 * @see DATACMNS-282
	 */
	@Test
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

	/**
	 * @see DATACMNS-282
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void discoversAmbiguousMappingUsingDirectAnnotationsOnAccessors() {

		try {
			context.getPersistentEntity(InvalidSample.class);
			fail("Expected MappingException!");
		} catch (MappingException o_O) {
			ConcurrentMap<TypeInformation<?>, ?> entities = (ConcurrentMap<TypeInformation<?>, ?>) ReflectionTestUtils
					.getField(context, "persistentEntities");
			assertThat(entities.containsKey(ClassTypeInformation.from(InvalidSample.class)), is(false));
		}
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

	static class Sample {

		@MyId
		String id;

		@MyAnnotation
		String field;
		String getter;
		String setter;

		@MyAnnotationAsMeta
		String meta;

		@MyAnnotation("field")
		String override;

		@MyAnnotation
		public String getGetter() {
			return getter;
		}

		@MyAnnotation
		public void setSetter(String setter) {
			this.setter = setter;
		}

		@MyAnnotation("method")
		public String getOverride() {
			return override;
		}
	}

	static class InvalidSample {

		String meta;

		@MyAnnotation
		public String getMeta() {
			return meta;
		}

		@MyAnnotation
		public void setMeta(String meta) {
			this.meta = meta;
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
	@Target(value = { FIELD, METHOD, ANNOTATION_TYPE })
	@Id
	public static @interface MyId {
	}
}
