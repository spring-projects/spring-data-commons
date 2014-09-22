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
package org.springframework.data.keyvalue.map;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 */
public class MapKeyValueAdapterUnitTests {

	private static final String COLLECTION_1 = "collection-1";
	private static final String COLLECTION_2 = "collection-2";
	private static final String STRING_1 = new String("1");

	private Object object1 = new SimpleObject("one");
	private Object object2 = new SimpleObject("two");

	private MapKeyValueAdapter adapter;

	@Before
	public void setUp() {
		this.adapter = new MapKeyValueAdapter();
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void putShouldThrowExceptionWhenAddingNullId() {
		adapter.put(null, object1, COLLECTION_1);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void putShouldThrowExceptionWhenCollectionIsNullValue() {
		adapter.put("1", object1, null);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void putReturnsNullWhenNoObjectForIdPresent() {
		assertThat(adapter.put("1", object1, COLLECTION_1), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void putShouldReturnPreviousObjectForIdWhenAddingNewOneWithSameIdPresent() {

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.put("1", object2, COLLECTION_1), equalTo(object1));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void containsShouldThrowExceptionWhenIdIsNull() {
		adapter.contains(null, COLLECTION_1);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void containsShouldThrowExceptionWhenTypeIsNull() {
		adapter.contains("", null);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void containsShouldReturnFalseWhenNoElementsPresent() {
		assertThat(adapter.contains("1", COLLECTION_1), is(false));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void containShouldReturnTrueWhenElementWithIdPresent() {

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.contains("1", COLLECTION_1), is(true));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void getShouldReturnNullWhenNoElementWithIdPresent() {
		assertThat(adapter.get("1", COLLECTION_1), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void getShouldReturnElementWhenMatchingIdPresent() {

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.get("1", COLLECTION_1), is(object1));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getShouldThrowExceptionWhenIdIsNull() {
		adapter.get(null, COLLECTION_1);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getShouldThrowExceptionWhenTypeIsNull() {
		adapter.get("1", null);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void getAllOfShouldReturnAllValuesOfGivenCollection() {

		adapter.put("1", object1, COLLECTION_1);
		adapter.put("2", object2, COLLECTION_1);
		adapter.put("3", STRING_1, COLLECTION_2);

		assertThat(adapter.getAllOf(COLLECTION_1), containsInAnyOrder(object1, object2));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getAllOfShouldThrowExceptionWhenTypeIsNull() {
		adapter.getAllOf(null);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void deleteShouldReturnNullWhenGivenIdThatDoesNotExist() {
		assertThat(adapter.delete("1", COLLECTION_1), nullValue());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void deleteShouldReturnDeletedObject() {

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.delete("1", COLLECTION_1), is(object1));
	}

	static class SimpleObject {

		protected String stringValue;

		public SimpleObject() {}

		SimpleObject(String value) {
			this.stringValue = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * ObjectUtils.nullSafeHashCode(this.stringValue);
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
			if (!(obj instanceof SimpleObject)) {
				return false;
			}
			SimpleObject that = (SimpleObject) obj;
			return ObjectUtils.nullSafeEquals(this.stringValue, that.stringValue);
		}
	}

}
