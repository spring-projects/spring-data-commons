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

import org.springframework.data.mapping.model.*;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class BasicPersistentEntity<T> implements PersistentEntity<T> {

  protected final Class<T> type;
  protected PreferredConstructor<T> preferredConstructor;
  protected PersistentProperty idProperty;
  protected Map<String, PersistentProperty> persistentProperties = new HashMap<String, PersistentProperty>();
  protected Map<String, Association> associations = new HashMap<String, Association>();
  protected final TypeInformation information;

  protected MappingContext mappingContext;
  
  
  public BasicPersistentEntity(MappingContext mappingContext, TypeInformation information) {
    this.mappingContext = mappingContext;
    this.type = (Class<T>) information.getType();
    this.information = information;
  }

  @Override
  public PreferredConstructor<T> getPreferredConstructor() {
    return preferredConstructor;
  }

  @Override
  public void setPreferredConstructor(PreferredConstructor<T> constructor) {
    this.preferredConstructor = constructor;
  }

  @Override
  public String getName() {
    return type.getName();
  }

  @Override
  public PersistentProperty getIdProperty() {
    return idProperty;
  }

  @Override
  public void setIdProperty(PersistentProperty property) {
    idProperty = property;
  }

  @Override
  public Collection<PersistentProperty> getPersistentProperties() {
    return persistentProperties.values();
  }

  @Override
  public void addPersistentProperty(PersistentProperty property) {
    persistentProperties.put(property.getName(), property);
  }

  @Override
  public Collection<Association> getAssociations() {
    return associations.values();
  }

  @Override
  public void addAssociation(Association association) {
    associations.put(association.getInverse().getName(), association);
  }

  public PersistentProperty getPersistentProperty(String name) {
    return persistentProperties.get(name);
  }

  @Override
  public Class<T> getType() {
    return type;
  }
  
  @Override
  public TypeInformation getPropertyInformation() {
    return information;
  }

  @Override
  public Collection<String> getPersistentPropertyNames() {
    return persistentProperties.keySet();
  }

  @Override
  public MappingContext getMappingContext() {
    return mappingContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {

  }

  @Override
  public void doWithProperties(PropertyHandler handler) {
    for (PersistentProperty property : persistentProperties.values()) {
      if (!property.isTransient() && !property.isAssociation() && !property.isIdProperty()) {
        handler.doWithPersistentProperty(property);
      }
    }
  }

  @Override
  public void doWithAssociations(AssociationHandler handler) {
    for (Association association : associations.values()) {
      handler.doWithAssociation(association);
    }
  }
}
