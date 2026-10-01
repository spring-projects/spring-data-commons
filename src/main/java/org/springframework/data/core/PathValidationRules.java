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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.data.core.PathValidationRuleDefaults.PathAccessValidator;
import org.springframework.util.Assert;

/**
 * Rules to be applied by {@link PathValidator}.
 * <p>
 * Use {@link #strict()} to work with a perdefined set of rules or turn to {@link #builder()} to compose your own.
 * 
 * <pre class="code">
 * PathValidationRules rules = PathValidationRules.builder() //
 * 		.dennyAccessTo("**.password") //
 * 		.mapKeys((key, keyType, context) -&gt; doesNotContainDots(key)) //
 * 		.maxSegmentLength(32) //
 * 		.build();
 * </pre>
 * 
 * @author Christoph Strobl
 * @since 4.2
 * @see PathValidator#validate(String, Class, PathValidationRules)
 */
public interface PathValidationRules {

	/**
	 * Decides on {@link SegmentType} for further treatment of the segment.
	 *
	 * @since 4.2
	 * @author Christoph Strobl
	 */
	@FunctionalInterface
	interface SegmentClassification {

		/**
		 * Returns the classifier treating every segment as a {@link SegmentType#PROPERTY property}. Drilling down into a
		 * {@link java.util.Map} or indexed property can be done using {@code [...]} as in {@code phoneBook[WORK].city} for
		 * {@link java.util.Map Map} keys and {@code addresses[0].city} for {@link java.util.Collection Collection} indexes.
		 *
		 * @return the strict {@link SegmentClassification}.
		 */
		static SegmentClassification strict() {
			return PathValidationRuleDefaults.STRICT;
		}

		/**
		 * Like #strict() but allows keys and indexes to be unbracketed, so that {@code phoneBook.WORK.city} and
		 * {@code addresses.0.city} are accepted as well.
		 *
		 * @return a {@link SegmentClassification} accepting keys and indexes as segments.
		 */
		static SegmentClassification lenient() {
			return PathValidationRuleDefaults.LENIENT;
		}

		/**
		 * Classifies the given segment.
		 *
		 * @param segment the segment to classify.
		 * @param context segment context information.
		 * @return never {@literal null}.
		 * @throws PathResolutionException if the segment is not acceptable.
		 */
		SegmentType classify(String segment, PathValidationContext context);
	}

	/**
	 * Decides whether a property can be navigated accessed at all.
	 * <p>
	 * The {@link #excluding(String...) exluding} variants allow creating an {@link AccessValidator} denying access to a
	 * navigable path that is one of the excluded paths. Allows prefix matches, like {@code "address"} which denies access
	 * to {@code address}, {@code address.city} and {@code address[0].city} as well as ant path style wildcards where
	 * {@code "**.password"} denies {@code password} at any depth. <br />
	 * <strong>Note:</strong> Ant Path style patterns are more costly to evaluate than prefix matches.
	 * 
	 * @since 4.2
	 * @author Christoph Strobl
	 */
	@FunctionalInterface
	interface AccessValidator {

		/**
		 * Returns a validator that does not exclude any navigable path.
		 *
		 * @return never {@literal null}.
		 */
		static AccessValidator none() {
			return PathValidationRuleDefaults.NO_ACCESS_CHECK;
		}

		/**
		 * @param deniedPaths must not be {@literal null}, nor contain empty elements.
		 * @return an {@link AccessValidator} denying the given paths.
		 */
		static AccessValidator excluding(String... deniedPaths) {

			Assert.notNull(deniedPaths, "Denied paths must not be null");

			return excluding(new LinkedHashSet<>(Arrays.asList(deniedPaths)));
		}

		/**
		 * Returns a validator denying every navigable path that is, or lies underneath, one of the given paths.
		 *
		 * @param deniedPaths must not be {@literal null}, nor contain empty elements.
		 * @return an {@link AccessValidator} denying the given paths.
		 */
		static AccessValidator excluding(Set<String> deniedPaths) {
			return new PathAccessValidator(deniedPaths);
		}

		/**
		 * Validates that the given property segment may be navigated.
		 *
		 * @param segment the property name reached.
		 * @param context segment context information.
		 * @throws PathResolutionException if the property must not be navigated; use {@code throw context.reject("…")} to
		 *           say why.
		 */
		void validateAccess(String segment, PathValidationContext context);
	}

	/**
	 * Validates a key addressing a single entry of a {@link java.util.Map} typed property.
	 */
	@FunctionalInterface
	interface MapKeyValidator {

		/**
		 * Returns a {@link MapKeyValidator} checks map keys for {@link Enum} and {@link Number numeric} types for validity.
		 * Other types are restricted to word characters and hyphens.
		 *
		 * @return the strict {@link MapKeyValidator}.
		 */
		static MapKeyValidator strict() {
			return PathValidationRuleDefaults.STRICT_KEY_VALIDATOR;
		}

