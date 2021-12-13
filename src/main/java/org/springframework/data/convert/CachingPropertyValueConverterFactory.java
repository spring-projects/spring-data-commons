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

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * TODO: This would be easier if we'd get rid of {@link PropertyValueConverterFactory#getConverter(Class)}.
 *
 * @author Christoph Strobl
 * @since 2021/12
 */
class CachingPropertyValueConverterFactory implements PropertyValueConverterFactory {

	private final PropertyValueConverterFactory delegate;
	private Cache cache = new Cache();

	public CachingPropertyValueConverterFactory(PropertyValueConverterFactory delegate) {
		this.delegate = delegate;
	}

	@Nullable
	@Override
	public <S, T> PropertyValueConverter<S, T> getConverter(PersistentProperty<?> property) {

		PropertyValueConverter converter = cache.get(property);
		if (converter != null) {
			return converter;
		}
		return cache.cache(property, delegate.getConverter(property));
	}

	@Override
	public <S, T> PropertyValueConverter<S, T> getConverter(Class<? extends PropertyValueConverter<S, T>> converterType) {

		PropertyValueConverter converter = cache.get(converterType);
		if (converter != null) {
			return converter;
		}
		return cache.cache(converterType, delegate.getConverter(converterType));
	}

	static class Cache {

		Map<PersistentProperty<?>, PropertyValueConverter<?, ?>> perPropertyCache = new HashMap<>();
		Map<Class<?>, PropertyValueConverter<?, ?>> typeCache = new HashMap<>();

		PropertyValueConverter<?, ?> get(PersistentProperty<?> property) {
			return perPropertyCache.get(property);
		}

		PropertyValueConverter<?, ?> get(Class<?> type) {
			return typeCache.get(type);
		}

		<S, T> PropertyValueConverter<S, T> cache(PersistentProperty<?> property, PropertyValueConverter<S, T> converter) {
			perPropertyCache.putIfAbsent(property, converter);
			cache(property.getValueConverterType(), converter);
			return converter;
		}

		<S, T> PropertyValueConverter<S, T> cache(Class<?> type, PropertyValueConverter<S, T> converter) {
			typeCache.putIfAbsent(type, converter);
			return converter;
		}
	}
}
