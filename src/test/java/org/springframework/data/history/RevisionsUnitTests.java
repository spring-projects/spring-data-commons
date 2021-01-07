/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link Revisions}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class RevisionsUnitTests {

	@Mock RevisionMetadata<Integer> first, second;
	Revision<Integer, Object> firstRevision, secondRevision;

	@BeforeEach
	void setUp() {

		when(first.getRevisionNumber()).thenReturn(Optional.of(0));
		when(second.getRevisionNumber()).thenReturn(Optional.of(10));

		firstRevision = Revision.of(first, new Object());
		secondRevision = Revision.of(second, new Object());
	}

	@Test
	void returnsCorrectLatestRevision() {
		assertThat(Revisions.of(Arrays.asList(firstRevision, secondRevision)).getLatestRevision())
				.isEqualTo(secondRevision);
	}

	@Test
	void iteratesInCorrectOrder() {

		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(firstRevision, secondRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.iterator();

		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(firstRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(secondRevision);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void reversedRevisionsStillReturnsCorrectLatestRevision() {
		assertThat(Revisions.of(Arrays.asList(firstRevision, secondRevision)).reverse().getLatestRevision())
				.isEqualTo(secondRevision);
	}

	@Test
	void iteratesReversedRevisionsInCorrectOrder() {

		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(firstRevision, secondRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.reverse().iterator();

		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(secondRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(firstRevision);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void forcesInvalidlyOrderedRevisionsToBeOrdered() {

		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(secondRevision, firstRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.iterator();

		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(firstRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(secondRevision);
		assertThat(iterator.hasNext()).isFalse();
	}
}
