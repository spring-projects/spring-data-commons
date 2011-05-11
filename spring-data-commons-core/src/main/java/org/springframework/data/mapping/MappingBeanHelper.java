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

package org.springframework.data.mapping;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.model.MappingInstantiationException;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.mapping.model.PreferredConstructor;
import org.springframework.data.mapping.model.PreferredConstructor.Parameter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ReflectionUtils;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Oliver Gierke
 */
public abstract class MappingBeanHelper {

	protected static GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
	protected static SpelExpressionParser parser = new SpelExpressionParser();
	protected static Set<Class<?>> simpleTypes = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

	static {
		simpleTypes.add(boolean.class);
		simpleTypes.add(boolean[].class);
		simpleTypes.add(long.class);
		simpleTypes.add(long[].class);
		simpleTypes.add(short.class);
		simpleTypes.add(short[].class);
		simpleTypes.add(int.class);
		simpleTypes.add(int[].class);
		simpleTypes.add(byte.class);
		simpleTypes.add(byte[].class);
		simpleTypes.add(float.class);
		simpleTypes.add(float[].class);
		simpleTypes.add(double.class);
		simpleTypes.add(double[].class);
		simpleTypes.add(char.class);
		simpleTypes.add(char[].class);
		simpleTypes.add(Boolean.class);
		simpleTypes.add(Long.class);
		simpleTypes.add(Short.class);
		simpleTypes.add(Integer.class);
		simpleTypes.add(Byte.class);
		simpleTypes.add(Float.class);
		simpleTypes.add(Double.class);
		simpleTypes.add(Character.class);
		simpleTypes.add(String.class);
		simpleTypes.add(java.util.Date.class);
		simpleTypes.add(Locale.class);
		simpleTypes.add(Class.class);
	}

	public static GenericConversionService getConversionService() {
		return conversionService;
	}

	public static void setConversionService(GenericConversionService conversionService) {
		MappingBeanHelper.conversionService = conversionService;
	}

	public static Set<Class<?>> getSimpleTypes() {
		return simpleTypes;
	}

	public static boolean isSimpleType(Class<?> type) {
		for (Class<?> clazz : simpleTypes) {
			if (type == clazz || type.isAssignableFrom(clazz)) {
				return true;
			}
		}
		return type.isEnum();
	}

	public static <T, P extends PersistentProperty<P>> T constructInstance(PersistentEntity<T, P> entity, PreferredConstructor.ParameterValueProvider provider) {
		return constructInstance(entity, provider, new StandardEvaluationContext());
	}

	@SuppressWarnings({"unchecked"})
	public static <T, P extends PersistentProperty<P>> T constructInstance(PersistentEntity<T, P> entity, PreferredConstructor.ParameterValueProvider provider, EvaluationContext spelCtx) {

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
					return (T) Array.newInstance(clazz, dims);
				} else {
					return BeanUtils.instantiateClass(entity.getType());
				}
			} catch (BeanInstantiationException e) {
				throw new MappingInstantiationException(e.getMessage(), e);
			}
		}

		List<Object> params = new LinkedList<Object>();
		if (null != provider && constructor.getParameters().size() > 0) {
			for (Parameter<?> parameter : constructor.getParameters()) {
				String key = parameter.getKey();
				Object obj;
				if (null != key) {
					Expression x = parser.parseExpression(key);
					obj = x.getValue(spelCtx);
				} else {
					obj = provider.getParameterValue(parameter);
				}
				params.add(obj);
			}
		}

		T obj = null;
		try {
			obj = BeanUtils.instantiateClass(constructor.getConstructor(), params.toArray());
		} catch (BeanInstantiationException e) {
			throw new MappingInstantiationException(e.getMessage(), e);
		}

		return obj;
	}

	public static void setProperty(Object on,
																 PersistentProperty<?> property,
																 Object value)
			throws IllegalAccessException, InvocationTargetException {
		setProperty(on, property, value, false);
	}

	public static void setProperty(Object on,
																 PersistentProperty<?> property,
																 Object value,
																 boolean fieldAccessOnly)
			throws IllegalAccessException, InvocationTargetException {

		Method setter = property.getPropertyDescriptor() != null ? property.getPropertyDescriptor().getWriteMethod() : null;

		if (fieldAccessOnly || null == setter) {
			Object valueToSet = getPotentiallyConvertedValue(value, property.getType());
			ReflectionUtils.makeAccessible(property.getField());
			ReflectionUtils.setField(property.getField(), on, valueToSet);
			return;
		}

		Class<?>[] paramTypes = setter.getParameterTypes();
		Object valueToSet = getPotentiallyConvertedValue(value, paramTypes[0]);
		ReflectionUtils.makeAccessible(setter);
		ReflectionUtils.invokeMethod(setter, on, valueToSet);
	}

	/**
	 * Converts the given source value if it is not assignable to the given target type.
	 *
	 * @param source
	 * @param targetType
	 * @return
	 */
	private static Object getPotentiallyConvertedValue(Object source, Class<?> targetType) {
		if (source != null && source.getClass().isAssignableFrom(targetType)) {
			return source;
		}

		return conversionService.convert(source, targetType);
	}

	@SuppressWarnings({"unchecked"})
	public static <T> T getProperty(Object from,
																	PersistentProperty<?> property,
																	Class<T> type,
																	boolean fieldAccessOnly)
			throws IllegalAccessException, InvocationTargetException {
		Object obj;
		Field field = property.getField();
		Method getter = (null != property.getPropertyDescriptor() ? property.getPropertyDescriptor().getReadMethod() : null);
		if (fieldAccessOnly || null == getter) {
			ReflectionUtils.makeAccessible(field);
			obj = ReflectionUtils.getField(field, from);
		} else {
			ReflectionUtils.makeAccessible(getter);
			obj = ReflectionUtils.invokeMethod(getter, from);
		}
		if (null != obj && !obj.getClass().isAssignableFrom(type)) {
			return conversionService.convert(obj, type);
		} else {
			return (T) obj;
		}
	}

	public static Class<?> getTargetType(TypeVariable<?> tv) {
		Class<?> targetType = Object.class;
		Type[] bounds = tv.getBounds();
		if (bounds.length > 0) {
			if (bounds[0] instanceof ParameterizedType) {
				return getTargetType((ParameterizedType) bounds[0]);
			} else if (bounds[0] instanceof TypeVariable) {
				return getTargetType((TypeVariable<?>) bounds[0]);
			} else {
				targetType = (Class<?>) bounds[0];
			}
		}
		return targetType;
	}

	public static Class<?> getTargetType(ParameterizedType ptype) {
		Class<?> targetType;
		Type[] types = ptype.getActualTypeArguments();
		if (types.length == 1) {
			if (types[0] instanceof TypeVariable) {
				// Placeholder type
				targetType = Object.class;
			} else {
				if (types[0] instanceof ParameterizedType) {
					return getTargetType((ParameterizedType) types[0]);
				} else {
					targetType = (Class<?>) types[0];
				}
			}
		} else {
			targetType = (Class<?>) ptype.getRawType();
		}
		return targetType;
	}


}
