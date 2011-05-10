package org.springframework.data.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

/**
 * Special {@link TypeDiscoverer} handling {@link GenericArrayType}s.
 *
 * @author Oliver Gierke
 */
public class ArrayTypeDiscoverer<S> extends TypeDiscoverer<S> {

	private GenericArrayType type;

	/**
	 * @param type
	 * @param parent
	 * @param parent
	 */
	protected ArrayTypeDiscoverer(GenericArrayType type, TypeDiscoverer<?> parent) {
		super(type, null, parent);
		this.type = type;
	}

	/* (non-Javadoc)
		 * @see org.springframework.data.util.TypeDiscoverer#getType()
		 */
	@Override
	@SuppressWarnings("unchecked")
	public Class<S> getType() {

		return (Class<S>) Array.newInstance(resolveType(type.getGenericComponentType()), 0).getClass();
	}

	/* (non-Javadoc)
		 * @see org.springframework.data.util.TypeDiscoverer#getComponentType()
		 */
	@Override
	public TypeInformation<?> getComponentType() {

		Type componentType = type.getGenericComponentType();
		return createInfo(componentType);
	}
}
