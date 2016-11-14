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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.data.util.Optionals;
import org.springframework.util.Assert;

/**
 * Special {@link PersistentProperty} that takes annotations at a property into account.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public abstract class AnnotationBasedPersistentProperty<P extends PersistentProperty<P>>
		extends AbstractPersistentProperty<P> {

	private static final String SPRING_DATA_PACKAGE = "org.springframework.data";

	private final Optional<Value> value;
	private final Map<Class<? extends Annotation>, Optional<? extends Annotation>> annotationCache = new HashMap<>();

	private Boolean isTransient;
	private boolean usePropertyAccess;

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 * 
	 * @param field must not be {@literal null}.
	 * @param propertyDescriptor can be {@literal null}.
	 * @param owner must not be {@literal null}.
	 */
	public AnnotationBasedPersistentProperty(Optional<Field> field, PropertyDescriptor propertyDescriptor,
			PersistentEntity<?, P> owner, SimpleTypeHolder simpleTypeHolder) {

		super(field, propertyDescriptor, owner, simpleTypeHolder);

		populateAnnotationCache(field);

		this.usePropertyAccess = findPropertyOrOwnerAnnotation(AccessType.class).map(it -> Type.PROPERTY.equals(it.value()))
				.orElse(false);
		this.value = findAnnotation(Value.class);
	}

	/**
	 * Populates the annotation cache by eagerly accessing the annotations directly annotated to the accessors (if
	 * available) and the backing field. Annotations override annotations found on field.
	 * 
	 * @param field
	 * @throws MappingException in case we find an ambiguous mapping on the accessor methods
	 */
	private final void populateAnnotationCache(Optional<Field> field) {

		Optionals.toStream(getGetter(), getSetter()).forEach(it -> {

			for (Annotation annotation : it.getAnnotations()) {

				Class<? extends Annotation> annotationType = annotation.annotationType();

				validateAnnotation(annotation,
						"Ambiguous mapping! Annotation %s configured "
								+ "multiple times on accessor methods of property %s in class %s!",
						annotationType.getSimpleName(), getName(), getOwner().getType().getSimpleName());

				annotationCache.put(annotationType, Optional.of(annotation));
			}
		});

		field.ifPresent(it -> {

			for (Annotation annotation : it.getAnnotations()) {

				Class<? extends Annotation> annotationType = annotation.annotationType();

				validateAnnotation(annotation,
						"Ambiguous mapping! Annotation %s configured " + "on field %s and one of its accessor methods in class %s!",
						annotationType.getSimpleName(), it.getName(), getOwner().getType().getSimpleName());

				annotationCache.put(annotationType, Optional.of(annotation));
			}
		});
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

		if (annotationCache.containsKey(annotationType)
				&& !annotationCache.get(annotationType).equals(Optional.of(candidate))) {
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
	public Optional<String> getSpelExpression() {
		return value.map(Value::value);
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
	public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null!");

		if (annotationCache != null && annotationCache.containsKey(annotationType)) {
			return (Optional<A>) annotationCache.get(annotationType);
		}

		return cacheAndReturn(annotationType, getAccessors()//
				.map(it -> it.map(inner -> AnnotatedElementUtils.findMergedAnnotation(inner, annotationType)))//
				.flatMap(it -> Optionals.toStream(it))//
				.findFirst());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#findPropertyOrOwnerAnnotation(java.lang.Class)
	 */
	@Override
	public <A extends Annotation> Optional<A> findPropertyOrOwnerAnnotation(Class<A> annotationType) {

		Optional<A> annotation = findAnnotation(annotationType);
		return annotation.isPresent() ? annotation : owner.findAnnotation(annotationType);
	}

	/**
	 * Puts the given annotation into the local cache and returns it.
	 * 
	 * @param annotation
	 * @return
	 */
	private <A extends Annotation> Optional<A> cacheAndReturn(Class<? extends A> type, Optional<A> annotation) {

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
		return findAnnotation(annotationType).isPresent();
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

		StringBuilder builder = new StringBuilder().append(annotationCache.values().stream()//
				.flatMap(it -> Optionals.toStream(it))//
				.map(Object::toString)//
				.collect(Collectors.joining(" ")));

		return builder.toString() + super.toString();
	}

	private Stream<Optional<? extends AnnotatedElement>> getAccessors() {
		return Arrays.<Optional<? extends AnnotatedElement>> asList(getGetter(), getSetter(), getField()).stream();
	}
}
