/*
 * Copyright 2026 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.web.SlicedModel.SliceMetadata;

/**
 * Unit tests for {@link SlicedModel}.
 *
 * @author Arnab Nandy
 */
class SlicedModelUnitTests {

	@Test // GH-3516
	void rejectsNullSlice() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SlicedModel<>(null));
	}

	@Test // GH-3516
	void exposesContentAndMetadata() {

		Slice<String> slice = new SliceImpl<>(List.of("first", "second"), PageRequest.of(1, 10), true);
		SlicedModel<String> model = new SlicedModel<>(slice);

		assertThat(model.getContent()).containsExactly("first", "second");

		SliceMetadata metadata = model.getMetadata();
		assertThat(metadata.size()).isEqualTo(10);
		assertThat(metadata.number()).isEqualTo(1);
		assertThat(metadata.numberOfElements()).isEqualTo(2);
		assertThat(metadata.hasNext()).isTrue();
	}

	@Test // GH-3516
	void equalsAndHashCode() {

		Slice<String> slice1 = new SliceImpl<>(List.of("foo"), PageRequest.of(0, 5), false);
		Slice<String> slice2 = new SliceImpl<>(List.of("foo"), PageRequest.of(0, 5), false);
		Slice<String> slice3 = new SliceImpl<>(List.of("bar"), PageRequest.of(0, 5), false);

		SlicedModel<String> model1 = new SlicedModel<>(slice1);
		SlicedModel<String> model2 = new SlicedModel<>(slice2);
		SlicedModel<String> model3 = new SlicedModel<>(slice3);

		assertThat(model1).isEqualTo(model1);
		assertThat(model1).isEqualTo(model2);
		assertThat(model1).hasSameHashCodeAs(model2);
		assertThat(model1).isNotEqualTo(model3);
		assertThat(model1).isNotEqualTo(null);
		assertThat(model1).isNotEqualTo("foo");
	}

	@Test // GH-3516
	void validatesMetadata() {

		assertThatIllegalArgumentException().isThrownBy(() -> new SliceMetadata(-1, 0, 0, false));
		assertThatIllegalArgumentException().isThrownBy(() -> new SliceMetadata(0, -1, 0, false));
		assertThatIllegalArgumentException().isThrownBy(() -> new SliceMetadata(0, 0, -1, false));

		assertThatNoException().isThrownBy(() -> new SliceMetadata(0, 0, 0, false));
	}
}
