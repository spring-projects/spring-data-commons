/*
 * Copyright 2012-present the original author or authors.
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

	@Override
	public Optional<AuditedUser> getCreatedBy() {
		return Optional.ofNullable(createdBy);
	}

	@Override
	public void setCreatedBy(AuditedUser createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	public Optional<LocalDateTime> getCreatedDate() {
		return Optional.ofNullable(createdDate);
	}

	@Override
	public void setCreatedDate(LocalDateTime creationDate) {
		this.createdDate = creationDate;
	}

	@Override
	public Optional<AuditedUser> getLastModifiedBy() {
		return Optional.ofNullable(modifiedBy);
	}

	@Override
	public void setLastModifiedBy(AuditedUser lastModifiedBy) {
		this.modifiedBy = lastModifiedBy;
	}

	@Override
	public Optional<LocalDateTime> getLastModifiedDate() {
		return Optional.ofNullable(modifiedDate);
	}

	@Override
	public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
		this.modifiedDate = lastModifiedDate;
	}
}
