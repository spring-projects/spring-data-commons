package org.springframework.data.util;

import java.util.Collection;
import java.util.Map;

/**
 * Interface to access property types and resolving generics on the way.
 * Starting with a {@link ClassTypeInformation} you can travers properties using
 * {@link #getProperty(String)} to access type information.
 * 
 * @author Oliver Gierke
 */
public interface TypeInformation {

  /**
   * Returns the property information for the property with the given name.
   * Supports proeprty traversal through dot notation.
   * 
   * @param fieldname
   * @return
   */
  TypeInformation getProperty(String fieldname);
  
  /**
   * Returns the component type for {@link Collection}s or the key type for {@link Map}s.
   * 
   * @return
   */
  TypeInformation getComponentType();
  
  /**
   * Will return the type of the value in case the underlying type is a {@link Map}.
   * 
   * @return
   */
  TypeInformation getMapValueType();

  /**
   * Returns the type of the property. Will resolve generics and the generic
   * context of
   * 
   * @return
   */
  Class<?> getType();
}