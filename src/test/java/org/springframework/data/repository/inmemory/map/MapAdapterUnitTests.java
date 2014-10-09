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
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.core.IsNot.*;
import static org.hamcrest.core.IsNull.*;
import static org.hamcrest.core.IsSame.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 */
@RunWith(Parameterized.class)
public class MapAdapterUnitTests {

	private static final String COLLECTION_1 = "collection-1";
	private static final String COLLECTION_2 = "collection-2";
	private static final String STRING_1 = new String("1");

	private Object object1;
	private Object object2;

	private MapAdapter adapter;

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { { new SimpleObject("one"), new SimpleObject("two") },
				{ new CloneableObject("one"), new CloneableObject("two") } });
	}

	public MapAdapterUnitTests(Object o1, Object o2) {
		this.object1 = o1;
		this.object2 = o2;
	}

	@Before
	public void setUp() {
		this.adapter = new MapAdapter();
	}

	@Test(expected = IllegalArgumentException.class)
	public void putShouldThrowExceptionWhenAddingNullId() {
		adapter.put(null, object1, COLLECTION_1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void putShouldThrowExceptionWhenCollectionIsNullValue() {
		adapter.put("1", object1, null);
	}

	@Test
	public void putReturnsNullWhenNoObjectForIdPresent() {
		assertThat(adapter.put("1", object1, COLLECTION_1), nullValue());
	}

	@Test
	public void putShouldReturnPreviousObjectForIdWhenAddingNewOneWithSameIdPresent() {

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.put("1", object2, COLLECTION_1), equalTo(object1));
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

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.contains("1", COLLECTION_1), is(true));
	}

	@Test
	public void getShouldReturnNullWhenNoElementWithIdPresent() {
		assertThat(adapter.get("1", COLLECTION_1), nullValue());
	}

	@Test
	public void getShouldReturnElementWhenMatchingIdPresent() {

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.get("1", COLLECTION_1), is(object1));
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

		adapter.put("1", object1, COLLECTION_1);
		adapter.put("2", object2, COLLECTION_1);
		adapter.put("3", STRING_1, COLLECTION_2);

		assertThat(adapter.getAllOf(COLLECTION_1), containsInAnyOrder(object1, object2));
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

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.delete("1", COLLECTION_1), is(object1));
	}

	@Test
	public void objectInAdapterShouldNotBeSameInstanceAsSourceObject() {

		adapter.put("1", object1, COLLECTION_1);
		assertThat(adapter.get("1", COLLECTION_1), not(sameInstance(object1)));
	}

	static class SimpleObject implements Serializable {

		private static final long serialVersionUID = -5360757012079566797L;
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

	static class CloneableObject extends SimpleObject implements Cloneable {

		private static final long serialVersionUID = -3584760574822342754L;

		public CloneableObject() {}

		public CloneableObject(String value) {
			super(value);
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {

			CloneableObject o = new CloneableObject();
			o.stringValue = this.stringValue;
			return o;
		}
	}

}
