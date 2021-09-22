/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.repository.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;

/**
 * Factory been to create {@link PersistentEntities} from a {@link MappingContext}.
 * 
 * @author Jens Schauder
 * @since 3.0
 */
public class PersistentEntitiesFactoryBean implements FactoryBean<PersistentEntities> {

	private final MappingContext context;

	/**
	 * Creates a new {@link PersistentEntitiesFactoryBean} for the given {@link MappingContext}.
	 *
	 * @param context must not be {@literal null}.
	 */
	public PersistentEntitiesFactoryBean(MappingContext context) {
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public PersistentEntities getObject() {
		return PersistentEntities.of(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return PersistentEntities.class;
	}
}
