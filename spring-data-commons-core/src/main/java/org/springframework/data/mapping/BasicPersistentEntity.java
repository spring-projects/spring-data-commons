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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.MappingContext;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.mapping.model.PreferredConstructor;
import org.springframework.data.util.TypeInformation;

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


  @SuppressWarnings({"unchecked"})
  public BasicPersistentEntity(MappingContext mappingContext, TypeInformation information) {
    this.mappingContext = mappingContext;
    this.type = (Class<T>) information.getType();
    this.information = information;
  }

  public PreferredConstructor<T> getPreferredConstructor() {
    return preferredConstructor;
  }

  public void setPreferredConstructor(PreferredConstructor<T> constructor) {
    this.preferredConstructor = constructor;
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

  public MappingContext getMappingContext() {
    return mappingContext;
  }

  public void afterPropertiesSet() throws Exception {

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
}
