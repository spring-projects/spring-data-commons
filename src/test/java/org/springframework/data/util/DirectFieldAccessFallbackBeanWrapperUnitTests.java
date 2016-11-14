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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.NotWritablePropertyException;

/**
 * Unit tests for {@link DirectFieldAccessFallbackBeanWrapper}.
 * 
 * @author Oliver Gierke
 */
public class DirectFieldAccessFallbackBeanWrapperUnitTests {

	/**
	 * @see DATACMNS-452
	 */
	@Test
	public void usesFieldAccessForReadIfNoAccessorCanBeFound() {

		Sample sample = new Sample();
		sample.firstname = "Dave";

		BeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(sample);

		assertThat(wrapper.getPropertyValue("firstname")).isEqualTo("Dave");
	}

	/**
	 * @see DATACMNS-452
	 */
	@Test
	public void usesFieldAccessForWriteIfNoAccessorCanBeFound() {

		Sample sample = new Sample();

		BeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(sample);
		wrapper.setPropertyValue("firstname", "Dave");

		assertThat(sample.firstname).isEqualTo("Dave");
	}

	/**
	 * @see DATACMNS-452
	 */
	@Test(expected = NotReadablePropertyException.class)
	public void throwsAppropriateExceptionIfNoFieldFoundForRead() {

		BeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(new Sample());
		wrapper.getPropertyValue("lastname");
	}

	/**
	 * @see DATACMNS-452
	 */
	@Test(expected = NotWritablePropertyException.class)
	public void throwsAppropriateExceptionIfNoFieldFoundForWrite() {

		BeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(new Sample());
		wrapper.setPropertyValue("lastname", "Matthews");
	}

	static class Sample {

		String firstname;
	}
}
