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

import java.util.function.Supplier;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 */
public interface PropertyValueConverterFactory {

	/**
	 * Get the {@link PropertyValueConverter} applicable for the given {@link PersistentProperty}.
	 *
	 * @param property must not be {@literal null}.
	 * @param <A> domain specific type
	 * @param <B> store native type
	 * @return can be {@literal null}.
	 */
	@Nullable
	default <A, B> PropertyValueConverter<A, B> getConverter(PersistentProperty<?> property) {
		if (!property.hasValueConverter()) {
			return null;
		}
		return getConverter((Class<PropertyValueConverter<A, B>>) property.getValueConverterType());
	}

	/**
	 * Get the {@link PropertyValueConverter} applicable for the given {@link PersistentProperty} or return a fallback
	 * converter via the given {@link Supplier}.
	 *
	 * @param property must not be {@literal null}.
	 * @param <A> domain specific type
	 * @param <B> store native type
	 * @return can be {@literal null}.
	 */
	default <A, B> PropertyValueConverter<A, B> getConverter(PersistentProperty<?> property,
			Supplier<? extends PropertyValueConverter<A, B>> fallback) {
		PropertyValueConverter<A, B> resolved = getConverter(property);
		return resolved != null ? resolved : fallback.get();
	}

	// TODO: do we actually need this one or should we only rely on 'resolve(PersistentProperty<?> property)'
	<S, T> PropertyValueConverter<S, T> getConverter(Class<? extends PropertyValueConverter<S, T>> converterType);

	// TODO: do we actually need this one or should we only rely on 'resolve(PersistentProperty<?> property)'
	default <S, T> PropertyValueConverter<S, T> getConverter(Class<? extends PropertyValueConverter<S, T>> converterType,
			Supplier<? extends PropertyValueConverter<S, T>> fallback) {
		PropertyValueConverter<S, T> resolved = getConverter(converterType);
		return resolved != null ? resolved : fallback.get();
	}

	/**
	 * Obtain a {@link PropertyValueConverterFactory} that creates {@link PropertyValueConverter converters} via their
	 * default constructor.
	 *
	 * @return new instance of {@link PropertyValueConverterFactory}.
	 */
	static PropertyValueConverterFactory simple() {
		return new SimplePropertyConverterFactory();
	}

	/**
	 * Obtain a {@link PropertyValueConverterFactory} that caches {@link PropertyValueConverter converters} created by the
	 * given {@link PropertyValueConverterFactory factory}.
	 *
	 * @return new instance of {@link PropertyValueConverterFactory}.
	 */
	static PropertyValueConverterFactory caching(PropertyValueConverterFactory factory) {
		return new CachingPropertyValueConverterFactory(factory);
	}
}
