/*
 * Copyright 2008-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.util;

import java.lang.reflect.Constructor;

import java.util.List;

/**
 * Interface to access property types and resolving generics on the way. Starting with a {@link ClassTypeInformation}
 * you can travers properties using {@link #getProperty(String)} to access type information.
 * 
 * @author Oliver Gierke
 */
public interface TypeInformation<S> {

	List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor);

	/**
	 * Returns the property information for the property with the given name. Supports proeprty traversal through dot
	 * notation.
	 * 
	 * @param fieldname
	 * @return
	 */
	TypeInformation<?> getProperty(String fieldname);

	/**
	 * Returns whether the type can be considered a collection, which means it's a container of elements, e.g. a
	 * {@link java.util.Collection} and {@link java.lang.reflect.Array} or anything implementing {@link Iterable}. If this
	 * returns {@literal true} you can expect {@link #getComponentType()} to return a non-{@literal null} value.
	 * 
	 * @return
	 */
	boolean isCollectionLike();

	/**
	 * Returns the component type for {@link java.util.Collection}s or the key type for {@link java.util.Map}s.
	 * 
	 * @return
	 */
	TypeInformation<?> getComponentType();

	/**
	 * Returns whether the property is a {@link java.util.Map}. If this returns {@literal true} you can expect
	 * {@link #getComponentType()} as well as {@link #getMapValueType()} to return something not {@literal null}.
	 * 
	 * @return
	 */
	boolean isMap();

	/**
	 * Will return the type of the value in case the underlying type is a {@link java.util.Map}.
	 * 
	 * @return
	 */
	TypeInformation<?> getMapValueType();

	/**
	 * Returns the type of the property. Will resolve generics and the generic context of
	 * 
	 * @return
	 */
	Class<S> getType();

	/**
	 * Transparently returns the {@link java.util.Map} value type if the type is a {@link java.util.Map}, returns the
	 * component type if the type {@link #isCollectionLike()} or the simple type if none of this applies.
	 * 
	 * @return
	 */
	TypeInformation<?> getActualType();
}