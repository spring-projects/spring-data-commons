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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.event.MappingContextEvent;
import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.MappingConfigurationException;
import org.springframework.data.mapping.model.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.mapping.model.PreferredConstructor;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.validation.Validator;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Oliver Gierke
 */
public class BasicMappingContext implements MappingContext, InitializingBean, ApplicationEventPublisherAware {

  private static final Set<String> UNMAPPED_FIELDS = new HashSet<String>(Arrays.asList("class", "this$0"));
  
  protected Logger log = LoggerFactory.getLogger(getClass());
  protected ApplicationEventPublisher applicationEventPublisher;
  protected ConcurrentMap<TypeInformation, PersistentEntity<?>> persistentEntities = new ConcurrentHashMap<TypeInformation, PersistentEntity<?>>();
  protected ConcurrentMap<PersistentEntity<?>, List<Validator>> validators = new ConcurrentHashMap<PersistentEntity<?>, List<Validator>>();
  protected final GenericConversionService conversionService;
  private List<Class<?>> customSimpleTypes = new ArrayList<Class<?>>();
  private Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();
  
  public BasicMappingContext() {
    this(ConversionServiceFactory.createDefaultConversionService());
  }

  public BasicMappingContext(GenericConversionService conversionService) {
    Assert.notNull(conversionService);
    this.conversionService = conversionService;
  }

  /**
   * @param customSimpleTypes the customSimpleTypes to set
   */
  public void setCustomSimpleTypes(List<Class<?>> customSimpleTypes) {
    this.customSimpleTypes = customSimpleTypes;
  }


  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void setInitialEntitySet(Set<Class<?>> initialEntitySet) {
    this.initialEntitySet = initialEntitySet;
  }

  public Collection<? extends PersistentEntity<?>> getPersistentEntities() {
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
  
  public <T> PersistentEntity<T> addPersistentEntity(Class<T> type) {
    return addPersistentEntity(new ClassTypeInformation(type));
  }

  @SuppressWarnings("unchecked")
  public <T> PersistentEntity<T> addPersistentEntity(TypeInformation typeInformation) {

    PersistentEntity<?> persistentEntity = persistentEntities.get(typeInformation);

    if (persistentEntity != null) {
      return (PersistentEntity<T>) persistentEntity;
    }

    Class<T> type = (Class<T>) typeInformation.getType();

    try {
      final BasicPersistentEntity<T> entity = createPersistentEntity(typeInformation, this);
      BeanInfo info = Introspector.getBeanInfo(type);

      final Map<String, PropertyDescriptor> descriptors = new HashMap<String, PropertyDescriptor>();
      for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
        descriptors.put(descriptor.getName(), descriptor);
      }

      ReflectionUtils.doWithFields(type, new FieldCallback() {

        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
          try {
            PropertyDescriptor descriptor = descriptors.get(field.getName());
            if (isPersistentProperty(field, descriptor)) {
              ReflectionUtils.makeAccessible(field);
              BasicPersistentProperty property = createPersistentProperty(field, descriptor, entity.getPropertyInformation());
              property.setOwner(entity);
              entity.addPersistentProperty(property);
              if (isAssociation(field, descriptor)) {
                Association association = createAssociation(property);
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

      entity.setPreferredConstructor(getPreferredConstructor(type));

      // Inform listeners
      if (null != applicationEventPublisher) {
        applicationEventPublisher.publishEvent(new MappingContextEvent(entity, typeInformation));
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

  public List<Validator> getEntityValidators(PersistentEntity<?> entity) {
    return validators.get(entity);
  }

  public boolean isPersistentEntity(Object value) {
    if (null != value) {
      Class<?> clazz;
      if (value instanceof Class) {
        clazz = ((Class<?>) value);
      } else {
        clazz = value.getClass();
      }
      return isPersistentEntity(clazz);
    }
    return false;
  }
  
  public boolean isPersistentEntity(Class<?> type) {
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

  protected <T> BasicPersistentEntity<T> createPersistentEntity(TypeInformation typeInformation, MappingContext mappingContext)
      throws MappingConfigurationException {
    return new BasicPersistentEntity<T>(mappingContext, typeInformation);
  }

  protected BasicPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
      TypeInformation information) throws MappingConfigurationException {
    return new BasicPersistentProperty(field, descriptor, information);
  }

  public boolean isPersistentProperty(Field field, PropertyDescriptor descriptor) throws MappingConfigurationException {
    if (UNMAPPED_FIELDS.contains(field.getName()) || isTransient(field)) {
      return false;
    }
    return true;
  }

  @SuppressWarnings({"unchecked"})
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
                @SuppressWarnings("rawtypes")
                Type[] bounds = ((TypeVariable) paramTypes[i]).getBounds();
                if (bounds.length > 0) {
                  targetType = (Class<?>) bounds[0];
                }
              } else if (paramTypes[i] instanceof Class<?>) {
                targetType = (Class<?>) paramTypes[i];
              }
            }
            String paramName = (null != paramNames ? paramNames[i] : "param" + i);
            preferredConstructor.addParameter(paramName, targetType, targetType.getDeclaredAnnotations());
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

  public Association createAssociation(BasicPersistentProperty property) {
    // Only support uni-directional associations in the Basic configuration
    Association association = new Association(property, null);
    property.setAssociation(association);

    return association;
  }

  protected boolean isTransient(Field field) {
    if (Modifier.isTransient(field.getModifiers())
        || null != field.getAnnotation(Transient.class)
        || null != field.getAnnotation(Value.class)) {
      return true;
    }
    return false;
  }

  public void afterPropertiesSet() throws Exception {
    for (Class<?> initialEntity : initialEntitySet) {
      addPersistentEntity(initialEntity);
    }
  }
}
