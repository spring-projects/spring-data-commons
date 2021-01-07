/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.mapping.model;

import static org.springframework.data.mapping.AccessOptions.SetOptions.SetNulls.*;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.AccessOptions;
import org.springframework.data.mapping.AccessOptions.GetOptions;
import org.springframework.data.mapping.AccessOptions.GetOptions.GetNulls;
import org.springframework.data.mapping.AccessOptions.SetOptions;
import org.springframework.data.mapping.AccessOptions.SetOptions.SetNulls;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PersistentPropertyPathAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link PersistentPropertyPathAccessor} that propagates attempts to set property values through collections and map
 * values. I.e. if a {@link PersistentPropertyPath} contains a path segment pointing to a collection or map based
 * property, the nested property will be set on all collection elements and map values.
 *
 * @author Oliver Gierke
 * @since 2.3
 * @soundtrack Ron Spielman - Nineth Song (Tip of My Tongue)
 */
class SimplePersistentPropertyPathAccessor<T> implements PersistentPropertyPathAccessor<T> {

	private static final Log logger = LogFactory.getLog(SimplePersistentPropertyPathAccessor.class);
	private static final GetOptions DEFAULT_GET_OPTIONS = AccessOptions.defaultGetOptions();

	private final PersistentPropertyAccessor<T> delegate;

	public SimplePersistentPropertyPathAccessor(PersistentPropertyAccessor<T> delegate) {
		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getBean()
	 */
	@Override
	public T getBean() {
		return delegate.getBean();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	@Nullable
	@Override
	public Object getProperty(PersistentProperty<?> property) {
		return delegate.getProperty(property);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyPathAccessor#getProperty(org.springframework.data.mapping.PersistentPropertyPath)
	 */
	@Nullable
	@Override
	public Object getProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path) {
		return getProperty(path, DEFAULT_GET_OPTIONS);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyPathAccessor#getProperty(org.springframework.data.mapping.PersistentPropertyPath, org.springframework.data.mapping.PersistentPropertyPathAccessor.Options)
	 */
	@Nullable
	@Override
	public Object getProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path, GetOptions options) {

		Object bean = getBean();
		Object current = bean;

		if (path.isEmpty()) {
			return bean;
		}

		for (PersistentProperty<?> property : path) {

			if (current == null) {
				return handleNull(path, options.getNullValues().toNullHandling());
			}

			PersistentEntity<?, ? extends PersistentProperty<?>> entity = property.getOwner();
			PersistentPropertyAccessor<Object> accessor = entity.getPropertyAccessor(current);

			current = accessor.getProperty(property);
		}

		return current;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#setProperty(org.springframework.data.mapping.PersistentProperty, java.lang.Object)
	 */
	@Override
	public void setProperty(PersistentProperty<?> property, @Nullable Object value) {
		delegate.setProperty(property, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyPathAccessor#setProperty(org.springframework.data.mapping.PersistentPropertyPath, java.lang.Object)
	 */
	@Override
	public void setProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path, @Nullable Object value) {
		setProperty(path, value, AccessOptions.defaultSetOptions());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.ConvertingPropertyAccessor#setProperty(org.springframework.data.mapping.PersistentPropertyPath, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path, @Nullable Object value,
			SetOptions options) {

		Assert.notNull(path, "PersistentPropertyPath must not be null!");
		Assert.isTrue(!path.isEmpty(), "PersistentPropertyPath must not be empty!");

		PersistentPropertyPath<? extends PersistentProperty<?>> parentPath = path.getParentPath();
		PersistentProperty<?> leafProperty = path.getRequiredLeafProperty();

		if (!options.propagate(parentPath.getLeafProperty())) {
			return;
		}

		GetOptions lookupOptions = options.getNullHandling() != REJECT
				? DEFAULT_GET_OPTIONS.withNullValues(GetNulls.EARLY_RETURN)
				: DEFAULT_GET_OPTIONS;

		Object parent = parentPath.isEmpty() ? getBean() : getProperty(parentPath, lookupOptions);

		if (parent == null) {
			handleNull(path, options.getNullHandling());
			return;
		}

		if (parent == getBean()) {

			setProperty(leafProperty, value);
			return;
		}

		PersistentProperty<?> parentProperty = parentPath.getRequiredLeafProperty();

		Object newValue;

		if (parentProperty.isCollectionLike()) {

			Collection<Object> source = getTypedProperty(parentProperty, Collection.class);

			if (source == null) {
				return;
			}

			newValue = source.stream() //
					.map(it -> setValue(it, leafProperty, value)) //
					.collect(Collectors.toCollection(() -> CollectionFactory.createApproximateCollection(source, source.size())));

		} else if (Map.class.isInstance(parent)) {

			Map<Object, Object> source = getTypedProperty(parentProperty, Map.class);

			if (source == null) {
				return;
			}

			Map<Object, Object> result = CollectionFactory.createApproximateMap(source, source.size());

			for (Entry<?, Object> entry : source.entrySet()) {
				result.put(entry.getKey(), setValue(entry.getValue(), leafProperty, value));
			}

			newValue = result;

		} else {
			newValue = setValue(parent, leafProperty, value);
		}

		if (newValue != parent) {
			setProperty(parentPath, newValue);
		}
	}

	/**
	 * @param path must not be {@literal null}.
	 * @param handling must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private Object handleNull(PersistentPropertyPath<? extends PersistentProperty<?>> path, SetNulls handling) {

		if (SKIP.equals(handling)) {
			return null;
		}

		String nullIntermediateMessage = "Cannot lookup property %s on null intermediate! Original path was: %s on %s.";

		if (SetNulls.SKIP_AND_LOG.equals(handling)) {
			logger.info(nullIntermediateMessage);
			return null;
		}

		PersistentPropertyPath<? extends PersistentProperty<?>> parentPath = path.getParentPath();

		throw new MappingException(String.format(nullIntermediateMessage, parentPath.getLeafProperty(), path.toDotPath(),
				getBean().getClass().getName()));
	}

	/**
	 * Sets the value for the given {@link PersistentProperty} on the given parent object and returns the potentially
	 * newly created instance.
	 *
	 * @param parent must not be {@literal null}.
	 * @param property must not be {@literal null}.
	 * @param newValue can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private static Object setValue(Object parent, PersistentProperty<?> property, @Nullable Object newValue) {

		PersistentPropertyAccessor<Object> accessor = property.getAccessorForOwner(parent);
		accessor.setProperty(property, newValue);
		return accessor.getBean();
	}

	/**
	 * Returns the value of the given {@link PersistentProperty} potentially applying type conversion to the given target
	 * type. The default implementation will not attempt any conversion and reject a type mismatch with a
	 * {@link MappingException}.
	 *
	 * @param property will never be {@literal null}.
	 * @param type will never be {@literal null}.
	 * @return can be {@literal null}.
	 */
	@Nullable
	protected <S> S getTypedProperty(PersistentProperty<?> property, Class<S> type) {

		Assert.notNull(property, "Property must not be null!");
		Assert.notNull(type, "Type must not be null!");

		Object value = getProperty(property);

		if (value == null) {
			return null;
		}

		if (!type.isInstance(value)) {
			throw new MappingException(String.format("Invalid property value type! Need %s but got %s!", //
					type.getName(), value.getClass().getName()));
		}

		return type.cast(value);
	}
}
