/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.mapping.context;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * This class is introspects the returned type in the context of a domain type for all reachable properties (w/o cycles)
 * to determine which property paths are subject to projection.
 *
 * @author Gerrit Meier
 * @author Mark Paluch
 * @since 2.7
 */
public class EntityProjectionDiscoverer {

	private final ProjectionFactory projectionFactory;
	private final ProjectionPredicate projectionPredicate;
	private final MappingContext<?, ?> mappingContext;

	private EntityProjectionDiscoverer(ProjectionFactory projectionFactory, ProjectionPredicate projectionPredicate,
			MappingContext<?, ?> mappingContext) {
		this.projectionFactory = projectionFactory;
		this.projectionPredicate = projectionPredicate;
		this.mappingContext = mappingContext;
	}

	/**
	 * Create a new {@link EntityProjectionDiscoverer} given {@link ProjectionFactory}, {@link ProjectionPredicate} and
	 * {@link MappingContext}.
	 *
	 * @param projectionFactory must not be {@literal null}.
	 * @param projectionPredicate must not be {@literal null}.
	 * @param mappingContext must not be {@literal null}.
	 * @return a new {@link EntityProjectionDiscoverer} instance.
	 */
	public static EntityProjectionDiscoverer create(ProjectionFactory projectionFactory,
			ProjectionPredicate projectionPredicate, MappingContext<?, ?> mappingContext) {

		Assert.notNull(projectionFactory, "ProjectionFactory must not be null");
		Assert.notNull(projectionPredicate, "ProjectionPredicate must not be null");
		Assert.notNull(mappingContext, "MappingContext must not be null");

		return new EntityProjectionDiscoverer(projectionFactory, projectionPredicate, mappingContext);
	}

	/**
	 * Introspect a {@link Class return type} in the context of a {@link Class domain type} whether the returned type is a
	 * projection and what property paths are participating in the projection.
	 * <p>
	 * Nested properties (direct types, within maps, collections) are introspected for nested projections and contain
	 * property paths for closed projections.
	 *
	 * @param returnType
	 * @param domainType
	 * @return
	 */
	public ReturnedTypeDescriptor introspectReturnType(Class<?> returnType, Class<?> domainType) {

		boolean isProjection = projectionPredicate.test(returnType, domainType);

		if (!isProjection) {
			return ReturnedTypeDescriptor.nonProjecting(returnType, domainType, Collections.emptyList());
		}

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnType);

		if (!projectionInformation.isClosed()) {
			return ReturnedTypeDescriptor.projecting(returnType, domainType, Collections.emptyList());
		}

		Set<Pair<Class<?>, Class<?>>> cycleGuard = new HashSet<>();

		PersistentEntity<?, ?> persistentEntity = mappingContext.getRequiredPersistentEntity(domainType);
		List<PropertyProjectionDescriptor> propertyDescriptors = getProperties(null, projectionInformation,
				persistentEntity, cycleGuard);

