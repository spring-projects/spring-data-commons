/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.mapping.model;

import lombok.Getter;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

/**
 * Value object to abstract the concept of a property backed by a {@link Field} and / or a {@link PropertyDescriptor}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class Property {

	private final @Getter Optional<Field> field;
	private final Optional<PropertyDescriptor> descriptor;

	private final Lazy<Class<?>> rawType;
	private final Lazy<Integer> hashCode;

	private Property(Optional<Field> field, Optional<PropertyDescriptor> descriptor) {

		this.field = field;
		this.descriptor = descriptor;
		this.hashCode = Lazy.of(this::computeHashCode);
		this.rawType = Lazy.of(
				() -> field.<Class<?>> map(Field::getType).orElseGet(() -> descriptor.map(PropertyDescriptor::getPropertyType)//
						.orElseThrow(IllegalStateException::new)));
	}

	/**
	 * Creates a new {@link Property} backed by the given field.
	 * 
	 * @param field must not be {@literal null}.
	 * @return
	 */
	public static Property of(Field field) {

		Assert.notNull(field, "Field must not be null!");

		return new Property(Optional.of(field), Optional.empty());
	}

	/**
	 * Creates a new {@link Property} backed by the given {@link Field} and {@link PropertyDescriptor}.
	 * 
	 * @param field must not be {@literal null}.
	 * @param descriptor must not be {@literal null}.
	 * @return
	 */
	public static Property of(Field field, PropertyDescriptor descriptor) {

		Assert.notNull(field, "Field must not be null!");
		Assert.notNull(descriptor, "PropertyDescriptor must not be null!");

		return new Property(Optional.of(field), Optional.of(descriptor));
	}

	/**
	 * Creates a new {@link Property} for the given {@link PropertyDescriptor}.
	 * 
	 * @param descriptor must not be {@literal null}.
	 * @return
	 */
	public static Property of(PropertyDescriptor descriptor) {

		Assert.notNull(descriptor, "PropertyDescriptor must not be null!");

		return new Property(Optional.empty(), Optional.of(descriptor));
	}

	/**
	 * Returns whether the property is backed by a field.
	 * 
	 * @return
	 */
	public boolean isFieldBacked() {
		return field.isPresent();
	}

	/**
	 * Returns the getter of the property if available and if it matches the type of the property.
	 * 
	 * @return will never be {@literal null}.
	 */
	public Optional<Method> getGetter() {
		return descriptor.map(PropertyDescriptor::getReadMethod)//
				.filter(it -> getType().isAssignableFrom(it.getReturnType()));
	}

	/**
	 * Returns the setter of the property if available and if its first (only) parameter matches the type of the property.
	 * 
	 * @return will never be {@literal null}.
	 */
	public Optional<Method> getSetter() {
		return descriptor.map(PropertyDescriptor::getWriteMethod)//
				.filter(it -> it.getParameterTypes()[0].isAssignableFrom(getType()));
	}

	/**
	 * Returns whether the property exposes a getter or a setter.
	 * 
	 * @return
	 */
	public boolean hasAccessor() {
		return getGetter().isPresent() || getSetter().isPresent();
	}

	/**
	 * Returns the name of the property.
	 * 
	 * @return will never be {@literal null}.
	 */
	public String getName() {

		return field.map(Field::getName)//
				.orElseGet(() -> descriptor.map(FeatureDescriptor::getName)//
						.orElseThrow(IllegalStateException::new));
	}

	/**
	 * Returns the type of the property.
	 * 
	 * @return will never be {@literal null}.
	 */
	public Class<?> getType() {
		return rawType.get();
	}

	private int computeHashCode() {

		return this.field.map(Field::hashCode)
				.orElseGet(() -> this.descriptor.map(PropertyDescriptor::hashCode).orElseThrow(IllegalStateException::new));
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hashCode.get();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Property)) {
			return false;
		}

		Property that = (Property) obj;

		return this.field.isPresent() ? this.field.equals(that.field) : this.descriptor.equals(that.descriptor);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return field.map(Object::toString)
				.orElseGet(() -> descriptor.map(Object::toString).orElseThrow(IllegalStateException::new));
	}
}
