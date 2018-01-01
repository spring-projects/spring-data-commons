/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.mapping;

import org.springframework.lang.Nullable;

/**
 * Interface for a component allowing the access of identifier values.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @see TargetAwareIdentifierAccessor
 */
public interface IdentifierAccessor {

	/**
	 * Returns the value of the identifier.
	 *
	 * @return the identifier of the underlying instance.
	 */
	@Nullable
	Object getIdentifier();

	/**
	 * Returns the identifier of the underlying instance. Implementations are strongly recommended to extends either
	 * {@link TargetAwareIdentifierAccessor} or override this method to add more context to the exception being thrown in
	 * case of the absence of an identifier.
	 *
	 * @return the identifier of the underlying instance
	 * @throws IllegalStateException in case no identifier could be retrieved.
	 * @since 2.0
	 */
	default Object getRequiredIdentifier() {

		Object identifier = getIdentifier();

		if (identifier != null) {
			return identifier;
		}

		throw new IllegalStateException("Could not obtain identifier!");
	}
}
