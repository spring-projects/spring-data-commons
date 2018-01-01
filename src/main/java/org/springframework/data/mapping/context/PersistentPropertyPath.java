/*
 * Copyright 2011-2018 the original author or authors.
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

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * Abstraction of a path of {@link PersistentProperty}s.
 *
 * @author Oliver Gierke
 */
public interface PersistentPropertyPath<T extends PersistentProperty<T>> extends Iterable<T> {

	/**
	 * Returns the dot based path notation using {@link PersistentProperty#getName()}.
	 *
	 * @return
	 */
	@Nullable
	String toDotPath();

	/**
	 * Returns the dot based path notation using the given {@link Converter} to translate individual
	 * {@link PersistentProperty}s to path segments.
	 *
	 * @param converter must not be {@literal null}.
	 * @return
	 */
	@Nullable
	String toDotPath(Converter<? super T, String> converter);

	/**
	 * Returns a {@link String} path with the given delimiter based on the {@link PersistentProperty#getName()}.
	 *
	 * @param delimiter must not be {@literal null}.
	 * @return
	 */
	@Nullable
	String toPath(String delimiter);

	/**
	 * Returns a {@link String} path with the given delimiter using the given {@link Converter} for
	 * {@link PersistentProperty} to String conversion.
	 *
	 * @param delimiter must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 * @return
	 */
	@Nullable
	String toPath(String delimiter, Converter<? super T, String> converter);

	/**
	 * Returns the last property in the {@link PersistentPropertyPath}. So for {@code foo.bar} it will return the
	 * {@link PersistentProperty} for {@code bar}. For a simple {@code foo} it returns {@link PersistentProperty} for
	 * {@code foo}.
	 *
	 * @return
	 */
	@Nullable
	T getLeafProperty();

	/**
	 * Returns the first property in the {@link PersistentPropertyPath}. So for {@code foo.bar} it will return the
	 * {@link PersistentProperty} for {@code foo}. For a simple {@code foo} it returns {@link PersistentProperty} for
	 * {@code foo}.
	 *
	 * @return
	 */
	@Nullable
	T getBaseProperty();

	/**
	 * Returns whether the given {@link PersistentPropertyPath} is a base path of the current one. This means that the
	 * current {@link PersistentPropertyPath} is basically an extension of the given one.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	boolean isBasePathOf(PersistentPropertyPath<T> path);

	/**
	 * Returns the sub-path of the current one as if it was based on the given base path. So for a current path
	 * {@code foo.bar} and a given base {@code foo} it would return {@code bar}. If the given path is not a base of the
	 * the current one the current {@link PersistentPropertyPath} will be returned as is.
	 *
	 * @param base must not be {@literal null}.
	 * @return
	 */
	PersistentPropertyPath<T> getExtensionForBaseOf(PersistentPropertyPath<T> base);

	/**
	 * Returns the parent path of the current {@link PersistentPropertyPath}, i.e. the path without the leaf property.
	 * This happens up to the base property. So for a direct property reference calling this method will result in
	 * returning the property.
	 *
	 * @return
	 */
	PersistentPropertyPath<T> getParentPath();

	/**
	 * Returns the length of the {@link PersistentPropertyPath}.
	 *
	 * @return
	 */
	int getLength();

	/**
	 * Returns whether the path is empty.
	 *
	 * @return
	 * @since 1.11
	 */
	boolean isEmpty();
}
