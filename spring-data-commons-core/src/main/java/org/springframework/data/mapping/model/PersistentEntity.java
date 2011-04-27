package org.springframework.data.mapping.model;

import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.util.TypeInformation;

import java.util.Collection;


/**
 * Represents a persistent entity
 *
 * @author Graeme Rocher
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public interface PersistentEntity<T> {

  /**
   * The entity name including any package prefix
   *
   * @return The entity name
   */
  String getName();

  PreferredConstructor<T> getPreferredConstructor();


    /**
     * Returns the id property of the {@link PersistentEntity}. Must never
     * return {@literal null} as a {@link PersistentEntity} instance must not be
     * created if there is no id property.
     * 
     * @return the id property of the {@link PersistentEntity}.
     */
  PersistentProperty getIdProperty();

  /**
   * A list of properties to be persisted
   *
   * @return A list of PersistentProperty instances
   */
  Collection<PersistentProperty> getPersistentProperties();

  /**
   * A list of the associations for this entity. This is typically a subset of the list returned by {@link #getPersistentProperties()}
   *
   * @return A list of associations
   */
  Collection<Association> getAssociations();

  /**
   * Obtains a PersistentProperty instance by name
   *
   * @param name The name of the property
   * @return The PersistentProperty or null if it doesn't exist
   */
  PersistentProperty getPersistentProperty(String name);

  /**
   * @return The underlying Java class for this entity
   */
  Class<T> getType();
  
  TypeInformation getPropertyInformation();

  /**
   * A list of property names
   *
   * @return A List of strings
   */
  Collection<String> getPersistentPropertyNames();

  void doWithProperties(PropertyHandler handler);

  void doWithAssociations(AssociationHandler handler);
}
