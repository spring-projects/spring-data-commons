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

import org.joda.time.DateTime;
import org.springframework.util.Assert;

/**
 * Wrapper to contain {@link RevisionMetadata} as well as the revisioned entity.
 * 
 * @author Oliver Gierke
 * @author Philipp Huegelmeyer
 */
public final class Revision<N extends Number & Comparable<N>, T> implements Comparable<Revision<N, ?>> {

	private final RevisionMetadata<N> metadata;
	private final T entity;

	/**
	 * Creates a new {@link Revision} consisting of the given {@link RevisionMetadata} and entity.
	 * 
	 * @param metadata must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 */
	public Revision(RevisionMetadata<N> metadata, T entity) {

		Assert.notNull(metadata);
		Assert.notNull(entity);

		this.metadata = metadata;
		this.entity = entity;
	}

	/**
	 * Returns the revision number of the revision.
	 * 
	 * @return the revision number.
	 */
	public N getRevisionNumber() {
		return metadata.getRevisionNumber();
	}

	/**
	 * Returns the revision date of the revision.
	 * 
	 * @return
	 */
	public DateTime getRevisionDate() {
		return metadata.getRevisionDate();
	}

	/**
	 * Returns the underlying entity.
	 * 
	 * @return the entity
	 */
	public T getEntity() {
		return entity;
	}

	/**
	 * Returns the {@link RevisionMetadata} for the current {@link Revision}.
	 * 
	 * @return the metadata
	 */
	public RevisionMetadata<N> getMetadata() {
		return metadata;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Revision<N, ?> that) {
		return getRevisionNumber().compareTo(that.getRevisionNumber());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Revision)) {
			return false;
		}

		Revision<N, T> that = (Revision<N, T>) obj;
		boolean sameRevisionNumber = this.metadata.getRevisionNumber().equals(that.metadata.getRevisionNumber());
		return !sameRevisionNumber ? false : this.entity.equals(that.entity);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;
		result += 31 * metadata.hashCode();
		result += 31 * entity.hashCode();
		return result;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Revision %s of entity %s - Revision metadata %s", getRevisionNumber(), entity, metadata);
	}
}
