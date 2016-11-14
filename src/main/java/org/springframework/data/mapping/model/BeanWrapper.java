/*
 * Copyright 2011-2015 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Domain service to allow accessing the values of {@link PersistentProperty}s on a given bean.
 * 
 * @author Oliver Gierke
 */
class BeanWrapper<T> implements PersistentPropertyAccessor {

	private final T bean;

	/**
	 * Creates a new {@link BeanWrapper} for the given bean.
	 * 
	 * @param bean must not be {@literal null}.
	 */
	protected BeanWrapper(T bean) {

		Assert.notNull(bean, "Bean must not be null!");
		this.bean = bean;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#setProperty(org.springframework.data.mapping.PersistentProperty, java.util.Optional)
	 */
	public void setProperty(PersistentProperty<?> property, Optional<? extends Object> value) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		try {

			if (!property.usePropertyAccess()) {

				Field field = property.getField().get();

				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, bean, value.orElse(null));
				return;
			}

			Optional<Method> setter = property.getSetter();

			setter.ifPresent(it -> {

				ReflectionUtils.makeAccessible(it);
				ReflectionUtils.invokeMethod(it, bean, value.orElse(null));
			});

		} catch (IllegalStateException e) {
			throw new MappingException("Could not set object property!", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	public Optional<? extends Object> getProperty(PersistentProperty<?> property) {
		return getProperty(property, property.getType());
	}

	/**
	 * Returns the value of the given {@link PersistentProperty} potentially converted to the given type.
	 * 
	 * @param <S>
	 * @param property must not be {@literal null}.
	 * @param type can be {@literal null}.
	 * @return
	 * @throws MappingException in case an exception occured when accessing the property.
	 */
	@SuppressWarnings("unchecked")
	public <S> Optional<S> getProperty(PersistentProperty<?> property, Class<? extends S> type) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		try {

			if (!property.usePropertyAccess()) {

				Field field = property.getField().get();
				ReflectionUtils.makeAccessible(field);
				return Optional.ofNullable((S) ReflectionUtils.getField(field, bean));
			}

			Optional<Method> getter = property.getGetter();

			return getter.map(it -> {

				ReflectionUtils.makeAccessible(it);
				return (S) ReflectionUtils.invokeMethod(it, bean);
			});

		} catch (IllegalStateException e) {
			throw new MappingException(
					String.format("Could not read property %s of %s!", property.toString(), bean.toString()), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getBean()
	 */
	public T getBean() {
		return bean;
	}
}
