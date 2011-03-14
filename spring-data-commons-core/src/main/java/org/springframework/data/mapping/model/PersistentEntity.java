package org.springframework.data.mapping.model;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;

import java.util.Collection;


/**
 * Represents a persistent entity
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PersistentEntity<T> extends InitializingBean {

  /**
   * The entity name including any package prefix
   *
   * @return The entity name
   */
  String getName();

  PreferredConstructor<T> getPreferredConstructor();

  void setPreferredConstructor(PreferredConstructor<T> constructor);

  /**
   * Returns the identity of the instance
   *
   * @return The identity
   */
  PersistentProperty<?> getIdProperty();

  void setIdProperty(PersistentProperty<?> property);

  /**
   * A list of properties to be persisted
   *
   * @return A list of PersistentProperty instances
   */
  Collection<PersistentProperty<?>> getPersistentProperties();

  void addPersistentProperty(PersistentProperty<?> property);

  /**
   * A list of the associations for this entity. This is typically a subset of the list returned by {@link #getPersistentProperties()}
   *
   * @return A list of associations
   */
  Collection<Association> getAssociations();

  void addAssociation(Association association);

  /**
   * Obtains a PersistentProperty instance by name
   *
   * @param name The name of the property
   * @return The PersistentProperty or null if it doesn't exist
   */
  PersistentProperty<?> getPersistentProperty(String name);

  /**
   * @return The underlying Java class for this entity
   */
  Class<T> getType();

  /**
   * A list of property names
   *
   * @return A List of strings
   */
  Collection<String> getPersistentPropertyNames();

  /**
   * Obtains the MappingContext where this PersistentEntity is defined
   *
   * @return The MappingContext instance
   */
  MappingContext getMappingContext();

  void doWithProperties(PropertyHandler handler);

  void doWithAssociations(AssociationHandler handler);

}
