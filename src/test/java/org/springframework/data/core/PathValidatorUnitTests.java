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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.core.PathValidationRules.AccessValidator;
import org.springframework.data.core.PathValidationRules.IndexValidator;
import org.springframework.data.core.PathValidationRules.MapKeyValidator;
import org.springframework.data.core.PathValidationRules.SegmentClassification;
import org.springframework.data.core.PathValidationRules.SegmentType;

/**
 * Unit tests for {@link PathValidator}.
 *
 * @author Christoph Strobl
 */
@SuppressWarnings("rawtypes")
class PathValidatorUnitTests {

	private static final TypeInformation<?> ROOT = TypeInformation.of(Root.class);
	private static final TypeInformation<?> CONCRETE = TypeInformation.of(ConcreteSample.class);
	private static final TypeInformation<?> RAW = TypeInformation.of(RawSample.class);
	private static final TypeInformation<?> OBJECTS = TypeInformation.of(ObjectSample.class);
	private static final TypeInformation<?> INTERFACE_TYPE = TypeInformation.of(InterfaceSample.class);
	private static final TypeInformation<?> GERNERIC_INTERFACE_TYPE = TypeInformation.of(GenericInterface.class);

	private static final PathValidationRules LONG_PATHS = new PathValidationRules() {

		@Override
		public int maxPathLength() {
			return Integer.MAX_VALUE;
		}
	};

