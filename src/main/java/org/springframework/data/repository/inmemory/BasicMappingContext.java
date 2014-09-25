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
package org.springframework.data.repository.inmemory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 */
public class BasicMappingContext extends
		AbstractMappingContext<BasicPersistentEntity<?, BasicPersistentProperty>, BasicPersistentProperty> {

	@Override
	protected <T> BasicPersistentEntity<?, BasicPersistentProperty> createPersistentEntity(
			TypeInformation<T> typeInformation) {

		return new BasicPersistentEntity<T, BasicPersistentProperty>(typeInformation);
	}

	@Override
	protected BasicPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
			BasicPersistentEntity<?, BasicPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		return new BasicPersistentProperty(field, descriptor, owner, simpleTypeHolder);
	}

}
