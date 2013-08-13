/*
 * Copyright 2011-2013 the original author or authors.
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
package org.springframework.data.repository.core.support;

import org.springframework.data.domain.Persistable;

import java.io.Serializable;

/**
 * Dummy implementation of {@link AbstractEntityInformation} that also provides ID information.
 *
 * @author Nick Williams
 */
public class DummyEntityAndIdInformation<T extends Persistable<ID>, ID extends Serializable>
		extends AbstractEntityInformation<T, ID> {

	private final Class<ID> idClass;

	/**
	 * Creates a new {@link DummyEntityInformation} for the given domain class.
	 *
	 * @param domainClass
	 */
	public DummyEntityAndIdInformation(Class<T> domainClass, Class<ID> idClass) {
		super(domainClass);
		this.idClass = idClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
	 */
	public ID getId(T entity) {
		return entity == null ? null : entity.getId();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	public Class<ID> getIdType() {
		return this.idClass;
	}
}
