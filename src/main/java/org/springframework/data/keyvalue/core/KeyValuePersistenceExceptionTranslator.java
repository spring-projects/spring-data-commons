/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.keyvalue.core;

import java.util.NoSuchElementException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * Simple {@link PersistenceExceptionTranslator} implementation for key/value stores that converts the given runtime
 * exception to an appropriate exception from the {@code org.springframework.dao} hierarchy.
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
public class KeyValuePersistenceExceptionTranslator implements PersistenceExceptionTranslator {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.dao.support.PersistenceExceptionTranslator#translateExceptionIfPossible(java.lang.RuntimeException)
	 */
	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException e) {

		if (e == null || e instanceof DataAccessException) {
			return (DataAccessException) e;
		}

		if (e instanceof NoSuchElementException || e instanceof IndexOutOfBoundsException
				|| e instanceof IllegalStateException) {
			return new DataRetrievalFailureException(e.getMessage(), e);
		}

		if (e.getClass().getName().startsWith("java")) {
			return new UncategorizedKeyValueException(e.getMessage(), e);
		}
		return null;
	}
}
