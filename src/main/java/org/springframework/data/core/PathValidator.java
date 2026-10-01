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

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.core.PathValidationRules.AccessValidator;
import org.springframework.data.core.PathValidationRules.IndexValidator;
import org.springframework.data.core.PathValidationRules.MapKeyValidator;
import org.springframework.data.core.PathValidationRules.PathValidationContext;
import org.springframework.data.core.PathValidationRules.SegmentClassification;
import org.springframework.data.core.PathValidationRules.SegmentType;
import org.springframework.util.Assert;

/**
 * Validates property paths against the properties available on a given type considering a dedicated
 * {@link PathValidationRules ruleset}. Intended for paths originating from untrusted input such as web request
 * parameters.
 * <p>
 * Path segments are required to be separated by {@code "."} and each segment needs to ba a valid Java identifier.
 * Neither {@code "_"} nor camel case works as a path separator, so a segment containing {@code "_"} like
 * {@code address_city} is treated as a property named {@literal "address_city"}.
 * <p>
 * Map, collection, array and wrapper types are traversed transparently, so {@code addresses.city} addresses the
 * {@code city} property of the value type of a {@code Map<String, Address> addresses} just as it does for the element
 * type of a {@code List<Address> addresses}.
 * <p>
 * Properties whose type is erased to {@link Object}, for example an unbound type variable of a generic supertype,
 * expose no properties and therefore terminate a path.
 *
 * <pre class="code">
 * PathValidator.validate("address.city", Person.class);
 * </pre>
 *
 * @author Christoph Strobl
 * @since 4.2
 * @see PathResolutionException
 */
public final class PathValidator {

	private static final Pattern PROPERTY_SEGMENT = Pattern
			.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

	private PathValidator() {
		// can't touch this, oh-oh oh oh oh-oh-oh - break it down!
	}

	/**
	 * Validates the given property path against the navigable paths available from the given type.
	 *
	 * @param path the property path to validate, must not be {@literal null}.
	 * @param type the type to validate the path against, must not be {@literal null}.
	 * @throws PathResolutionException if the path is rejected.
	 */
	public static void validate(String path, Class<?> type) {
		validate(path, type, PathValidationRules.strict());
	}

	/**
	 * Validates the given property path against the navigable paths available from the given type, applying the given
	 * {@link PathValidationRules rules}.
	 *
	 * @param path the property path to validate, must not be {@literal null}.
	 * @param type the type to validate the path against, must not be {@literal null}.
	 * @param rules the rules to apply, must not be {@literal null}.
	 * @throws PathResolutionException if the path is rejected.
	 */
	public static void validate(String path, Class<?> type, PathValidationRules rules) {

		Assert.notNull(type, "Type must not be null");

		validate(path, TypeInformation.of(type), rules);
	}

	/**
	 * Validates the given property path against the properties available on the given type.
	 *
	 * @param path the property path to validate, must not be {@literal null}.
	 * @param type the type to validate the path against, must not be {@literal null}.
	 * @throws PathResolutionException if the path is malformed or does not refer to a property of {@code type}.
	 */
	public static void validate(String path, TypeInformation<?> type) {
		validate(path, type, PathValidationRules.strict());
	}

