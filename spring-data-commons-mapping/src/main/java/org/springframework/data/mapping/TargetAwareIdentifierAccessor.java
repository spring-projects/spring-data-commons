/*
 * Copyright 2017-2025 the original author or authors.
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
package org.springframework.data.mapping;

/**
 * {@link IdentifierAccessor} that is aware of the target bean to obtain the identifier from so that it can generate a
 * more meaningful exception in case of an absent identifier and a call to {@link #getRequiredIdentifier()}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @since 2.0
 * @soundtrack Anika Nilles - Greenfield (Pikalar)
 */
public abstract class TargetAwareIdentifierAccessor implements IdentifierAccessor {

	private final Object target;

	public TargetAwareIdentifierAccessor(Object target) {
		this.target = target;
	}

	@Override
	public Object getRequiredIdentifier() {

		Object identifier = getIdentifier();

		if (identifier != null) {
			return identifier;
		}

		throw new IllegalStateException(String.format("Could not obtain identifier from %s", target));
	}
}
