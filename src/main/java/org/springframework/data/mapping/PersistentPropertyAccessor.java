/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping;

import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Domain service to allow accessing and setting {@link PersistentProperty}s of an entity. Usually obtained through
 * {@link PersistentEntity#getPropertyAccessor(Object)}. In case type conversion shall be applied on property access,
 * use a {@link ConvertingPropertyAccessor}.
 * <p />
 * This service supports mutation for immutable classes by creating new object instances. These are managed as state of
 * {@link PersistentPropertyAccessor} and must be obtained from {@link #getBean()} after processing all updates.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.10
 * @see PersistentEntity#getPropertyAccessor(Object)
 * @see ConvertingPropertyAccessor
 */
public interface PersistentPropertyAccessor<T> {

	/**
	 * Sets the given {@link PersistentProperty} to the given value. Will do type conversion if a
	 * {@link org.springframework.core.convert.ConversionService} is configured.
	 *
	 * @param property must not be {@literal null}.
	 * @param value can be {@literal null}.
	 * @throws MappingException in case an exception occurred when setting the property value.
	 */
	void setProperty(PersistentProperty<?> property, @Nullable Object value);

	/**
	 * Sets the given value for the {@link PersistentProperty} pointed to by the given {@link PersistentPropertyPath}. The
	 * lookup of intermediate values must not yield {@literal null}.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @param value can be {@literal null}.
	 * @since 2.1
	 * @deprecated since 2.3, use {@link PersistentPropertyPathAccessor#setProperty(PersistentPropertyPath, Object)}
	 *             instead.
	 */
	@Deprecated
	default void setProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path, @Nullable Object value) {

		Assert.notNull(path, "PersistentPropertyPath must not be null!");
		Assert.isTrue(!path.isEmpty(), "PersistentPropertyPath must not be empty!");

		PersistentPropertyPath<? extends PersistentProperty<?>> parentPath = path.getParentPath();
		PersistentProperty<?> leafProperty = path.getRequiredLeafProperty();
		PersistentProperty<?> parentProperty = parentPath.isEmpty() ? null : parentPath.getLeafProperty();

		if (parentProperty != null && (parentProperty.isCollectionLike() || parentProperty.isMap())) {
			throw new MappingException(
					String.format("Cannot traverse collection or map intermediate %s", parentPath.toDotPath()));
		}

		Object parent = parentPath.isEmpty() ? getBean() : getProperty(parentPath);

		if (parent == null) {

			String nullIntermediateMessage = "Cannot lookup property %s on null intermediate! Original path was: %s on %s.";

			throw new MappingException(
					String.format(nullIntermediateMessage, parentProperty, path.toDotPath(), getBean().getClass().getName()));
		}

		PersistentPropertyAccessor<?> accessor = parent == getBean() //
				? this //
				: leafProperty.getOwner().getPropertyAccessor(parent);

		accessor.setProperty(leafProperty, value);

		if (parentPath.isEmpty()) {
			return;
		}

		Object bean = accessor.getBean();

		if (bean != parent) {
			setProperty(parentPath, bean);
		}
	}

	/**
	 * Returns the value of the given {@link PersistentProperty} of the underlying bean instance.
	 *
	 * @param property must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	@Nullable
	Object getProperty(PersistentProperty<?> property);

	/**
	 * Return the value pointed to by the given {@link PersistentPropertyPath}. If the given path is empty, the wrapped
	 * bean is returned.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 * @since 2.1
	 * @deprecated since 2.3, use {@link PersistentPropertyPathAccessor#getProperty(PersistentPropertyPath)} instead
	 */
	@Deprecated
	@Nullable
	default Object getProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path) {
		return getProperty(path, new TraversalContext());
	}

	/**
	 * Return the value pointed to by the given {@link PersistentPropertyPath}. If the given path is empty, the wrapped
	 * bean is returned. On each path segment value lookup, the resulting value is post-processed by handlers registered
	 * on the given {@link TraversalContext} context. This can be used to unwrap container types that are encountered
	 * during the traversal.
	 *
	 * @param path must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @return
	 * @since 2.2
	 * @deprecated since 2.3, use
	 *             {@link PersistentPropertyPathAccessor#getProperty(PersistentPropertyPath, org.springframework.data.mapping.AccessOptions.GetOptions)}
	 *             instead.
	 */
	@Nullable
	@Deprecated
	default Object getProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path, TraversalContext context) {

		Object bean = getBean();
		Object current = bean;

		if (path.isEmpty()) {
			return bean;
		}

		for (PersistentProperty<?> property : path) {

			if (current == null) {

				String nullIntermediateMessage = "Cannot lookup property %s on null intermediate! Original path was: %s on %s.";

				throw new MappingException(
						String.format(nullIntermediateMessage, property, path.toDotPath(), bean.getClass().getName()));
			}

			PersistentEntity<?, ? extends PersistentProperty<?>> entity = property.getOwner();
			PersistentPropertyAccessor<Object> accessor = entity.getPropertyAccessor(current);

			current = context.postProcess(property, accessor.getProperty(property));
		}

		return current;
	}

	/**
	 * Returns the underlying bean. The actual instance may change between
	 * {@link #setProperty(PersistentProperty, Object)} calls.
	 *
	 * @return will never be {@literal null}.
	 */
	T getBean();
}
