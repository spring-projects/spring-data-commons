/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.mapping;

import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.lang.Nullable;

/**
 * Domain service to allow accessing and setting {@link PersistentProperty}s of an entity. Usually obtained through
 * {@link PersistentEntity#getPropertyAccessor(Object)}. In case type conversion shall be applied on property access,
 * use a {@link ConvertingPropertyAccessor}.
 *
 * @author Oliver Gierke
 * @since 1.10
 * @see PersistentEntity#getPropertyAccessor(Object)
 * @see ConvertingPropertyAccessor
 */
public interface PersistentPropertyAccessor {

	/**
	 * Sets the given {@link PersistentProperty} to the given value. Will do type conversion if a
	 * {@link org.springframework.core.convert.ConversionService} is configured.
	 *
	 * @param property must not be {@literal null}.
	 * @param value can be {@literal null}.
	 * @throws MappingException in case an exception occurred when setting the property value.
	 */
	void setProperty(PersistentProperty<?> property, @Nullable Object value);

	/**
	 * Returns the value of the given {@link PersistentProperty} of the underlying bean instance.
	 *
	 * @param property must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	@Nullable
	Object getProperty(PersistentProperty<?> property);

	/**
	 * Returns the underlying bean.
	 *
	 * @return will never be {@literal null}.
	 */
	Object getBean();
}
