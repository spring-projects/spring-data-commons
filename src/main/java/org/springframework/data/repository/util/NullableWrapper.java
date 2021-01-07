/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.repository.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

/**
 * Simple value object to wrap a nullable delegate. Used to be able to write {@link Converter} implementations that
 * convert {@literal null} into an object of some sort.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.8
 * @see QueryExecutionConverters
 * @deprecated since 2.4, use {@link org.springframework.data.util.NullableWrapper} instead.
 */
@Deprecated
public class NullableWrapper extends org.springframework.data.util.NullableWrapper {

	/**
	 * Creates a new {@link NullableWrapper} for the given value.
	 *
	 * @param value can be {@literal null}.
	 */
	public NullableWrapper(@Nullable Object value) {
		super(value);
	}
}
