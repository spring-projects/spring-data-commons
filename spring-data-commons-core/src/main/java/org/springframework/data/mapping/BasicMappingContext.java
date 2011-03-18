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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.event.MappingContextEvent;
import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.MappingConfigurationBuilder;
import org.springframework.data.mapping.model.MappingConfigurationException;
import org.springframework.data.mapping.model.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.validation.Validator;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BasicMappingContext implements MappingContext, InitializingBean, ApplicationContextAware {

  protected Logger log = LoggerFactory.getLogger(getClass());
  protected ApplicationContext applicationContext;
  protected MappingConfigurationBuilder builder;
  protected ConcurrentMap<TypeInformation, PersistentEntity<?>> persistentEntities = new ConcurrentHashMap<TypeInformation, PersistentEntity<?>>();
  protected ConcurrentMap<PersistentEntity<?>, List<Validator>> validators = new ConcurrentHashMap<PersistentEntity<?>, List<Validator>>();
  protected GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
  private List<Class<?>> customSimpleTypes = new ArrayList<Class<?>>();

  public BasicMappingContext() {
    builder = new BasicMappingConfigurationBuilder();
  }

  public BasicMappingContext(MappingConfigurationBuilder builder) {
    this.builder = builder;
  }

  public BasicMappingContext(MappingConfigurationBuilder builder, GenericConversionService conversionService) {
    this.builder = builder;
    this.conversionService = conversionService;
  }

  /**
   * @param customSimpleTypes the customSimpleTypes to set
   */
  public void setCustomSimpleTypes(List<Class<?>> customSimpleTypes) {
    this.customSimpleTypes = customSimpleTypes;
  }

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public Collection<PersistentEntity<?>> getPersistentEntities() {
    return persistentEntities.values();
  }

  /* (non-Javadoc)
   * @see org.springframework.data.mapping.model.MappingContext#getPersistentEntity(java.lang.Class)
   */
  public <T> PersistentEntity<T> getPersistentEntity(Class<T> type) {
    return getPersistentEntity(new ClassTypeInformation(type));
  }

  @SuppressWarnings({"unchecked"})
  public <T> PersistentEntity<T> getPersistentEntity(TypeInformation type) {
    return (PersistentEntity<T>) persistentEntities.get(type);
  }

  @SuppressWarnings("unchecked")
  public <T> PersistentEntity<T> addPersistentEntity(TypeInformation typeInformation) {

    PersistentEntity<?> persistentEntity = persistentEntities.get(typeInformation);

    if (persistentEntity != null) {
      return (PersistentEntity<T>) persistentEntity;
    }

    Class<T> type = (Class<T>) typeInformation.getType();

    try {
      final PersistentEntity<T> entity = builder.createPersistentEntity(typeInformation, this);
      BeanInfo info = Introspector.getBeanInfo(type);

      final Map<String, PropertyDescriptor> descriptors = new HashMap<String, PropertyDescriptor>();
      for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
        descriptors.put(descriptor.getName(), descriptor);
      }

      ReflectionUtils.doWithFields(type, new FieldCallback() {

        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
          try {
            PropertyDescriptor descriptor = descriptors.get(field.getName());
            if (builder.isPersistentProperty(field, descriptor)) {
              ReflectionUtils.makeAccessible(field);
              PersistentProperty property = builder.createPersistentProperty(field, descriptor, entity.getPropertyInformation());
              property.setOwner(entity);
              entity.addPersistentProperty(property);
              if (builder.isAssociation(field, descriptor)) {
                Association association = builder.createAssociation(property);
                entity.addAssociation(association);
              }

              if (property.isIdProperty()) {
                entity.setIdProperty(property);
              }

              TypeInformation nestedType = getNestedTypeToAdd(property);
              if (nestedType != null) {
                addPersistentEntity(nestedType);
              }
            }
          } catch (MappingConfigurationException e) {
            log.error(e.getMessage(), e);
          }
        }
      }, new ReflectionUtils.FieldFilter() {
        public boolean matches(Field field) {
          return !Modifier.isStatic(field.getModifiers());
        }
      });

      entity.setPreferredConstructor(builder.getPreferredConstructor(type));

      // Inform listeners
      if (null != applicationContext) {
        applicationContext.publishEvent(new MappingContextEvent(entity, typeInformation));
      }

      // Cache
      persistentEntities.put(entity.getPropertyInformation(), entity);

      return entity;
    } catch (MappingConfigurationException e) {
      log.error(e.getMessage(), e);
    } catch (IntrospectionException e) {
      throw new MappingException(e.getMessage(), e);
    }

    return null;
  }

  /**
   * Returns a potential nested type tha needs to be added when adding the given property in the course of adding a
   * {@link PersistentEntity}. Will return the property's {@link TypeInformation} directly if it is a potential entity,
   * a collections component type if it's a collection as well as the value type of a {@link Map} if it's a map
   * property.
   *
   * @param property
   * @return the TypeInformation to be added as {@link PersistentEntity} or {@literal
   */
  private TypeInformation getNestedTypeToAdd(PersistentProperty property) {

    TypeInformation typeInformation = property.getTypeInformation();

    if (customSimpleTypes.contains(typeInformation.getType())) {
      return null;
    }

    if (property.isEntity()) {
      return typeInformation;
    }

    if (property.isCollection()) {
      return getTypeInformationIfNotSimpleType(typeInformation.getComponentType());
    }

    if (property.isMap()) {
      return getTypeInformationIfNotSimpleType(typeInformation.getMapValueType());
    }

    return null;
  }

  private TypeInformation getTypeInformationIfNotSimpleType(TypeInformation information) {
    return information == null || MappingBeanHelper.isSimpleType(information.getType()) ? null : information;
  }

  public <T> PersistentEntity<T> addPersistentEntity(Class<T> type) {
    return addPersistentEntity(new ClassTypeInformation(type));
  }

  public void addEntityValidator(PersistentEntity<?> entity, Validator validator) {
    List<Validator> v = validators.get(entity);
    if (null == v) {
      v = new ArrayList<Validator>();
      validators.put(entity, v);
    }
    v.add(validator);
  }

  public <S, T> void addTypeConverter(Converter<S, T> converter) {
    conversionService.addConverter(converter);
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public ConverterRegistry getConverterRegistry() {
    return conversionService;
  }

  public List<Validator> getEntityValidators(PersistentEntity<?> entity) {
    return validators.get(entity);
  }

  public MappingConfigurationBuilder getMappingConfigurationBuilder() {
    return builder;
  }

  public void setMappingConfigurationBuilder(MappingConfigurationBuilder builder) {
    this.builder = builder;
  }

  public boolean isPersistentEntity(Object value) {
    if (null != value) {
      Class<?> clazz;
      if (value instanceof Class) {
        clazz = ((Class<?>) value);
      } else {
        clazz = value.getClass();
      }
      return builder.isPersistentEntity(clazz);
    }
    return false;
  }

  public void afterPropertiesSet() throws Exception {
    Assert.notNull(builder, "No mapping configuration provider configured.");
  }
}
