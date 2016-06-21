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
package org.springframework.data.repository.core.support;

import java.io.Serializable;
import java.util.Optional;

/**
 * Dummy implementation of {@link AbstractEntityInformation}.
 * 
 * @author Oliver Gierke
 */
public class DummyEntityInformation<T> extends AbstractEntityInformation<T, Serializable> {

	/**
	 * Creates a new {@link DummyEntityInformation} for the given domain class.
	 * 
	 * @param domainClass
	 */
	public DummyEntityInformation(Class<T> domainClass) {
		super(domainClass);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
	 */
	public Optional<Serializable> getId(Object entity) {
		return Optional.ofNullable(entity == null ? null : entity.toString());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	public Class<Serializable> getIdType() {
		return Serializable.class;
	}
}
