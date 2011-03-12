package org.springframework.data.mapping.model;

import org.springframework.beans.factory.annotation.Value;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PersistentProperty<T> {

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
  Class<T> getType();

  PropertyDescriptor getPropertyDescriptor();

  Field getField();

  Value getValueAnnotation();

  boolean isTransient();

  boolean isAssociation();

  Association getAssociation();

  void setAssociation(Association association);

  boolean isCollection();

  boolean isComplexType();

  Class<?> getComponentType();

  boolean isIdProperty();
}
