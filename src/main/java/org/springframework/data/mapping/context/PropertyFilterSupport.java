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

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * This class is responsible for creating a List of {@link PropertyPath} entries that contains all reachable
 * properties (w/o circles).
 */
public class PropertyFilterSupport {

	public static Map<PropertyPath, Boolean> addPropertiesFrom(Class<?> returnType, Class<?> domainType,
															   ProjectionFactory projectionFactory,
															   Predicate<Class<?>> simpleTypePredicate,  // TODO SimpleTypeHolder or CustomConversions
															   MappingContext<?, ?> mappingContext) {

		ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnType);

		boolean openProjection = !projectionInformation.isClosed(); // if not closed projection, we would need everything from the entity
		boolean typeFromHierarchy = returnType.isAssignableFrom(domainType) || // hierarchy
				domainType.isAssignableFrom(returnType);  // interface domainType is interface

		if (openProjection || typeFromHierarchy) {
			// *Mark wants to do something with object checks here
			return Collections.emptyMap();
		}
		// if ^^ false -> DTO / interface projection

		Map<PropertyPath, Boolean> propertyPaths = new ConcurrentHashMap<>();
		for (PropertyDescriptor inputProperty : projectionInformation.getInputProperties()) {
			addPropertiesFrom(returnType, domainType, projectionFactory, simpleTypePredicate, propertyPaths, inputProperty.getName(), mappingContext);
		}
		return propertyPaths;
	}

	private static void addPropertiesFrom(Class<?> returnedType, Class<?> domainType, ProjectionFactory factory,
										  Predicate<Class<?>> simpleTypePredicate,
										  Map<PropertyPath, Boolean> filteredProperties, String inputProperty,
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
			filteredProperties.put(propertyPath, false);
		} else if (mappingContext.hasPersistentEntityFor(propertyType)) {
			filteredProperties.put(propertyPath, true);
		} else {
			ProjectionInformation nestedProjectionInformation = factory.getProjectionInformation(propertyType);
			// Closed projection should get handled as above (recursion)
			if (nestedProjectionInformation.isClosed()) {
				filteredProperties.put(propertyPath, false);
				for (PropertyDescriptor nestedInputProperty : nestedProjectionInformation.getInputProperties()) {
					PropertyPath nestedPropertyPath = propertyPath.nested(nestedInputProperty.getName());

					if (propertyPath.hasNext() && (domainType.equals(propertyPath.getLeafProperty().getOwningType().getType())
							|| returnedType.equals(propertyPath.getLeafProperty().getOwningType().getType()))) {
						break;
					}
					addPropertiesFrom(domainType, returnedType, factory, simpleTypePredicate, filteredProperties,
							nestedPropertyPath.toDotPath(), mappingContext);
				}
			} else {
				// an open projection at this place needs to get replaced with the matching (real) entity
				PropertyPath domainTypeBasedPropertyPath = PropertyPath.from(propertyPath.toDotPath(), domainType);
				filteredProperties.put(domainTypeBasedPropertyPath, true);
			}
		}
	}

}
