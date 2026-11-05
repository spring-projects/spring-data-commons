/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.core;

import static org.springframework.data.core.PathResolutionException.abbreviate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.data.core.PathValidationRules.AccessValidationContext;
import org.springframework.data.core.PathValidationRules.AccessValidator;
import org.springframework.data.core.PathValidationRules.IndexValidator;
import org.springframework.data.core.PathValidationRules.MapKeyValidator;
import org.springframework.data.core.PathValidationRules.PathValidationContext;
import org.springframework.data.core.PathValidationRules.SegmentClassification;
import org.springframework.data.core.PathValidationRules.SegmentType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

/**
 * Default {@link PathValidationRules} implementation.
 *
 * @author Christoph Strobl
 * @since 4.2
 */
class PathValidationRuleDefaults {

	static final PathValidationRules INSTANCE = new PathValidationRules() {};

	/**
	 * Matches an index of an indexed structure.
	 */
	static final Pattern INDEX_LITERAL = Pattern.compile("\\d+");

	/**
	 * Matches a map key excluding quotes, backslashes, whitespace, dots, commas, braces, {@code $}, ...
	 */
	static final Pattern KEY_LITERAL = Pattern.compile("[\\w-]+", Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * Upper bound for the number of segments.
	 */
	static final int MAX_SEGMENTS = 100;

	/**
	 * Upper bound for the number of characters the entire path may consist of.
	 */
	static final int MAX_PATH_LENGTH = 1000;

	/**
	 * Leaves individual path elements unbounded.
	 */
	static final int MAX_SEGMENT_LENGTH = -1;

	static final SegmentClassification LENIENT = PathValidationRuleDefaults::keyOrIndexSegment;
	static final SegmentClassification STRICT = (segment, context) -> SegmentType.PROPERTY;

	static final AccessValidator NO_ACCESS_CHECK = (segment, context) -> {};
	static final IndexValidator NUMERIC_INDEX_VALIDATOR = PathValidationRuleDefaults::validateIndex;
	static final MapKeyValidator STRICT_KEY_VALIDATOR = PathValidationRuleDefaults::validateKey;

	private static SegmentType keyOrIndexSegment(String segment, PathValidationContext context) {

		TypeInformation<?> type = context.type();

		if (!type.isMap() && !type.isCollectionLike()) {
			return SegmentType.PROPERTY;
		}

		TypeInformation<?> actualType = type.getActualType();
		if (actualType != null && !actualType.equals(type) && actualType.getProperty(segment) != null) {
			return SegmentType.PROPERTY;
		}

		return SegmentType.INDEX;
	}

	/**
	 * Derives the overall length limit from the element limits, as a path cannot be longer than that many elements of
	 * that length. Saturates rather than overflowing into a negative limit.
	 *
	 * @param maxSegmentLength the maximum element length, a value smaller than one meaning elements are unbounded.
	 * @param maxAllowedSegments the maximum number of elements.
	 * @return the maximum path length.
	 */
	static int derivePathLength(int maxSegmentLength, int maxAllowedSegments) {

		if (maxSegmentLength < 1) {
			return MAX_PATH_LENGTH;
		}

		long derived = (long) maxSegmentLength * maxAllowedSegments;

		return derived > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) derived;
	}

	private static void validateKey(String key, @Nullable TypeInformation<?> keyType, PathValidationContext context) {

		Class<?> rawKeyType = keyType == null ? Object.class : keyType.getType();

		if (rawKeyType.isEnum()) {

			if (!isEnumConstant(rawKeyType, key)) {
				throw context.reject(
						String.format("Key '%s' is no constant of map key type '%s'", abbreviate(key), rawKeyType.getSimpleName()));
			}

			return;
		}

		if (!KEY_LITERAL.matcher(key).matches()) {
			throw context.reject(String.format("Key '%s' contains characters not allowed in a map key", abbreviate(key)));
		}

		if (Number.class.isAssignableFrom(rawKeyType) && !INDEX_LITERAL.matcher(key).matches()) {
			throw context.reject(
					String.format("Key '%s' is no value of map key type '%s'", abbreviate(key), rawKeyType.getSimpleName()));
		}
	}

	private static void validateIndex(String index, @Nullable TypeInformation<?> elementType,
			PathValidationContext context) {

		if (!INDEX_LITERAL.matcher(index).matches()) {
			throw context.reject(String.format("Index '%s' of property '%s' is not a number", abbreviate(index),
					abbreviate(context.property())));
		}
	}

	static boolean isEnumConstant(Class<?> enumType, String name) {

		for (Object constant : enumType.getEnumConstants()) {
			if (((Enum<?>) constant).name().equals(name)) {
				return true;
			}
		}

		return false;
	}

	static class PathAccessValidator implements AccessValidator {

		private final AntPathMatcher pathMatcher = new AntPathMatcher(".");
		private final Set<String> deniedPaths;
		private final List<String> deniedPatterns;

		public PathAccessValidator(Set<String> source) {

			Assert.notNull(source, "Denied paths must not be null");

			this.deniedPaths = new HashSet<>(source.size());
			this.deniedPatterns = new ArrayList<>(source.size());

			for (String deniedPath : source) {

				Assert.hasText(deniedPath, "Denied path must not be empty");

				if (deniedPath.indexOf('[') != -1 || deniedPath.indexOf(']') != -1) {
					throw new IllegalArgumentException(
							String.format("Denied path '%s' must not contain index or key markers", deniedPath));
				}

				if (pathMatcher.isPattern(deniedPath)) {
					deniedPatterns.add(deniedPath);
				} else {
					this.deniedPaths.add(deniedPath);
				}
			}
		}

		@Override
		public void validateAccess(String segment, AccessValidationContext context) {

			String navigablePath = context.navigablePath();

			if (deniedPaths.contains(navigablePath)) {
				throw context.reject(
						String.format("Access to property path '%s' is denied", PathResolutionException.abbreviate(navigablePath)));
			}

			for (String pattern : deniedPatterns) {
				if (pathMatcher.match(pattern, navigablePath)) {
					throw context.reject(String.format("Access to property path '%s' is denied",
							PathResolutionException.abbreviate(navigablePath)));
				}
			}
		}
	}
}
