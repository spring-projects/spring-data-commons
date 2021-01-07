/*
 * Copyright 2015-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.PropertyMatches;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Exception to indicate a source path couldn't be resolved into a {@link PersistentPropertyPath} completely.
 *
 * @author Oliver Gierke
 * @soundtrack John Mayer - Slow Dancing In A Burning Room (Acoustic, The Village Sessions)
 */
public class InvalidPersistentPropertyPath extends MappingException {

	private static final long serialVersionUID = 2805815643641094488L;
	private static final String DEFAULT_MESSAGE = "No property '%s' found on %s! Did you mean: %s?";

	private final String source;
	private final String unresolvableSegment;
	private final String resolvedPath;
	private final TypeInformation<?> type;

	/**
	 * Creates a new {@link InvalidPersistentPropertyPath} for the given resolved path and message.
	 *
	 * @param source must not be {@literal null}.
	 * @param unresolvableSegment must not be {@literal null} or empty.
	 * @param resolvedPath
	 * @param message must not be {@literal null} or empty.
	 */
	public InvalidPersistentPropertyPath(String source, TypeInformation<?> type, String unresolvableSegment,
			PersistentPropertyPath<? extends PersistentProperty<?>> resolvedPath) {

		super(createMessage(resolvedPath.isEmpty() ? type : resolvedPath.getRequiredLeafProperty().getTypeInformation(),
				unresolvableSegment));

		Assert.notNull(source, "Source property path must not be null!");
		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(unresolvableSegment, "Unresolvable segment must not be null!");

		this.source = source;
		this.type = type;
		this.unresolvableSegment = unresolvableSegment;
		this.resolvedPath = toDotPathOrEmpty(resolvedPath);
	}

	/**
	 * Returns the source property path.
	 *
	 * @return the source will never be {@literal null}.
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Returns the type the source property path was attempted to be resolved on.
	 *
	 * @return the type will never be {@literal null}.
	 */
	public TypeInformation<?> getType() {
		return type;
	}

	/**
	 * Returns the segment of the source property path that could not be resolved.
	 *
	 * @return the unresolvableSegment
	 */
	public String getUnresolvableSegment() {
		return unresolvableSegment;
	}

	/**
	 * Returns the part of the source path until which the source property path could be resolved.
	 *
	 * @return the resolvedPath
	 */
	public String getResolvedPath() {
		return resolvedPath;
	}

	private static String toDotPathOrEmpty(@Nullable PersistentPropertyPath<? extends PersistentProperty<?>> path) {

		if (path == null) {
			return "";
		}

		String dotPath = path.toDotPath();

		return dotPath == null ? "" : dotPath;
	}

	private static String createMessage(TypeInformation<?> type, String unresolvableSegment) {

		Set<String> potentialMatches = detectPotentialMatches(unresolvableSegment, type.getType());
		Object match = StringUtils.collectionToCommaDelimitedString(potentialMatches);

		return String.format(DEFAULT_MESSAGE, unresolvableSegment, type.getType(), match);
	}

	private static Set<String> detectPotentialMatches(String propertyName, Class<?> type) {

		Set<String> result = new HashSet<>();
		result.addAll(Arrays.asList(PropertyMatches.forField(propertyName, type).getPossibleMatches()));
		result.addAll(Arrays.asList(PropertyMatches.forProperty(propertyName, type).getPossibleMatches()));

		return result;
	}
}
