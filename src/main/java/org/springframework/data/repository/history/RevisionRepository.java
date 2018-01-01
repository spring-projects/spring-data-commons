/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.repository.history;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionSort;
import org.springframework.data.history.Revisions;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * A repository which can access entities held in a variety of {@link Revisions}.
 *
 * @author Oliver Gierke
 * @author Philipp Huegelmeyer
 */
@NoRepositoryBean
public interface RevisionRepository<T, ID, N extends Number & Comparable<N>> extends Repository<T, ID> {

	/**
	 * Returns the revision of the entity it was last changed in.
	 *
	 * @param id must not be {@literal null}.
	 * @return
	 */
	Optional<Revision<N, T>> findLastChangeRevision(ID id);

	/**
	 * Returns all {@link Revisions} of an entity with the given id.
	 *
	 * @param id must not be {@literal null}.
	 * @return
	 */
	Revisions<N, T> findRevisions(ID id);

	/**
	 * Returns a {@link Page} of revisions for the entity with the given id. Note, that it's not guaranteed that
	 * implementations have to support sorting by all properties.
	 *
	 * @param id must not be {@literal null}.
	 * @param pageable
	 * @see RevisionSort
	 * @return
	 */
	Page<Revision<N, T>> findRevisions(ID id, Pageable pageable);

	/**
	 * Returns the entity with the given ID in the given revision number.
	 *
	 * @param id must not be {@literal null}.
	 * @param revisionNumber must not be {@literal null}.
	 * @return the {@link Revision} of the entity with the given ID in the given revision number.
	 * @since 1.12
	 */
	Optional<Revision<N, T>> findRevision(ID id, N revisionNumber);
}
