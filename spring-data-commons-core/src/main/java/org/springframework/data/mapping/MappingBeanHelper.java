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

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.model.MappingInstantiationException;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.mapping.model.PreferredConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

  public static <T> T constructInstance(PersistentEntity<T> entity,
                                        PreferredConstructor.ParameterValueProvider provider) {
    return constructInstance(entity, provider, new StandardEvaluationContext());
  }

  public static <T> T constructInstance(PersistentEntity<T> entity,
                                        PreferredConstructor.ParameterValueProvider provider,
                                        EvaluationContext spelCtx) {

    PreferredConstructor<T> constructor = entity.getPreferredConstructor();
    if (null == constructor) {
      try {
        return BeanUtils.instantiateClass(entity.getType());  
      } catch (BeanInstantiationException e) {
        throw new MappingInstantiationException(e.getMessage(), e);
      }
    }

    List<Object> params = new LinkedList<Object>();
    if (null != provider && constructor.getParameters().size() > 0) {
      for (PreferredConstructor.Parameter parameter : constructor.getParameters()) {
        Value v = parameter.getValue();
        Object obj;
        if (null != v) {
          Expression x = parser.parseExpression(v.value());
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
                                 PersistentProperty property,
                                 Object value)
      throws IllegalAccessException, InvocationTargetException {
    setProperty(on, property, value, false);
  }

  public static void setProperty(Object on,
                                 PersistentProperty property,
                                 Object value,
                                 boolean fieldAccessOnly)
      throws IllegalAccessException, InvocationTargetException {

    Field field = property.getField();
    Method setter = (null != property.getPropertyDescriptor() ? property.getPropertyDescriptor().getWriteMethod() : null);
    if (fieldAccessOnly || null == setter) {
      if (null != value && value.getClass().isAssignableFrom(field.getType())) {
        field.set(on, value);
      } else {
        field.set(on, conversionService.convert(value, field.getType()));
      }
      return;
    }

    Class<?>[] paramTypes = setter.getParameterTypes();
    if (null != value && paramTypes.length > 0 && !value.getClass().isAssignableFrom(paramTypes[0])) {
      setter.invoke(on, conversionService.convert(value, paramTypes[0]));
    } else {
      setter.invoke(on, value);
    }
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T getProperty(Object from,
                                  PersistentProperty property,
                                  Class<T> type,
                                  boolean fieldAccessOnly)
      throws IllegalAccessException, InvocationTargetException {
    Object obj;
    Field field = property.getField();
    Method getter = (null != property.getPropertyDescriptor() ? property.getPropertyDescriptor().getReadMethod() : null);
    if (fieldAccessOnly || null == getter) {
      obj = field.get(from);
    } else {
      obj = getter.invoke(from);
    }
    if (null != obj && !obj.getClass().isAssignableFrom(type)) {
      return conversionService.convert(obj, type);
    } else {
      return (T) obj;
    }
  }

}