		return ReturnedTypeDescriptor.projecting(returnType, domainType, propertyDescriptors);
	}

	private List<PropertyProjectionDescriptor> getProperties(@Nullable PropertyPath propertyPath,
			ProjectionInformation projectionInformation, PersistentEntity<?, ?> persistentEntity,
			Set<Pair<Class<?>, Class<?>>> cycleGuard) {

		List<PropertyProjectionDescriptor> propertyDescriptors = new ArrayList<>();
		for (PropertyDescriptor inputProperty : projectionInformation.getInputProperties()) {

			PersistentProperty<?> persistentProperty = persistentEntity.getPersistentProperty(inputProperty.getName());

			if (persistentProperty == null) {
				continue;
			}

			Class<?> returnedType = inputProperty.getPropertyType();
			Class<?> domainType = persistentProperty.getActualType();

			PropertyPath nestedPropertyPath = propertyPath == null
					? PropertyPath.from(persistentProperty.getName(), persistentEntity.getTypeInformation())
					: propertyPath.nested(persistentProperty.getName());

			if (projectionPredicate.test(returnedType, domainType)) {

				List<PropertyProjectionDescriptor> nestedPropertyDescriptors;

				if (cycleGuard.add(Pair.of(returnedType, domainType))) {
					nestedPropertyDescriptors = getProjectedProperties(nestedPropertyPath, returnedType, domainType, cycleGuard);
				} else {
					nestedPropertyDescriptors = Collections.emptyList();
				}

				propertyDescriptors.add(PropertyProjectionDescriptor.projecting(nestedPropertyPath, returnedType, domainType,
						nestedPropertyDescriptors));
			} else {
				propertyDescriptors
						.add(PropertyProjectionDescriptor.nonProjecting(nestedPropertyPath, returnedType, domainType));
			}
		}

		return propertyDescriptors;
	}

	private List<PropertyProjectionDescriptor> getProjectedProperties(PropertyPath propertyPath, Class<?> returnedType,
			Class<?> domainType, Set<Pair<Class<?>, Class<?>>> cycleGuard) {

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnedType);
		PersistentEntity<?, ?> persistentEntity = mappingContext.getRequiredPersistentEntity(domainType);

		// Closed projection should get handled as above (recursion)
		return projectionInformation.isClosed()
				? getProperties(propertyPath, projectionInformation, persistentEntity, cycleGuard)
				: Collections.emptyList();
	}

	/**
	 * Descriptor for a top-level return type.
	 */
	public static class ReturnedTypeDescriptor {

		private final Class<?> returnedType;
		private final Class<?> domainType;
		private final List<PropertyProjectionDescriptor> nested;
		private final boolean projecting;

		ReturnedTypeDescriptor(Class<?> returnedType, Class<?> domainType, List<PropertyProjectionDescriptor> nested,
				boolean projecting) {
			this.domainType = domainType;
			this.returnedType = returnedType;
			this.nested = nested;
			this.projecting = projecting;
		}

		/**
		 * Create a projecting variant of a return type.
		 *
		 * @param returnedType
		 * @param domainType
		 * @param nested
		 * @return
		 */
		public static ReturnedTypeDescriptor projecting(Class<?> returnedType, Class<?> domainType,
				List<PropertyProjectionDescriptor> nested) {
			return new ReturnedTypeDescriptor(returnedType, domainType, nested, true);
		}

		/**
		 * Create a non-projecting variant of a return type.
		 *
		 * @param returnedType
		 * @param domainType
		 * @param nested
		 * @return
		 */
		public static ReturnedTypeDescriptor nonProjecting(Class<?> returnedType, Class<?> domainType,
				List<PropertyProjectionDescriptor> nested) {
			return new ReturnedTypeDescriptor(returnedType, domainType, nested, false);
		}

		public Class<?> getDomainType() {
			return domainType;
		}

		public Class<?> getReturnedType() {
			return returnedType;
		}

		public boolean isProjecting() {
			return projecting;
		}

		List<PropertyProjectionDescriptor> getNested() {
			return nested;
		}

		/**
		 * Perform the given {@code action} for each element of the {@code ReturnedTypeDescriptor} until all elements have
		 * been processed or the action throws an exception.
		 *
		 * @param action the action to be performed for each element
		 */
		public void forEach(Consumer<PropertyPath> action) {

			for (PropertyProjectionDescriptor descriptor : nested) {

				if (descriptor.getNested().isEmpty()) {
					action.accept(descriptor.getPropertyPath());
				} else {
					descriptor.forEach(action);
				}
			}
		}

		@Override
		public String toString() {

			if (isProjecting()) {
				return String.format("Projection(%s AS %s): %s", getDomainType().getName(), getReturnedType().getName(),
						nested);
			}

			return String.format("Domain(%s): %s", getReturnedType().getName(), nested);
		}
	}

	/**
	 * Descriptor for a property-level type along its potential projection.
	 */
	public static class PropertyProjectionDescriptor extends ReturnedTypeDescriptor {

		private final PropertyPath propertyPath;

		PropertyProjectionDescriptor(PropertyPath propertyPath, Class<?> returnedType, Class<?> domainType,
				List<PropertyProjectionDescriptor> nested, boolean projecting) {
			super(returnedType, domainType, nested, projecting);
			this.propertyPath = propertyPath;
		}

		/**
		 * Create a projecting variant of a return type.
		 *
		 * @param propertyPath
		 * @param returnedType
		 * @param domainType
		 * @param nested
		 * @return
		 */
		public static PropertyProjectionDescriptor projecting(PropertyPath propertyPath, Class<?> returnedType,
				Class<?> domainType, List<PropertyProjectionDescriptor> nested) {
			return new PropertyProjectionDescriptor(propertyPath, returnedType, domainType, nested, true);
		}

		/**
		 * Create a non-projecting variant of a return type.
		 *
		 * @param propertyPath
		 * @param returnedType
		 * @param domainType
		 * @return
		 */
		public static PropertyProjectionDescriptor nonProjecting(PropertyPath propertyPath, Class<?> returnedType,
				Class<?> domainType) {
			return new PropertyProjectionDescriptor(propertyPath, returnedType, domainType, Collections.emptyList(), false);
		}

		public PropertyPath getPropertyPath() {
			return propertyPath;
		}

		@Override
		public String toString() {
			return String.format("%s AS %s", propertyPath.toDotPath(), getReturnedType().getName());
		}
	}

	/**
	 * Represents a predicate (boolean-valued function) of a {@link Class target type} and its {@link Class underlying
	 * type}.
	 */
	public interface ProjectionPredicate {

		/**
		 * Evaluates this predicate on the given arguments.
		 *
		 * @param target the target type.
		 * @param target the underlying type.
		 * @return {@code true} if the input argument matches the predicate, otherwise {@code false}.
		 */
		boolean test(Class<?> target, Class<?> underlyingType);

		/**
		 * Return a composed predicate that represents a short-circuiting logical AND of this predicate and another. When
		 * evaluating the composed predicate, if this predicate is {@code false}, then the {@code other} predicate is not
		 * evaluated.
		 * <p>
		 * Any exceptions thrown during evaluation of either predicate are relayed to the caller; if evaluation of this
		 * predicate throws an exception, the {@code other} predicate will not be evaluated.
		 *
		 * @param other a predicate that will be logically-ANDed with this predicate
		 * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the
		 *         {@code other} predicate
		 */
		default ProjectionPredicate and(ProjectionPredicate other) {
			return (target, underlyingType) -> test(target, underlyingType) && other.test(target, underlyingType);
		}

		/**
		 * Return a predicate that represents the logical negation of this predicate.
		 *
		 * @return a predicate that represents the logical negation of this predicate
		 */
		default ProjectionPredicate negate() {
			return (target, underlyingType) -> !test(target, underlyingType);
		}

		/**
		 * Return a predicate that considers whether the {@code target type} is participating in the type hierarchy.
		 */
		static ProjectionPredicate typeHierarchy() {

			ProjectionPredicate predicate = (target, underlyingType) -> target.isAssignableFrom(underlyingType) || // hierarchy
					underlyingType.isAssignableFrom(target);
			return predicate.negate();
		}

	}

}
