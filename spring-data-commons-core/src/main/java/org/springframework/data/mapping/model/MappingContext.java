/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.util.TypeInformation;
import org.springframework.validation.Validator;

/**
 * <p>This interface defines the overall context including all known
 * PersistentEntity instances and methods to obtain instances on demand</p>
 * <p/>
 * <p>This interface is used internally to establish associations
 * between entities and also at runtime to obtain entities by name</p>
 * <p/>
 * <p>The generic type parameters T & R are used to specify the
 * mapped form of a class (example Table) and property (example Column) respectively.</p>
 * <p/>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface MappingContext extends InitializingBean {


  /**
   * Obtains a list of PersistentEntity instances
   *
   * @return A list of PersistentEntity instances
   */
  Collection<PersistentEntity<?>> getPersistentEntities();

  <T> PersistentEntity<T> getPersistentEntity(Class<T> type);

  <T> PersistentEntity<T> getPersistentEntity(TypeInformation type);

  /**
   * Adds a PersistentEntity instance
   *
   * @param type The Java class representing the entity
   * @return The PersistentEntity instance
   */
  <T> PersistentEntity<T> addPersistentEntity(Class<T> type);

  /**
   * Adds a validator to be used by the entity for validation
   *
   * @param entity    The PersistentEntity
   * @param validator The validator
   */
  void addEntityValidator(PersistentEntity<?> entity, Validator validator);

  /**
   * Add a converter used to convert property values to and from the datastore
   *
   * @param converter The converter to add
   */
  <S, T> void addTypeConverter(Converter<S, T> converter);

  /**
   * Obtains the ConversionService instance to use for type conversion
   *
   * @return The conversion service instance
   */
  ConversionService getConversionService();

  /**
   * Obtains the converter registry
   *
   * @return The converter registry used for type conversion
   */
  ConverterRegistry getConverterRegistry();

  /**
   * Obtains a validator for the given entity
   *
   * @param entity The entity
   * @return A validator or null if none exists for the given entity
   */
  List<Validator> getEntityValidators(PersistentEntity<?> entity);

  MappingConfigurationBuilder getMappingConfigurationBuilder();

  void setMappingConfigurationBuilder(MappingConfigurationBuilder builder);

  /**
   * Returns whether the specified value is a persistent entity
   *
   * @param value The value to check
   * @return True if it is
   */
  boolean isPersistentEntity(Object value);

}
