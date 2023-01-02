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

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Accessor to obtain metadata for {@link PropertyValueConverter} from an annotated {@link PersistentProperty}.
 *
 * @author Mark Paluch
 * @since 2.7
 */
class AnnotatedPropertyValueConverterAccessor {

	private final ValueConverter annotation;

	public AnnotatedPropertyValueConverterAccessor(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null");
		annotation = property.findAnnotation(ValueConverter.class);
	}

	/**
	 * Obtain the {@link PropertyValueConverter converter type} to be used for reading and writing property values. Uses
	 * the {@link ValueConverter} annotation and extracts its {@link ValueConverter#value() value} attribute.
	 *
	 * @return {@literal null} if none defined. Check {@link #hasValueConverter()} to check if the annotation is present
	 *         at all.
	 */
	@Nullable
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<? extends PropertyValueConverter<?, ?, ? extends ValueConversionContext<? extends PersistentProperty<?>>>> getValueConverterType() {
		return annotation != null ? (Class) annotation.value() : null;
	}

	/**
	 * Return whether a value converter is configured. Uses {@link ValueConverter} as annotation type.
	 *
	 * @return {@literal true} if a value converter is configured.
	 */
	public boolean hasValueConverter() {
		return annotation != null;
	}

}
