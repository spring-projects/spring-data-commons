package org.springframework.data.mapping.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

/**
 * @author Graeme Rocher
 * @since 1.0
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

  Class<?> getComponentType();

  boolean isIdProperty();
}
