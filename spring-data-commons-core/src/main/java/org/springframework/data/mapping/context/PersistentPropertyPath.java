/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.mapping.context;

import java.util.Iterator;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Abstraction of a path of {@link PersistentProperty}s.
 *
 * @author Oliver Gierke
 */
public interface PersistentPropertyPath<T extends PersistentProperty<T>> extends Iterable<T> {

	/**
	 * Returns the dot based path notation using the {@link PersistentProperty}'s name attribute.
	 * 
	 * @return
	 */
	String toDotPath();

	/**
	 * Returns the dot based path notation using the given {@link Converter} to translate individual
	 * {@link PersistentProperty}s to path segments.
	 * 
	 * @param converter
	 * @return
	 */
	String toDotPath(Converter<? super T, String> converter);

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	Iterator<T> iterator();
}