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
package org.springframework.data.auditing;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Auditable;

/**
 * Sample implementation of {@link Auditable}.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
class AuditedUser implements Auditable<AuditedUser, Long, LocalDateTime> {

	private static final long serialVersionUID = -840865084027597951L;

	Long id;
	AuditedUser createdBy;
	AuditedUser modifiedBy;
	LocalDateTime createdDate;
	LocalDateTime modifiedDate;

	public Long getId() {
		return id;
	}

	public boolean isNew() {
		return id == null;
	}

	public Optional<AuditedUser> getCreatedBy() {
		return Optional.ofNullable(createdBy);
	}

	public void setCreatedBy(Optional<? extends AuditedUser> createdBy) {
		this.createdBy = createdBy.orElse(null);
	}

	public Optional<LocalDateTime> getCreatedDate() {
		return Optional.ofNullable(createdDate);
	}

	public void setCreatedDate(Optional<? extends LocalDateTime> creationDate) {
		this.createdDate = creationDate.orElse(null);
	}

	public Optional<AuditedUser> getLastModifiedBy() {
		return Optional.ofNullable(modifiedBy);
	}

	public void setLastModifiedBy(Optional<? extends AuditedUser> lastModifiedBy) {
		this.modifiedBy = lastModifiedBy.orElse(null);
	}

	public Optional<LocalDateTime> getLastModifiedDate() {
		return Optional.ofNullable(modifiedDate);
	}

	public void setLastModifiedDate(Optional<? extends LocalDateTime> lastModifiedDate) {
		this.modifiedDate = lastModifiedDate.orElse(null);
	}
}
