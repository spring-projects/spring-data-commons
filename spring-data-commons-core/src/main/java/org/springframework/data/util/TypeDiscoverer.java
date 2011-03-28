package org.springframework.data.util;

import static org.springframework.util.ObjectUtils.*;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Basic {@link TypeDiscoverer} that contains basic functionality to discover
 * property types.
 * 
 * @author Oliver Gierke
 */
class TypeDiscoverer implements TypeInformation {

  private final Type type;
  @SuppressWarnings("rawtypes")
  private final Map<TypeVariable, Type> typeVariableMap;
  private final Map<String, TypeInformation> fieldTypes = new ConcurrentHashMap<String, TypeInformation>();
  private final TypeDiscoverer parent;

  /**
   * Creates a ne {@link TypeDiscoverer} for the given type, type variable map and parent.
   * 
   * @param type must not be null.
   * @param typeVariableMap
   * @param parent
   */
  @SuppressWarnings("rawtypes")
  protected TypeDiscoverer(Type type, Map<TypeVariable, Type> typeVariableMap,
      TypeDiscoverer parent) {

    Assert.notNull(type);
    this.type = type;
    this.typeVariableMap = typeVariableMap;
    this.parent = parent;
  }

  /**
   * Returns the type variable map. Will traverse the parents up to the root on
   * and use it's map.
   * 
   * @return
   */
  @SuppressWarnings("rawtypes")
  private Map<TypeVariable, Type> getTypeVariableMap() {

    return parent != null ? parent.getTypeVariableMap() : typeVariableMap;
  }

  /**
   * Creates {@link TypeInformation} for the given {@link Type}.
   * 
   * @param fieldType
   * @return
   */
  protected TypeInformation createInfo(Type fieldType) {

    if (fieldType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) fieldType;
      return new TypeDiscoverer(parameterizedType, null, this);
    }

    if (fieldType instanceof TypeVariable) {
      TypeVariable<?> variable = (TypeVariable<?>) fieldType;
      return new TypeVariableTypeInformation(variable, type, this);
    }

    if (fieldType instanceof Class) {
      return new ClassTypeInformation((Class<?>) fieldType, this);
    }
    
    if (fieldType instanceof GenericArrayType) {
      return new ArrayTypeDiscoverer((GenericArrayType) fieldType, this);
    }

    throw new IllegalArgumentException();
  }

  /**
   * Resolves the given type into a plain {@link Class}.
   * 
   * @param type
   * @return
   */
  protected Class<?> resolveType(Type type) {

    return GenericTypeResolver.resolveType(type, getTypeVariableMap());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.springframework.data.document.mongodb.TypeDiscovererTest.FieldInformation
   * #getField(java.lang.String)
   */
  public TypeInformation getProperty(String fieldname) {

    int separatorIndex = fieldname.indexOf(".");

    if (separatorIndex == -1) {
      if (fieldTypes.containsKey(fieldname)) {
        return fieldTypes.get(fieldname);
      }

      TypeInformation propertyInformation = getPropertyInformation(fieldname);
      if (propertyInformation != null) {
        fieldTypes.put(fieldname, propertyInformation);
      }
      return propertyInformation;
    }

    String head = fieldname.substring(0, separatorIndex);
    TypeInformation info = fieldTypes.get(head);
    return info.getProperty(fieldname.substring(separatorIndex + 1));
  }

  private TypeInformation getPropertyInformation(String fieldname) {

    Field field = ReflectionUtils.findField(getType(), fieldname);

    if (field == null) {
      return null;
    }

    return createInfo(field.getGenericType());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.springframework.data.document.mongodb.TypeDiscovererTest.FieldInformation
   * #getType()
   */
  public Class<?> getType() {
    return resolveType(type);
  }
  
  /* (non-Javadoc)
   * @see org.springframework.data.util.TypeInformation#isMap()
   */
  @Override
  public boolean isMap() {
    Class<?> rawType = getType();
    return rawType == null ? false : Map.class.isAssignableFrom(rawType);
  }
  
  /* (non-Javadoc)
   * @see org.springframework.data.util.TypeInformation#getMapValueType()
   */
  public TypeInformation getMapValueType() {
    
    if (!Map.class.isAssignableFrom(getType())) {
      return null;
    }
    
    ParameterizedType parameterizedType = (ParameterizedType) type;
    return createInfo(parameterizedType.getActualTypeArguments()[1]);
  }
  
  /* (non-Javadoc)
   * @see org.springframework.data.util.TypeInformation#isCollectionLike()
   */
  @Override
  public boolean isCollectionLike() {
   
    Class<?> rawType = getType();
    return rawType == null ? null : rawType.isArray() || Iterable.class.isAssignableFrom(rawType);
  }
  
  /* (non-Javadoc)
   * @see org.springframework.data.util.TypeInformation#getComponentType()
   */
  public TypeInformation getComponentType() {
    
    if (!(Map.class.isAssignableFrom(getType()) || isCollectionLike())) {
      return null;
    }
    
    ParameterizedType parameterizedType = (ParameterizedType) type;
    return createInfo(parameterizedType.getActualTypeArguments()[0]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }

    TypeDiscoverer that = (TypeDiscoverer) obj;

    boolean typeEqual = nullSafeEquals(this.type, that.type);
    boolean typeVariableMapEqual = nullSafeEquals(this.typeVariableMap,
        that.typeVariableMap);
    boolean parentEqual = nullSafeEquals(this.parent, that.parent);

    return typeEqual && typeVariableMapEqual && parentEqual;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {

    int result = 17;
    result += nullSafeHashCode(type);
    result += nullSafeHashCode(typeVariableMap);
    result += nullSafeHashCode(parent);
    return result;
  }
}