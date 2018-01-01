/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * A dedicated {@link Sort} implementation that allows the definition of the ordering of revisions independently of the
 * property name the revision number is held in.
 *
 * @author Oliver Gierke
 * @since 1.13
 * @soundtrack Benny Greb's Moving Parts - Soulfood (Live)
 */
public class RevisionSort extends Sort {

	private static final long serialVersionUID = 618238321589063537L;

	private static final String PROPERTY = "__revisionNumber__";
	private static final RevisionSort ASC = new RevisionSort(Direction.ASC);
	private static final RevisionSort DESC = new RevisionSort(Direction.DESC);

	/**
	 * Creates a new {@link RevisionSort} using the given direction for sorting by revision number.
	 *
	 * @param direction must not be {@literal null}.
	 */
	private RevisionSort(Direction direction) {
		super(direction, PROPERTY);
	}

	/**
	 * Creates a {@link RevisionSort} with ascending order for the revision number property, i.e. more recent revisions
	 * will come last.
	 *
	 * @return
	 */
	public static RevisionSort asc() {
		return ASC;
	}

	/**
	 * Creates a {@link RevisionSort} with descending order for the revision number property, i.e. more recent revisions
	 * will come first.
	 *
	 * @return
	 */
	public static RevisionSort desc() {
		return DESC;
	}

	/**
	 * Returns in which direction to sort revisions for the given {@link Sort} instance. Defaults to
	 * {@link Direction#ASC}.
	 *
	 * @param sort must not be {@literal null}.
	 * @return
	 */
	public static Direction getRevisionDirection(Sort sort) {

		Assert.notNull(sort, "Sort must not be null!");

		Order order = sort.getOrderFor(PROPERTY);
		return order == null ? Direction.ASC : order.getDirection();
	}
}