		/**
		 * Validates the given map key.
		 *
		 * @param key the key, with quotes already removed.
		 * @param keyType the key type of the map, {@literal null} for a raw map exposing none.
		 * @param context key context information.
		 * @throws PathResolutionException if the key is no valid key of the map.
		 */
		void validateKey(String key, @Nullable TypeInformation<?> keyType, PathValidationContext context);
	}

	/**
	 * Validates an index addressing a single element of an indexed property (like {@link java.util.Collection}, array or
	 * {@link TypeInformation#isCollectionLike() collection like}) types.
	 */
	@FunctionalInterface
	interface IndexValidator {

		/**
		 * Returns the index validator that requires the index to be a {@link Number number}.
		 *
		 * @return the numeric {@link IndexValidator}.
		 */
		static IndexValidator numeric() {
			return PathValidationRuleDefaults.NUMERIC_INDEX_VALIDATOR;
		}

		/**
		 * Validates the given index.
		 *
		 * @param index the index, with quotes already removed.
		 * @param elementType the element type of the indexed structure.
		 * @param context index context information.
		 * @throws PathResolutionException if the index is not valid.
		 */
		void validateIndex(String index, @Nullable TypeInformation<?> elementType, PathValidationContext context);
	}

	/**
	 * Obtain a {@link PathValidationRules ruleset} that restricts access strictly to properties navigable from a given
	 * root. Requires indexes for collection like properties as well as map keys to be enclosed in square brackets.
	 * Validates map keys for {@link Enum} and {@link Number numeric} types of collection indexes.
	 *
	 * @return the strict {@link PathValidationRules}.
	 */
	static PathValidationRules strict() {
		return PathValidationRuleDefaults.INSTANCE;
	}

	/**
	 * Much like {@link #strict()}, but allows keys and indexes to be unbracketed.
	 *
	 * @return {@link PathValidationRules} accepting keys and indexes as segments.
	 */
	static PathValidationRules lenient() {

		return new PathValidationRules() {
			@Override
			public SegmentClassification segmentClassifier() {
				return SegmentClassification.lenient();
			}
		};
	}

	/**
	 * Returns a {@link Builder} to compose rules from the individual validators and limits, starting from the
	 * {@link #strict() strict} rules:
	 *
	 * @return new instance of {@link Builder}.
	 */
	static Builder builder() {
		return new Builder();
	}

	/**
	 * The maximum number of segments.
	 *
	 * @return value > 0
	 */
	default int maxAllowedSegments() {
		return PathValidationRuleDefaults.MAX_SEGMENTS;
	}

	/**
	 * The maximum number of characters in a single path segment. Use {@code -1} for unbounded.
	 *
	 * @return -1 for unbounded
	 */
	default int maxSegmentLength() {
		return PathValidationRuleDefaults.MAX_SEGMENT_LENGTH;
	}

	/**
	 * The maximum number of characters a path can have.
	 *
	 * @return value > 0.
	 */
	default int maxPathLength() {
		return PathValidationRuleDefaults.derivePathLength(maxSegmentLength(), maxAllowedSegments());
	}

	/**
	 * @return the {@link SegmentClassification} to apply, never {@literal null}.
	 */
	default SegmentClassification segmentClassifier() {
		return SegmentClassification.strict();
	}

	/**
	 * @return the {@link AccessValidator} to apply, never {@literal null}.
	 */
	default AccessValidator accessValidator() {
		return AccessValidator.none();
	}

	/**
	 * @return the {@link MapKeyValidator} to apply, never {@literal null}.
	 */
	default MapKeyValidator mapKeyValidator() {
		return MapKeyValidator.strict();
	}

	/**
	 * @return the {@link IndexValidator} to apply, never {@literal null}.
	 */
	default IndexValidator indexValidator() {
		return IndexValidator.numeric();
	}

	/**
	 * Customizer interface for {@link PathValidationRules}.
	 */
	interface ValidationRulesCustomizer {

		ValidationRulesCustomizer segments(SegmentClassification classifier);

		ValidationRulesCustomizer indexValidation(IndexValidator validator);

		ValidationRulesCustomizer mapKeyValidation(MapKeyValidator validator);

		ValidationRulesCustomizer accessValidation(AccessValidator validator);

		ValidationRulesCustomizer maxSegmentsAllowed(int maxAllowedSegments);

		ValidationRulesCustomizer maxSegmentLength(int maxSegmentLength);

		ValidationRulesCustomizer maxPathLength(int maxPathLength);

