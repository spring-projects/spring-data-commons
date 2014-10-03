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
package org.springframework.data.repository.inmemory;

import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.collection.IsEmptyCollection.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.hamcrest.core.IsSame.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 */
public abstract class GenericInMemoryOperationsUnitTests {

	private static final Foo FOO_ONE = new Foo("one");
	private static final Foo FOO_TWO = new Foo("two");
	private static final Foo FOO_THREE = new Foo("three");
	private static final Bar BAR_ONE = new Bar("one");
	private static final ClassWithTypeAlias ALIASED = new ClassWithTypeAlias("super");
	private static final SubclassOfAliasedType SUBCLASS_OF_ALIASED = new SubclassOfAliasedType("sub");

	private InMemoryOperations operations;

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException {
		this.operations = getInMemoryOperations();
	}

	@After
	public void tearDown() throws Exception {
		this.operations.destroy();
	}

	@Test
	public void createShouldNotThorwErrorWhenExecutedHavingNonExistingIdAndNonNullValue() {
		operations.create("1", FOO_ONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createShouldThrowExceptionForNullId() {
		operations.create(null, FOO_ONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createShouldThrowExceptionForNullObject() {
		operations.create("some-id", null);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void createShouldThrowExecptionWhenObjectOfSameTypeAlreadyExists() {

		operations.create("1", FOO_ONE);
		operations.create("1", FOO_TWO);
	}

	@Test
	public void createShouldWorkCorrectlyWhenObjectsOfDifferentTypesWithSameIdAreInserted() {

		operations.create("1", FOO_ONE);
		operations.create("1", BAR_ONE);
	}

	@Test
	public void createShouldGenerateId() {

		ClassWithStringId target = operations.create(new ClassWithStringId());

		assertThat(target.id, notNullValue());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void createShouldThrowErrorWhenIdCannotBeResolved() {
		operations.create(FOO_ONE);
	}

	@Test
	public void createShouldReturnSameInstanceGenerateId() {

		ClassWithStringId source = new ClassWithStringId();
		ClassWithStringId target = operations.create(source);

		assertThat(target, sameInstance(source));
	}

	@Test
	public void createShouldRespectExistingId() {

		ClassWithStringId source = new ClassWithStringId();
		source.id = "one";

		operations.create(source);

		assertThat(operations.read("one", ClassWithStringId.class), is(source));
	}

	@Test
	public void readShouldReturnNullWhenNoElementsPresent() {
		assertNull(operations.read("1", Foo.class));
	}

	@Test
	public void readShouldReturnEntireCollection() {

		operations.create("1", FOO_ONE);
		operations.create("2", FOO_TWO);

		assertThat(operations.read(Foo.class), containsInAnyOrder(FOO_ONE, FOO_TWO));
	}

	@Test
	public void readShouldReturnObjectWithMatchingIdAndType() {

		operations.create("1", FOO_ONE);
		assertThat(operations.read("1", Foo.class), is(FOO_ONE));
	}

	@Test
	public void readSouldReturnNullIfNoMatchingIdFound() {

		operations.create("1", FOO_ONE);
		assertThat(operations.read("2", Foo.class), nullValue());
	}

	@Test
	public void readSouldReturnNullIfNoMatchingTypeFound() {

		operations.create("1", FOO_ONE);
		assertThat(operations.read("1", Bar.class), nullValue());
	}

	@Test(expected = IllegalArgumentException.class)
	public void readShouldThrowExceptionWhenGivenNullType() {
		operations.read(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void readShouldThrowExceptionWhenGivenNullId() {
		operations.read((Serializable) null, Foo.class);
	}

	@Test
	public void readMatching() {

		operations.create("1", FOO_ONE);
		operations.create("2", FOO_TWO);

		List<Foo> result = (List<Foo>) operations.read(getInMemoryQuery(), Foo.class);
		assertThat(result, hasSize(1));
		assertThat(result.get(0), is(FOO_TWO));
	}

	@Test
	public void readShouldRespectOffset() {

		operations.create("1", FOO_ONE);
		operations.create("2", FOO_TWO);
		operations.create("3", FOO_THREE);

		assertThat(operations.read(1, 5, Foo.class), hasSize(2));
	}

	@Test
	public void readShouldReturnEmptyCollectionIfOffsetOutOfRange() {

		operations.create("1", FOO_ONE);
		operations.create("2", FOO_TWO);
		operations.create("3", FOO_THREE);

		assertThat(operations.read(5, 5, Foo.class), empty());
	}

	@Test
	public void updateShouldReplaceExistingObject() {

		operations.create("1", FOO_ONE);
		operations.update("1", FOO_TWO);
		assertThat(operations.read("1", Foo.class), is(FOO_TWO));
	}

	@Test(expected = IllegalArgumentException.class)
	public void updateShouldThrowExceptionWhenGivenNullId() {
		operations.update(null, FOO_ONE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void updateShouldThrowExceptionWhenGivenNullObject() {
		operations.update("1", null);
	}

	@Test
	public void updateShouldRespectTypeInformation() {

		operations.create("1", FOO_ONE);
		operations.update("1", BAR_ONE);

		assertThat(operations.read("1", Foo.class), is(FOO_ONE));
	}

	@Test
	public void updateShouldUseExtractedIdInformation() {

		ClassWithStringId source = new ClassWithStringId();

		ClassWithStringId saved = operations.create(source);
		saved.value = "foo";

		operations.update(saved);

		assertThat(operations.read(saved.id, ClassWithStringId.class), is(saved));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void updateShouldThrowErrorWhenIdInformationCannotBeExtracted() {
		operations.update(FOO_ONE);
	}

	@Test
	public void deleteShouldRemoveObjectCorrectly() {

		operations.create("1", FOO_ONE);
		operations.delete("1", Foo.class);
		assertThat(operations.read("1", Foo.class), nullValue());
	}

	@Test
	public void deleteReturnsNullWhenNotExisting() {

		operations.create("1", FOO_ONE);
		assertThat(operations.delete("2", Foo.class), nullValue());
	}

	@Test
	public void deleteReturnsRemovedObject() {

		operations.create("1", FOO_ONE);
		assertThat(operations.delete("1", Foo.class), is(FOO_ONE));
	}

	@Test
	public void deleteRemovesObjectUsingExtractedId() {

		ClassWithStringId source = new ClassWithStringId();
		operations.create(source);

		assertThat(operations.delete(source), is(source));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void deleteThrowsExceptionWhenIdCannotBeExctracted() {
		operations.delete(FOO_ONE);
	}

	@Test
	public void countShouldReturnZeroWhenNoElementsPresent() {
		operations.count(Foo.class);
	}

	@Test
	public void countShouldReturnCollectionSize() {

		operations.create("1", FOO_ONE);
		operations.create("2", FOO_ONE);
		operations.create("1", BAR_ONE);

		assertThat(operations.count(Foo.class), is(2L));
		assertThat(operations.count(Bar.class), is(1L));
	}

	@Test(expected = IllegalArgumentException.class)
	public void countShouldThrowErrorOnNullType() {
		operations.count(null);
	}

	@Test
	public void countShouldReturnNumberMatchingElements() {

		operations.create("1", FOO_ONE);
		operations.create("2", FOO_TWO);

		assertThat(operations.count(getInMemoryQuery(), Foo.class), is(1L));
	}

	@Test
	public void putShouldRespectTypeAlias() {

		operations.create("1", ALIASED);
		operations.create("2", SUBCLASS_OF_ALIASED);

		assertThat(operations.read(ALIASED.getClass()), containsInAnyOrder(ALIASED, SUBCLASS_OF_ALIASED));
	}

	@Test
	public void getAllOfShouldRespectTypeAliasAndFilterNonMatchingTypes() {

		operations.create("1", ALIASED);
		operations.create("2", SUBCLASS_OF_ALIASED);

		assertThat(operations.read(SUBCLASS_OF_ALIASED.getClass()), containsInAnyOrder(SUBCLASS_OF_ALIASED));
	}

	@Test
	public void getSouldRespectTypeAliasAndFilterNonMatching() {

		operations.create("1", ALIASED);
		assertThat(operations.read("1", SUBCLASS_OF_ALIASED.getClass()), nullValue());
	}

	protected abstract InMemoryOperations getInMemoryOperations();

	protected abstract InMemoryQuery getInMemoryQuery();

	static class Foo implements Serializable {

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

	@TypeAlias("aliased")
	static class ClassWithTypeAlias implements Serializable {

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

		public SubclassOfAliasedType(String name) {
			super(name);
		}

	}
}
