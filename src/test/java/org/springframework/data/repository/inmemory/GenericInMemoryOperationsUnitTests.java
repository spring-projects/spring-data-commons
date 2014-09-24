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
import static org.hamcrest.collection.IsIterableContainingInOrder.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * @author Christoph Strobl
 */
public abstract class GenericInMemoryOperationsUnitTests {

	private static final Foo FOO1 = new Foo("one");
	private static final Foo FOO2 = new Foo("two");
	private static final Bar BAR1 = new Bar("one");

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
		operations.create("1", FOO1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createShouldThrowExceptionForNullId() {
		operations.create(null, FOO1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createShouldThrowExceptionForNullObject() {
		operations.create("some-id", null);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void createShouldThrowExecptionWhenObjectOfSameTypeAlreadyExists() {

		operations.create("1", FOO1);
		operations.create("1", FOO2);
	}

	@Test
	public void createShouldWorkCorrectlyWhenObjectsOfDifferentTypesWithSameIdAreInserted() {

		operations.create("1", FOO1);
		operations.create("1", BAR1);
	}

	@Test
	public void readShouldReturnNullWhenNoElementsPresent() {
		assertNull(operations.read("1", Foo.class));
	}

	@Test
	public void readShouldReturnEntireCollection() {

		operations.create("1", FOO1);
		operations.create("2", FOO2);

		assertThat(operations.read(Foo.class), contains(FOO1, FOO2));
	}

	@Test
	public void readShouldReturnObjectWithMatchingIdAndType() {

		operations.create("1", FOO1);
		assertThat(operations.read("1", Foo.class), is(FOO1));
	}

	@Test
	public void readSouldReturnNullIfNoMatchingIdFound() {

		operations.create("1", FOO1);
		assertThat(operations.read("2", Foo.class), nullValue());
	}

	@Test
	public void readSouldReturnNullIfNoMatchingTypeFound() {

		operations.create("1", FOO1);
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
	public void updateShouldReplaceExistingObject() {

		operations.create("1", FOO1);
		operations.update("1", FOO2);
		assertThat(operations.read("1", Foo.class), is(FOO2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void updateShouldThrowExceptionWhenGivenNullId() {
		operations.update(null, FOO1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void updateShouldThrowExceptionWhenGivenNullObject() {
		operations.update("1", null);
	}

	@Test
	public void updateShouldRespectTypeInformation() {

		operations.create("1", FOO1);
		operations.update("1", BAR1);

		assertThat(operations.read("1", Foo.class), is(FOO1));
	}

	@Test
	public void deleteShouldRemoveObjectCorrectly() {

		operations.create("1", FOO1);
		operations.delete("1", Foo.class);
		assertThat(operations.read("1", Foo.class), nullValue());
	}

	@Test
	public void deleteReturnsNullWhenNotExisting() {

		operations.create("1", FOO1);
		assertThat(operations.delete("2", Foo.class), nullValue());
	}

	@Test
	public void deleteReturnsRemovedObject() {

		operations.create("1", FOO1);
		assertThat(operations.delete("1", Foo.class), is(FOO1));
	}

	@Test
	public void countShouldReturnZeroWhenNoElementsPresent() {
		operations.count(Foo.class);
	}

	@Test
	public void countShouldReturnCollectionSize() {

		operations.create("1", FOO1);
		operations.create("2", FOO1);
		operations.create("1", BAR1);

		assertThat(operations.count(Foo.class), is(2L));
		assertThat(operations.count(Bar.class), is(1L));
	}

	@Test(expected = IllegalArgumentException.class)
	public void countShouldThrowErrorOnNullType() {
		operations.count(null);
	}

	@Test
	public void readMatching() {

		operations.create("1", FOO1);
		operations.create("2", FOO2);

		List<Foo> result = (List<Foo>) operations.read(getInMemoryQuery(), Foo.class);
		assertThat(result, hasSize(1));
		assertThat(result.get(0), is(FOO2));
	}

	@Test
	public void countShouldReturnNumberMatchingElements() {

		operations.create("1", FOO1);
		operations.create("2", FOO2);

		assertThat(operations.count(getInMemoryQuery(), Foo.class), is(1L));
	}

	protected abstract InMemoryOperations getInMemoryOperations();

	protected abstract InMemoryQuery getInMemoryQuery();

	static class Foo {

		String foo;

		public Foo(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return foo;
		}

	}

	static class Bar {
		String bar;

		public Bar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return bar;
		}

	}

}
