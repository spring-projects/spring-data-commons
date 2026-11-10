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
import java.util.HashSet;
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
		 * Validates that the given property segment may be navigated.
		 *
		 * @param segment the property name reached.
		 * @param context segment context information.
		 * @throws PathResolutionException
		 */
		void validateAccess(String segment, AccessValidationContext context);
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
	 * Collection of paths that must not be accessible.
	 *
	 * @return never {@literal null}.
	 */
	default Set<String> deniedPaths() {
		return Set.of();
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

		/**
		 * Applies the given {@link SegmentClassification}.
		 *
		 * @param classifier must not be {@literal null}.
		 * @return this {@link Builder}.
		 */
		ValidationRulesCustomizer segments(SegmentClassification classifier);

		/**
		 * Applies the given {@link IndexValidator}.
		 *
		 * @param validator must not be {@literal null}.
		 * @return this {@link Builder}.
		 */
		ValidationRulesCustomizer indexValidation(IndexValidator validator);

		/**
		 * Applies the given {@link MapKeyValidator}.
		 *
		 * @param validator must not be {@literal null}.
		 * @return this {@link Builder}.
		 */
		ValidationRulesCustomizer mapKeyValidation(MapKeyValidator validator);

		/**
		 * Applies the given {@link AccessValidator}.
		 *
		 * @param validator must not be {@literal null}.
		 * @return this {@link Builder}.
		 */
		ValidationRulesCustomizer accessValidation(AccessValidator validator);

		/**
		 * Limits the number of segments and indexes a path may consist of.
		 *
		 * @param maxAllowedSegments must be greater than zero.
		 * @return this {@link Builder}.
		 * @see PathValidationRules#maxAllowedSegments()
		 */
		ValidationRulesCustomizer maxSegmentsAllowed(int maxAllowedSegments);

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
		ValidationRulesCustomizer maxSegmentLength(int maxSegmentLength);

		/**
		 * Limits the number of characters a path may consist of, overriding what would be derived from
		 * {@link #maxSegmentLength(int)} and {@link #maxSegmentsAllowed(int)}.
		 *
		 * @param maxPathLength must be greater than zero.
		 * @return this {@link Builder}.
		 * @see PathValidationRules#maxPathLength()
		 */
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
		private final Set<String> deniedPaths = new LinkedHashSet<>();
		private int maxAllowedSegments = PathValidationRuleDefaults.MAX_SEGMENTS;
		private int maxSegmentLength = PathValidationRuleDefaults.MAX_SEGMENT_LENGTH;
		private @Nullable Integer maxPathLength;

		public Builder segments(SegmentClassification classifier) {

			Assert.notNull(classifier, "SegmentClassifier must not be null");

			this.segmentClassifier = classifier;
			return this;
		}

		public Builder mapKeyValidation(MapKeyValidator validator) {

			Assert.notNull(validator, "MapKeyValidator must not be null");

			this.mapKeyValidator = validator;
			return this;
		}

		public Builder indexValidation(IndexValidator validator) {

			Assert.notNull(validator, "IndexValidator must not be null");

			this.indexValidator = validator;
			return this;
		}

		public Builder maxSegmentsAllowed(int maxAllowedSegments) {

			Assert.isTrue(maxAllowedSegments > 0, "Maximum path depth must be greater than zero");

			this.maxAllowedSegments = maxAllowedSegments;
			return this;
		}

		public Builder maxSegmentLength(int maxSegmentLength) {

			this.maxSegmentLength = maxSegmentLength;
			return this;
		}

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
		 * Denies every navigable path that is, or lies underneath, one of the given paths. Adds to the paths denied so far
		 * rather than replacing them.
		 *
		 * @param deniedAccess must not be {@literal null} and must not contain a key or an index.
		 * @return this {@link Builder}.
		 */
		@Override
		public Builder denyAccess(Set<String> deniedAccess) {

			Assert.notNull(deniedAccess, "Denied paths must not be null");

			this.deniedPaths.addAll(deniedAccess);
			return this;
		}

		/**
		 * Denies every navigable path that is, or lies underneath, one of the given paths. Adds to the paths denied so far
		 * rather than replacing them.
		 *
		 * @param deniedAccess must not be {@literal null} and must not contain a key or an index.
		 * @return this {@link Builder}.
		 */
		public Builder denyAccess(String... deniedAccess) {

			Assert.notNull(deniedAccess, "Denied paths must not be null");

			return denyAccess(new LinkedHashSet<>(Arrays.asList(deniedAccess)));
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

			HashSet<String> inaccessiblePaths = new LinkedHashSet<>(this.deniedPaths);
			return new SimplePathValidationRules(this.segmentClassifier, getOrBuildAccessValidator(inaccessiblePaths),
					this.mapKeyValidator, this.indexValidator, this.maxAllowedSegments, this.maxSegmentLength, maxPathLength,
					inaccessiblePaths);
		}

		/**
		 * Combines the {@link #accessValidation(AccessValidator) configured} validator with the {@link #denyAccess(Set)
		 * denied paths} so that neither silently replaces the other.
		 *
		 * @return the {@link AccessValidator} to apply.
		 */
		private AccessValidator getOrBuildAccessValidator(Set<String> inaccessiblePaths) {

			if (this.accessValidator != AccessValidator.none() || inaccessiblePaths.isEmpty()) {
				return this.accessValidator;
			}

			return new PathAccessValidator(this.deniedPaths);
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
	 * @author Christoph Strobl
	 */
	interface PathValidationContext {

		String navigablePath();

		String path();

		String property();

		TypeInformation<?> type();

		int index();

		PathResolutionException reject(String reason); // TODO: lacks better concept
	}

	interface AccessValidationContext extends PathValidationContext {

		Set<String> deniedPaths();
	}
}
