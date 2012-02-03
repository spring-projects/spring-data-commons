package org.springframework.data.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import org.springframework.util.ObjectUtils;

/**
 * Base class for {@link TypeInformation} implementations that need parent type awareness.
 * 
 * @author Oliver Gierke
 */
public abstract class ParentTypeAwareTypeInformation<S> extends TypeDiscoverer<S> {

	private final TypeDiscoverer<?> parent;

	/**
	 * Creates a new {@link ParentTypeAwareTypeInformation}.
	 * 
	 * @param type
	 * @param typeVariableMap
	 */
	@SuppressWarnings("rawtypes")
	protected ParentTypeAwareTypeInformation(Type type, TypeDiscoverer<?> parent, Map<TypeVariable, Type> map) {
		super(type, map);
		this.parent = parent;
	}

	/**
	 * Considers the parent's type variable map before invoking the super class method.
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected Map<TypeVariable, Type> getTypeVariableMap() {
		return parent == null ? super.getTypeVariableMap() : parent.getTypeVariableMap();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#createInfo(java.lang.reflect.Type)
	 */
	@Override
	protected TypeInformation<?> createInfo(Type fieldType) {

		if (parent.getType().equals(fieldType)) {
			return parent;
		}

		return super.createInfo(fieldType);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (!super.equals(obj)) {
			return false;
		}

		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}

		ParentTypeAwareTypeInformation<?> that = (ParentTypeAwareTypeInformation<?>) obj;
		return this.parent == null ? that.parent == null : this.parent.equals(that.parent);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode() + 31 * ObjectUtils.nullSafeHashCode(parent);
	}
}