	/**
	 * Validates the given property path against the properties available on the given type, applying the given
	 * {@link PathValidationRules} to keys and indexes.
	 *
	 * @param path the property path to validate, must not be {@literal null}.
	 * @param type the type to validate the path against, must not be {@literal null}.
	 * @param rules the rules to apply to keys and indexes, must not be {@literal null}.
	 * @throws PathResolutionException if the path is malformed or does not refer to a property of {@code type}.
	 */
	public static void validate(String path, TypeInformation<?> type, PathValidationRules rules) {

		Assert.notNull(path, "Path must not be null");
		Assert.notNull(type, "Type must not be null");
		Assert.notNull(rules, "PathValidationRules must not be null");

		validateMaxLength(path, rules.maxPathLength());

		int maxDepth = rules.maxAllowedSegments();
		int maxSegmentLength = rules.maxSegmentLength();

		SegmentClassification segmentClassifier = rules.segmentClassifier();
		AccessValidator accessValidator = rules.accessValidator();
		MapKeyValidator mapKeyValidator = rules.mapKeyValidator();
		IndexValidator indexValidator = rules.indexValidator();

		TypeInformation<?> currentType = type;
		String owningSegment = "";
		String navigablePath = "";
		int position = 0;
		int depth = 0;

		while (true) {

			checkDepth(++depth, maxDepth, path);

			// scanning stops one character past the limit, so an oversized segment is neither read nor copied in full
			int scanLimit = maxSegmentLength > 0 && path.length() - position > maxSegmentLength
					? position + maxSegmentLength + 1
					: path.length();
			int end = position;

			while (end < scanLimit && path.charAt(end) != '.' && path.charAt(end) != '[') {
				end++;
			}

			String segment = path.substring(position, end);

			// the segment was deliberately not read in full, so it is reported by position rather than by value
			if (maxSegmentLength > 0 && segment.length() > maxSegmentLength) {
				throw new PathResolutionException(path,
						String.format("Property path '%s' is invalid; Segment at index %d must not be longer than %d characters",
								abbreviate(path), position, maxSegmentLength));
			}

			String segmentPath = navigablePath.isEmpty() ? segment : navigablePath + "." + segment;
			PathValidationContext context = new PathValidationContext(path, owningSegment.isEmpty() ? segment : owningSegment,
					currentType, position, segmentPath);

			TypeInformation<?> resolved;
			if (segmentClassifier.classify(segment, context) == SegmentType.INDEX) {

				// the segment addresses an entry of the current type rather than a property of its value type
				resolved = resolveIndex(segment,
						new PathValidationContext(path, context.property(), currentType, position, navigablePath), mapKeyValidator,
						indexValidator);
			} else {

				accessValidator.validateAccess(segment, context);

				if (!PROPERTY_SEGMENT.matcher(segment).matches()) {
					throw new PathResolutionException(path,
							String.format("Property path '%s' is invalid; Segment '%s' is not a valid Java identifier",
									abbreviate(path), abbreviate(segment)));
				}

				resolved = resolveProperty(navigable(currentType), segment, path);
				owningSegment = segment;
				navigablePath = segmentPath;
			}

			position = end;

			// indexes apply to the resolved type itself, hence they must not be unwrapped in between
			while (position < path.length() && path.charAt(position) == '[') {

				int close = path.indexOf(']', position);

				if (close == -1) {
					throw new PathResolutionException(path,
							String.format("Property path '%s' is invalid; Unbalanced '[' at index %d", abbreviate(path), position));
				}

				checkDepth(++depth, maxDepth, path);

				// checked before the literal is copied, so an oversized one is not materialized either
				if (maxSegmentLength > 0 && close - position - 1 > maxSegmentLength) {
					throw new PathResolutionException(path,
							String.format("Property path '%s' is invalid; Index at index %d must not be longer than %d characters",
									abbreviate(path), position, maxSegmentLength));
				}

				resolved = resolveIndex(path.substring(position + 1, close),
						new PathValidationContext(path, segment, resolved, position, navigablePath), mapKeyValidator,
						indexValidator);
				position = close + 1;
			}

			if (position == path.length()) {
				return;
			}

			if (path.charAt(position) != '.') {
				throw new PathResolutionException(path,
						String.format("Property path '%s' is invalid; Unexpected character '%s' at index %d", abbreviate(path),
								path.charAt(position), position));
			}

			// retained unwrapped so that the rules can tell a container apart from its value type
			currentType = resolved;
			position++;
		}
	}

