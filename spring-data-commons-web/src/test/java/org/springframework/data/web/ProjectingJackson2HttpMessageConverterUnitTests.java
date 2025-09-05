/*
 * Copyright 2016-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.web;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Unit tests for {@link ProjectingJackson2HttpMessageConverter}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Richard Spaven - Tribute (Whole Other*)
 * @since 1.13
 */
class ProjectingJackson2HttpMessageConverterUnitTests {

	ProjectingJackson2HttpMessageConverter converter = new ProjectingJackson2HttpMessageConverter();
	MediaType ANYTHING_JSON = MediaType.parseMediaType("application/*+json");

	@Test // DATCMNS-885
	void canReadJsonIntoAnnotatedInterface() {
		assertThat(converter.canRead(SampleInterface.class, ANYTHING_JSON)).isTrue();
	}

	@Test // DATCMNS-885
	void cannotReadUnannotatedInterface() {
		assertThat(converter.canRead(UnannotatedInterface.class, ANYTHING_JSON)).isFalse();
	}

	@Test // DATCMNS-885
	void cannotReadClass() {
		assertThat(converter.canRead(SampleClass.class, ANYTHING_JSON)).isFalse();
	}

	@Test // DATACMNS-972
	void doesNotConsiderTypeVariableBoundTo() throws Throwable {

		var method = BaseController.class.getDeclaredMethod("createEntity", AbstractDto.class);
		var type = method.getGenericParameterTypes()[0];

		assertThat(converter.canRead(type, BaseController.class, ANYTHING_JSON)).isFalse();
	}

	@Test // DATACMNS-972
	void genericTypeOnConcreteOne() throws Throwable {

		var method = ConcreteController.class.getMethod("createEntity", AbstractDto.class);
		var type = method.getGenericParameterTypes()[0];

		assertThat(converter.canRead(type, ConcreteController.class, ANYTHING_JSON)).isFalse();
	}

	@ProjectedPayload
	interface SampleInterface {}

	interface UnannotatedInterface {}

	class SampleClass {}

	class AbstractDto {}

	abstract class BaseController<D extends AbstractDto> {
		public void createEntity(D dto) {}
	}

	class ConcreteController extends BaseController<AbstractDto> {}
}
