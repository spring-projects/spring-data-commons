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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.model.*;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BasicMappingContext implements MappingContext, InitializingBean {

  protected Logger log = LoggerFactory.getLogger(getClass());
  protected MappingConfigurationBuilder builder;
  protected ConcurrentMap<String, PersistentEntity<?>> persistentEntities = new ConcurrentHashMap<String, PersistentEntity<?>>();
  protected ConcurrentMap<PersistentEntity<?>, List<Validator>> validators = new ConcurrentHashMap<PersistentEntity<?>, List<Validator>>();
  protected ConcurrentSkipListSet<Listener> listeners = new ConcurrentSkipListSet<Listener>();
  protected GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();

  public BasicMappingContext() {
    builder = new BasicMappingConfigurationBuilder(this);
  }

  public BasicMappingContext(MappingConfigurationBuilder builder) {
    this.builder = builder;
  }

  public BasicMappingContext(MappingConfigurationBuilder builder, GenericConversionService conversionService) {
    this.builder = builder;
    this.conversionService = conversionService;
  }

  @Override
  public Collection<PersistentEntity<?>> getPersistentEntities() {
    return persistentEntities.values();
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> PersistentEntity<T> getPersistentEntity(Class<T> type) {
    return (PersistentEntity<T>) persistentEntities.get(type.getName());
  }

  @Override
  public PersistentEntity<?> getPersistentEntity(String name) {
    return persistentEntities.get(name);
  }

  @Override
  public <T> PersistentEntity<T> addPersistentEntity(Class<T> type) {
    if (null == persistentEntities.get(type.getName())) {
      try {
        PersistentEntity<T> entity = builder.createPersistentEntity(type);
        BeanInfo info = Introspector.getBeanInfo(type);

        Map<String, PropertyDescriptor> descriptors = new HashMap<String, PropertyDescriptor>();
        for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
          descriptors.put(descriptor.getName(), descriptor);
        }

        for (Field field : type.getDeclaredFields()) {
          PropertyDescriptor descriptor = descriptors.get(field.getName());
          if (builder.isPersistentProperty(field, descriptor)) {
            PersistentProperty<?> property = builder.createPersistentProperty(field, descriptor);
            property.setOwner(entity);
            entity.addPersistentProperty(property);
            if (builder.isAssociation(field, descriptor)) {
              Association association = builder.createAssociation(property);
              entity.addAssociation(association);
            }
          }
        }

        entity.setIdProperty(builder.getIdProperty(type));
        entity.setPreferredConstructor(builder.getPreferredConstructor(type));

        // Inform listeners
        List<Listener> listenersToRemove = new ArrayList<Listener>();
        for (Listener listener : listeners) {
          if (!listener.persistentEntityAdded(entity)) {
            listenersToRemove.add(listener);
          }
        }
        for (Listener listener : listenersToRemove) {
          listeners.remove(listener);
        }

        persistentEntities.put(type.getName(), entity);

        return entity;
      } catch (MappingConfigurationException e) {
        log.error(e.getMessage(), e);
      } catch (IntrospectionException e) {
        throw new MappingException(e.getMessage(), e);
      }
    }
    return null;
  }

  @Override
  public void addEntityValidator(PersistentEntity<?> entity, Validator validator) {
    List<Validator> v = validators.get(entity);
    if (null == v) {
      v = new ArrayList<Validator>();
      validators.put(entity, v);
    }
    v.add(validator);
  }

  @Override
  public <S, T> void addTypeConverter(Converter<S, T> converter) {
    conversionService.addConverter(converter);
  }

  @Override
  public ConversionService getConversionService() {
    return conversionService;
  }

  @Override
  public ConverterRegistry getConverterRegistry() {
    return conversionService;
  }

  @Override
  public List<Validator> getEntityValidators(PersistentEntity<?> entity) {
    return validators.get(entity);
  }

  @Override
  public MappingConfigurationBuilder getMappingConfigurationBuilder() {
    return builder;
  }

  @Override
  public void setMappingConfigurationBuilder(MappingConfigurationBuilder builder) {
    this.builder = builder;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public boolean isPersistentEntity(Object value) {
    if (null != value) {
      Class clazz;
      if (value instanceof Class) {
        clazz = ((Class) value);
      } else {
        clazz = value.getClass();
      }
      return builder.isPersistentEntity(clazz);
    }
    return false;
  }

  @Override
  public void addContextListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(builder, "No mapping configuration provider configured.");
  }
}
