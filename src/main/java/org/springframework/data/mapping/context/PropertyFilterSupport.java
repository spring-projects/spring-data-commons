/*
 * Copyright 2011-2021 the original author or authors.
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

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * This class is responsible for creating a List of {@link PropertyPath} entries that contains all reachable
 * properties (w/o circles).
 */
public class PropertyFilterSupport {

	public static List<PropertyPath> addPropertiesFrom(Class<?> returnType, Class<?> domainType,
													   ProjectionFactory projectionFactory,
													   Predicate<Class<?>> simpleTypePredicate,  // TODO SimpleTypeHolder or CustomConversions
													   MappingContext<?, ?> mappingContext) {

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnType);

		boolean openProjection = !projectionInformation.isClosed(); // if not closed projection, we would need everything from the entity
		boolean typeFromHierarchy = returnType.isAssignableFrom(domainType) || // hierarchy
				domainType.isAssignableFrom(returnType);  // interface domainType is interface

		if (openProjection || typeFromHierarchy) {
			// *Mark wants to do something with object checks here
			return Collections.emptyList();
		}
		// if ^^ false -> DTO / interface projection

		List<PropertyPath> propertyPaths = new ArrayList<>();
		for (PropertyDescriptor inputProperty : projectionInformation.getInputProperties()) {
			addPropertiesFrom(returnType, domainType, projectionFactory, simpleTypePredicate, propertyPaths, inputProperty.getName(), mappingContext);
		}
		return propertyPaths;
	}

	private static void addPropertiesFrom(Class<?> returnedType, Class<?> domainType, ProjectionFactory factory,
										  Predicate<Class<?>> simpleTypePredicate,
										  Collection<PropertyPath> filteredProperties, String inputProperty,
										  MappingContext<?, ?> mappingContext) {

		ProjectionInformation projectionInformation = factory.getProjectionInformation(returnedType);
		PropertyPath propertyPath;

		// If this is a closed projection we can assume that the return type (possible projection type) contains
		// only fields accessible with a property path.
		if (projectionInformation.isClosed()) {
			propertyPath = PropertyPath.from(inputProperty, returnedType);
		} else {
			// otherwise the domain type is used right from the start
			propertyPath = PropertyPath.from(inputProperty, domainType);
		}

		Class<?> propertyType = propertyPath.getLeafType();
		// 1. Simple types can be added directly
		// 2. Something that looks like an entity needs to get processed as such
		// 3. Embedded projection
		if (simpleTypePredicate.test(propertyType)) {
			filteredProperties.add(propertyPath);
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			// avoid recursion / cycles
			if (propertyType.equals(domainType)) {
				return;
			}
			processEntity(propertyPath, filteredProperties, simpleTypePredicate, mappingContext);

		} else {
			ProjectionInformation nestedProjectionInformation = factory.getProjectionInformation(propertyType);
			filteredProperties.add(propertyPath);
			// Closed projection should get handled as above (recursion)
			if (nestedProjectionInformation.isClosed()) {
				for (PropertyDescriptor nestedInputProperty : nestedProjectionInformation.getInputProperties()) {
					PropertyPath nestedPropertyPath = propertyPath.nested(nestedInputProperty.getName());
					filteredProperties.add(nestedPropertyPath);
					addPropertiesFrom(domainType, returnedType, factory, simpleTypePredicate, filteredProperties,
							nestedPropertyPath.toDotPath(), mappingContext);
				}
			} else {
				// an open projection at this place needs to get replaced with the matching (real) entity
				PropertyPath domainTypeBasedPropertyPath = PropertyPath.from(propertyPath.toDotPath(), domainType);
				processEntity(domainTypeBasedPropertyPath, filteredProperties, simpleTypePredicate, mappingContext);
			}
		}
	}

	private static void processEntity(PropertyPath propertyPath, Collection<PropertyPath> filteredProperties,
									  Predicate<Class<?>> ding,
									  MappingContext<?, ?> mappingContext) {

		PropertyPath leafProperty = propertyPath.getLeafProperty();
		TypeInformation<?> propertyParentType = leafProperty.getOwningType();
		String inputProperty = leafProperty.getSegment();

		PersistentEntity<?, ?> persistentEntity = mappingContext.getPersistentEntity(propertyParentType);
		PersistentProperty<?> persistentProperty = persistentEntity.getPersistentProperty(inputProperty);
		Class<?> propertyEntityType = persistentProperty.getActualType();

		// Use domain type as root type for the property path
		addPropertiesFromEntity(filteredProperties, propertyPath, ding, propertyEntityType, mappingContext, new HashSet<>());
	}

	private static void addPropertiesFromEntity(Collection<PropertyPath> filteredProperties, PropertyPath propertyPath,
												Predicate<Class<?>> ding,
												Class<?> propertyType, MappingContext<?, ?> mappingContext,
												Collection<PersistentEntity<?, ?>> processedEntities) {

		PersistentEntity<?, ?> persistentEntityFromProperty = mappingContext.getPersistentEntity(propertyType);
		// break the recursion / cycles
		if (hasProcessedEntity(persistentEntityFromProperty, processedEntities)) {
			return;
		}
		processedEntities.add(persistentEntityFromProperty);

		// save base/root entity/projection type to avoid recursion later
		Class<?> pathRootType = propertyPath.getOwningType().getType();
		if (mappingContext.hasPersistentEntityFor(pathRootType)) {
			processedEntities.add(mappingContext.getPersistentEntity(pathRootType));
		}

		takeAllPropertiesFromEntity(filteredProperties, ding, propertyPath, mappingContext, persistentEntityFromProperty, processedEntities);
	}

	private static boolean hasProcessedEntity(PersistentEntity<?, ?> persistentEntityFromProperty,
											  Collection<PersistentEntity<?, ?>> processedEntities) {

		return processedEntities.contains(persistentEntityFromProperty);
	}

	private static void takeAllPropertiesFromEntity(Collection<PropertyPath> filteredProperties,
													Predicate<Class<?>> ding, PropertyPath propertyPath,
													MappingContext<?, ?> mappingContext,
													PersistentEntity<?, ?> persistentEntityFromProperty,
													Collection<PersistentEntity<?, ?>> processedEntities) {

		filteredProperties.add(propertyPath);

		persistentEntityFromProperty.doWithAll(persistentProperty -> {
			addPropertiesFromEntity(filteredProperties, propertyPath.nested(persistentProperty.getName()), ding, mappingContext, processedEntities);
		});
	}

	private static void addPropertiesFromEntity(Collection<PropertyPath> filteredProperties, PropertyPath propertyPath,
												Predicate<Class<?>> ding, MappingContext<?, ?> mappingContext,
										  		Collection<PersistentEntity<?, ?>> processedEntities) {

		// break the recursion / cycles
		if (filteredProperties.contains(propertyPath)) {
			return;
		}
		Class<?> propertyType = propertyPath.getLeafType();
		// simple types can get added directly to the list.
		if (ding.test(propertyType)) {
			filteredProperties.add(propertyPath);
		// Other types are handled also as entities because there cannot be any nested projection within a real entity.
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			addPropertiesFromEntity(filteredProperties, propertyPath, ding, propertyType, mappingContext, processedEntities);
		}
	}
}
