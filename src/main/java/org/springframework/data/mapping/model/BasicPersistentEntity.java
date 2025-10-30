/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.Persistable;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.mapping.*;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.support.PersistableIsNewStrategy;
import org.springframework.data.util.Lazy;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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
 * @author Johannes Englmeier
 */
public class BasicPersistentEntity<T, P extends PersistentProperty<P>>
		implements MutablePersistentEntity<T, P>, EnvironmentCapable {

	private static final String TYPE_MISMATCH = "Target bean of type %s is not of type of the persistent entity (%s)";

	private final @Nullable InstanceCreatorMetadata<P> creator;
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
	private EvaluationContextProvider evaluationContextProvider = EvaluationContextProvider.DEFAULT;
	private @Nullable Environment environment = null;

	private final Lazy<Alias> typeAlias;
	private final Lazy<IsNewStrategy> isNewStrategy;
	private final Lazy<Boolean> isImmutable;
	private final Lazy<Boolean> requiresPropertyPopulation;

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

		Assert.notNull(information, "Information must not be null");

		this.information = information;
		this.properties = new ArrayList<>();
		this.persistentPropertiesCache = new ArrayList<>();
		this.comparator = comparator;
		this.creator = InstanceCreatorMetadataDiscoverer.discover(this);
		this.associations = comparator == null ? new HashSet<>() : new TreeSet<>(new AssociationComparator<>(comparator));

		this.propertyCache = new HashMap<>(16, 1.0f);
		this.annotationCache = new ConcurrentHashMap<>(16);
		this.propertyAnnotationCache = CollectionUtils.toMultiValueMap(new ConcurrentHashMap<>(16));
		this.propertyAccessorFactory = BeanWrapperPropertyAccessorFactory.INSTANCE;
		this.typeAlias = Lazy.of(() -> getAliasFromAnnotation(getType()));
		this.isNewStrategy = Lazy.of(() -> Persistable.class.isAssignableFrom(information.getType()) //
				? PersistableIsNewStrategy.INSTANCE
				: getFallbackIsNewStrategy());

		this.isImmutable = Lazy.of(() -> isAnnotationPresent(Immutable.class));
		this.requiresPropertyPopulation = Lazy.of(() -> !isImmutable() && properties.stream() //
				.anyMatch(it -> !(isCreatorArgument(it) || it.isTransient())));
	}

	@Nullable
	@Override
	public InstanceCreatorMetadata<P> getInstanceCreatorMetadata() {
		return creator;
	}

	@Override
	public boolean isCreatorArgument(PersistentProperty<?> property) {
		return creator != null && creator.isCreatorParameter(property);
	}

	@Override
	public boolean isIdProperty(PersistentProperty<?> property) {
		return idProperty != null && idProperty.equals(property);
	}

	@Override
	public boolean isVersionProperty(PersistentProperty<?> property) {
		return versionProperty != null && versionProperty.equals(property);
	}

	@Override
	public String getName() {
		return getType().getName();
	}

	@Override
	@Nullable
	public P getIdProperty() {
		return idProperty;
	}

	@Override
	@Nullable
	public P getVersionProperty() {
		return versionProperty;
	}

	@Override
	public boolean hasIdProperty() {
		return idProperty != null;
	}

	@Override
	public boolean hasVersionProperty() {
		return versionProperty != null;
	}

	@Override
	public void addPersistentProperty(P property) {

		Assert.notNull(property, "Property must not be null");

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
						String.format("Attempt to add version property %s but already have property %s registered "
								+ "as version; Check your mapping configuration", property.getField(), versionProperty.getField()));
			}

			this.versionProperty = property;
		}
	}

	@Override
	public void setEvaluationContextProvider(EvaluationContextProvider provider) {
		this.evaluationContextProvider = provider;
	}

	/**
	 * @param environment the {@code Environment} that this component runs in.
	 * @since 3.3
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {

		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

		return this.environment;
	}

	/**
	 * Returns the given property if it is a better candidate for the id property than the current id property.
	 *
	 * @param property the new id property candidate, will never be {@literal null}.
	 * @return the given id property or {@literal null} if the given property is not an id property.
	 */
	protected @Nullable P returnPropertyIfBetterIdPropertyCandidateOrNull(P property) {

		if (!property.isIdProperty()) {
			return null;
		}

		P idProperty = this.idProperty;

		if (idProperty != null) {
			throw new MappingException(String.format("Attempt to add id property %s but already have property %s registered "
					+ "as id; Check your mapping configuration ", property.getField(), idProperty.getField()));
		}

		return property;
	}

	@Override
	public void addAssociation(Association<P> association) {

		Assert.notNull(association, "Association must not be null");

		associations.add(association);
	}

	@Override
	@Nullable
	public P getPersistentProperty(String name) {
		return propertyCache.get(name);
	}

	@Override
	public Iterable<P> getPersistentProperties(Class<? extends Annotation> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null");
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

	@Override
	public Class<T> getType() {
		return information.getType();
	}

	@Override
	public Alias getTypeAlias() {
		return typeAlias.get();
	}

	@Override
	public TypeInformation<T> getTypeInformation() {
		return information;
	}

	@Override
	public void doWithProperties(PropertyHandler<P> handler) {

		Assert.notNull(handler, "PropertyHandler must not be null");

		for (P property : persistentPropertiesCache) {
			handler.doWithPersistentProperty(property);
		}
	}

	@Override
	public void doWithProperties(SimplePropertyHandler handler) {

		Assert.notNull(handler, "Handler must not be null");

		for (PersistentProperty<?> property : persistentPropertiesCache) {
			handler.doWithPersistentProperty(property);
		}
	}

	@Override
	public void doWithAssociations(AssociationHandler<P> handler) {

		Assert.notNull(handler, "Handler must not be null");

		for (Association<P> association : associations) {
			handler.doWithAssociation(association);
		}
	}

	@Override
	public void doWithAssociations(SimpleAssociationHandler handler) {

		Assert.notNull(handler, "Handler must not be null");

		for (Association<? extends PersistentProperty<?>> association : associations) {
			handler.doWithAssociation(association);
		}
	}

	@Nullable
	@Override
	public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
		return doFindAnnotation(annotationType).orElse(null);
	}

	@Override
	public <A extends Annotation> boolean isAnnotationPresent(Class<A> annotationType) {
		return doFindAnnotation(annotationType).isPresent();
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> Optional<A> doFindAnnotation(Class<A> annotationType) {

		return (Optional<A>) annotationCache.computeIfAbsent(annotationType,
				it -> Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(getType(), it)));
	}

	@Override
	public void verify() {

		if (comparator != null) {
			properties.sort(comparator);
			persistentPropertiesCache.sort(comparator);
		}
	}

	@Override
	public void setPersistentPropertyAccessorFactory(PersistentPropertyAccessorFactory factory) {
		this.propertyAccessorFactory = factory;
	}

	@Override
	public <B> PersistentPropertyAccessor<B> getPropertyAccessor(B bean) {

		verifyBeanType(bean);

		return propertyAccessorFactory.getPropertyAccessor(this, bean);
	}

	@Override
	public <B> PersistentPropertyPathAccessor<B> getPropertyPathAccessor(B bean) {
		return new SimplePersistentPropertyPathAccessor<>(getPropertyAccessor(bean));
	}

	@Override
	public IdentifierAccessor getIdentifierAccessor(Object bean) {

		verifyBeanType(bean);

		if (Persistable.class.isAssignableFrom(getType())) {
			return new PersistableIdentifierAccessor((Persistable<?>) bean);
		}

		return hasIdProperty() ? new IdPropertyIdentifierAccessor(this, bean) : new AbsentIdentifierAccessor(bean);
	}

	@Override
	public boolean isNew(Object bean) {

		verifyBeanType(bean);

		return isNewStrategy.get().isNew(bean);
	}

	@Override
	public boolean isImmutable() {
		return isImmutable.get();
	}

	@Override
	public boolean requiresPropertyPopulation() {
		return requiresPropertyPopulation.get();
	}

	@Override
	public Iterator<P> iterator() {

		Iterator<P> iterator = properties.iterator();

		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public P next() {
				return iterator.next();
			}
		};
	}

	/**
	 * Obtain a {@link EvaluationContext} for a {@code rootObject}.
	 *
	 * @param rootObject must not be {@literal null}.
	 * @return the evaluation context including all potential extensions.
	 * @since 2.1
	 */
	protected EvaluationContext getEvaluationContext(@Nullable Object rootObject) {
		return evaluationContextProvider.getEvaluationContext(rootObject);
	}

	/**
	 * Obtain a {@link EvaluationContext} for a {@code rootObject} given {@link ExpressionDependencies}.
	 *
	 * @param rootObject must not be {@literal null}.
	 * @param dependencies must not be {@literal null}.
	 * @return the evaluation context with extensions loaded that satisfy {@link ExpressionDependencies}.
	 * @since 2.5
	 */
	protected EvaluationContext getEvaluationContext(@Nullable Object rootObject, ExpressionDependencies dependencies) {
		return evaluationContextProvider.getEvaluationContext(rootObject, dependencies);
	}

	/**
	 * Obtain a {@link ValueEvaluationContext} for a {@code rootObject}.
	 *
	 * @param rootObject must not be {@literal null}.
	 * @return the evaluation context including all potential extensions.
	 * @since 3.3
	 */
	protected ValueEvaluationContext getValueEvaluationContext(@Nullable Object rootObject) {
		return ValueEvaluationContext.of(getEnvironment(), getEvaluationContext(rootObject));
	}

	/**
	 * Obtain a {@link ValueEvaluationContext} for a {@code rootObject} given {@link ExpressionDependencies}.
	 *
	 * @param rootObject must not be {@literal null}.
	 * @param dependencies must not be {@literal null}.
	 * @return the evaluation context with extensions loaded that satisfy {@link ExpressionDependencies}.
	 * @since 3.3
	 */
	protected ValueEvaluationContext getValueEvaluationContext(@Nullable Object rootObject,
			ExpressionDependencies dependencies) {
		return ValueEvaluationContext.of(getEnvironment(), getEvaluationContext(rootObject, dependencies));
	}

	/**
	 * Returns the default {@link IsNewStrategy} to be used. Will be a {@link PersistentEntityIsNewStrategy} by default.
	 * Note, that this strategy only gets used if the entity doesn't implement {@link Persistable} as this indicates the
	 * user wants to be in control over whether an entity is new or not.
	 *
	 * @return
	 * @since 2.1
	 */
	protected IsNewStrategy getFallbackIsNewStrategy() {
		return PersistentEntityIsNewStrategy.of(this);
	}

	/**
	 * Verifies the given bean type to no be {@literal null} and of the type of the current {@link PersistentEntity}.
	 *
	 * @param bean must not be {@literal null}.
	 */
	private void verifyBeanType(Object bean) {

		Assert.notNull(bean, "Target bean must not be null");
		Assert.isInstanceOf(getType(), bean,
				() -> String.format(TYPE_MISMATCH, bean.getClass().getName(), getType().getName()));
	}

	/**
	 * Calculates the {@link Alias} to be used for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static Alias getAliasFromAnnotation(Class<?> type) {

		TypeAlias typeAlias = AnnotatedElementUtils.findMergedAnnotation(type, TypeAlias.class);

		if (typeAlias != null && StringUtils.hasText(typeAlias.value())) {
			return Alias.of(typeAlias.value());
		}

		return Alias.empty();
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
	private record AssociationComparator<P extends PersistentProperty<P>>(
			Comparator<P> delegate) implements Comparator<Association<P>>, Serializable {

		@Override
		public int compare(@Nullable Association<P> left, @Nullable Association<P> right) {

			if (left == null) {
				throw new IllegalArgumentException("Left argument must not be null");
			}

			if (right == null) {
				throw new IllegalArgumentException("Right argument must not be null");
			}

			return delegate.compare(left.getInverse(), right.getInverse());
		}
	}
}
