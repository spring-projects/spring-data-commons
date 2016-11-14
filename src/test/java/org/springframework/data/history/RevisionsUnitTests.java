/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.history;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link Revisions}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RevisionsUnitTests {

	@Mock RevisionMetadata<Integer> first, second;
	Revision<Integer, Object> firstRevision, secondRevision;

	@Before
	public void setUp() {

		when(first.getRevisionNumber()).thenReturn(Optional.of(0));
		when(second.getRevisionNumber()).thenReturn(Optional.of(10));

		firstRevision = Revision.of(first, new Object());
		secondRevision = Revision.of(second, new Object());
	}

	@Test
	public void returnsCorrectLatestRevision() {
		assertThat(Revisions.of(Arrays.asList(firstRevision, secondRevision)).getLatestRevision())
				.isEqualTo(secondRevision);
	}

	@Test
	public void iteratesInCorrectOrder() {

		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(firstRevision, secondRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.iterator();

		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(firstRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(secondRevision);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void reversedRevisionsStillReturnsCorrectLatestRevision() {
		assertThat(Revisions.of(Arrays.asList(firstRevision, secondRevision)).reverse().getLatestRevision())
				.isEqualTo(secondRevision);
	}

	@Test
	public void iteratesReversedRevisionsInCorrectOrder() {

		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(firstRevision, secondRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.reverse().iterator();

		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(secondRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(firstRevision);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void forcesInvalidlyOrderedRevisionsToBeOrdered() {

		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(secondRevision, firstRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.iterator();

		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(firstRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(secondRevision);
		assertThat(iterator.hasNext()).isFalse();
	}
}
