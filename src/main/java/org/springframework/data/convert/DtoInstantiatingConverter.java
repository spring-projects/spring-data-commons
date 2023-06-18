/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Spring {@link Converter} to create instances of the given DTO type from the source value handed into the conversion.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @since 2.7
 */
public class DtoInstantiatingConverter implements Converter<Object, Object> {

	private final Class<?> targetType;
	private final MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context;
	private final EntityInstantiator instantiator;

	/**
	 * Create a new {@link Converter} to instantiate DTOs.
	 *
	 * @param dtoType must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @param instantiators must not be {@literal null}.
	 */
	public DtoInstantiatingConverter(Class<?> dtoType,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context,
			EntityInstantiators instantiators) {

		Assert.notNull(dtoType, "DTO type must not be null");
		Assert.notNull(context, "MappingContext must not be null");
		Assert.notNull(instantiators, "EntityInstantiators must not be null");

		this.targetType = dtoType;
		this.context = context;
		this.instantiator = instantiators.getInstantiatorFor(context.getRequiredPersistentEntity(dtoType));
	}

	@NonNull
	@Override
	public Object convert(Object source) {

		if (targetType.isInterface()) {
			return source;
		}

		PersistentEntity<?, ? extends PersistentProperty<?>> sourceEntity = context
				.getRequiredPersistentEntity(source.getClass());
		PersistentPropertyAccessor<Object> sourceAccessor = sourceEntity.getPropertyAccessor(source);
		PersistentEntity<?, ? extends PersistentProperty<?>> targetEntity = context.getRequiredPersistentEntity(targetType);

		@SuppressWarnings({ "rawtypes", "unchecked" })
		Object dto = instantiator.createInstance(targetEntity, new ParameterValueProvider() {

			@Override
			@Nullable
			public Object getParameterValue(Parameter parameter) {

				String name = parameter.getName();

				if (name == null) {
					throw new IllegalArgumentException(String.format("Parameter %s does not have a name", parameter));
				}

				return sourceAccessor.getProperty(sourceEntity.getRequiredPersistentProperty(name));
			}
		});

		PersistentPropertyAccessor<Object> targetAccessor = targetEntity.getPropertyAccessor(dto);
		InstanceCreatorMetadata<? extends PersistentProperty<?>> creator = targetEntity.getInstanceCreatorMetadata();

		targetEntity.doWithProperties((SimplePropertyHandler) property -> {

			if ((creator != null) && creator.isCreatorParameter(property)) {
				return;
			}

			targetAccessor.setProperty(property,
					sourceAccessor.getProperty(sourceEntity.getRequiredPersistentProperty(property.getName())));
		});

		return dto;
	}
}
