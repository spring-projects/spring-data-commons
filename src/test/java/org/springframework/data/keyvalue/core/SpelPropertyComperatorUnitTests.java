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

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.number.OrderingComparison.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class SpelPropertyComperatorUnitTests {

	private static final SomeType ONE = new SomeType("one", Integer.valueOf(1), 1);
	private static final SomeType TWO = new SomeType("two", Integer.valueOf(2), 2);
	private static final WrapperType WRAPPER_ONE = new WrapperType("w-one", ONE);
	private static final WrapperType WRAPPER_TWO = new WrapperType("w-two", TWO);

	@Test
	public void shouldCompareStringAscCorrectly() {

		assertThat(new SpelPropertyComperator<SomeType>("stringProperty").compare(ONE, TWO), is(ONE.getStringProperty()
				.compareTo(TWO.getStringProperty())));
	}

	@Test
	public void shouldCompareStringDescCorrectly() {

		assertThat(new SpelPropertyComperator<SomeType>("stringProperty").desc().compare(ONE, TWO), is(TWO
				.getStringProperty().compareTo(ONE.getStringProperty())));
	}

	@Test
	public void shouldCompareIntegerAscCorrectly() {

		assertThat(new SpelPropertyComperator<SomeType>("integerProperty").compare(ONE, TWO), is(ONE.getIntegerProperty()
				.compareTo(TWO.getIntegerProperty())));
	}

	@Test
	public void shouldCompareIntegerDescCorrectly() {

		assertThat(new SpelPropertyComperator<SomeType>("integerProperty").desc().compare(ONE, TWO), is(TWO
				.getIntegerProperty().compareTo(ONE.getIntegerProperty())));
	}

	@Test
	public void shouldComparePrimitiveIntegerAscCorrectly() {

		assertThat(new SpelPropertyComperator<SomeType>("primitiveProperty").compare(ONE, TWO),
				is(Integer.valueOf(ONE.getPrimitiveProperty()).compareTo(Integer.valueOf(TWO.getPrimitiveProperty()))));
	}

	@Test
	public void shouldNotFailOnNullValues() {
		new SpelPropertyComperator<SomeType>("stringProperty").compare(ONE, new SomeType(null, null, 2));
	}

	@Test
	public void shouldComparePrimitiveIntegerDescCorrectly() {

		assertThat(new SpelPropertyComperator<SomeType>("primitiveProperty").desc().compare(ONE, TWO),
				is(Integer.valueOf(TWO.getPrimitiveProperty()).compareTo(Integer.valueOf(ONE.getPrimitiveProperty()))));
	}

	@Test
	public void shouldSortNullsFirstCorrectly() {
		assertThat(
				new SpelPropertyComperator<SomeType>("stringProperty").nullsFirst().compare(ONE, new SomeType(null, null, 2)),
				equalTo(1));
	}

	@Test
	public void shouldSortNullsLastCorrectly() {
		assertThat(
				new SpelPropertyComperator<SomeType>("stringProperty").nullsLast().compare(ONE, new SomeType(null, null, 2)),
				equalTo(-1));
	}

	@Test
	public void shouldCompareNestedTypesCorrectly() {

		assertThat(new SpelPropertyComperator<WrapperType>("nestedType.stringProperty").compare(WRAPPER_ONE, WRAPPER_TWO),
				is(WRAPPER_ONE.getNestedType().getStringProperty().compareTo(WRAPPER_TWO.getNestedType().getStringProperty())));
	}

	@Test
	public void shouldCompareNestedTypesCorrectlyWhenOneOfThemHasNullValue() {

		assertThat(new SpelPropertyComperator<WrapperType>("nestedType.stringProperty").compare(WRAPPER_ONE,
				new WrapperType("two", null)), is(greaterThanOrEqualTo(1)));
	}

	static class WrapperType {

		private String stringPropertyWrapper;
		private SomeType nestedType;

		public WrapperType(String stringPropertyWrapper, SomeType nestedType) {
			this.stringPropertyWrapper = stringPropertyWrapper;
			this.nestedType = nestedType;
		}

		public String getStringPropertyWrapper() {
			return stringPropertyWrapper;
		}

		public void setStringPropertyWrapper(String stringPropertyWrapper) {
			this.stringPropertyWrapper = stringPropertyWrapper;
		}

		public SomeType getNestedType() {
			return nestedType;
		}

		public void setNestedType(SomeType nestedType) {
			this.nestedType = nestedType;
		}

	}

	static class SomeType {

		public SomeType() {

		}

		public SomeType(String stringProperty, Integer integerProperty, int primitiveProperty) {
			this.stringProperty = stringProperty;
			this.integerProperty = integerProperty;
			this.primitiveProperty = primitiveProperty;
		}

		String stringProperty;
		Integer integerProperty;
		int primitiveProperty;

		public String getStringProperty() {
			return stringProperty;
		}

		public void setStringProperty(String stringProperty) {
			this.stringProperty = stringProperty;
		}

		public Integer getIntegerProperty() {
			return integerProperty;
		}

		public void setIntegerProperty(Integer integerProperty) {
			this.integerProperty = integerProperty;
		}

		public int getPrimitiveProperty() {
			return primitiveProperty;
		}

		public void setPrimitiveProperty(int primitiveProperty) {
			this.primitiveProperty = primitiveProperty;
		}

	}

}
