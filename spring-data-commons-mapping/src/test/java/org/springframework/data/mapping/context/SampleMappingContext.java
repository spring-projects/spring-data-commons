/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.mapping.context;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

public class SampleMappingContext
		extends AbstractMappingContext<BasicPersistentEntity<Object, SamplePersistentProperty>, SamplePersistentProperty> {

	@Override
	@SuppressWarnings("unchecked")
	protected <S> BasicPersistentEntity<Object, SamplePersistentProperty> createPersistentEntity(
			TypeInformation<S> typeInformation) {
		return new BasicPersistentEntity<>((TypeInformation<Object>) typeInformation);
	}

	@Override
	protected SamplePersistentProperty createPersistentProperty(Property property,
			BasicPersistentEntity<Object, SamplePersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		return new SamplePersistentProperty(property, owner, simpleTypeHolder);
	}
}
