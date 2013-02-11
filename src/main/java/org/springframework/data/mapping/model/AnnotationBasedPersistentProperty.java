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

import static org.springframework.core.annotation.AnnotationUtils.*;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * Special {@link PersistentProperty} that takes annotations at a property into account.
 * 
 * @author Oliver Gierke
 */
public abstract class AnnotationBasedPersistentProperty<P extends PersistentProperty<P>> extends
		AbstractPersistentProperty<P> {

	private final Value value;
	private final Map<Class<? extends Annotation>, Annotation> annotationCache = new HashMap<Class<? extends Annotation>, Annotation>();

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 * 
	 * @param field must not be {@literal null}.
	 * @param propertyDescriptor can be {@literal null}.
	 * @param owner must not be {@literal null}.
	 */
	public AnnotationBasedPersistentProperty(Field field, PropertyDescriptor propertyDescriptor,
			PersistentEntity<?, P> owner, SimpleTypeHolder simpleTypeHolder) {

		super(field, propertyDescriptor, owner, simpleTypeHolder);
		populateAnnotationCache(field);
		this.value = findAnnotation(Value.class);
	}

	/**
	 * Populates the annotation cache by eagerly accessing the annotations directly annotated to the accessors (if
	 * available) and the backing field. Annotations override annotations found on field.
	 * 
	 * @param field
	 * @throws MappingException in case we find an ambiguous mapping on the accessor methods
	 */
	private final void populateAnnotationCache(Field field) {

		for (Method method : Arrays.asList(getGetter(), getSetter())) {

			if (method == null) {
				continue;
			}

			for (Annotation annotation : method.getAnnotations()) {

				Class<? extends Annotation> annotationType = annotation.annotationType();

				if (annotationCache.containsKey(annotationType)) {
					throw new MappingException(String.format("Ambiguous mapping! Annotation %s configured "
							+ "multiple times on accessor methods of property %s in class %s!", annotationType, getName(), getOwner()
							.getType().getName()));
				}

				annotationCache.put(annotationType, annotation);
			}
		}

		for (Annotation annotation : field.getAnnotations()) {

			Class<? extends Annotation> annotationType = annotation.annotationType();

			if (!annotationCache.containsKey(annotationType)) {
				annotationCache.put(annotationType, annotation);
			}
		}
	}

	/**
	 * Inspects a potentially available {@link Value} annotation at the property and returns the {@link String} value of
	 * it.
	 * 
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#getSpelExpression()
	 */
	@Override
	public String getSpelExpression() {
		return value == null ? null : value.value();
	}

	/**
	 * Considers plain transient fields, fields annotated with {@link Transient}, {@link Value} or {@link Autowired} as
	 * transien.
	 * 
	 * @see org.springframework.data.mapping.BasicPersistentProperty#isTransient()
	 */
	@Override
	public boolean isTransient() {

		boolean isTransient = super.isTransient() || isAnnotationPresent(Transient.class);
		return isTransient || isAnnotationPresent(Value.class) || isAnnotationPresent(Autowired.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isIdProperty()
	 */
	public boolean isIdProperty() {
		return isAnnotationPresent(Id.class);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isVersionProperty()
	 */
	public boolean isVersionProperty() {
		return isAnnotationPresent(Version.class);
	}

	/**
	 * Considers the property an {@link Association} if it is annotated with {@link Reference}.
	 */
	@Override
	public boolean isAssociation() {
		return !isTransient() && isAnnotationPresent(Reference.class);
	}

	/**
	 * Returns the annotation found for the current {@link AnnotationBasedPersistentProperty}. Will prefer field
	 * annotations over ones found at getters or setters.
	 * 
	 * @param annotationType must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A findAnnotation(Class<? extends A> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null!");

		if (annotationCache != null && annotationCache.containsKey(annotationType)) {
			return (A) annotationCache.get(annotationType);
		}

		for (Method method : Arrays.asList(getGetter(), getSetter())) {

			if (method == null) {
				continue;
			}

			A annotation = AnnotationUtils.findAnnotation(method, annotationType);

			if (annotation != null) {
				return cacheAndReturn(annotationType, annotation);
			}
		}

		return cacheAndReturn(annotationType, getAnnotation(field, annotationType));
	}

	/**
	 * Puts the given annotation into the local cache and returns it.
	 * 
	 * @param annotation
	 * @return
	 */
	private <A extends Annotation> A cacheAndReturn(Class<? extends A> type, A annotation) {

		if (annotationCache != null) {
			annotationCache.put(type, annotation);
		}

		return annotation;
	}

	/**
	 * Returns whether the property carries the an annotation of the given type.
	 * 
	 * @param annotationType the annotation type to look up.
	 * @return
	 */
	protected boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return findAnnotation(annotationType) != null;
	}
}
