/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.history;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RevisionMetadata}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ExtendWith(MockitoExtension.class)
class RevisionUnitTests {

	@Mock RevisionMetadata<Integer> firstMetadata, secondMetadata;

	@Test
	void comparesCorrectly() {

		when(firstMetadata.getRevisionNumber()).thenReturn(Optional.of(1));
		when(secondMetadata.getRevisionNumber()).thenReturn(Optional.of(2));

		var first = Revision.of(firstMetadata, new Object());
		var second = Revision.of(secondMetadata, new Object());

		var revisions = Stream.of(second, first).sorted().collect(Collectors.toList());

		assertThat(revisions.get(0)).isEqualTo(first);
		assertThat(revisions.get(1)).isEqualTo(second);
	}

	@Test // DATACMNS-187
	void returnsRevisionNumber() {

		var reference = Optional.of(4711);
		when(firstMetadata.getRevisionNumber()).thenReturn(reference);

		assertThat(Revision.of(firstMetadata, new Object()).getRevisionNumber()).isEqualTo(reference);
	}

	@Test // DATACMNS-1251
	void returnsRevisionInstant() {

		var reference = Optional.of(Instant.now());
		when(firstMetadata.getRevisionInstant()).thenReturn(reference);

		assertThat(Revision.of(firstMetadata, new Object()).getRevisionInstant()).isEqualTo(reference);
	}

	@Test // DATACMNS-218
	void returnsRevisionMetadata() {
		assertThat(Revision.of(firstMetadata, new Object()).getMetadata()).isEqualTo(firstMetadata);
	}
}
