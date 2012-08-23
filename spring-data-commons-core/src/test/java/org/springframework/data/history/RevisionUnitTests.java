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
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link RevisionMetadata}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RevisionUnitTests {

	@Mock
	RevisionMetadata<Integer> firstMetadata, secondMetadata;

	@Test
	@SuppressWarnings("unchecked")
	public void comparesCorrectly() {

		when(firstMetadata.getRevisionNumber()).thenReturn(1);
		when(secondMetadata.getRevisionNumber()).thenReturn(2);

		Revision<Integer, Object> first = new Revision<Integer, Object>(firstMetadata, new Object());
		Revision<Integer, Object> second = new Revision<Integer, Object>(secondMetadata, new Object());

		List<Revision<Integer, Object>> revisions = Arrays.asList(second, first);
		Collections.sort(revisions);

		assertThat(revisions.get(0), is(first));
		assertThat(revisions.get(1), is(second));
	}

	/**
	 * @see DATACMNS-187
	 */
	@Test
	public void returnsRevisionNumber() {

		when(firstMetadata.getRevisionNumber()).thenReturn(4711);

		Revision<Integer, Object> revision = new Revision<Integer, Object>(firstMetadata, new Object());

		assertThat(revision.getRevisionNumber(), is(4711));
	}

	/**
	 * @see DATACMNS-187
	 */
	@Test
	public void returnsRevisionDate() {

		DateTime reference = new DateTime();
		when(firstMetadata.getRevisionDate()).thenReturn(reference);

		Revision<Integer, Object> revision = new Revision<Integer, Object>(firstMetadata, new Object());

		assertThat(revision.getRevisionDate(), is(reference));
	}

	/**
	 * @see DATACMNS-218
	 */
	@Test
	public void returnsRevisionMetadata() {

		Revision<Integer, Object> revision = new Revision<Integer, Object>(firstMetadata, new Object());
		assertThat(revision.getMetadata(), is(firstMetadata));
	}
}
