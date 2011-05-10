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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;


/**
 * Special {@link PersistentProperty} that takes annotations at a property into account.
 *
 * @author Oliver Gierke
 */
public abstract class AnnotationBasedPersistentProperty<P extends PersistentProperty<P>> extends AbstractPersistentProperty<P> {

	private final Value value;

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 *
	 * @param field
	 * @param propertyDescriptor
	 * @param owner
	 */
	public AnnotationBasedPersistentProperty(Field field,
																					 PropertyDescriptor propertyDescriptor, PersistentEntity<?, P> owner) {

		super(field, propertyDescriptor, owner);
		this.value = field.getAnnotation(Value.class);
		field.isAnnotationPresent(Autowired.class);
	}

	/**
	 * Inspects a potentially available {@link Value} annotation at the property and returns the {@link String} value of
	 * it.
	 *
	 * @see org.springframework.data.mapping.AbstractPersistentProperty#getSpelExpression()
	 */
	public String getSpelExpression() {
		return value == null ? null : value.value();
	}

	/**
	 * Considers plain transient fields, fields annotated with {@link Transient}, {@link Value} or {@link Autowired} as
	 * transien.
	 *
	 * @see org.springframework.data.mapping.BasicPersistentProperty#isTransient()
	 */
	public boolean isTransient() {

		boolean isTransient = super.isTransient() || field.isAnnotationPresent(Transient.class);

		return isTransient || field.isAnnotationPresent(Value.class) || field.isAnnotationPresent(Autowired.class);
	}

	/**
	 * Regards the property as ID if there is an {@link Id} annotation found on it.
	 */
	public boolean isIdProperty() {
		return field.isAnnotationPresent(Id.class);
	}

	/**
	 * Considers the property an {@link Association} if it is annotated with {@link Reference}.
	 */
	@Override
	public boolean isAssociation() {

		if (isTransient()) {
			return false;
		}
		if (field.isAnnotationPresent(Reference.class)) {
			return true;
		}

		// TODO: do we need this? Shouldn't the section above already find that annotation?
		for (Annotation annotation : field.getDeclaredAnnotations()) {
			if (annotation.annotationType().isAnnotationPresent(Reference.class)) {
				return true;
			}
		}

		return false;
	}
}
