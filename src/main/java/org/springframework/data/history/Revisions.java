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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Simple wrapper class for a {@link List} of {@link Revisions} allowing to canonically access the latest revision.
 * Allows iterating over the underlying {@link Revisions} starting with older revisions.
 * 
 * @author Oliver Gierke
 */
public class Revisions<N extends Number & Comparable<N>, T> implements Iterable<Revision<N, T>> {

	private final List<Revision<N, T>> revisions;
	private final boolean latestLast;

	/**
	 * Creates a new {@link Revisions} instance containing the given revisions. Will make sure they are ordered
	 * ascendingly.
	 * 
	 * @param revisions must not be {@literal null}.
	 */
	public Revisions(List<? extends Revision<N, T>> revisions) {
		this(revisions, true);
	}

	/**
	 * Creates a new {@link Revisions} instance using the given revisions.
	 * 
	 * @param revisions must not be {@literal null}.
	 * @param latestLast
	 */
	private Revisions(List<? extends Revision<N, T>> revisions, boolean latestLast) {

		Assert.notNull(revisions);
		this.revisions = new ArrayList<Revision<N, T>>(revisions);
		this.latestLast = latestLast;

		Collections.sort(this.revisions);

		if (!latestLast) {
			Collections.reverse(this.revisions);
		}
	}

	/**
	 * Returns the latest revision of the revisions backing the wrapper independently of the order.
	 * 
	 * @return
	 */
	public Revision<N, T> getLatestRevision() {
		int index = latestLast ? revisions.size() - 1 : 0;
		return revisions.get(index);
	}

	/**
	 * Reverses the current {@link Revisions}. By default this will return the revisions with the latest revision first.
	 * 
	 * @return
	 */
	public Revisions<N, T> reverse() {
		List<Revision<N, T>> result = new ArrayList<Revision<N, T>>(revisions);
		Collections.reverse(result);
		return new Revisions<N, T>(result, !latestLast);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Revision<N, T>> iterator() {
		return revisions.iterator();
	}

	/**
	 * Returns the content of the {@link Revisions} instance.
	 * 
	 * @return
	 */
	public List<Revision<N, T>> getContent() {
		return Collections.unmodifiableList(revisions);
	}
}
