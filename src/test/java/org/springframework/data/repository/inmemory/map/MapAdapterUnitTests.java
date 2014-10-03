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
package org.springframework.data.repository.inmemory.map;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class MapAdapterUnitTests {

	private MapAdapter adapter;
	private static final String COLLECTION_1 = "collection-1";
	private static final String COLLECTION_2 = "collection-2";
	private static final Object OBJECT_1 = new Object();
	private static final Object OBJECT_2 = new Object();
	private static final String STRING_1 = new String("1");

	@Before
	public void setUp() {
		this.adapter = new MapAdapter();
	}

	@Test(expected = IllegalArgumentException.class)
	public void putShouldThrowExceptionWhenAddingNullId() {
		adapter.put(null, OBJECT_1, COLLECTION_1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void putShouldThrowExceptionWhenCollectionIsNullValue() {
		adapter.put("1", OBJECT_1, null);
	}

	@Test
	public void putReturnsNullWhenNoObjectForIdPresent() {
		assertThat(adapter.put("1", OBJECT_1, COLLECTION_1), nullValue());
	}

	@Test
	public void putShouldReturnPreviousObjectForIdWhenAddingNewOneWithSameIdPresent() {

		adapter.put("1", OBJECT_1, COLLECTION_1);
		assertThat(adapter.put("1", OBJECT_2, COLLECTION_1), is(OBJECT_1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void containsShouldThrowExceptionWhenIdIsNull() {
		adapter.contains(null, COLLECTION_1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void containsShouldThrowExceptionWhenTypeIsNull() {
		adapter.contains("", null);
	}

	@Test
	public void containsShouldReturnFalseWhenNoElementsPresent() {
		assertThat(adapter.contains("1", COLLECTION_1), is(false));
	}

	@Test
	public void containShouldReturnTrueWhenElementWithIdPresent() {

		adapter.put("1", OBJECT_1, COLLECTION_1);
		assertThat(adapter.contains("1", COLLECTION_1), is(true));
	}

	@Test
	public void getShouldReturnNullWhenNoElementWithIdPresent() {
		assertThat(adapter.get("1", COLLECTION_1), nullValue());
	}

	@Test
	public void getShouldReturnElementWhenMatchingIdPresent() {

		adapter.put("1", OBJECT_1, COLLECTION_1);
		assertThat(adapter.get("1", COLLECTION_1), is(OBJECT_1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShouldThrowExceptionWhenIdIsNull() {
		adapter.get(null, COLLECTION_1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShouldThrowExceptionWhenTypeIsNull() {
		adapter.get("1", null);
	}

	@Test
	public void getAllOfShouldReturnAllValuesOfGivenCollection() {

		adapter.put("1", OBJECT_1, COLLECTION_1);
		adapter.put("2", OBJECT_2, COLLECTION_1);
		adapter.put("3", STRING_1, COLLECTION_2);

		assertThat(adapter.getAllOf(COLLECTION_1), containsInAnyOrder(OBJECT_1, OBJECT_2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getAllOfShouldThrowExceptionWhenTypeIsNull() {
		adapter.getAllOf(null);
	}

	@Test
	public void deleteShouldReturnNullWhenGivenIdThatDoesNotExist() {
		assertThat(adapter.delete("1", COLLECTION_1), nullValue());
	}

	@Test
	public void deleteShouldReturnDeletedObject() {

		adapter.put("1", OBJECT_1, COLLECTION_1);
		assertThat(adapter.delete("1", COLLECTION_1), is(OBJECT_1));
	}

}
