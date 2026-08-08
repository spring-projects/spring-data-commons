/*
 * Copyright 2026-present the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Unit tests for SliceImpl serialization using Jackson 2.
 *
 * @author Adrien Caubel
 */
@SuppressWarnings("removal")
class SliceImplJsonJackson2SerializationUnitTests {

	private static final Slice<String> SLICE = new SliceImpl<>(List.of("first", "second"), PageRequest.of(1, 2), true);

	@Test // GH-3516
	void serializesSliceImplAsJson() throws JsonProcessingException {

		String result = write(PageSerializationMode.DIRECT, SLICE);

		assertThat(JsonPath.<Object> read(result, "$.pageable")).isNotNull();
		assertThat(JsonPath.<Boolean> read(result, "$.first")).isFalse();
	}

	@Test // GH-3516
	void serializesSliceImplAsSlicedModel() throws JsonProcessingException {

		String result = write(PageSerializationMode.VIA_DTO, SLICE);

		assertThat(JsonPath.<List<String>> read(result, "$.content")).containsExactly("first", "second");
		assertThat(JsonPath.<Integer> read(result, "$.page.size")).isEqualTo(2);
		assertThat(JsonPath.<Integer> read(result, "$.page.number")).isEqualTo(1);
		assertThat(JsonPath.<Boolean> read(result, "$.page.hasNext")).isTrue();
	}

	@Test // GH-3516
	void doesNotRenderTotalsForSlice() throws JsonProcessingException {

		String result = write(PageSerializationMode.VIA_DTO, SLICE);

		assertThat(result).doesNotContain("totalElements", "totalPages");
	}

	@Test // GH-3516
	void serializesCustomSliceAsSlicedModel() throws JsonProcessingException {

		String result = write(PageSerializationMode.VIA_DTO, new Extension<>("header"));

		assertThat(JsonPath.<Object> read(result, "$.page")).isNotNull();
		assertThat(result).doesNotContain("header");
	}

	@Test // GH-3516
	void stillSerializesPageImplAsPagedModel() throws JsonProcessingException {

		String result = write(PageSerializationMode.VIA_DTO, new PageImpl<>(List.of("a"), PageRequest.of(0, 2), 3));

		assertThat(JsonPath.<Integer> read(result, "$.page.totalElements")).isEqualTo(3);
		assertThat(JsonPath.<Integer> read(result, "$.page.totalPages")).isEqualTo(2);
	}

	private static String write(PageSerializationMode mode, Slice<?> slice) throws JsonProcessingException {

		SpringDataWebSettings settings = new SpringDataWebSettings(mode);

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new SpringDataJacksonConfiguration.PageModule(settings));

		return mapper.writeValueAsString(slice);
	}

	static class Extension<T> extends SliceImpl<T> {

		private final Object header;

		public Extension(Object header) {

			super(Collections.emptyList());

			this.header = header;
		}

		public Object getHeader() {
			return header;
		}
	}
}
