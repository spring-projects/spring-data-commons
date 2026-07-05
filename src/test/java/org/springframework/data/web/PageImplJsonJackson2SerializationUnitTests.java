/*
 * Copyright 2024-present the original author or authors.
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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Unit tests for PageImpl serialization.
 *
 * @author Oliver Drotbohm
 * @author Mark Paluch
 */
class PageImplJsonJackson2SerializationUnitTests {

	@Test // GH-3024
	void serializesPageImplAsJson() {
		assertJsonRendering(PageSerializationMode.DIRECT, "$.pageable", "$.last", "$.first");
	}

	@Test // GH-3024
	void serializesPageImplAsPagedModel() {
		assertJsonRendering(PageSerializationMode.VIA_DTO, "$.content", "$.page");
	}

	@Test // GH-3137
	void serializesCustomPageAsPageImpl() {
		assertJsonRendering(PageSerializationMode.DIRECT, new Extension<>("header"), "$.pageable", "$.last", "$.first");
	}

	private static void assertJsonRendering(PageSerializationMode mode, String... jsonPaths) {
		assertJsonRendering(mode, new PageImpl<>(Collections.emptyList()), jsonPaths);
	}

	private static void assertJsonRendering(PageSerializationMode mode, PageImpl<?> page, String... jsonPaths) {

		SpringDataWebSettings settings = new SpringDataWebSettings(mode);

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new SpringDataJacksonConfiguration.PageModule(settings));

		assertThatNoException().isThrownBy(() -> {

			String result = mapper.writeValueAsString(page);

			for (String jsonPath : jsonPaths) {
				assertThat(JsonPath.<Object> read(result, jsonPath)).isNotNull();
			}
		});
	}

	static class Extension<T> extends PageImpl<T> {

		private Object header;

		public Extension(Object header) {
			super(Collections.emptyList());
		}

		public Object getHeader() {
			return header;
		}
	}
}
