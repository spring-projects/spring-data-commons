/*
 * Copyright (c) 2011 by the original author(s).
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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Value object to allow creation of objects using the metamodel, setting and getting properties.
 * 
 * @author Oliver Gierke
 */
public class BeanWrapper<E extends PersistentEntity<T, ?>, T> {

	private final T bean;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link BeanWrapper} for the given bean instance and {@link ConversionService}. If
	 * {@link ConversionService} is {@literal null} no property type conversion will take place.
	 * 
	 * @param <E>
	 * @param <T>
	 * @param bean must not be {@literal null}
	 * @param conversionService
	 * @return
	 */
	public static <E extends PersistentEntity<T, ?>, T> BeanWrapper<E, T> create(T bean,
			ConversionService conversionService) {
		return new BeanWrapper<E, T>(bean, conversionService);
	}

	/**
	 * Creates a new {@link BeanWrapper} using the given {@link PersistentEntity} and {@link ParameterValueProvider}. Will
	 * instantly create a bean instance using the {@link PreferredConstructor} of the {@link PersistentEntity}.
	 * 
	 * @param <E>
	 * @param <T>
	 * @param entity
	 * @param provider
	 * @param conversionService
	 * @return
	 */
	public static <E extends PersistentEntity<T, ?>, T> BeanWrapper<E, T> create(E entity,
			ParameterValueProvider provider, ConversionService conversionService) {
		return new BeanWrapper<E, T>(entity, provider, conversionService);
	}

	private BeanWrapper(T bean, ConversionService conversionService) {
		Assert.notNull(bean);
		this.bean = bean;
		this.conversionService = conversionService;
	}

	@SuppressWarnings("unchecked")
	private BeanWrapper(E entity, ParameterValueProvider provider, ConversionService conversionService) {

		this.conversionService = conversionService;

		T bean = null;

		PreferredConstructor<T> constructor = entity.getPreferredConstructor();
		if (null == constructor) {
			try {
				Class<T> clazz = entity.getType();
				if (clazz.isArray()) {
					Class<?> ctype = clazz;
					int dims = 0;
					while (ctype.isArray()) {
						ctype = ctype.getComponentType();
						dims++;
					}
					bean = (T) Array.newInstance(clazz, dims);
				} else {
					bean = BeanUtils.instantiateClass(entity.getType());
				}
			} catch (BeanInstantiationException e) {
				throw new MappingInstantiationException(e.getMessage(), e);
			}
		}

		List<Object> params = new LinkedList<Object>();
		if (null != provider && constructor.hasParameters()) {
			for (Parameter<?> parameter : constructor.getParameters()) {
				params.add(provider.getParameterValue(parameter));
			}
		}

		try {
			bean = BeanUtils.instantiateClass(constructor.getConstructor(), params.toArray());
		} catch (BeanInstantiationException e) {
			throw new MappingInstantiationException(e.getMessage(), e);
		}

		this.bean = bean;
	}

	/**
	 * Sets the given {@link PersistentProperty} to the given value. Will do type conversion if a
	 * {@link ConversionService} is configured. Will use the accessor method of the given {@link PersistentProperty} if it
	 * has one or field access otherwise.
	 * 
	 * @param property
	 * @param value
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void setProperty(PersistentProperty<?> property, Object value) throws IllegalAccessException,
			InvocationTargetException {
		setProperty(property, value, false);
	}

	/**
	 * Sets the given {@link PersistentProperty} to the given value. Will do type conversion if a
	 * {@link ConversionService} is configured.
	 * 
	 * @param property
	 * @param value
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void setProperty(PersistentProperty<?> property, Object value, boolean fieldAccessOnly)
			throws IllegalAccessException, InvocationTargetException {

		Method setter = property.getPropertyDescriptor() != null ? property.getPropertyDescriptor().getWriteMethod() : null;

		if (fieldAccessOnly || null == setter) {
			Object valueToSet = getPotentiallyConvertedValue(value, property.getType());
			ReflectionUtils.makeAccessible(property.getField());
			ReflectionUtils.setField(property.getField(), bean, valueToSet);
			return;
		}

		Class<?>[] paramTypes = setter.getParameterTypes();
		Object valueToSet = getPotentiallyConvertedValue(value, paramTypes[0]);
		ReflectionUtils.makeAccessible(setter);
		ReflectionUtils.invokeMethod(setter, bean, valueToSet);
	}

	/**
	 * Returns the value of the given {@link PersistentProperty} of the underlying bean instance.
	 * 
	 * @param <S>
	 * @param property
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Object getProperty(PersistentProperty<?> property)
		throws IllegalAccessException, InvocationTargetException {
		return getProperty(property, property.getType(), false);
	}

	/**
	 * Returns the value of the given {@link PersistentProperty} potentially converted to the given type.
	 * 
	 * @param <S>
	 * @param property
	 * @param type
	 * @param fieldAccessOnly
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public <S> S getProperty(PersistentProperty<?> property, Class<? extends S> type, boolean fieldAccessOnly)
			throws IllegalAccessException, InvocationTargetException {
		Object obj;
		Field field = property.getField();
		Method getter = (null != property.getPropertyDescriptor() ? property.getPropertyDescriptor().getReadMethod() : null);
		if (fieldAccessOnly || null == getter) {
			ReflectionUtils.makeAccessible(field);
			obj = ReflectionUtils.getField(field, bean);
		} else {
			ReflectionUtils.makeAccessible(getter);
			obj = ReflectionUtils.invokeMethod(getter, bean);
		}
		
		return getPotentiallyConvertedValue(obj, type);
	}

	/**
	 * Converts the given source value if it is not assignable to the given target type.
	 * 
	 * @param source
	 * @param targetType
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
	 * @return
	 */
	public T getBean() {
		return bean;
	}
}