	@Test // GH-1365
	void rejectsNullArguments() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> PathValidator.validate("nested", (Class<?>) null));
		assertThatIllegalArgumentException() //
				.isThrownBy(() -> PathValidator.validate("nested", (TypeInformation<?>) null));
		assertThatIllegalArgumentException() //
				.isThrownBy(() -> PathValidator.validate(null, Root.class));
	}

	@Test // GH-1365
	void exceptionMessageDoesNotRevealDomainInternals() {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.firstnam", ROOT)) //
				.withMessage("Invalid property path 'nested.firstnam'") //
				.withMessageNotContainingAny("Nested", "Root", "firstname") //
				.satisfies(e -> {
					assertThat(e.getDetailedMessage()) //
							.contains("No property 'firstnam' found for type 'Nested'") //
							.contains("Did you mean 'firstname'");
				});
	}

	@Test // GH-1365
	void detailedExceptionIsRetainedInStackTrace() {

		PathResolutionException exception = catchThrowableOfType(PathResolutionException.class,
				() -> PathValidator.validate("nested.unknown", ROOT));

		StringWriter trace = new StringWriter();
		exception.printStackTrace(new PrintWriter(trace));

		assertThat(exception).hasCauseInstanceOf(PropertyReferenceException.class);
		assertThat(trace.toString()) //
				.contains("Invalid property path 'nested.unknown'") //
				.contains("No property 'unknown' found for type 'Nested'");
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "nested", "nested.firstname", "nesteds.firstname", "nested.first_name" })
	void acceptsResolvableProperties(String path) {
		assertThatNoException().isThrownBy(() -> PathValidator.validate(path, ROOT));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "nested_firstname", "nestedFirstname", "nesteds_firstname" })
	void rejectsPathsWithUnderscoresThatDoNotExactlyMatch(String path) {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(path, ROOT));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "nested firstname", "nested-firstname", "1nested", "nested..firstname", ".nested", "nested.",
			"nested.firstname()", "${nested.firstname}", "" })
	void rejectsPathsContainingNonJavaIdentifier(String property) {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(property, ROOT)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()).contains("is not a valid Java identifier"));
	}

	@Test // GH-1365
	void rejectsPathsExceedingMaxNumberOfSegments() {

		assertThatNoException() //
				.isThrownBy(() -> PathValidator.validate("nested" + ".nested".repeat(99), ROOT, LONG_PATHS));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested" + ".nested".repeat(100), ROOT, LONG_PATHS));
	}

	@Test // GH-1365
	void rejectsExcessivelyNestedPropertiesWithoutResolvingThem() {

		String property = "nested" + ".nested".repeat(100_000);

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(property, ROOT, LONG_PATHS)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()).contains("not consist of more than 100 segments"));
	}

	@Test // GH-1365
	void rejectsPathsExceedingMaxLength() {

		// 692 chars, 99 segments - this is fine
		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested" + ".nested".repeat(98), ROOT));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested" + ".nested".repeat(200), ROOT)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()).containsSubsequence("longer", "than 1000"));
	}

	@Test // GH-1365
	void appliesConfiguredMaxLength() {

		PathValidationRules shortPaths = PathValidationRules.builder().maxPathLength(8).build();

		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested", ROOT, shortPaths));
		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT, shortPaths)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()).containsSubsequence("longer", "than 8"));
	}

	@Test // GH-1365
	void abbreviatesOversizedPathsInMessages() {

		String property = "a".repeat(500);

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(property, ROOT, LONG_PATHS)) //
				.satisfies(e -> {

					assertThat(e.getMessage()).hasSizeLessThan(150).contains("... (500 chars)");
					assertThat(e.getPath()).isEqualTo(property);
					assertThat(e.getDetailedMessage()).contains(property);
				});
	}

	@Test // GH-1365
	void appliesConfiguredMaxSegments() {

		PathValidationRules only2segments = PathValidationRules.builder().maxSegmentsAllowed(2).build();

		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT, only2segments));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nestedMaps[a][b].firstname", ROOT, only2segments)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()).contains("not consist of more than 2 segments"));
	}

	@Test // GH-1365
	void rejectsUnknownProperties() {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.unknown", ROOT));
	}

	@Test // GH-1365
	void caseMattersForProperties() {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.Firstname", ROOT));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "value", "value.firstname", "values", "values.firstname", "abstractValue.firstname" })
	void resolvesInheritedGenericProperties(String property) {

		assertThatNoException() //
				.isThrownBy(() -> PathValidator.validate(property, CONCRETE));
	}

	@Test // GH-1365
	void rejectsUnknownPropertiesOfInheritedGenericProperties() {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("abstractValue.unknown", CONCRETE));
	}

	@Test // GH-1365
	void rejectsTraversalIntoErasedGenerics() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("value", RAW));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("values", RAW));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("value.firstname", RAW));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("values.firstname", RAW));
	}

	@Test // GH-1365
	void rejectsTraversalIntoObjectProperties() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("anything", OBJECTS));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("anything.firstname", OBJECTS));
	}

	@Test // GH-1365
	void resolvesGenericInterfaceProperties() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("payload.firstname", INTERFACE_TYPE));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("computed", INTERFACE_TYPE));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("payload", GERNERIC_INTERFACE_TYPE));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("payload.firstname", GERNERIC_INTERFACE_TYPE));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "list.firstname", "array.firstname", "nesteds.firstname" })
	void traversesCollectionsAndArraysTransparently(String property) {
		assertThatNoException().isThrownBy(() -> PathValidator.validate(property, ROOT));
	}

	@Test // GH-1365
	void traversesMapsIntoTheirValueTypeViaNotNotation() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("byName", ROOT));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("byName.firstname", ROOT));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byName.unknown", ROOT)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()).contains("No property 'unknown' found for type 'Nested'"));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "byName[WORK]", "byName[WORK].firstname", "byName['on-call'].firstname",
			"byName[\"WORK\"].firstname", "byName[0].firstname" })
	void resolvesBracketedMapKeys(String property) {
		assertThatNoException().isThrownBy(() -> PathValidator.validate(property, ROOT));
	}

	@Test // GH-1365
	void rejectsUnknownPropertiesBehindMapKeys() {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byName[WORK].unknown", ROOT));
	}

	@Test // GH-1365
	void validatesEnumMapKeysAgainstConstants() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("byKind[HOME].firstname", ROOT));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byKind[OFFICE].firstname", ROOT));
	}

	@Test // GH-1365
	void validatesNumericMapKeys() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("byNumber[1].firstname", ROOT));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("byNumber['1'].firstname", ROOT));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byNumber[home].firstname", ROOT));
	}

	@Test // GH-1365
	void resolvesIndexesOfIndexedStructures() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("list[0].firstname", ROOT));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("list['0'].firstname", ROOT));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("array[0].firstname", ROOT));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("array['0'].firstname", ROOT));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("list[first].firstname", ROOT));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(
			strings = { "matrix[0][1].firstname", "listsByName[WORK][0].firstname", "nestedMaps[outer][inner].firstname" })
	void resolvesChainedIndexes(String property) {
		assertThatNoException().isThrownBy(() -> PathValidator.validate(property, ROOT));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "nested[0]", "nested['firstname']", "nested.firstname[0]" })
	void rejectsIndexingOfNonIndexedProperties(String property) {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(property, ROOT));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(
			strings = { "byName[", "byName[]", "byName[WORK", "byName[WORK]x", "[0].firstname", "byName]", "byName[WORK]]" })
	void rejectsMalformedIndexes(String property) {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(property, ROOT));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "byName['; drop']", "byName[$where]", "byName['a.b']", "byName['a,b']",
			"byName[T(java.lang.String)]", "byName['{$gt: 1}']", "byName['a b']" })
	void rejectsDangerousMapKeys(String property) {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(property, ROOT));
	}

	@Test // GH-1365
	void rejectsTraversalIntoRawMapValues() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("raw[any]", ROOT));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("raw[any].firstname", ROOT));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("raw.any", ROOT));
	}

	@Test // GH-1365
	void rejectsKeysOfMapsWithoutValueType() {

		assertThatNoException().isThrownBy(() -> PathValidator.validate("rawMap", ROOT));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("rawMap[any]", ROOT));
	}

	@Test // GH-1365
	void cannotConstrainNonEnumMapKeys() {
		assertThatNoException().isThrownBy(() -> PathValidator.validate("byName[whateverKeyThisIs]", ROOT));
	}

	@Test // GH-1365
	void appliesCustomMapKeyRule() {

		PathValidationRules anyKey = PathValidationRules.builder().mapKeyValidation((key, keyType, context) -> {}).build();

		assertThatNoException().isThrownBy(() -> PathValidator.validate("byName['on.call'].firstname", ROOT, anyKey));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("byKind[OFFICE].firstname", ROOT, anyKey));
	}

	@Test // GH-1365
	void appliesCustomIndexRule() {

		PathValidationRules anyIndex = PathValidationRules.builder().indexValidation((key, elementType, context) -> {})
				.build();

		assertThatNoException().isThrownBy(() -> PathValidator.validate("list[last].firstname", ROOT, anyIndex));
	}

	@Test // GH-1365
	void customRulesDoNotChangePropertyResolution() {

		PathValidationRules permissive = PathValidationRules.builder() //
				.mapKeyValidation((key, keyType, context) -> {}) //
				.indexValidation((index, elementType, context) -> {}) //
				.build();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.unknown", ROOT, permissive));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested firstname", ROOT, permissive));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "byName.WORK.firstname", "list.0.firstname", "array.0.firstname",
			"nestedMaps.outer.inner.firstname", "listsByName.WORK.0.firstname", "matrix.0.1.firstname",
			"byKind.HOME.firstname", "byNumber.1.firstname", "byName[WORK].firstname", "list[0].firstname",
			"matrix[0][1].firstname", "byName.firstname", "list.firstname", "nesteds.firstname", "nested.firstname" })
	void treatsSegmentsAsKeysAndIndexesWhenConfigured(String path) {

		assertThatNoException() //
				.isThrownBy(() -> PathValidator.validate(path, ROOT, PathValidationRules.lenient()));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "list.bogus.firstname", "byKind.OFFICE.firstname", "byNumber.home.firstname",
			"byName.$where.firstname" })
	void appliesKeyAndIndexRulesToSegmentsWhenUsingLenientRules(String path) {

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(path, ROOT, PathValidationRules.lenient()));
	}

	@Test // GH-1365
	void cannotConstrainStringMapKeysUsingLenientRules() {

		assertThatNoException() //
				.isThrownBy(() -> PathValidator.validate("byName.whateverKeyThisIs", ROOT, PathValidationRules.lenient()));
	}

	@Test // GH-1365
	void appliesCustomSegmentClassifier() {

		PathValidationRules indexedOnly = PathValidationRules.builder()
				.segments((segment, context) -> context.type().isCollectionLike() ? SegmentType.INDEX : SegmentType.PROPERTY)
				.build();

		assertThatNoException().isThrownBy(() -> PathValidator.validate("list.0.firstname", ROOT, indexedOnly));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byName.WORK.firstname", ROOT, indexedOnly));
	}

	@Test // GH-1365
	void rejectsSegmentClassifiedAsIndexOnNonIndexedType() {

		PathValidationRules everythingIsAnIndex = PathValidationRules.builder()
				.segments((segment, context) -> SegmentType.INDEX).build();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested", ROOT, everythingIsAnIndex));
	}

	@Test // GH-1365
	void appliesConfiguredMaxSegmentLength() {

		PathValidationRules shortSegments = PathValidationRules.builder().maxSegmentLength(6).build();

		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested", ROOT, shortSegments));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("firstname", ROOT, shortSegments));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT, shortSegments));
	}

	@Test // GH-1365
	void appliesConfiguredMaxSegmentLengthToKeysAndIndexes() {

		PathValidationRules shortSegments = PathValidationRules.builder().maxSegmentLength(6).build();

		assertThatNoException().isThrownBy(() -> PathValidator.validate("byName[WORK]", ROOT, shortSegments));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byName[averyverylongkey]", ROOT, shortSegments)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()) //
						.contains("Index at index 6 must not be longer than 6 characters"));
	}

	@Test // GH-1365
	void doesNotLimitSegmentsByDefault() {

		assertThat(PathValidationRules.strict().maxSegmentLength()).isNegative();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("a".repeat(999), ROOT)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()).contains("No property"));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(ints = { 0, -1, Integer.MIN_VALUE })
	void treatsNonPositiveMaxSegmentLengthAsNoLimit(int maxSegmentLength) {

		PathValidationRules rules = PathValidationRules.builder().maxSegmentLength(maxSegmentLength).build();

		assertThat(rules.maxPathLength()).isEqualTo(1000);
		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT, rules));
	}

	@Test // GH-1365
	void derivesMaxLengthFromSegmentLengthAndDepth() {

		assertThat(PathValidationRules.builder().maxSegmentLength(64).build().maxPathLength()).isEqualTo(64 * 100);

		assertThat(PathValidationRules.builder().maxSegmentLength(Integer.MAX_VALUE).build().maxPathLength())
				.isEqualTo(Integer.MAX_VALUE);
	}

	@Test // GH-1365
	void exposesPathAndTypeToSegmentClassifier() {

		List<String> seen = new ArrayList<>();

		PathValidationRules recording = PathValidationRules.builder().segments((segment, context) -> {

			seen.add("%s@%d owner=%s type=%s".formatted(segment, context.index(), context.property(),
					context.type().getType().getSimpleName()));
			assertThat(context.path()).isEqualTo("byName[WORK].firstname");

			return SegmentType.PROPERTY;
		}).build();

		PathValidator.validate("byName[WORK].firstname", ROOT, recording);

		assertThat(seen).containsExactly( //
				"byName@0 owner=byName type=Root", //
				"firstname@13 owner=byName type=Nested");
	}

	@Test // GH-1365
	void letsSegmentClassifierRejectWithAReason() {

		PathValidationRules noNested = PathValidationRules.builder().segments((segment, context) -> {

			if (context.index() > 0) {
				throw context
						.reject("Nested paths are not supported, '%s' follows '%s'".formatted(segment, context.property()));
			}

			return SegmentType.PROPERTY;
		}).build();

		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested", ROOT, noNested));

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT, noNested)) //
				.withMessage("Invalid property path 'nested.firstname'") //
				.satisfies(e -> assertThat(e.getDetailedMessage()) //
						.contains("Nested paths are not supported, 'firstname' follows 'nested'"));
	}

	@Test // GH-1365
	void exposesIndexedTypeToKeyAndIndexRules() {

		List<String> seen = new ArrayList<>();

		PathValidationRules recording = new PathValidationRules() {

			@Override
			public MapKeyValidator mapKeyValidator() {
				return (key, keyType, context) -> seen
						.add("key=%s keyType=%s owner=%s type=%s@%d".formatted(key, keyType.getType().getSimpleName(),
								context.property(), context.type().getType().getSimpleName(), context.index()));
			}

			@Override
			public IndexValidator indexValidator() {
				return (index, elementType, context) -> seen
						.add("index=%s elementType=%s owner=%s type=%s@%d".formatted(index, elementType.getType().getSimpleName(),
								context.property(), context.type().getType().getSimpleName(), context.index()));
			}
		};

		PathValidator.validate("byName[WORK].firstname", ROOT, recording);
		PathValidator.validate("list[0].firstname", ROOT, recording);

		assertThat(seen).containsExactly( //
				"key=WORK keyType=String owner=byName type=Map@6", //
				"index=0 elementType=Nested owner=list type=List@4");
	}

	@Test // GH-1365
	void builderDefaultsToTheDefaultRules() {

		PathValidationRules rules = PathValidationRules.builder().build();

		assertThat(rules.segmentClassifier()).isSameAs(SegmentClassification.strict());
		assertThat(rules.mapKeyValidator()).isSameAs(MapKeyValidator.strict());
		assertThat(rules.indexValidator()).isSameAs(IndexValidator.numeric());
		assertThat(rules.maxAllowedSegments()).isEqualTo(100);
		assertThat(rules.maxSegmentLength()).isNegative();
		assertThat(rules.maxPathLength()).isEqualTo(1000);

		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT, rules));
		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byName.WORK.firstname", ROOT, rules));
	}

	@Test // GH-3521
	void builderHonoursExplicitPathLength() {

		PathValidationRules rules = PathValidationRules.builder() //
				.maxSegmentLength(64) //
				.maxPathLength(100) //
				.build();

		assertThat(rules.maxPathLength()).isEqualTo(100);

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested" + ".nested".repeat(20), ROOT, rules)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()).containsSubsequence("longer", "than 100"));
	}

	@Test // GH-1365
	void builderIsReusable() {

		PathValidationRules.Builder builder = PathValidationRules.builder().maxSegmentLength(8);
		PathValidationRules first = builder.build();

		builder.maxSegmentLength(16);
		PathValidationRules second = builder.build();

		assertThat(first.maxSegmentLength()).isEqualTo(8);
		assertThat(second.maxSegmentLength()).isEqualTo(16);
	}

	@Test // GH-1365
	void deniesSubtreeOfAnExcludedProperty() {

		PathValidationRules rules = PathValidationRules.builder().denyAccess("nested").build();

		for (String property : List.of("nested", "nested.firstname", "nested.nested.firstname")) {
			assertThatExceptionOfType(PathResolutionException.class) //
					.isThrownBy(() -> PathValidator.validate(property, ROOT, rules)) //
					.satisfies(e -> assertThat(e.getDetailedMessage()).contains("Access to property path 'nested' is denied"));
		}
	}

	@Test // GH-1365
	void accumulatesDeniedPathsAcrossInvocations() {

		PathValidationRules rules = PathValidationRules.builder() //
				.denyAccess("nested") //
				.denyAccess("**.firstname") //
				.denyAccess(Set.of("list")) //
				.build();

		for (String property : List.of("nested", "byName[WORK].firstname", "list[0]")) {
			assertThatExceptionOfType(PathResolutionException.class) //
					.isThrownBy(() -> PathValidator.validate(property, ROOT, rules));
		}
	}

	@Test // GH-1365
	void providesDeniedPathsInContext() {

		List<String> seen = new ArrayList<>();

		PathValidationRules rules = PathValidationRules.builder() //
				.accessValidation((segment, context) -> {

					seen.addAll(context.deniedPaths());
					if (context.deniedPaths().contains(context.property())) {
						throw context.reject("Cannot access '%s'".formatted(context.property()));
					}
				}) //
				.denyAccess("nested") //
				.build();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested", ROOT, rules));

		assertThat(seen).containsExactly("nested");
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "byName", "byName.firstname", "byName[WORK]", "byName[WORK].firstname", "list", "list[0]",
			"list[0].firstname", "list.firstname" })
	void deniesMapAndCollectionRegardlessOfKeyOrIndex(String path) {

		PathValidationRules rules = PathValidationRules.builder().denyAccess("byName", "list").build();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate(path, ROOT, rules));
	}

	@ParameterizedTest // GH-1365
	@ValueSource(strings = { "byName[WORK]", "list[0]", "byName['WORK'].firstname" })
	void rejectsExclusionsNamingAKeyOrIndex(String path) {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> PathValidationRules.builder().denyAccess(path).build());
	}

	@Test // GH-1365
	void deniesNestedPropertyBehindAMapKey() {

		PathValidationRules rules = PathValidationRules.builder().denyAccess("byName.firstname").build();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byName[WORK].firstname", ROOT, rules)) //
				.satisfies(e -> assertThat(e.getDetailedMessage()) //
						.contains("Access to property path 'byName.firstname' is denied"));

		assertThatNoException().isThrownBy(() -> PathValidator.validate("byName[WORK].first_name", ROOT, rules));
	}

	@Test // GH-1365
	void deniesMatchingAntStyleWildcardPaths() {

		PathValidationRules oneLevel = PathValidationRules.builder().denyAccess("*.firstname").build();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT, oneLevel));
		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested.nested.firstname", ROOT, oneLevel));

		PathValidationRules anyDepth = PathValidationRules.builder().denyAccess("**.firstname").build();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.nested.firstname", ROOT, anyDepth));
		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("byName[WORK].firstname", ROOT, anyDepth));

		PathValidationRules subtree = PathValidationRules.builder().denyAccess("nested.**").build();

		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested", ROOT, subtree));
		assertThatExceptionOfType(PathResolutionException.class) //
				.isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT, subtree));
	}

	@Test // GH-1365
	void deniesNothingByDefault() {

		assertThat(PathValidationRules.strict().accessValidator()).isSameAs(AccessValidator.none());
		assertThat(PathValidationRules.builder().build().accessValidator()).isSameAs(AccessValidator.none());

		assertThatNoException().isThrownBy(() -> PathValidator.validate("nested.firstname", ROOT));
	}

	@Test // GH-1365
	void exposesNavigablePath() {

		List<String> seen = new ArrayList<>();

		PathValidationRules recording = PathValidationRules.builder() //
				.accessValidation((segment, context) -> seen.add(segment + "@" + context.navigablePath())) //
				.build();

		PathValidator.validate("byName[WORK].firstname", ROOT, recording);

		assertThat(seen).containsExactly("byName@byName", "firstname@byName.firstname");
	}

	static class Root {

		Nested nested;
		Collection<Nested> nesteds;
		Map<String, Nested> byName;
		Map<Kind, Nested> byKind;
		Map<Integer, Nested> byNumber;
		Map<String, List<Nested>> listsByName;
		Map<String, Map<String, Nested>> nestedMaps;
		Map raw;
		RawMapType rawMap;
		List<Nested> list;
		List<List<Nested>> matrix;
		Nested[] array;
	}

	static class Nested {
		String firstname;
		String first_name;
		Nested nested;
	}

	enum Kind {
		HOME, WORK
	}

	@SuppressWarnings("rawtypes")
	static abstract class RawMapType implements Map {}

	static abstract class GenericBase<T> {

		T value;
		List<T> values;

		public abstract T getAbstractValue();
	}

	static class ConcreteSample extends GenericBase<Nested> {

		@Override
		public Nested getAbstractValue() {
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	static class RawSample extends GenericBase {

		@Override
		public Object getAbstractValue() {
			return null;
		}
	}

	interface GenericInterface<T> {

		T getPayload();

		default String getComputed() {
			return null;
		}
	}

	static class InterfaceSample implements GenericInterface<Nested> {

		@Override
		public Nested getPayload() {
			return null;
		}
	}

	static class ObjectSample {
		Object anything;
	}
}
