/*
 * Copyright 2008-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.domain;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.UnitTestUtils.*;

import org.junit.Test;
import org.springframework.data.domain.Sort.Direction;

/**
 * Unit test for {@link PageRequest}.
 * 
 * @author Oliver Gierke
 */
public class PageRequestUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void preventsNegativePage() {
		new PageRequest(-1, 10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsNegativeSize() {
		new PageRequest(0, -1);
	}

	@Test
	public void navigatesPageablesCorrectly() {

		Pageable request = new PageRequest(1, 10);

		assertThat(request.hasPrevious(), is(true));
		assertThat(request.next(), is((Pageable) new PageRequest(2, 10)));

		Pageable first = request.previousOrFirst();

		assertThat(first.hasPrevious(), is(false));
		assertThat(first, is((Pageable) new PageRequest(0, 10)));
		assertThat(first, is(request.first()));
		assertThat(first.previousOrFirst(), is(first));
	}

	@Test
	public void equalsRegardsSortCorrectly() {

		Sort sort = new Sort(Direction.DESC, "foo");
		PageRequest request = new PageRequest(0, 10, sort);

		// Equals itself
		assertEqualsAndHashcode(request, request);

		// Equals another instance with same setup
		assertEqualsAndHashcode(request, new PageRequest(0, 10, sort));

		// Equals without sort entirely
		assertEqualsAndHashcode(new PageRequest(0, 10), new PageRequest(0, 10));

		// Is not equal to instance without sort
		assertNotEqualsAndHashcode(request, new PageRequest(0, 10));

		// Is not equal to instance with another sort
		assertNotEqualsAndHashcode(request, new PageRequest(0, 10, Direction.ASC, "foo"));
	}

	@Test
	public void equalsHonoursPageAndSize() {

		PageRequest request = new PageRequest(0, 10);

		// Equals itself
		assertEqualsAndHashcode(request, request);

		// Equals same setup
		assertEqualsAndHashcode(request, new PageRequest(0, 10));

		// Does not equal on different page
		assertNotEqualsAndHashcode(request, new PageRequest(1, 10));

		// Does not equal on different size
		assertNotEqualsAndHashcode(request, new PageRequest(0, 11));
	}

	/**
	 * @see DATACMNS-377
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsPageSizeLessThanOne() {
		new PageRequest(0, 0);
	}
}
