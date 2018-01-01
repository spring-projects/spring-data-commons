/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.PropertyMatches;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Exception being thrown when creating {@link PropertyPath} instances.
 *
 * @author Oliver Gierke
 */
public class PropertyReferenceException extends RuntimeException {

	private static final long serialVersionUID = -5254424051438976570L;
	private static final String ERROR_TEMPLATE = "No property %s found for type %s!";
	private static final String HINTS_TEMPLATE = " Did you mean %s?";

	private final String propertyName;
	private final TypeInformation<?> type;
	private final List<PropertyPath> alreadyResolvedPath;
	private final Set<String> propertyMatches;

	/**
	 * Creates a new {@link PropertyReferenceException}.
	 *
	 * @param propertyName the name of the property not found on the given type, must not be {@literal null} or empty.
	 * @param type the type the property could not be found on, must not be {@literal null}.
	 * @param alreadyResolvedPah the previously calculated {@link PropertyPath}s, must not be {@literal null}.
	 */
	public PropertyReferenceException(String propertyName, TypeInformation<?> type,
			List<PropertyPath> alreadyResolvedPah) {

		Assert.hasText(propertyName, "Property name must not be null!");
		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(alreadyResolvedPah, "Already resolved paths must not be null!");

		this.propertyName = propertyName;
		this.type = type;
		this.alreadyResolvedPath = alreadyResolvedPah;
		this.propertyMatches = detectPotentialMatches(propertyName, type.getType());
	}

	/**
	 * Returns the name of the property not found.
	 *
	 * @return will not be {@literal null} or empty.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Returns the type the property could not be found on.
	 *
	 * @return will never be {@literal null}.
	 */
	public TypeInformation<?> getType() {
		return type;
	}

	/**
	 * Returns the properties that the invalid property might have been meant to be referred to.
	 *
	 * @return will never be {@literal null}.
	 */
	Collection<String> getPropertyMatches() {
		return propertyMatches;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {

		StringBuilder builder = new StringBuilder(
				String.format(ERROR_TEMPLATE, propertyName, type.getType().getSimpleName()));

		if (!propertyMatches.isEmpty()) {
			String matches = StringUtils.collectionToDelimitedString(propertyMatches, ",", "'", "'");
			builder.append(String.format(HINTS_TEMPLATE, matches));
		}

		if (!alreadyResolvedPath.isEmpty()) {
			builder.append(" Traversed path: ");
			builder.append(alreadyResolvedPath.get(0).toString());
			builder.append(".");
		}

		return builder.toString();
	}

	/**
	 * Returns the {@link PropertyPath} which could be resolved so far.
	 *
	 * @return
	 */
	@Nullable
	public PropertyPath getBaseProperty() {
		return alreadyResolvedPath.isEmpty() ? null : alreadyResolvedPath.get(alreadyResolvedPath.size() - 1);
	}

	/**
	 * Returns whether the given {@link PropertyReferenceException} has a deeper resolution depth (i.e. a longer path of
	 * already resolved properties) than the current exception.
	 *
	 * @param exception must not be {@literal null}.
	 * @return
	 */
	public boolean hasDeeperResolutionDepthThan(PropertyReferenceException exception) {
		return this.alreadyResolvedPath.size() > exception.alreadyResolvedPath.size();
	}

	/**
	 * Detects all potential matches for the given property name and type.
	 *
	 * @param propertyName must not be {@literal null} or empty.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static Set<String> detectPotentialMatches(String propertyName, Class<?> type) {

		Set<String> result = new HashSet<>();
		result.addAll(Arrays.asList(PropertyMatches.forField(propertyName, type).getPossibleMatches()));
		result.addAll(Arrays.asList(PropertyMatches.forProperty(propertyName, type).getPossibleMatches()));

		return result;
	}
}
