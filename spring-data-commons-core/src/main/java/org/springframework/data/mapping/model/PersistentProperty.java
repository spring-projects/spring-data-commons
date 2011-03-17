package org.springframework.data.mapping.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @author Oliver Gierke
 */
public interface PersistentProperty {

  Object getOwner();

  void setOwner(Object owner);

  /**
   * The name of the property
   *
   * @return The property name
   */
  String getName();

  /**
   * The type of the property
   *
   * @return The property type
   */
  Class<?> getType();
  
  TypeInformation getTypeInformation();

  PropertyDescriptor getPropertyDescriptor();

  Field getField();

  Value getValueAnnotation();

  boolean isTransient();

  boolean isAssociation();

  Association getAssociation();

  void setAssociation(Association association);

  boolean isCollection();
  
  boolean isMap();
  
  boolean isArray();

  boolean isComplexType();

  /**
   * Returns whether the property has to be regarded as entity which means its type will be also be considered to be a
   * {@link PersistentEntity}.
   * 
   * @return
   */
  boolean isEntity();

  /**
   * Returns the component type of the type if it is a {@link Collection}. Will return the type of the key if the
   * property is a {@link Map}.
   * 
   * @return the component type, the map's key type or {@literal null} if neither {@link Collection} nor {@link Map}. 
   */
  Class<?> getComponentType();
  
  /**
   * Returns the type of the values if the property is a {@link Map}.
   * 
   * @return the map's value type or {@literal null} if no {@link Map}
   */
  Class<?> getMapValueType();

  boolean isIdProperty();
}
