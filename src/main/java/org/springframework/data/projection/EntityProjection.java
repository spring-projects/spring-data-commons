/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.projection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * Descriptor for a top-level mapped type representing a view onto a domain type structure. The view may exactly match
 * the domain type or be a DTO/interface {@link #isProjection() projection}.
 *
 * @param <M> the mapped type acting as view onto the domain type.
 * @param <D> the domain type.
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Oliver Drotbohm
 * @since 2.7
 */
public class EntityProjection<M, D> implements Streamable<EntityProjection.PropertyProjection<?, ?>> {

	private final TypeInformation<M> mappedType;
	private final TypeInformation<D> domainType;
	private final List<PropertyProjection<?, ?>> properties;
	private final boolean projection;
	private final ProjectionType projectionType;

	EntityProjection(TypeInformation<M> mappedType, TypeInformation<D> domainType,
			List<PropertyProjection<?, ?>> properties, boolean projection, ProjectionType projectionType) {
		this.mappedType = mappedType;
		this.domainType = domainType;
		this.properties = new ArrayList<>(properties);
		this.projection = projection;
		this.projectionType = projectionType;
	}

	/**
	 * Create a projecting variant of a mapped type.
	 *
	 * @param mappedType the target projection type. Must not be {@literal null}.
	 * @param domainType the source domain type. Must not be {@literal null}.
	 * @param properties properties to include.
	 * @param projectionType must not be {@literal null}.
	 * @return new instance of {@link EntityProjection}.
	 */
	public static <M, D> EntityProjection<M, D> projecting(TypeInformation<M> mappedType, TypeInformation<D> domainType,
			List<PropertyProjection<?, ?>> properties, ProjectionType projectionType) {
		return new EntityProjection<>(mappedType, domainType, properties, true, projectionType);
	}

	/**
	 * Create a non-projecting variant of a mapped type.
	 *
	 * @param mappedType the target projection type. Must not be {@literal null}.
	 * @param domainType the source domain type. Must not be {@literal null}.
	 * @param properties properties to include.
	 * @return new instance of {@link EntityProjection}.
	 */
	public static <M, D> EntityProjection<M, D> nonProjecting(TypeInformation<M> mappedType,
			TypeInformation<D> domainType, List<PropertyProjection<?, ?>> properties) {
		return new EntityProjection<>(mappedType, domainType, properties, false, ProjectionType.CLOSED);
	}

	/**
	 * Create a non-projecting variant of a {@code type}.
	 *
	 * @param type must not be {@literal null}.
	 * @return new instance of {@link EntityProjection}.
	 */
	public static <T> EntityProjection<T, T> nonProjecting(Class<T> type) {

		TypeInformation<T> typeInformation = TypeInformation.of(type);

		return new EntityProjection<>(typeInformation, typeInformation, Collections.emptyList(), false,
				ProjectionType.CLOSED);
	}

	/**
	 * Performs the given action for each element of the {@link Streamable} recursively until all elements of the graph
	 * have been processed or the action throws an {@link Exception}. Unless otherwise specified by the implementing
	 * class, actions are performed in the order of iteration (if an iteration order is specified). Exceptions thrown by
	 * the action are relayed to the caller.
	 *
	 * @param action must not be {@literal null}.
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
	 * @throws IllegalStateException if the actual type cannot be resolved.
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
	 * @throws IllegalStateException if the actual type cannot be resolved.
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
		return isProjection()
				&& (ProjectionType.CLOSED.equals(projectionType) || ProjectionType.DTO.equals(projectionType));
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
				List<PropertyProjection<?, ?>> properties, boolean projecting, ProjectionType projectionType) {
			super(mappedType, domainType, properties, projecting, projectionType);
			this.propertyPath = propertyPath;
		}

		/**
		 * Create a projecting variant of a mapped type.
		 *
		 * @param propertyPath the {@link PropertyPath path} to the actual property.
		 * @param mappedType the target projection type. Must not be {@literal null}.
		 * @param domainType the source domain type. Must not be {@literal null}.
		 * @param properties properties to include.
		 * @param projectionType must not be {@literal null}.
		 * @return new instance of {@link PropertyProjection}.
		 */
		public static <M, D> PropertyProjection<M, D> projecting(PropertyPath propertyPath, TypeInformation<M> mappedType,
				TypeInformation<D> domainType, List<PropertyProjection<?, ?>> properties, ProjectionType projectionType) {
			return new PropertyProjection<>(propertyPath, mappedType, domainType, properties, true, projectionType);
		}

		/**
		 * Create a non-projecting variant of a mapped type.
		 *
		 * @param propertyPath the {@link PropertyPath path} to the actual property.
		 * @param mappedType the target projection type. Must not be {@literal null}.
		 * @param domainType the source domain type. Must not be {@literal null}.
		 * @return
		 */
		public static <M, D> PropertyProjection<M, D> nonProjecting(PropertyPath propertyPath,
				TypeInformation<M> mappedType, TypeInformation<D> domainType) {
			return new PropertyProjection<>(propertyPath, mappedType, domainType, Collections.emptyList(), false,
					ProjectionType.OPEN);
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
				List<PropertyProjection<?, ?>> properties, boolean projecting, ProjectionType projectionType) {
			super(propertyPath, mappedType, domainType, properties, projecting, projectionType);
		}

		/**
		 * Create a projecting variant of a mapped type.
		 *
		 * @param propertyPath the {@link PropertyPath path} to the actual property.
		 * @param mappedType the target projection type. Must not be {@literal null}.
		 * @param domainType the source domain type. Must not be {@literal null}.
		 * @param properties properties to include.
		 * @param projectionType must not be {@literal null}.
		 * @return new instance of {@link ContainerPropertyProjection}.
		 */
		public static <M, D> ContainerPropertyProjection<M, D> projecting(PropertyPath propertyPath,
				TypeInformation<M> mappedType, TypeInformation<D> domainType, List<PropertyProjection<?, ?>> properties,
				ProjectionType projectionType) {
			return new ContainerPropertyProjection<>(propertyPath, mappedType, domainType, properties, true, projectionType);
		}

		/**
		 * Create a non-projecting variant of a mapped type.
		 *
		 * @param propertyPath the {@link PropertyPath path} to the actual property.
		 * @param mappedType the target projection type. Must not be {@literal null}.
		 * @param domainType the source domain type. Must not be {@literal null}.
		 * @return new instance of {@link ContainerPropertyProjection}.
		 */
		public static <M, D> ContainerPropertyProjection<M, D> nonProjecting(PropertyPath propertyPath,
				TypeInformation<M> mappedType, TypeInformation<D> domainType) {
			return new ContainerPropertyProjection<>(propertyPath, mappedType, domainType, Collections.emptyList(), false,
					ProjectionType.OPEN);
		}

	}

	/**
	 * Projection type.
	 *
	 * @since 2.7
	 */
	public enum ProjectionType {

		/**
		 * A DTO projection defines a value type that hold properties for the fields that are supposed to be retrieved.
		 */
		DTO,

		/**
		 * An open projection has accessor methods in the interface that can be used to compute new values by using the
		 * {@link org.springframework.beans.factory.annotation.Value} annotation.
		 */
		OPEN,

		/**
		 * A closed projection only contains accessor methods that all match properties of the target aggregate.
		 */
		CLOSED;

		/**
		 * Obtain the {@link ProjectionType} from a given {@link ProjectionInformation}.
		 *
		 * @param information must not be {@literal null}.
		 * @return the {@link ProjectionType} according to {@link ProjectionInformation#getType() type} and
		 *         {@link ProjectionInformation#isClosed()}.
		 */
		public static ProjectionType from(ProjectionInformation information) {

			if (!information.getType().isInterface()) {
				return DTO;
			}

			return information.isClosed() ? CLOSED : OPEN;
		}
	}
}
