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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import org.springframework.data.annotation.Reference;
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
public abstract class AbstractPersistentProperty<P extends PersistentProperty<P>> implements PersistentProperty<P> {

	protected final String name;
	protected final PropertyDescriptor propertyDescriptor;
	protected final TypeInformation<?> information;
	protected final Class<?> rawType;
	protected final Field field;
	protected final Association<P> association;
	protected final PersistentEntity<?, P> owner;

	public AbstractPersistentProperty(Field field, PropertyDescriptor propertyDescriptor, PersistentEntity<?, P> owner) {
		this.name = field.getName();
		this.rawType = field.getType();
		this.information = owner.getTypeInformation().getProperty(this.name);
		this.propertyDescriptor = propertyDescriptor;
		this.field = field;
		this.association = isAssociation() ? createAssociation() : null;
		this.owner = owner;
	}
	
	protected abstract Association<P> createAssociation();

	public PersistentEntity<?, P> getOwner() {
		return owner;
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

	public TypeInformation<?> getTypeInformation() {
		return information;
	}

	public PropertyDescriptor getPropertyDescriptor() {
		return propertyDescriptor;
	}

	public Field getField() {
		return field;
	}

	public String getSpelExpression() {
		return null;
	}

	public boolean isTransient() {
		return Modifier.isTransient(field.getModifiers());
	}

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
}
