/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.springframework.data.domain.Persistable;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.TargetAwareIdentifierAccessor;
import org.springframework.lang.Nullable;

/**
 * {@link IdentifierAccessor} that invokes {@link Persistable#getId()}.
 *
 * @author Oliver Gierke
 */
class PersistableIdentifierAccessor extends TargetAwareIdentifierAccessor {

	private final Persistable<?> target;

	/**
	 * Creates a new {@link PersistableIdentifierAccessor} for the given target.
	 *
	 * @param target must not be {@literal null}.
	 */
	public PersistableIdentifierAccessor(Persistable<?> target) {

		super(target);

		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.IdentifierAccessor#getIdentifier()
	 */
	@Override
	@Nullable
	public Object getIdentifier() {
		return target.getId();
	}
}
