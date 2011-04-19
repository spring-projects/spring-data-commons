/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.util.TypeInformation;

/**
 * Simple impementation of {@link PersistentProperty}.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Oliver Gierke
 */
public class BasicPersistentProperty implements PersistentProperty {

	protected final String name;
	protected final PropertyDescriptor propertyDescriptor;
	protected final TypeInformation information;
	protected final Class<?> rawType;
	protected final Field field;
	protected Association association;
	protected Value value;
	protected boolean isTransient = false;
	protected PersistentEntity<?> owner;

	public BasicPersistentProperty(Field field, PropertyDescriptor propertyDescriptor, TypeInformation information) {
		this.name = field.getName();
		this.rawType = field.getType();
		this.information = information.getProperty(this.name);
		this.propertyDescriptor = propertyDescriptor;
		this.field = field;
		this.isTransient = Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(Transient.class);
		if (field.isAnnotationPresent(Value.class)) {
			this.value = field.getAnnotation(Value.class);
			// Fields with @Value annotations are considered the same as transient fields
			this.isTransient = true;
		}
		if (field.isAnnotationPresent(Autowired.class)) {
			this.isTransient = true;
		}
	}

	public Object getOwner() {
		return owner;
	}

	public void setOwner(Object owner) {
		if (null != owner && owner.getClass().isAssignableFrom(PersistentEntity.class)) {
			this.owner = (PersistentEntity<?>) owner;
		}
	}

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		return information.getType();
	}

	public Class<?> getRawType() {
		return this.rawType;
	}

	public TypeInformation getTypeInformation() {
		return information;
	}

	public PropertyDescriptor getPropertyDescriptor() {
		return propertyDescriptor;
	}

	public Field getField() {
		return field;
	}

	public Value getValueAnnotation() {
		return value;
	}

	public boolean isTransient() {
		return isTransient;
	}

	public boolean isAssociation() {
		return null != association;
	}

	public Association getAssociation() {
		return association;
	}

	public void setAssociation(Association association) {
		this.association = association;
	}

	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType()) || isArray();
	}

	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/* (non-Javadoc)
		 * @see org.springframework.data.mapping.model.PersistentProperty#isArray()
		 */
	public boolean isArray() {
		return getType().isArray();
	}

	public boolean isComplexType() {
		if (isCollection() || isArray()) {
			return !MappingBeanHelper.isSimpleType(getComponentType());
		} else {
			return !MappingBeanHelper.isSimpleType(getType());
		}
	}

	public boolean isEntity() {
		return isComplexType() && !isTransient() && !isCollection() && !isMap();
	}

	public Class<?> getComponentType() {
		return isMap() || isCollection() ? information.getComponentType().getType() : null;
	}

	/* (non-Javadoc)
		 * @see org.springframework.data.mapping.model.PersistentProperty#getMapValueType()
		 */
	public Class<?> getMapValueType() {
		return isMap() ? information.getMapValueType().getType() : null;
	}

	public boolean isIdProperty() {
		return field.isAnnotationPresent(Id.class);
	}
}
