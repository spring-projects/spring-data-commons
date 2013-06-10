/*
 * Copyright 2011-2013 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Simple impementation of {@link PersistentProperty}.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public abstract class AbstractPersistentProperty<P extends PersistentProperty<P>> implements PersistentProperty<P> {

	protected final String name;
	protected final PropertyDescriptor propertyDescriptor;
	protected final TypeInformation<?> information;
	protected final Class<?> rawType;
	protected final Field field;
	protected final Association<P> association;
	protected final PersistentEntity<?, P> owner;
	private final SimpleTypeHolder simpleTypeHolder;

	public AbstractPersistentProperty(Field field, PropertyDescriptor propertyDescriptor, PersistentEntity<?, P> owner,
			SimpleTypeHolder simpleTypeHolder) {

		Assert.notNull(field);
		Assert.notNull(simpleTypeHolder);
		Assert.notNull(owner);

		this.name = field.getName();
		this.rawType = field.getType();
		this.information = owner.getTypeInformation().getProperty(this.name);
		this.propertyDescriptor = propertyDescriptor;
		this.field = field;
		this.association = isAssociation() ? createAssociation() : null;
		this.owner = owner;
		this.simpleTypeHolder = simpleTypeHolder;
	}

	protected abstract Association<P> createAssociation();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getOwner()
	 */
	public PersistentEntity<?, P> getOwner() {
		return owner;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getType()
	 */
	public Class<?> getType() {
		return information.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getRawType()
	 */
	public Class<?> getRawType() {
		return this.rawType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getTypeInformation()
	 */
	public TypeInformation<?> getTypeInformation() {
		return information;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getPersistentEntityType()
	 */
	public Iterable<? extends TypeInformation<?>> getPersistentEntityType() {

		List<TypeInformation<?>> result = new ArrayList<TypeInformation<?>>();

		TypeInformation<?> type = getTypeInformation();

		if (isEntity()) {
			result.add(type);
		}

		if (type.isCollectionLike() || isMap()) {
			TypeInformation<?> nestedType = getTypeInformationIfNotSimpleType(getTypeInformation().getActualType());
			if (nestedType != null) {
				result.add(nestedType);
			}
		}

		return result;
	}

	private TypeInformation<?> getTypeInformationIfNotSimpleType(TypeInformation<?> information) {
		return information == null || simpleTypeHolder.isSimpleType(information.getType()) ? null : information;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getGetter()
	 */
	public Method getGetter() {

		if (propertyDescriptor == null) {
			return null;
		}

		Method getter = propertyDescriptor.getReadMethod();

		if (getter == null) {
			return null;
		}

		return rawType.isAssignableFrom(getter.getReturnType()) ? getter : null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getSetter()
	 */
	public Method getSetter() {

		if (propertyDescriptor == null) {
			return null;
		}

		Method setter = propertyDescriptor.getWriteMethod();

		if (setter == null) {
			return null;
		}

		return setter.getParameterTypes()[0].isAssignableFrom(rawType) ? setter : null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getField()
	 */
	public Field getField() {
		return field;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getSpelExpression()
	 */
	public String getSpelExpression() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isTransient()
	 */
	public boolean isTransient() {
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#shallBePersisted()
	 */
	public boolean shallBePersisted() {
		return !isTransient();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isAssociation()
	 */
	public boolean isAssociation() {
		if (field.isAnnotationPresent(Reference.class)) {
			return true;
		}
		for (Annotation annotation : field.getDeclaredAnnotations()) {
			if (annotation.annotationType().isAnnotationPresent(Reference.class)) {
				return true;
			}
		}

		return false;
	}

	public Association<P> getAssociation() {
		return association;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isCollectionLike()
	 */
	public boolean isCollectionLike() {
		return information.isCollectionLike();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isMap()
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isArray()
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isEntity()
	 */
	public boolean isEntity() {

		TypeInformation<?> actualType = information.getActualType();
		boolean isComplexType = actualType == null ? false : !simpleTypeHolder.isSimpleType(actualType.getType());
		return isComplexType && !isTransient() && !isCollectionLike() && !isMap();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getComponentType()
	 */
	public Class<?> getComponentType() {

		if (!isMap() && !isCollectionLike()) {
			return null;
		}

		TypeInformation<?> componentType = information.getComponentType();
		return componentType == null ? null : componentType.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getMapValueType()
	 */
	public Class<?> getMapValueType() {
		return isMap() ? information.getMapValueType().getType() : null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getActualType()
	 */
	@Override
	public Class<?> getActualType() {
		return information.getActualType().getType();
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

		if (!(obj instanceof AbstractPersistentProperty)) {
			return false;
		}

		AbstractPersistentProperty<?> that = (AbstractPersistentProperty<?>) obj;
		return this.field.equals(that.field);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.field.hashCode();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.field.toString();
	}
}
