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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Iterator;

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

	@Mock
	RevisionMetadata<Integer> first, second;
	Revision<Integer, Object> firstRevision, secondRevision;

	@Before
	public void setUp() {

		when(first.getRevisionNumber()).thenReturn(0);
		when(second.getRevisionNumber()).thenReturn(10);

		firstRevision = new Revision<Integer, Object>(first, new Object());
		secondRevision = new Revision<Integer, Object>(second, new Object());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void returnsCorrectLatestRevision() {

		Revisions<Integer, Object> revisions = new Revisions<Integer, Object>(Arrays.asList(firstRevision, secondRevision));
		assertThat(revisions.getLatestRevision(), is(secondRevision));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void iteratesInCorrectOrder() {

		Revisions<Integer, Object> revisions = new Revisions<Integer, Object>(Arrays.asList(firstRevision, secondRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.iterator();

		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(firstRevision));
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(secondRevision));
		assertThat(iterator.hasNext(), is(false));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reversedRevisionsStillReturnsCorrectLatestRevision() {

		Revisions<Integer, Object> revisions = new Revisions<Integer, Object>(Arrays.asList(firstRevision, secondRevision));
		assertThat(revisions.reverse().getLatestRevision(), is(secondRevision));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void iteratesReversedRevisionsInCorrectOrder() {

		Revisions<Integer, Object> revisions = new Revisions<Integer, Object>(Arrays.asList(firstRevision, secondRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.reverse().iterator();

		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(secondRevision));
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(firstRevision));
		assertThat(iterator.hasNext(), is(false));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void forcesInvalidlyOrderedRevisionsToBeOrdered() {

		Revisions<Integer, Object> revisions = new Revisions<Integer, Object>(Arrays.asList(secondRevision, firstRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.iterator();

		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(firstRevision));
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(secondRevision));
		assertThat(iterator.hasNext(), is(false));
	}
}
