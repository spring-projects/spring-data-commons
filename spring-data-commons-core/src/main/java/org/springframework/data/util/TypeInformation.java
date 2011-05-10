package org.springframework.data.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface to access property types and resolving generics on the way.
 * Starting with a {@link ClassTypeInformation} you can travers properties using
 * {@link #getProperty(String)} to access type information.
 *
 * @author Oliver Gierke
 */
public interface TypeInformation<S> {

	List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor);

	/**
	 * Returns the property information for the property with the given name.
	 * Supports proeprty traversal through dot notation.
	 *
	 * @param fieldname
	 * @return
	 */
	TypeInformation<?> getProperty(String fieldname);

	/**
	 * Returns whether the type can be considered a collection, which means it's a container of elements, e.g. a
	 * {@link Collection} and {@link Array} or anything implementing {@link Iterable}. If this returns {@literal true} you
	 * can expect {@link #getComponentType()} to return a non-{@literal null} value.
	 *
	 * @return
	 */
	boolean isCollectionLike();

	/**
	 * Returns the component type for {@link Collection}s or the key type for {@link Map}s.
	 *
	 * @return
	 */
	TypeInformation<?> getComponentType();

	/**
	 * Returns whether the property is a {@link Map}. If this returns {@literal true} you can expect
	 * {@link #getComponentType()} as well as {@link #getMapValueType()} to return something not {@literal null}.
	 *
	 * @return
	 */
	boolean isMap();

	/**
	 * Will return the type of the value in case the underlying type is a {@link Map}.
	 *
	 * @return
	 */
	TypeInformation<?> getMapValueType();

	/**
	 * Returns the type of the property. Will resolve generics and the generic
	 * context of
	 *
	 * @return
	 */
	Class<S> getType();
}