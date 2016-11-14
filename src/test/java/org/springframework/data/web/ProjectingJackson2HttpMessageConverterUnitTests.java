/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.web;

import static org.hamcrest.CoreMatchers.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * Unit tests for {@link ProjectingJackson2HttpMessageConverter}.
 * 
 * @author Oliver Gierke
 * @soundtrack Richard Spaven - Tribute (Whole Other*)
 * @since 1.13
 */
public class ProjectingJackson2HttpMessageConverterUnitTests {

	ProjectingJackson2HttpMessageConverter converter = new ProjectingJackson2HttpMessageConverter();
	MediaType ANYTHING_JSON = MediaType.parseMediaType("application/*+json");

	/**
	 * @see DATCMNS-885
	 */
	@Test
	public void canReadJsonIntoAnnotatedInterface() {
		assertThat(converter.canRead(SampleInterface.class, ANYTHING_JSON)).isTrue();
	}

	/**
	 * @see DATCMNS-885
	 */
	@Test
	public void cannotReadUnannotatedInterface() {
		assertThat(converter.canRead(UnannotatedInterface.class, ANYTHING_JSON)).isFalse();
	}

	/**
	 * @see DATCMNS-885
	 */
	@Test
	public void cannotReadClass() {
		assertThat(converter.canRead(SampleClass.class, ANYTHING_JSON)).isFalse();
	}

	@ProjectedPayload
	interface SampleInterface {}

	interface UnannotatedInterface {}

	class SampleClass {}
}
