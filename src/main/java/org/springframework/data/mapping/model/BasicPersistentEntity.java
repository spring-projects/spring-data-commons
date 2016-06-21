/*
 * Copyright 2011-2016 by the original author(s).
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
package org.springframework.data.mapping.model;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Simple value object to capture information of {@link PersistentEntity}s.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Patryk Wasik
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class BasicPersistentEntity<T, P extends PersistentProperty<P>> implements MutablePersistentEntity<T, P> {

	private static final String TYPE_MISMATCH = "Target bean of type %s is not of type of the persistent entity (%s)!";

	private final Optional<PreferredConstructor<T, P>> constructor;
	private final TypeInformation<T> information;
	private final List<P> properties;
	private final Optional<Comparator<P>> comparator;
	private final Set<Association<P>> associations;

	private final Map<String, P> propertyCache;
	private final Map<Class<? extends Annotation>, Optional<Annotation>> annotationCache;

	private Optional<P> idProperty = Optional.empty();
	private Optional<P> versionProperty = Optional.empty();
	private PersistentPropertyAccessorFactory propertyAccessorFactory;

	private final Lazy<Alias> typeAlias;

	/**
	 * Creates a new {@link BasicPersistentEntity} from the given {@link TypeInformation}.
	 *
	 * @param information must not be {@literal null}.
	 */
	public BasicPersistentEntity(TypeInformation<T> information) {
		this(information, Optional.empty());
	}

	/**
	 * Creates a new {@link BasicPersistentEntity} for the given {@link TypeInformation} and {@link Comparator}. The given
	 * {@link Comparator} will be used to define the order of the {@link PersistentProperty} instances added to the
	 * entity.
	 *
	 * @param information must not be {@literal null}.
	 * @param comparator can be {@literal null}.
	 */
	public BasicPersistentEntity(TypeInformation<T> information, Optional<Comparator<P>> comparator) {

		Assert.notNull(information);

		this.information = information;
		this.properties = new ArrayList<>();
		this.comparator = comparator;
		this.constructor = new PreferredConstructorDiscoverer<>(this).getConstructor();
		this.associations = comparator.<Set<Association<P>>>map(it -> new TreeSet<>(new AssociationComparator<>(it)))
				.orElseGet(() -> new HashSet<>());

		this.propertyCache = new HashMap<>();
		this.annotationCache = new HashMap<>();
		this.propertyAccessorFactory = BeanWrapperPropertyAccessorFactory.INSTANCE;

		this.typeAlias = Lazy.of(() -> Alias
				.ofOptional(Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(getType(), TypeAlias.class))//
						.map(TypeAlias::value)//
						.filter(it -> StringUtils.hasText(it))));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistenceConstructor()
	 */
	public Optional<PreferredConstructor<T, P>> getPersistenceConstructor() {
		return constructor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isConstructorArgument(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isConstructorArgument(PersistentProperty<?> property) {
		return constructor.map(it -> it.isConstructorParameter(property)).orElse(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isIdProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isIdProperty(PersistentProperty<?> property) {
		return this.idProperty.map(it -> it.equals(property)).orElse(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isVersionProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isVersionProperty(PersistentProperty<?> property) {
		return this.versionProperty.map(it -> it.equals(property)).orElse(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getName()
	 */
	public String getName() {
		return getType().getName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getIdProperty()
	 */
	public Optional<P> getIdProperty() {
		return idProperty;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getVersionProperty()
	 */
	public Optional<P> getVersionProperty() {
		return versionProperty;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#hasIdProperty()
	 */
	public boolean hasIdProperty() {
		return idProperty.isPresent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#hasVersionProperty()
	 */
	public boolean hasVersionProperty() {
		return versionProperty.isPresent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addPersistentProperty(P)
	 */
	public void addPersistentProperty(P property) {

		Assert.notNull(property);

		if (properties.contains(property)) {
			return;
		}

		properties.add(property);

		if (!propertyCache.containsKey(property.getName())) {
			propertyCache.put(property.getName(), property);
		}

		P candidate = returnPropertyIfBetterIdPropertyCandidateOrNull(property);

		if (candidate != null) {
			this.idProperty = Optional.of(candidate);
		}

		if (property.isVersionProperty()) {

			this.versionProperty.ifPresent(it -> {

				throw new MappingException(
						String.format("Attempt to add version property %s but already have property %s registered "
								+ "as version. Check your mapping configuration!", property.getField(), it.getField()));
			});

			this.versionProperty = Optional.of(property);
		}
	}

	/**
	 * Returns the given property if it is a better candidate for the id property than the current id property.
	 *
	 * @param property the new id property candidate, will never be {@literal null}.
	 * @return the given id property or {@literal null} if the given property is not an id property.
	 */
	protected P returnPropertyIfBetterIdPropertyCandidateOrNull(P property) {

		if (!property.isIdProperty()) {
			return null;
		}

		this.idProperty.ifPresent(it -> {
			throw new MappingException(String.format("Attempt to add id property %s but already have property %s registered "
					+ "as id. Check your mapping configuration!", property.getField(), it.getField()));
		});

		return property;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addAssociation(org.springframework.data.mapping.model.Association)
	 */
	public void addAssociation(Association<P> association) {

		if (!associations.contains(association)) {
			associations.add(association);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistentProperty(java.lang.String)
	 */
	public Optional<P> getPersistentProperty(String name) {
		return Optional.ofNullable(propertyCache.get(name));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistentProperty(java.lang.Class)
	 */
	@Override
	public Optional<P> getPersistentProperty(Class<? extends Annotation> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null!");

		Optional<P> property = properties.stream()//
				.filter(it -> it.isAnnotationPresent(annotationType))//
				.findAny();

		if (property.isPresent()) {
			return property;
		}

		return associations.stream().map(Association::getInverse)//
				.filter(it -> it.isAnnotationPresent(annotationType)).findAny();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getType()
	 */
	public Class<T> getType() {
		return information.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getTypeAlias()
	 */
	public Alias getTypeAlias() {
		return typeAlias.get();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getTypeInformation()
	 */
	public TypeInformation<T> getTypeInformation() {
		return information;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithProperties(org.springframework.data.mapping.PropertyHandler)
	 */
	public void doWithProperties(PropertyHandler<P> handler) {

		Assert.notNull(handler);

		properties.stream()//
				.filter(it -> !it.isTransient() && !it.isAssociation())//
				.forEach(it -> handler.doWithPersistentProperty(it));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithProperties(org.springframework.data.mapping.PropertyHandler.Simple)
	 */
	@Override
	public void doWithProperties(SimplePropertyHandler handler) {

		Assert.notNull(handler);

		properties.stream()//
				.filter(it -> !it.isTransient() && !it.isAssociation())//
				.forEach(it -> handler.doWithPersistentProperty(it));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithAssociations(org.springframework.data.mapping.AssociationHandler)
	 */
	public void doWithAssociations(AssociationHandler<P> handler) {

		Assert.notNull(handler);

		for (Association<P> association : associations) {
			handler.doWithAssociation(association);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithAssociations(org.springframework.data.mapping.SimpleAssociationHandler)
	 */
	public void doWithAssociations(SimpleAssociationHandler handler) {

		Assert.notNull(handler);

		for (Association<? extends PersistentProperty<?>> association : associations) {
			handler.doWithAssociation(association);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#findAnnotation(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {

		return (Optional<A>) annotationCache.computeIfAbsent(annotationType,
				it -> Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(getType(), it)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#verify()
	 */
	public void verify() {
		comparator.ifPresent(it -> Collections.sort(properties, it));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MutablePersistentEntity#setPersistentPropertyAccessorFactory(org.springframework.data.mapping.model.PersistentPropertyAccessorFactory)
	 */
	@Override
	public void setPersistentPropertyAccessorFactory(PersistentPropertyAccessorFactory factory) {
		this.propertyAccessorFactory = factory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPropertyAccessor(java.lang.Object)
	 */
	@Override
	public PersistentPropertyAccessor getPropertyAccessor(Object bean) {

		Assert.notNull(bean, "Target bean must not be null!");

		Assert.isTrue(getType().isInstance(bean),
				String.format(TYPE_MISMATCH, bean.getClass().getName(), getType().getName()));

		return propertyAccessorFactory.getPropertyAccessor(this, bean);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getIdentifierAccessor(java.lang.Object)
	 */
	@Override
	public IdentifierAccessor getIdentifierAccessor(Object bean) {

		Assert.notNull(bean, "Target bean must not be null!");
		Assert.isTrue(getType().isInstance(bean), "Target bean is not of type of the persistent entity!");

		return hasIdProperty() ? new IdPropertyIdentifierAccessor(this, bean) : NullReturningIdentifierAccessor.INSTANCE;
	}

	/**
	 * A null-object implementation of {@link IdentifierAccessor} to be able to return an accessor for entities that do
	 * not have an identifier property.
	 *
	 * @author Oliver Gierke
	 */
	private static enum NullReturningIdentifierAccessor implements IdentifierAccessor {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.IdentifierAccessor#getIdentifier()
		 */
		@Override
		public Optional<Object> getIdentifier() {
			return Optional.empty();
		}
	}

	/**
	 * Simple {@link Comparator} adaptor to delegate ordering to the inverse properties of the association.
	 *
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	private static final class AssociationComparator<P extends PersistentProperty<P>>
			implements Comparator<Association<P>>, Serializable {

		private static final long serialVersionUID = 4508054194886854513L;
		private final @NonNull Comparator<P> delegate;

		/*
		 * (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Association<P> left, Association<P> right) {
			return delegate.compare(left.getInverse(), right.getInverse());
		}
	}
}
