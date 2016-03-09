/*
 * Copyright 2011-2016 the original author or authors.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
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
 * @author Christoph Strobl
 */
public abstract class AnnotationBasedPersistentProperty<P extends PersistentProperty<P>> extends
		AbstractPersistentProperty<P> {

	private static final String SPRING_DATA_PACKAGE = "org.springframework.data";

	private final Value value;
	private final Map<Class<? extends Annotation>, Annotation> annotationCache = new HashMap<Class<? extends Annotation>, Annotation>();

	private Boolean isTransient;
	private boolean usePropertyAccess;

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

		AccessType accessType = findPropertyOrOwnerAnnotation(AccessType.class);
		this.usePropertyAccess = accessType == null ? false : Type.PROPERTY.equals(accessType.value());
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

				validateAnnotation(annotation, "Ambiguous mapping! Annotation %s configured "
						+ "multiple times on accessor methods of property %s in class %s!", annotationType.getSimpleName(),
						getName(), getOwner().getType().getSimpleName());

				annotationCache.put(annotationType, annotation);
			}
		}

		if (field == null) {
			return;
		}

		for (Annotation annotation : field.getAnnotations()) {

			Class<? extends Annotation> annotationType = annotation.annotationType();

			validateAnnotation(annotation, "Ambiguous mapping! Annotation %s configured "
					+ "on field %s and one of its accessor methods in class %s!", annotationType.getSimpleName(),
					field.getName(), getOwner().getType().getSimpleName());

			annotationCache.put(annotationType, annotation);
		}
	}

	/**
	 * Verifies the given annotation candidate detected. Will be rejected if it's a Spring Data annotation and we already
	 * found another one with a different configuration setup (i.e. other attribute values).
	 * 
	 * @param candidate must not be {@literal null}.
	 * @param message must not be {@literal null}.
	 * @param arguments must not be {@literal null}.
	 */
	private void validateAnnotation(Annotation candidate, String message, Object... arguments) {

		Class<? extends Annotation> annotationType = candidate.annotationType();

		if (!annotationType.getName().startsWith(SPRING_DATA_PACKAGE)) {
			return;
		}

		if (annotationCache.containsKey(annotationType) && !annotationCache.get(annotationType).equals(candidate)) {
			throw new MappingException(String.format(message, arguments));
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

		if (this.isTransient == null) {
			boolean potentiallyTransient = super.isTransient() || isAnnotationPresent(Transient.class);
			this.isTransient = potentiallyTransient || isAnnotationPresent(Value.class)
					|| isAnnotationPresent(Autowired.class);
		}

		return this.isTransient;
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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return !isTransient() && !isAnnotationPresent(ReadOnlyProperty.class);
	}

	/**
	 * Returns the annotation found for the current {@link AnnotationBasedPersistentProperty}. Will prefer getters or
	 * setters annotations over ones found at the backing field as the former can be used to reconfigure the metadata in
	 * subclasses.
	 * 
	 * @param annotationType must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A findAnnotation(Class<A> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null!");

		if (annotationCache != null && annotationCache.containsKey(annotationType)) {
			return (A) annotationCache.get(annotationType);
		}

		for (Method method : Arrays.asList(getGetter(), getSetter())) {

			if (method == null) {
				continue;
			}

			A annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);

			if (annotation != null) {
				return cacheAndReturn(annotationType, annotation);
			}
		}

		return cacheAndReturn(annotationType,
				field == null ? null : AnnotatedElementUtils.findMergedAnnotation(field, annotationType));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#findPropertyOrOwnerAnnotation(java.lang.Class)
	 */
	@Override
	public <A extends Annotation> A findPropertyOrOwnerAnnotation(Class<A> annotationType) {

		A annotation = findAnnotation(annotationType);
		return annotation == null ? owner.findAnnotation(annotationType) : annotation;
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
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return findAnnotation(annotationType) != null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#usePropertyAccess()
	 */
	@Override
	public boolean usePropertyAccess() {
		return super.usePropertyAccess() || usePropertyAccess;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#toString()
	 */
	@Override
	public String toString() {

		if (annotationCache.isEmpty()) {
			populateAnnotationCache(field);
		}

		StringBuilder builder = new StringBuilder();

		for (Annotation annotation : annotationCache.values()) {
			if (annotation != null) {
				builder.append(annotation.toString()).append(" ");
			}
		}

		return builder.toString() + super.toString();
	}
}