		default ValidationRulesCustomizer denyAccess(String... deniedPaths) {
			return denyAccess(new LinkedHashSet<>(Arrays.asList(deniedPaths)));
		}

		ValidationRulesCustomizer denyAccess(Set<String> deniedAccess);
	}

	/**
	 * Builder composing {@link PathValidationRules} from the individual validators and limits.
	 *
	 * @author Christoph Strobl
	 * @since 4.2
	 * @see PathValidationRules#builder()
	 */
	final class Builder implements ValidationRulesCustomizer {

		private SegmentClassification segmentClassifier = SegmentClassification.strict();
		private MapKeyValidator mapKeyValidator = MapKeyValidator.strict();
		private IndexValidator indexValidator = IndexValidator.numeric();
		private AccessValidator accessValidator = AccessValidator.none();
		private int maxAllowedSegments = PathValidationRuleDefaults.MAX_SEGMENTS;
		private int maxSegmentLength = PathValidationRuleDefaults.MAX_SEGMENT_LENGTH;
		private @Nullable Integer maxPathLength;

		/**
		 * Applies the given {@link SegmentClassification}.
		 *
		 * @param classifier must not be {@literal null}.
		 * @return this {@link Builder}.
		 */
		public Builder segments(SegmentClassification classifier) {

			Assert.notNull(classifier, "SegmentClassifier must not be null");

			this.segmentClassifier = classifier;
			return this;
		}

		/**
		 * Applies the given {@link MapKeyValidator}.
		 *
		 * @param validator must not be {@literal null}.
		 * @return this {@link Builder}.
		 */
		public Builder mapKeyValidation(MapKeyValidator validator) {

			Assert.notNull(validator, "MapKeyValidator must not be null");

			this.mapKeyValidator = validator;
			return this;
		}

		/**
		 * Applies the given {@link IndexValidator}.
		 *
		 * @param validator must not be {@literal null}.
		 * @return this {@link Builder}.
		 */
		public Builder indexValidation(IndexValidator validator) {

			Assert.notNull(validator, "IndexValidator must not be null");

			this.indexValidator = validator;
			return this;
		}

		/**
		 * Limits the number of segments and indexes a path may consist of.
		 *
		 * @param maxAllowedSegments must be greater than zero.
		 * @return this {@link Builder}.
		 * @see PathValidationRules#maxAllowedSegments()
		 */
		public Builder maxSegmentsAllowed(int maxAllowedSegments) {

			Assert.isTrue(maxAllowedSegments > 0, "Maximum path depth must be greater than zero");

			this.maxAllowedSegments = maxAllowedSegments;
			return this;
		}

		/**
		 * Limits the number of characters a single path element may consist of. Any value smaller than one leaves
		 * individual elements unbounded.
		 * <p>
		 * Unless {@link #maxPathLength(int)} is given as well, this also derives the overall length limit, as a path cannot
		 * be longer than {@link #maxSegmentsAllowed(int)} elements of this length.
		 *
		 * @param maxSegmentLength the maximum element length, or a value smaller than one to not limit elements.
		 * @return this {@link Builder}.
		 * @see PathValidationRules#maxSegmentLength()
		 */
		public Builder maxSegmentLength(int maxSegmentLength) {

			this.maxSegmentLength = maxSegmentLength;
			return this;
		}

		/**
		 * Limits the number of characters a path may consist of, overriding what would be derived from
		 * {@link #maxSegmentLength(int)} and {@link #maxSegmentsAllowed(int)}.
		 *
		 * @param maxPathLength must be greater than zero.
		 * @return this {@link Builder}.
		 * @see PathValidationRules#maxPathLength()
		 */
		public Builder maxPathLength(int maxPathLength) {

			Assert.isTrue(maxPathLength > 0, "Maximum path length must be greater than zero");

			this.maxPathLength = maxPathLength;
			return this;
		}

		/**
		 * Applies the given {@link AccessValidator}.
		 *
		 * @param validator must not be {@literal null}.
		 * @return this {@link Builder}.
		 */
		@Override
		public Builder accessValidation(AccessValidator validator) {

			Assert.notNull(validator, "AccessValidator must not be null");

			this.accessValidator = validator;
			return this;
		}

		/**
		 * Denies every navigable path that is, or lies underneath, one of the given paths.
		 *
		 * @param deniedAccess must not be {@literal null} and must not contain a key or an index.
		 * @return this {@link Builder}.
		 * @see AccessValidator#excluding(Set)
		 */
		@Override
		public Builder denyAccess(Set<String> deniedAccess) {
			return accessValidation(AccessValidator.excluding(deniedAccess));
		}

		/**
		 * Denies every navigable path that is, or lies underneath, one of the given paths.
		 *
		 * @param deniedAccess must not be {@literal null} and must not contain a key or an index.
		 * @return this {@link Builder}.
		 * @see AccessValidator#excluding(String...)
		 */
		public Builder denyAccess(String... deniedAccess) {
			return accessValidation(AccessValidator.excluding(deniedAccess));
		}

