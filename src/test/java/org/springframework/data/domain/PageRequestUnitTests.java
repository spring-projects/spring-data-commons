/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.UnitTestUtils.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Direction;

/**
 * Unit test for {@link PageRequest}.
 *
 * @author Oliver Gierke
 */
class PageRequestUnitTests extends AbstractPageRequestUnitTests {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.AbstractPageRequestUnitTests#newPageRequest(int, int)
	 */
	@Override
	public AbstractPageRequest newPageRequest(int page, int size) {
		return PageRequest.of(page, size);
	}

	AbstractPageRequest newPageRequest(int page, int size, Sort sort) {
		return PageRequest.of(page, size, sort);
	}

	@Test
	void equalsRegardsSortCorrectly() {

		Sort sort = Sort.by(Direction.DESC, "foo");
		AbstractPageRequest request = PageRequest.of(0, 10, sort);

		// Equals itself
		assertEqualsAndHashcode(request, request);

		// Equals another instance with same setup
		assertEqualsAndHashcode(request, PageRequest.of(0, 10, sort));

		// Equals without sort entirely
		assertEqualsAndHashcode(PageRequest.of(0, 10), PageRequest.of(0, 10));

		// Is not equal to instance without sort
		assertNotEqualsAndHashcode(request, PageRequest.of(0, 10));

		// Is not equal to instance with another sort
		assertNotEqualsAndHashcode(request, PageRequest.of(0, 10, Direction.ASC, "foo"));
	}

	@Test // DATACMNS-1581
	void rejectsNullSort() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> PageRequest.of(0, 10, null));
	}
}
