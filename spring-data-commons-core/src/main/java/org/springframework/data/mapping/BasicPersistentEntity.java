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

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.mapping.model.PreferredConstructor;
import org.springframework.data.util.TypeInformation;

/**
 * Simple value object to capture information of {@link PersistentEntity}s.
 * 
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BasicPersistentEntity<T> implements PersistentEntity<T> {

  protected final Class<T> type;
  protected final PreferredConstructor<T> preferredConstructor;
  protected final TypeInformation information;
  protected final Map<String, PersistentProperty> persistentProperties = new HashMap<String, PersistentProperty>();
  protected final Map<String, Association> associations = new HashMap<String, Association>();
  protected PersistentProperty idProperty;


  /**
   * Creates a new {@link BasicPersistentEntity} from the given {@link TypeInformation}.
   * 
   * @param information
   */
  @SuppressWarnings("unchecked")
  public BasicPersistentEntity(TypeInformation information) {
    this.type = (Class<T>) information.getType();
    this.information = information;
    this.preferredConstructor = getPreferredConstructor(type);
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private final PreferredConstructor<T> getPreferredConstructor(Class<T> type) {
      // Find the right constructor
      PreferredConstructor<T> preferredConstructor = null;

      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
              if (constructor.getParameterTypes().length != 0) {
                      // Non-no-arg constructor
                      if (null == preferredConstructor || constructor.isAnnotationPresent(PersistenceConstructor.class)) {
                              preferredConstructor = new PreferredConstructor<T>((Constructor<T>) constructor);

                              String[] paramNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(constructor);
                              Type[] paramTypes = constructor.getGenericParameterTypes();

                              for (int i = 0; i < paramTypes.length; i++) {
                                      Class<?> targetType = Object.class;
                                      Class<?> rawType = constructor.getParameterTypes()[i];
                                      if (paramTypes[i] instanceof ParameterizedType) {
                                              ParameterizedType ptype = (ParameterizedType) paramTypes[i];
                                              targetType = getTargetType(ptype);
                                      } else {
                                              if (paramTypes[i] instanceof TypeVariable) {
                                                      targetType = getTargetType((TypeVariable) paramTypes[i]);
                                              } else if (paramTypes[i] instanceof Class<?>) {
                                                      targetType = (Class<?>) paramTypes[i];
                                              }
                                      }
                                      String paramName = (null != paramNames ? paramNames[i] : "param" + i);
                                      preferredConstructor.addParameter(paramName, targetType, rawType, targetType.getDeclaredAnnotations());
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
  
  private Class<?> getTargetType(TypeVariable<?> tv) {
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

  private Class<?> getTargetType(ParameterizedType ptype) {
      Class<?> targetType = Object.class;
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

  public PreferredConstructor<T> getPreferredConstructor() {
    return preferredConstructor;
  }

  public String getName() {
    return type.getName();
  }

  public PersistentProperty getIdProperty() {
    return idProperty;
  }

  public void setIdProperty(PersistentProperty property) {
    idProperty = property;
  }

  public Collection<PersistentProperty> getPersistentProperties() {
    return persistentProperties.values();
  }

  public void addPersistentProperty(PersistentProperty property) {
    persistentProperties.put(property.getName(), property);
  }

  public Collection<Association> getAssociations() {
    return associations.values();
  }

  public void addAssociation(Association association) {
    associations.put(association.getInverse().getName(), association);
  }

  public PersistentProperty getPersistentProperty(String name) {
    return persistentProperties.get(name);
  }

  public Class<T> getType() {
    return type;
  }

  public TypeInformation getPropertyInformation() {
    return information;
  }

  public Collection<String> getPersistentPropertyNames() {
    return persistentProperties.keySet();
  }

  public void doWithProperties(PropertyHandler handler) {
    for (PersistentProperty property : persistentProperties.values()) {
      if (!property.isTransient() && !property.isAssociation() && !property.isIdProperty()) {
        handler.doWithPersistentProperty(property);
      }
    }
  }

  public void doWithAssociations(AssociationHandler handler) {
    for (Association association : associations.values()) {
      handler.doWithAssociation(association);
    }
  }

  /**
   * Callback method to trigger validation of the {@link PersistentEntity}. As {@link BasicPersistentEntity} is not
   * immutable there might be some verification steps necessary after the object has reached is final state.
   */
  public void verify() {

  }
}