	private static void validateMaxLength(String path, int maxLength) {
		if (path.length() > maxLength) {
			throw new PathResolutionException(path,
					String.format("Invalid path '%s'. Must not be longer than %d", abbreviate(path), maxLength));
		}
	}

	private static void checkDepth(int depth, int maxDepth, String path) {

		if (depth > maxDepth) {
			throw new PathResolutionException(path,
					String.format("Property path '%s' is invalid; Paths must not consist of more than %d segments",
							abbreviate(path), maxDepth));
		}
	}

	/**
	 * Resolves the given segment as a property of the given owning type.
	 *
	 * @param owningType the type to look the property up on.
	 * @param segment the property name.
	 * @param path the full property path, for error reporting.
	 * @return the type of the resolved property.
	 * @throws PathResolutionException if the owning type has no such property, wrapping the
	 *           {@link PropertyReferenceException} describing the failure.
	 */
	private static TypeInformation<?> resolveProperty(TypeInformation<?> owningType, String segment, String path) {

		TypeInformation<?> propertyType = owningType.getProperty(segment);

		if (propertyType == null) {
			throw new PathResolutionException(path, new PropertyReferenceException(segment, owningType, List.of()));
		}

		return propertyType;
	}

	/**
	 * Resolves a single {@code […]} index applied to the given type, returning the type it evaluates to. Indexing
	 * requires the type to actually be a {@link java.util.Map} or an indexed structure; anything else is rejected, so
	 * that an index cannot be used to address something the store could not index either.
	 *
	 * @param literal the raw index, quotes included.
	 * @param context the position within the path, exposing the type being indexed.
	 * @param mapKeyValidator the validator to apply to a map key.
	 * @param indexValidator the validator to apply to an index.
	 * @return the map value or component type.
	 * @throws PathResolutionException if the type is neither a map nor indexed, the literal is no valid key or index, or
	 *           the type exposes no value or component type.
	 */
	private static TypeInformation<?> resolveIndex(String literal, PathValidationContext context,
			MapKeyValidator mapKeyValidator, IndexValidator indexValidator) {

		TypeInformation<?> type = context.type();
		String segment = context.property();
		String key = unquote(literal);

		if (type.isMap()) {

			mapKeyValidator.validateKey(key, type.getComponentType(), context);

			TypeInformation<?> valueType = type.getMapValueType();

			if (valueType == null) {
				throw context.reject(String.format("Property '%s' is a raw map exposing no value type", abbreviate(segment)));
			}

			return valueType;
		}

		if (type.isCollectionLike()) {

			TypeInformation<?> componentType = type.getComponentType();

			if (componentType == null) {
				throw context
						.reject(String.format("Property '%s' is a raw collection exposing no component type", abbreviate(segment)));
			}

			indexValidator.validateIndex(key, componentType, context);

			return componentType;
		}

		throw context.reject(String.format("Property '%s' of type '%s' is neither a map nor an indexed structure",
				abbreviate(segment), type.getType().getSimpleName()));
	}

	private static String unquote(String literal) {

		if (literal.length() > 1) {

			char first = literal.charAt(0);

			if ((first == '\'' || first == '"') && literal.charAt(literal.length() - 1) == first) {
				return literal.substring(1, literal.length() - 1);
			}
		}

		return literal;
	}

	/**
	 * Unwraps the given type into the type the next path segment is to be resolved against, yielding the map value type
	 * for a {@link java.util.Map}, the component type for a collection or array and the type itself otherwise. Mirrors
	 * {@link TypeInformation#getRequiredActualType()} as applied by {@link PropertyPath#from(String, Class)}, so that a
	 * path using nothing but {@code "."} resolves exactly as it does there.
	 *
	 * @param type the type reached by the current segment.
	 * @return the type to resolve the next segment against.
	 */
	private static TypeInformation<?> navigable(TypeInformation<?> type) {

		TypeInformation<?> actualType = type.getActualType();

		return actualType == null ? type : actualType;
	}
}
