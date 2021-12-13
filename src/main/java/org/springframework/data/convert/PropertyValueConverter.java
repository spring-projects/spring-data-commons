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

import org.springframework.lang.Nullable;

/**
 * {@link PropertyValueConverter} provides a symmetric way of converting certain properties from domain to store
 * specific values.
 * <p>
 * A {@link PropertyValueConverter} is, other than a {@link ReadingConverter} or {@link WritingConverter}, only applied
 * to special annotated fields which allows a fine grained conversion of certain values within a specific context.
 *
 * @author Christoph Strobl
 * @param <A> domain specific type
 * @param <B> store native type
 * @since 2.7
 */
public interface PropertyValueConverter<A, B> {

	/**
	 * Convert the given store specific value into it's domain value representation.
	 *
	 * @param nativeValue can be {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	A /*read*/nativeToDomain(@Nullable B nativeValue);

	/**
	 * Convert the given domain specific value into it's native store representation.
	 *
	 * @param domainValue can be {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	B /*write*/domainToNative(@Nullable A domainValue);

	enum ObjectToObjectPropertyValueConverter implements PropertyValueConverter<Object, Object> {

		INSTANCE;

		@Override
		public Object nativeToDomain(Object value) {
			return value;
		}

		@Override
		public Object domainToNative(Object value) {
			return value;
		}
	}
}
