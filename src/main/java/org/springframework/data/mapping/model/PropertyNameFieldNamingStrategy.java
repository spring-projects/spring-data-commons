/*
 * Copyright 2013-2020 the original author or authors.
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

import org.springframework.data.annotation.Embedded;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.StringUtils;

/**
 * {@link FieldNamingStrategy} simply using the {@link PersistentProperty}'s name.
 *
 * @since 1.9
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public enum PropertyNameFieldNamingStrategy implements FieldNamingStrategy {

	INSTANCE;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.FieldNamingStrategy#getFieldName(org.springframework.data.mapping.PersistentProperty)
	 */
	public String getFieldName(PersistentProperty<?> property) {

		if (!property.isEmbedded()) {
			return property.getName();
		}

		String prefix = property.findAnnotation(Embedded.class).prefix();
		if (!StringUtils.hasText(prefix)) {
			return property.getName();
		}

		return prefix + property.getName();
	}
}
