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

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class SpelSortUnitTests {

	private static final SomeType ONE = new SomeType("one", Integer.valueOf(1), 1);
	private static final SomeType TWO = new SomeType("two", Integer.valueOf(2), 2);

	@Test
	public void shouldCompareStringAscCorrectly() {

		Assert.assertThat(new SpelSort<SomeType>("stringProperty").compare(ONE, TWO),
				Is.is(ONE.getStringProperty().compareTo(TWO.getStringProperty())));
	}

	@Test
	public void shouldCompareStringDescCorrectly() {

		Assert.assertThat(new SpelSort<SomeType>("stringProperty").desc().compare(ONE, TWO),
				Is.is(TWO.getStringProperty().compareTo(ONE.getStringProperty())));
	}

	@Test
	public void shouldCompareIntegerAscCorrectly() {

		Assert.assertThat(new SpelSort<SomeType>("integerProperty").compare(ONE, TWO),
				Is.is(ONE.getIntegerProperty().compareTo(TWO.getIntegerProperty())));
	}

	@Test
	public void shouldCompareIntegerDescCorrectly() {

		Assert.assertThat(new SpelSort<SomeType>("integerProperty").desc().compare(ONE, TWO),
				Is.is(TWO.getIntegerProperty().compareTo(ONE.getIntegerProperty())));
	}

	@Test
	public void shouldComparePrimitiveIntegerAscCorrectly() {

		Assert.assertThat(new SpelSort<SomeType>("primitiveProperty").compare(ONE, TWO),
				Is.is(Integer.valueOf(ONE.getPrimitiveProperty()).compareTo(Integer.valueOf(TWO.getPrimitiveProperty()))));
	}

	@Test
	public void shouldNotFailOnNullValues() {
		new SpelSort<SomeType>("stringProperty").compare(ONE, new SomeType(null, null, 2));
	}

	@Test
	public void shouldComparePrimitiveIntegerDescCorrectly() {

		Assert.assertThat(new SpelSort<SomeType>("primitiveProperty").desc().compare(ONE, TWO),
				Is.is(Integer.valueOf(TWO.getPrimitiveProperty()).compareTo(Integer.valueOf(ONE.getPrimitiveProperty()))));
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
