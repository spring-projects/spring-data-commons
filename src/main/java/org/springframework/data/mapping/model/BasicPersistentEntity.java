/*
 * Copyright 2011-2018 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.TargetAwareIdentifierAccessor;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MultiValueMap;
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

	private final @Nullable PreferredConstructor<T, P> constructor;
	private final TypeInformation<T> information;
	private final List<P> properties;
	private final List<P> persistentPropertiesCache;
	private final @Nullable Comparator<P> comparator;
	private final Set<Association<P>> associations;

	private final Map<String, P> propertyCache;
	private final Map<Class<? extends Annotation>, Optional<Annotation>> annotationCache;
	private final MultiValueMap<Class<? extends Annotation>, P> propertyAnnotationCache;

	private @Nullable P idProperty;
	private @Nullable P versionProperty;
	private PersistentPropertyAccessorFactory propertyAccessorFactory;

	private final Lazy<Alias> typeAlias;

	/**
	 * Creates a new {@link BasicPersistentEntity} from the given {@link TypeInformation}.
	 *
	 * @param information must not be {@literal null}.
	 */
	public BasicPersistentEntity(TypeInformation<T> information) {
		this(information, null);
	}

	/**
	 * Creates a new {@link BasicPersistentEntity} for the given {@link TypeInformation} and {@link Comparator}. The given
	 * {@link Comparator} will be used to define the order of the {@link PersistentProperty} instances added to the
	 * entity.
	 *
	 * @param information must not be {@literal null}.
	 * @param comparator can be {@literal null}.
	 */
	public BasicPersistentEntity(TypeInformation<T> information, @Nullable Comparator<P> comparator) {

		Assert.notNull(information, "Information must not be null!");

		this.information = information;
		this.properties = new ArrayList<>();
		this.persistentPropertiesCache = new ArrayList<>();
		this.comparator = comparator;
		this.constructor = PreferredConstructorDiscoverer.discover(this);
		this.associations = comparator == null ? new HashSet<>() : new TreeSet<>(new AssociationComparator<>(comparator));

		this.propertyCache = new ConcurrentReferenceHashMap<>();
		this.annotationCache = new ConcurrentReferenceHashMap<>();
		this.propertyAnnotationCache = CollectionUtils.toMultiValueMap(new ConcurrentReferenceHashMap<>());
		this.propertyAccessorFactory = BeanWrapperPropertyAccessorFactory.INSTANCE;
		this.typeAlias = Lazy.of(() -> getAliasFromAnnotation(getType()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistenceConstructor()
	 */
	@Nullable
	public PreferredConstructor<T, P> getPersistenceConstructor() {
		return constructor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isConstructorArgument(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isConstructorArgument(PersistentProperty<?> property) {
		return constructor != null && constructor.isConstructorParameter(property);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isIdProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isIdProperty(PersistentProperty<?> property) {
		return idProperty != null && idProperty.equals(property);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isVersionProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isVersionProperty(PersistentProperty<?> property) {
		return versionProperty != null && versionProperty.equals(property);
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
	@Nullable
	public P getIdProperty() {
		return idProperty;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getVersionProperty()
	 */
	@Nullable
	public P getVersionProperty() {
		return versionProperty;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#hasIdProperty()
	 */
	public boolean hasIdProperty() {
		return idProperty != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#hasVersionProperty()
	 */
	public boolean hasVersionProperty() {
		return versionProperty != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MutablePersistentEntity#addPersistentProperty(P)
	 */
	public void addPersistentProperty(P property) {

		Assert.notNull(property, "Property must not be null!");

		if (properties.contains(property)) {
			return;
		}

		properties.add(property);

		if (!property.isTransient() && !property.isAssociation()) {
			persistentPropertiesCache.add(property);
		}

		propertyCache.computeIfAbsent(property.getName(), key -> property);

		P candidate = returnPropertyIfBetterIdPropertyCandidateOrNull(property);

		if (candidate != null) {
			this.idProperty = candidate;
		}

		if (property.isVersionProperty()) {

			P versionProperty = this.versionProperty;

			if (versionProperty != null) {

				throw new MappingException(
						String.format(
								"Attempt to add version property %s but already have property %s registered "
										+ "as version. Check your mapping configuration!",
								property.getField(), versionProperty.getField()));
			}

			this.versionProperty = property;
		}
	}

	/**
	 * Returns the given property if it is a better candidate for the id property than the current id property.
	 *
	 * @param property the new id property candidate, will never be {@literal null}.
	 * @return the given id property or {@literal null} if the given property is not an id property.
	 */
	@Nullable
	protected P returnPropertyIfBetterIdPropertyCandidateOrNull(P property) {

		if (!property.isIdProperty()) {
			return null;
		}

		P idProperty = this.idProperty;

		if (idProperty != null) {
			throw new MappingException(String.format("Attempt to add id property %s but already have property %s registered "
					+ "as id. Check your mapping configuration!", property.getField(), idProperty.getField()));
		}

		return property;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MutablePersistentEntity#addAssociation(org.springframework.data.mapping.model.Association)
	 */
	public void addAssociation(Association<P> association) {

		Assert.notNull(association, "Association must not be null!");

		if (!associations.contains(association)) {
			associations.add(association);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistentProperty(java.lang.String)
	 */
	@Override
	@Nullable
	public P getPersistentProperty(String name) {
		return propertyCache.get(name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistentProperties(java.lang.String)
	 */
	@Override
	public Iterable<P> getPersistentProperties(Class<? extends Annotation> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null!");
		return propertyAnnotationCache.computeIfAbsent(annotationType, this::doFindPersistentProperty);
	}

	private List<P> doFindPersistentProperty(Class<? extends Annotation> annotationType) {

		List<P> annotatedProperties = properties.stream() //
				.filter(it -> it.isAnnotationPresent(annotationType)) //
				.collect(Collectors.toList());

		if (!annotatedProperties.isEmpty()) {
			return annotatedProperties;
		}

		return associations.stream() //
				.map(Association::getInverse) //
				.filter(it -> it.isAnnotationPresent(annotationType)).collect(Collectors.toList());
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

		Assert.notNull(handler, "PropertyHandler must not be null!");

		for (P property : persistentPropertiesCache) {
			handler.doWithPersistentProperty(property);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithProperties(org.springframework.data.mapping.PropertyHandler.Simple)
	 */
	@Override
	public void doWithProperties(SimplePropertyHandler handler) {

		Assert.notNull(handler, "Handler must not be null!");

		for (PersistentProperty<?> property : persistentPropertiesCache) {
			handler.doWithPersistentProperty(property);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithAssociations(org.springframework.data.mapping.AssociationHandler)
	 */
	public void doWithAssociations(AssociationHandler<P> handler) {

		Assert.notNull(handler, "Handler must not be null!");

		for (Association<P> association : associations) {
			handler.doWithAssociation(association);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithAssociations(org.springframework.data.mapping.SimpleAssociationHandler)
	 */
	public void doWithAssociations(SimpleAssociationHandler handler) {

		Assert.notNull(handler, "Handler must not be null!");

		for (Association<? extends PersistentProperty<?>> association : associations) {
			handler.doWithAssociation(association);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#findAnnotation(java.lang.Class)
	 */
	@Nullable
	@Override
	public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
		return doFindAnnotation(annotationType).orElse(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isAnnotationPresent(java.lang.Class)
	 */
	@Override
	public <A extends Annotation> boolean isAnnotationPresent(Class<A> annotationType) {
		return doFindAnnotation(annotationType).isPresent();
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> Optional<A> doFindAnnotation(Class<A> annotationType) {

		return (Optional<A>) annotationCache.computeIfAbsent(annotationType,
				it -> Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(getType(), it)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MutablePersistentEntity#verify()
	 */
	public void verify() {

		if (comparator != null) {
			properties.sort(comparator);
			persistentPropertiesCache.sort(comparator);
		}
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
				() -> String.format(TYPE_MISMATCH, bean.getClass().getName(), getType().getName()));

		return propertyAccessorFactory.getPropertyAccessor(this, bean);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getIdentifierAccessor(java.lang.Object)
	 */
	@Override
	public IdentifierAccessor getIdentifierAccessor(Object bean) {

		Assert.notNull(bean, "Target bean must not be null!");
		Assert.isTrue(getType().isInstance(bean),
				() -> String.format(TYPE_MISMATCH, bean.getClass().getName(), getType().getName()));

		return hasIdProperty() ? new IdPropertyIdentifierAccessor(this, bean) : new AbsentIdentifierAccessor(bean);
	}

	@Override
	public Iterator<P> iterator() {
		return Collections.unmodifiableList(properties).iterator();
	}

	/**
	 * Calculates the {@link Alias} to be used for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static Alias getAliasFromAnnotation(Class<?> type) {

		Optional<String> typeAliasValue = Optional
				.ofNullable(AnnotatedElementUtils.findMergedAnnotation(type, TypeAlias.class))//
				.map(TypeAlias::value)//
				.filter(StringUtils::hasText);

		return Alias.ofNullable(typeAliasValue.orElse(null));
	}

	/**
	 * A null-object implementation of {@link IdentifierAccessor} to be able to return an accessor for entities that do
	 * not have an identifier property.
	 *
	 * @author Oliver Gierke
	 */
	private static class AbsentIdentifierAccessor extends TargetAwareIdentifierAccessor {

		public AbsentIdentifierAccessor(Object target) {
			super(target);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.IdentifierAccessor#getIdentifier()
		 */
		@Override
		@Nullable
		public Object getIdentifier() {
			return null;
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
		public int compare(@Nullable Association<P> left, @Nullable Association<P> right) {

			if (left == null) {
				throw new IllegalArgumentException("Left argument must not be null!");
			}

			if (right == null) {
				throw new IllegalArgumentException("Right argument must not be null!");
			}

			return delegate.compare(left.getInverse(), right.getInverse());
		}
	}
}
