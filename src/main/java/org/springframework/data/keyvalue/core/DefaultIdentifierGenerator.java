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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link IdentifierGenerator} to generate identifiers of types {@link UUID}, String,
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
enum DefaultIdentifierGenerator implements IdentifierGenerator {

	INSTANCE;

	private static final String ALGORITHM = "NativePRNGBlocking";

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.IdentifierGenerator#newIdForType(org.springframework.data.util.TypeInformation)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T generateIdentifierOfType(TypeInformation<T> identifierType) {

		Class<?> type = identifierType.getType();

		if (ClassUtils.isAssignable(UUID.class, type)) {
			return (T) UUID.randomUUID();
		} else if (ClassUtils.isAssignable(String.class, type)) {
			return (T) UUID.randomUUID().toString();
		} else if (ClassUtils.isAssignable(Integer.class, type)) {

			try {
				return (T) SecureRandom.getInstance(ALGORITHM);
			} catch (NoSuchAlgorithmException e) {
				throw new InvalidDataAccessApiUsageException("Could not create SecureRandom instance.", e);
			}

		} else if (ClassUtils.isAssignable(Long.class, type)) {

			try {
				return (T) Long.valueOf(SecureRandom.getInstance(ALGORITHM).nextLong());
			} catch (NoSuchAlgorithmException e) {
				throw new InvalidDataAccessApiUsageException("Could not create SecureRandom instance.", e);
			}
		}

		throw new InvalidDataAccessApiUsageException("Non gereratable id type....");
	}
}
