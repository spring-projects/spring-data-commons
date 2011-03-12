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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.annotation.*;
import org.springframework.data.mapping.model.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BasicMappingConfigurationBuilder implements MappingConfigurationBuilder {

  protected static ConcurrentMap<Class<?>, BeanInfo> beanInfo = new ConcurrentHashMap<Class<?>, BeanInfo>();
  protected Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public <T> boolean isPersistentEntity(Class<T> type) {
    if (type.isAnnotationPresent(Persistent.class)) {
      return true;
    } else {
      for (Annotation annotation : type.getDeclaredAnnotations()) {
        if (annotation.annotationType().isAnnotationPresent(Persistent.class)) {
          return true;
        }
      }
      for (Field field : type.getDeclaredFields()) {
        if (field.isAnnotationPresent(Id.class)) {
          return true;
        }
      }
    }
    return false;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> PersistentEntity<T> createPersistentEntity(Class<T> type, MappingContext mappingContext) throws MappingConfigurationException {
    return new BasicPersistentEntity<T>(mappingContext, type);
  }

  @Override
  public boolean isPersistentProperty(Field field, PropertyDescriptor descriptor) throws MappingConfigurationException {
    if ("class".equals(field.getName()) || isTransient(field)) {
      return false;
    }
    return true;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public PersistentProperty<?> createPersistentProperty(Field field, PropertyDescriptor descriptor) throws MappingConfigurationException {
    return new BasicPersistentProperty(field.getName(), field.getType(), field, descriptor);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> PreferredConstructor<T> getPreferredConstructor(Class<T> type) throws MappingConfigurationException {
    // Find the right constructor
    PreferredConstructor<T> preferredConstructor = null;

    for (Constructor<?> constructor : type.getConstructors()) {
      if (constructor.getParameterTypes().length != 0) {
        // Non-no-arg constructor
        if (null == preferredConstructor || constructor.isAnnotationPresent(PersistenceConstructor.class)) {
          preferredConstructor = new PreferredConstructor<T>((Constructor<T>) constructor);

          String[] paramNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(constructor);
          Type[] paramTypes = constructor.getGenericParameterTypes();

          for (int i = 0; i < paramTypes.length; i++) {
            Class<?> targetType = Object.class;
            if (paramTypes[i] instanceof ParameterizedType) {
              ParameterizedType ptype = (ParameterizedType) paramTypes[i];
              Type[] types = ptype.getActualTypeArguments();
              if (types.length == 1) {
                if (types[0] instanceof TypeVariable) {
                  // Placeholder type
                  targetType = Object.class;
                } else {
                  targetType = (Class<?>) types[0];
                }
              } else {
                targetType = (Class<?>) ptype.getRawType();
              }
            } else {
              if (paramTypes[i] instanceof TypeVariable) {
                Type[] bounds = ((TypeVariable) paramTypes[i]).getBounds();
                if (bounds.length > 0) {
                  targetType = (Class<?>) bounds[0];
                }
              } else if (paramTypes[i] instanceof Class<?>) {
                targetType = (Class<?>) paramTypes[i];
              }
            }
            preferredConstructor.addParameter(paramNames[i], targetType, targetType.getDeclaredAnnotations());
          }

          if (constructor.isAnnotationPresent(PersistenceConstructor.class)) {
            // We're done
            break;
          }
        }
      }
    }

    return preferredConstructor;
  }

  @Override
  public boolean isAssociation(Field field, PropertyDescriptor descriptor) throws MappingConfigurationException {
    if (!isTransient(field)) {
      if (field.isAnnotationPresent(Reference.class)) {
        return true;
      }
      for (Annotation annotation : field.getDeclaredAnnotations()) {
        if (annotation.annotationType().isAnnotationPresent(Reference.class)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Association createAssociation(PersistentProperty<?> property) {
    // Only support uni-directional associations in the Basic configuration
    Association association = new Association(property, null);
    property.setAssociation(association);

    return association;
  }

  protected BeanInfo getBeanInfo(Class<?> type) {
    BeanInfo info = beanInfo.get(type);
    if (null == info) {
      try {
        info = Introspector.getBeanInfo(type);
      } catch (IntrospectionException e) {
        throw new MappingException(e.getMessage(), e);
      }
      beanInfo.put(type, info);
    }
    return info;
  }

  protected boolean isTransient(Field field) {
    if (Modifier.isTransient(field.getModifiers())
        || null != field.getAnnotation(Transient.class)
        || null != field.getAnnotation(Value.class)) {
      return true;
    }
    return false;
  }
}
