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
package org.springframework.data.convert;

import java.util.EnumSet;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * Trivial implementation of {@link PropertyValueConverter}.
 *
 * @author Christoph Strobl
 */
class SimplePropertyConverterFactory implements PropertyValueConverterFactory {

	@Override
	public <S, T> PropertyValueConverter<S, T> getConverter(Class<? extends PropertyValueConverter<S, T>> converterType) {

		Assert.notNull(converterType, "ConverterType must not be null!");

		if (converterType.isEnum()) {
			return (PropertyValueConverter<S, T>) EnumSet.allOf((Class) converterType).iterator().next();
		}
		return BeanUtils.instantiateClass(converterType);
	}
}
