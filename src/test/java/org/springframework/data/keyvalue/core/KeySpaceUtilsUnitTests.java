/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.keyvalue.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.keyvalue.core.KeySpaceUtils.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.keyvalue.annotation.KeySpace;
import org.springframework.data.keyvalue.core.KeyValueTemplateUnitTests.AliasedEntity;
import org.springframework.data.keyvalue.core.KeyValueTemplateUnitTests.ClassWithDirectKeySpaceAnnotation;
import org.springframework.data.keyvalue.core.KeyValueTemplateUnitTests.EntityWithPersistentAnnotation;
import org.springframework.data.keyvalue.core.KeyValueTemplateUnitTests.Foo;

/**
 * Unit tests for {@link KeySpaceUtils}.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
public class KeySpaceUtilsUnitTests {

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void shouldResolveKeySpaceDefaultValueCorrectly() {
		assertThat(getKeySpace(EntityWithDefaultKeySpace.class), is((Object) "daenerys"));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void shouldResolveKeySpaceCorrectly() {
		assertThat(getKeySpace(EntityWithSetKeySpace.class), is((Object) "viserys"));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void shouldReturnNullWhenNoKeySpaceFoundOnComposedPersistentAnnotation() {
		assertThat(getKeySpace(AliasedEntity.class), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void shouldReturnNullWhenPersistentIsFoundOnNonComposedAnnotation() {
		assertThat(getKeySpace(EntityWithPersistentAnnotation.class), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void shouldReturnNullWhenPersistentIsNotFound() {
		assertThat(getKeySpace(Foo.class), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void shouldResolveInheritedKeySpaceCorrectly() {
		assertThat(getKeySpace(EntityWithInheritedKeySpace.class), is((Object) "viserys"));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void shouldResolveDirectKeySpaceAnnotationCorrectly() {
		assertThat(getKeySpace(ClassWithDirectKeySpaceAnnotation.class), is((Object) "rhaegar"));
	}

	@PersistentAnnotationWithExplicitKeySpace
	static class EntityWithDefaultKeySpace {

	}

	@PersistentAnnotationWithExplicitKeySpace(firstname = "viserys")
	static class EntityWithSetKeySpace {

	}

	static class EntityWithInheritedKeySpace extends EntityWithSetKeySpace {

	}

	@Persistent
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	static @interface PersistentAnnotationWithExplicitKeySpace {

		@KeySpace
		String firstname() default "daenerys";

		String lastnamne() default "targaryen";
	}

	@Persistent
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	static @interface ExplicitKeySpace {

		@KeySpace
		String name() default "";
	}
}
