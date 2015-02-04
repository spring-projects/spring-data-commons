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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Value object to allow creation of objects using the metamodel, setting and getting properties.
 * 
 * @author Oliver Gierke
 */
public class BeanWrapper<T> {

	private final T bean;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link BeanWrapper} for the given bean instance and {@link ConversionService}. If
	 * {@link ConversionService} is {@literal null} no property type conversion will take place.
	 * 
	 * @param <T>
	 * @param bean must not be {@literal null}.
	 * @param conversionService can be {@literal null}.
	 * @return
	 */
	public static <T> BeanWrapper<T> create(T bean, ConversionService conversionService) {

		Assert.notNull(bean, "Wrapped instance must not be null!");
		return new BeanWrapper<T>(bean, conversionService);
	}

	private BeanWrapper(T bean, ConversionService conversionService) {

		this.bean = bean;
		this.conversionService = conversionService;
	}

	/**
	 * Sets the given {@link PersistentProperty} to the given value. Will do type conversion if a
	 * {@link ConversionService} is configured.
	 * 
	 * @param property must not be {@literal null}.
	 * @param value can be {@literal null}.
	 * @throws MappingException in case an exception occurred when setting the property value.
	 */
	public void setProperty(PersistentProperty<?> property, Object value) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		try {

			if (!property.usePropertyAccess()) {

				Object valueToSet = getPotentiallyConvertedValue(value, property.getType());
				ReflectionUtils.makeAccessible(property.getField());
				ReflectionUtils.setField(property.getField(), bean, valueToSet);
				return;
			}

			Method setter = property.getSetter();

			if (property.usePropertyAccess() && setter != null) {

				Class<?>[] paramTypes = setter.getParameterTypes();
				Object valueToSet = getPotentiallyConvertedValue(value, paramTypes[0]);
				ReflectionUtils.makeAccessible(setter);
				ReflectionUtils.invokeMethod(setter, bean, valueToSet);
			}

		} catch (IllegalStateException e) {
			throw new MappingException("Could not set object property!", e);
		}
	}

	/**
	 * Returns the value of the given {@link PersistentProperty} of the underlying bean instance.
	 * 
	 * @param <S>
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public Object getProperty(PersistentProperty<?> property) {
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
	public <S> S getProperty(PersistentProperty<?> property, Class<? extends S> type) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		try {

			Object obj = null;

			if (!property.usePropertyAccess()) {

				Field field = property.getField();
				ReflectionUtils.makeAccessible(field);
				obj = ReflectionUtils.getField(field, bean);
			}

			Method getter = property.getGetter();

			if (property.usePropertyAccess() && getter != null) {

				ReflectionUtils.makeAccessible(getter);
				obj = ReflectionUtils.invokeMethod(getter, bean);
			}

			return getPotentiallyConvertedValue(obj, type);

		} catch (IllegalStateException e) {
			throw new MappingException(String.format("Could not read property %s of %s!", property.toString(),
					bean.toString()), e);
		}
	}

	/**
	 * Converts the given source value if it is not assignable to the given target type.
	 * 
	 * @param source can be {@literal null}.
	 * @param targetType can be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <S> S getPotentiallyConvertedValue(Object source, Class<S> targetType) {

		boolean conversionServiceAvailable = conversionService != null;
		boolean conversionNeeded = source == null || !targetType.isAssignableFrom(source.getClass());

		if (conversionServiceAvailable && conversionNeeded) {
			return conversionService.convert(source, targetType);
		}

		return (S) source;
	}

	/**
	 * Returns the underlying bean instance.
	 * 
	 * @return will never be {@literal null}.
	 */
	public T getBean() {
		return bean;
	}
}
