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

import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.collection.IsEmptyCollection.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.hamcrest.core.IsSame.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.keyvalue.annotation.KeySpace;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.keyvalue.map.MapKeyValueAdapter;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 */
public class KeyValueTemplateTests {

	private static final Foo FOO_ONE = new Foo("one");
	private static final Foo FOO_TWO = new Foo("two");
	private static final Foo FOO_THREE = new Foo("three");
	private static final Bar BAR_ONE = new Bar("one");
	private static final ClassWithTypeAlias ALIASED = new ClassWithTypeAlias("super");
	private static final SubclassOfAliasedType SUBCLASS_OF_ALIASED = new SubclassOfAliasedType("sub");

	private static final KeyValueQuery<String> STRING_QUERY = new KeyValueQuery<String>("foo == 'two'");

	private KeyValueTemplate operations;

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException {
		this.operations = new KeyValueTemplate(new MapKeyValueAdapter());
	}

	@After
	public void tearDown() throws Exception {
		this.operations.destroy();
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void insertShouldNotThorwErrorWhenExecutedHavingNonExistingIdAndNonNullValue() {
		operations.insert("1", FOO_ONE);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void insertShouldThrowExceptionForNullId() {
		operations.insert(null, FOO_ONE);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void insertShouldThrowExceptionForNullObject() {
		operations.insert("some-id", null);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void insertShouldThrowExecptionWhenObjectOfSameTypeAlreadyExists() {

		operations.insert("1", FOO_ONE);
		operations.insert("1", FOO_TWO);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void insertShouldWorkCorrectlyWhenObjectsOfDifferentTypesWithSameIdAreInserted() {

		operations.insert("1", FOO_ONE);
		operations.insert("1", BAR_ONE);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void createShouldReturnSameInstanceGenerateId() {

		ClassWithStringId source = new ClassWithStringId();
		ClassWithStringId target = operations.insert(source);

		assertThat(target, sameInstance(source));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void createShouldRespectExistingId() {

		ClassWithStringId source = new ClassWithStringId();
		source.id = "one";

		operations.insert(source);

		assertThat(operations.findById("one", ClassWithStringId.class), is(source));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findByIdShouldReturnObjectWithMatchingIdAndType() {

		operations.insert("1", FOO_ONE);
		assertThat(operations.findById("1", Foo.class), is(FOO_ONE));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findByIdSouldReturnNullIfNoMatchingIdFound() {

		operations.insert("1", FOO_ONE);
		assertThat(operations.findById("2", Foo.class), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findByIdShouldReturnNullIfNoMatchingTypeFound() {

		operations.insert("1", FOO_ONE);
		assertThat(operations.findById("1", Bar.class), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findShouldExecuteQueryCorrectly() {

		operations.insert("1", FOO_ONE);
		operations.insert("2", FOO_TWO);

		List<Foo> result = (List<Foo>) operations.find(STRING_QUERY, Foo.class);
		assertThat(result, hasSize(1));
		assertThat(result.get(0), is(FOO_TWO));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void readShouldReturnEmptyCollectionIfOffsetOutOfRange() {

		operations.insert("1", FOO_ONE);
		operations.insert("2", FOO_TWO);
		operations.insert("3", FOO_THREE);

		assertThat(operations.findInRange(5, 5, Foo.class), empty());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void updateShouldReplaceExistingObject() {

		operations.insert("1", FOO_ONE);
		operations.update("1", FOO_TWO);
		assertThat(operations.findById("1", Foo.class), is(FOO_TWO));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void updateShouldRespectTypeInformation() {

		operations.insert("1", FOO_ONE);
		operations.update("1", BAR_ONE);

		assertThat(operations.findById("1", Foo.class), is(FOO_ONE));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void deleteShouldRemoveObjectCorrectly() {

		operations.insert("1", FOO_ONE);
		operations.delete("1", Foo.class);
		assertThat(operations.findById("1", Foo.class), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void deleteReturnsNullWhenNotExisting() {

		operations.insert("1", FOO_ONE);
		assertThat(operations.delete("2", Foo.class), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void deleteReturnsRemovedObject() {

		operations.insert("1", FOO_ONE);
		assertThat(operations.delete("1", Foo.class), is(FOO_ONE));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void deleteThrowsExceptionWhenIdCannotBeExctracted() {
		operations.delete(FOO_ONE);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void countShouldReturnZeroWhenNoElementsPresent() {
		assertThat(operations.count(Foo.class), is(0L));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void insertShouldRespectTypeAlias() {

		operations.insert("1", ALIASED);
		operations.insert("2", SUBCLASS_OF_ALIASED);

		assertThat(operations.findAllOf(ALIASED.getClass()), containsInAnyOrder(ALIASED, SUBCLASS_OF_ALIASED));
	}

	static class Foo implements Serializable {

		private static final long serialVersionUID = -8912754229220128922L;

		String foo;

		public Foo(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return foo;
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.foo);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Foo)) {
				return false;
			}
			Foo other = (Foo) obj;
			return ObjectUtils.nullSafeEquals(this.foo, other.foo);
		}

	}

	static class Bar implements Serializable {

		private static final long serialVersionUID = 196011921826060210L;
		String bar;

		public Bar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return bar;
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.bar);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Bar)) {
				return false;
			}
			Bar other = (Bar) obj;
			return ObjectUtils.nullSafeEquals(this.bar, other.bar);
		}

	}

	static class ClassWithStringId implements Serializable {

		private static final long serialVersionUID = -7481030649267602830L;
		@Id String id;
		String value;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ObjectUtils.nullSafeHashCode(this.id);
			result = prime * result + ObjectUtils.nullSafeHashCode(this.value);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ClassWithStringId)) {
				return false;
			}
			ClassWithStringId other = (ClassWithStringId) obj;
			if (!ObjectUtils.nullSafeEquals(this.id, other.id)) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(this.value, other.value)) {
				return false;
			}
			return true;
		}

	}

	@ExplicitKeySpace(name = "aliased")
	static class ClassWithTypeAlias implements Serializable {

		private static final long serialVersionUID = -5921943364908784571L;
		@Id String id;
		String name;

		public ClassWithTypeAlias(String name) {
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ObjectUtils.nullSafeHashCode(this.id);
			result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ClassWithTypeAlias)) {
				return false;
			}
			ClassWithTypeAlias other = (ClassWithTypeAlias) obj;
			if (!ObjectUtils.nullSafeEquals(this.id, other.id)) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(this.name, other.name)) {
				return false;
			}
			return true;
		}

	}

	static class SubclassOfAliasedType extends ClassWithTypeAlias {

		private static final long serialVersionUID = -468809596668871479L;

		public SubclassOfAliasedType(String name) {
			super(name);
		}

	}

	@Documented
	@Persistent
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	private static @interface ExplicitKeySpace {

		@KeySpace
		String name() default "";

	}
}
