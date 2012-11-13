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

import org.joda.time.DateTime;
import org.springframework.data.domain.Auditable;

/**
 * Sample implementation of {@link Auditable}.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
class AuditedUser implements Auditable<AuditedUser, Long> {

	private static final long serialVersionUID = -840865084027597951L;

	Long id;
	AuditedUser createdBy;
	AuditedUser modifiedBy;
	DateTime createdDate;
	DateTime modifiedDate;

	public Long getId() {
		return id;
	}

	public boolean isNew() {
		return id == null;
	}

	public AuditedUser getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(AuditedUser createdBy) {
		this.createdBy = createdBy;
	}

	public DateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(DateTime creationDate) {
		this.createdDate = creationDate;
	}

	public AuditedUser getLastModifiedBy() {
		return modifiedBy;
	}

	public void setLastModifiedBy(AuditedUser lastModifiedBy) {
		this.modifiedBy = lastModifiedBy;
	}

	public DateTime getLastModifiedDate() {
		return modifiedDate;
	}

	public void setLastModifiedDate(DateTime lastModifiedDate) {
		this.modifiedDate = lastModifiedDate;
	}
}
