package org.springframework.data.util;

import static org.springframework.util.ObjectUtils.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.springframework.util.Assert;

/**
 * Special {@link TypeDiscoverer} to determine the actual type for a {@link TypeVariable}. Will consider the
 * context the {@link TypeVariable} is being used in.
 * 
 * @author Oliver Gierke
 */
class TypeVariableTypeInformation<T> extends TypeDiscoverer<T> {

	private final TypeVariable<?> variable;
	private final Type owningType;

	/**
	 * Creates a bew {@link TypeVariableTypeInformation} for the given {@link TypeVariable} owning {@link Type} and
	 * parent {@link TypeDiscoverer}.
	 * 
	 * @param variable must not be {@literal null}
	 * @param owningType must not be {@literal null}
	 * @param parent
	 */
	public TypeVariableTypeInformation(TypeVariable<?> variable, Type owningType, TypeDiscoverer<?> parent) {

		super(variable, null, parent);
		Assert.notNull(variable);
		this.variable = variable;
		this.owningType = owningType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.document.mongodb.TypeDiscovererTest.TypeDiscoverer#getType()
	 */
	@Override
	public Class<T> getType() {

	  int index = getIndex(variable);
	  
		if (owningType instanceof ParameterizedType && index != -1) {
			Type fieldType = ((ParameterizedType) owningType).getActualTypeArguments()[index];
			return resolveType(fieldType);
		}

		return resolveType(variable);
	}

	/**
	 * Returns the index of the type parameter binding the given {@link TypeVariable}.
	 * @param variable
	 * @return
	 */
	private int getIndex(TypeVariable<?> variable) {

		Class<?> rawType = resolveType(owningType);
		TypeVariable<?>[] typeParameters = rawType.getTypeParameters();

		for (int i = 0; i < typeParameters.length; i++) {
			if (variable.equals(typeParameters[i])) {
				return i;
			}
		}

		return -1;
	}
	
  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.data.util.TypeDiscoverer#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }

    TypeVariableTypeInformation<?> that = (TypeVariableTypeInformation<?>) obj;
    return nullSafeEquals(this.owningType, that.owningType)
        && nullSafeEquals(this.variable, that.variable);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.data.util.TypeDiscoverer#hashCode()
   */
  @Override
  public int hashCode() {
    int result = super.hashCode();
    result += 31 * nullSafeHashCode(this.owningType);
    result += 31 * nullSafeHashCode(this.variable);
    return result;
  }
}