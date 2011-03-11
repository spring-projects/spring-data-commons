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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class MappingBeanHelper {

  protected static GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
  protected static SpelExpressionParser parser = new SpelExpressionParser();
  protected static Set<String> simpleTypes = new ConcurrentSkipListSet<String>() {{
    add(boolean.class.getName());
    add(long.class.getName());
    add(short.class.getName());
    add(int.class.getName());
    add(byte.class.getName());
    add(float.class.getName());
    add(double.class.getName());
    add(char.class.getName());
    add(Boolean.class.getName());
    add(Long.class.getName());
    add(Short.class.getName());
    add(Integer.class.getName());
    add(Byte.class.getName());
    add(Float.class.getName());
    add(Double.class.getName());
    add(Character.class.getName());
    add(String.class.getName());
    add(java.util.Date.class.getName());
    add(Locale.class.getName());
    add(Class.class.getName());
  }};

  public static GenericConversionService getConversionService() {
    return conversionService;
  }

  public static void setConversionService(GenericConversionService conversionService) {
    MappingBeanHelper.conversionService = conversionService;
  }

  public static Set<String> getSimpleTypes() {
    return simpleTypes;
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
        return entity.getType().newInstance();
      } catch (InstantiationException e) {
        throw new MappingInstantiationException(e.getMessage(), e);
      } catch (IllegalAccessException e) {
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
      obj = constructor.getConstructor().newInstance(params.toArray());
    } catch (InstantiationException e) {
      throw new MappingInstantiationException(e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new MappingInstantiationException(e.getMessage(), e);
    } catch (InvocationTargetException e) {
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

    Field field = property.getField();
    if (fieldAccessOnly || (null == property.getPropertyDescriptor() || null == property.getPropertyDescriptor().getWriteMethod())) {
      field.setAccessible(true);
      if (null != value && value.getClass().isAssignableFrom(field.getType())) {
        field.set(on, value);
      } else {
        field.set(on, conversionService.convert(value, field.getType()));
      }
      return;
    }

    Method setter = property.getPropertyDescriptor().getWriteMethod();
    Class<?>[] paramTypes = setter.getParameterTypes();
    if (null != value && paramTypes.length > 0 && !value.getClass().isAssignableFrom(paramTypes[0])) {
      setter.invoke(on, conversionService.convert(value, paramTypes[0]));
    } else {
      setter.invoke(on, value);
    }
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T getProperty(Object from,
                                  PersistentProperty<?> property,
                                  Class<T> type,
                                  boolean fieldAccessOnly)
      throws IllegalAccessException, InvocationTargetException {

    Field field = property.getField();
    if (fieldAccessOnly || (null == property.getPropertyDescriptor() || null == property.getPropertyDescriptor().getReadMethod())) {
      field.setAccessible(true);
      Object obj = field.get(from);
      if (null != obj && !obj.getClass().isAssignableFrom(type)) {
        return conversionService.convert(obj, type);
      } else {
        return (T) obj;
      }
    }

    Object obj = property.getPropertyDescriptor().getReadMethod().invoke(from);
    if (null != obj && !obj.getClass().isAssignableFrom(type)) {
      return conversionService.convert(obj, type);
    } else {
      return (T) obj;
    }
  }

}