		/**
		 * Creates the {@link PathValidationRules} as configured.
		 *
		 * @return the configured {@link PathValidationRules}.
		 */
		public PathValidationRules build() {

			int maxPathLength = this.maxPathLength != null //
					? this.maxPathLength //
					: PathValidationRuleDefaults.derivePathLength(this.maxSegmentLength, this.maxAllowedSegments);

			return new SimplePathValidationRules(this.segmentClassifier, this.accessValidator, this.mapKeyValidator,
					this.indexValidator, this.maxAllowedSegments, this.maxSegmentLength, maxPathLength);
		}
	}

	/**
	 * Classification of a path segment
	 *
	 * @see PathValidationRules#segmentClassifier()
	 */
	enum SegmentType {

		/**
		 * The segment names a property and is required to be a valid Java identifier resolving against the type it appears
		 * on, unwrapping {@link java.util.Map}, collection and array types transparently.
		 */
		PROPERTY,

		/**
		 * The segment addresses a single entry of the type it appears on, which therefore has to be a {@link java.util.Map}
		 * or an indexed structure. The segment is validated as a key or an index rather than as a Java identifier, exactly
		 * as a bracketed one is.
		 */
		INDEX
	}

	/**
	 * Contextual information about a path element.
	 *
	 * @author Christoph Strobl
	 * @since 4.2
	 */
	final class PathValidationContext {

		private final String path;
		private final String property;
		private final TypeInformation<?> type;
		private final int index;
		private final String navigablePath;

		PathValidationContext(String path, String property, TypeInformation<?> type, int index, String navigablePath) {

			this.path = path;
			this.property = property;
			this.type = type;
			this.index = index;
			this.navigablePath = navigablePath;
		}

		/**
		 * Returns the path reached so far as a chain of property names separated by {@code "."}, with keys and indexes left
		 * out, so that {@code phoneBook[WORK].city} is navigated as {@code phoneBook.city}. For a property segment this
		 * includes the segment itself.
		 * <p>
		 * Only maintained while an {@link AccessValidator} other than {@link AccessValidator#none()} is configured, as
		 * building it costs allocation a path would otherwise not pay for; it is empty otherwise.
		 *
		 * @return the navigable path reached so far, never {@literal null}.
		 */
		public String navigablePath() {
			return navigablePath;
		}

		/**
		 * Returns the full property path being validated.
		 *
		 * @return the full property path being validated.
		 */
		public String path() {
			return path;
		}

		/**
		 * Returns the property the path element belongs to, that is the property a key or index is applied to, or the one a
		 * segment follows. The segment or literal being checked is passed to the callback itself rather than exposed here.
		 *
		 * @return the owning property.
		 */
		public String property() {
			return property;
		}

		/**
		 * Returns the type the path element is applied to, not unwrapped, so that a {@link java.util.Map} or indexed
		 * property can be told apart from its value or element type. Note that the type a check is about is passed to the
		 * callback itself; for a map {@link TypeInformation#getComponentType()} is the key and
		 * {@link TypeInformation#getMapValueType()} the value type, which is easy to mix up.
		 *
		 * @return the type the path element is applied to.
		 */
		public TypeInformation<?> type() {
			return type;
		}

		/**
		 * Returns the index of the path element within {@link #path()}, counted in characters.
		 *
		 * @return the character index the path element starts at.
		 */
		public int index() {
			return index;
		}

		/**
		 * Creates the {@link PathResolutionException} to throw for the given reason. The reason becomes part of the
		 * {@link PathResolutionException#getDetailedMessage() detailed message} and is therefore free to name types and
		 * properties; it does not reach {@link PathResolutionException#getMessage()}. Returned rather than thrown so that
		 * control flow stays visible to the compiler and the reader, as in {@code throw context.reject("…")}; a rule
		 * returning a value could not otherwise reject without a dead {@code return} behind the call.
		 *
		 * @param reason why the path element was rejected.
		 * @return the exception to throw.
		 */
		public PathResolutionException reject(String reason) {
			return new PathResolutionException(path,
					String.format("Property path '%s' is invalid; %s", PathResolutionException.abbreviate(path), reason));
		}

		public void error(String reason) {
			throw new PathResolutionException(path,
					String.format("Property path '%s' is invalid; %s", PathResolutionException.abbreviate(path), reason));
		}

		@Override
		public String toString() {
			return String.format("PathValidationContext[path='%s', property='%s', type=%s, index=%d, navigablePath='%s']",
					path, property, type.getType().getSimpleName(), index, navigablePath);
		}
	}
}
