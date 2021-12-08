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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * Descriptor for a top-level mapped type representing a view onto a domain type structure. The view may exactly match
 * the domain type or be a DTO/interface {@link #isProjection() projection}.
 *
 * @param <M> the mapped type acting as view onto the domain type.
 * @param <D> the domain type.
 * @since 2.7
 */
public class EntityProjection<M, D> implements Streamable<EntityProjection.PropertyProjection<?, ?>> {

	private final TypeInformation<M> mappedType;
	private final TypeInformation<D> domainType;
	private final List<PropertyProjection<?, ?>> properties;
	private final boolean projection;
	private final boolean closedProjection;

	EntityProjection(TypeInformation<M> mappedType, TypeInformation<D> domainType,
			List<PropertyProjection<?, ?>> properties, boolean projection, boolean closedProjection) {
		this.mappedType = mappedType;
		this.domainType = domainType;
		this.properties = properties;
		this.projection = projection;
		this.closedProjection = closedProjection;
	}

	/**
	 * Create a projecting variant of a mapped type.
	 *
	 * @param mappedType
	 * @param domainType
	 * @param properties
	 * @return
	 */
	public static <M, D> EntityProjection<M, D> projecting(TypeInformation<M> mappedType, TypeInformation<D> domainType,
			List<PropertyProjection<?, ?>> properties, boolean closedProjection) {
		return new EntityProjection<>(mappedType, domainType, properties, true, closedProjection);
	}

	/**
	 * Create a non-projecting variant of a mapped type.
	 *
	 * @param mappedType
	 * @param domainType
	 * @param properties
	 * @return
	 */
	public static <M, D> EntityProjection<M, D> nonProjecting(TypeInformation<M> mappedType,
			TypeInformation<D> domainType, List<PropertyProjection<?, ?>> properties) {
		return new EntityProjection<>(mappedType, domainType, properties, false, false);
	}

	/**
	 * Create a non-projecting variant of a {@code type}.
	 *
	 * @param type
	 * @return
	 */
	public static <T> EntityProjection<T, T> nonProjecting(Class<T> type) {
		ClassTypeInformation<T> typeInformation = ClassTypeInformation.from(type);
		return new EntityProjection<>(typeInformation, typeInformation, Collections.emptyList(), false, false);
	}

	/**
	 * Performs the given action for each element of the {@link Streamable} recursively until all elements of the graph
	 * have been processed or the action throws an exception. Unless otherwise specified by the implementing class,
	 * actions are performed in the order of iteration (if an iteration order is specified). Exceptions thrown by the
	 * action are relayed to the caller.
	 *
	 * @param action
	 */
	public void forEachRecursive(Consumer<? super PropertyProjection<?, ?>> action) {

		for (PropertyProjection<?, ?> descriptor : properties) {

			if (descriptor instanceof ContainerPropertyProjection) {
				action.accept(descriptor);
				descriptor.forEachRecursive(action);
			} else if (descriptor.getProperties().isEmpty()) {
				action.accept(descriptor);
			} else {
				descriptor.forEachRecursive(action);
			}
		}
	}

	@Override
	public Iterator<PropertyProjection<?, ?>> iterator() {
		return properties.iterator();
	}

	/**
	 * @return the mapped type used by this type view.
	 */
	public TypeInformation<M> getMappedType() {
		return mappedType;
	}

	/**
	 * @return the actual mapped type used by this type view. Should be used for collection-like and map-like properties
	 *         to determine the actual view type.
	 */
	public TypeInformation<?> getActualMappedType() {
		return mappedType.getRequiredActualType();
	}

	/**
	 * @return the domain type represented by this type view.
	 */
	public TypeInformation<D> getDomainType() {
		return domainType;
	}

	/**
	 * @return the actual domain type represented by this type view. Should be used for collection-like and map-like
	 *         properties to determine the actual domain type.
	 */
	public TypeInformation<?> getActualDomainType() {
		return domainType.getRequiredActualType();
	}

	/**
	 * @return {@code true} if the {@link #getMappedType()} is a projection.
	 */
	public boolean isProjection() {
		return projection;
	}

	/**
	 * @return {@code true} if the {@link #getMappedType()} is a closed projection.
	 */
	public boolean isClosedProjection() {
		return isProjection() && closedProjection;
	}

	List<PropertyProjection<?, ?>> getProperties() {
		return properties;
	}

	/**
	 * Return a {@link EntityProjection} for a property identified by {@code name}.
	 *
	 * @param name the property name.
	 * @return the type view, if the property is known; {@code null} otherwise.
	 */
	@Nullable
	public EntityProjection<?, ?> findProperty(String name) {

		for (PropertyProjection<?, ?> descriptor : properties) {

			if (descriptor.propertyPath.getLeafProperty().getSegment().equals(name)) {
				return descriptor;
			}
		}

		return null;
	}

	@Override
	public String toString() {

		if (isProjection()) {
			return String.format("Projection(%s AS %s): %s", getActualDomainType().getType().getName(),
					getActualMappedType().getType().getName(), properties);
		}

		return String.format("Domain(%s): %s", getActualDomainType().getType().getName(), properties);
	}

	/**
	 * Descriptor for a property-level type along its potential projection.
	 *
	 * @param <M> the mapped type acting as view onto the domain type.
	 * @param <D> the domain type.
	 */
	public static class PropertyProjection<M, D> extends EntityProjection<M, D> {

		private final PropertyPath propertyPath;

		PropertyProjection(PropertyPath propertyPath, TypeInformation<M> mappedType, TypeInformation<D> domainType,
				List<PropertyProjection<?, ?>> properties, boolean projecting, boolean closedProjection) {
			super(mappedType, domainType, properties, projecting, closedProjection);
			this.propertyPath = propertyPath;
		}

		/**
		 * Create a projecting variant of a mapped type.
		 *
		 * @param propertyPath
		 * @param mappedType
		 * @param domainType
		 * @param properties
		 * @return
		 */
		public static <M, D> PropertyProjection<M, D> projecting(PropertyPath propertyPath, TypeInformation<M> mappedType,
				TypeInformation<D> domainType, List<PropertyProjection<?, ?>> properties, boolean closedProjection) {
			return new PropertyProjection<>(propertyPath, mappedType, domainType, properties, true, closedProjection);
		}

		/**
		 * Create a non-projecting variant of a mapped type.
		 *
		 * @param propertyPath
		 * @param mappedType
		 * @param domainType
		 * @return
		 */
		public static <M, D> PropertyProjection<M, D> nonProjecting(PropertyPath propertyPath,
				TypeInformation<M> mappedType, TypeInformation<D> domainType) {
			return new PropertyProjection<>(propertyPath, mappedType, domainType, Collections.emptyList(), false, false);
		}

		/**
		 * @return the property path representing this property within the root domain type.
		 */
		public PropertyPath getPropertyPath() {
			return propertyPath;
		}

		@Override
		public String toString() {
			return String.format("%s AS %s", propertyPath.toDotPath(), getActualMappedType().getType().getName());
		}

	}

	/**
	 * Descriptor for a property-level type along its potential projection that is held within a {@link Collection}-like
	 * or {@link Map}-like container. Property paths within containers use the deeply unwrapped actual type of the
	 * container as root type and as they cannot be tied immediately to the root entity.
	 *
	 * @param <M> the mapped type acting as view onto the domain type.
	 * @param <D> the domain type.
	 */
	public static class ContainerPropertyProjection<M, D> extends PropertyProjection<M, D> {

		ContainerPropertyProjection(PropertyPath propertyPath, TypeInformation<M> mappedType, TypeInformation<D> domainType,
				List<PropertyProjection<?, ?>> properties, boolean projecting, boolean closedProjection) {
			super(propertyPath, mappedType, domainType, properties, projecting, closedProjection);
		}

		/**
		 * Create a projecting variant of a mapped type.
		 *
		 * @param propertyPath
		 * @param mappedType
		 * @param domainType
		 * @param properties
		 * @return
		 */
		public static <M, D> ContainerPropertyProjection<M, D> projecting(PropertyPath propertyPath,
				TypeInformation<M> mappedType, TypeInformation<D> domainType, List<PropertyProjection<?, ?>> properties,
				boolean closedProjection) {
			return new ContainerPropertyProjection<>(propertyPath, mappedType, domainType, properties, true,
					closedProjection);
		}

		/**
		 * Create a non-projecting variant of a mapped type.
		 *
		 * @param propertyPath
		 * @param mappedType
		 * @param domainType
		 * @return
		 */
		public static <M, D> ContainerPropertyProjection<M, D> nonProjecting(PropertyPath propertyPath,
				TypeInformation<M> mappedType, TypeInformation<D> domainType) {
			return new ContainerPropertyProjection<>(propertyPath, mappedType, domainType, Collections.emptyList(), false,
					false);
		}

	}
}
