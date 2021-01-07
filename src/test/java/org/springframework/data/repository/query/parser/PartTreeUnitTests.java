/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.repository.query.parser;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.repository.query.parser.Part.Type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree.OrPart;

/**
 * Unit tests for {@link PartTree}.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author Thomas Darimont
 * @author Martin Baumgartner
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Michael Cramer
 * @author Mariusz Mączkowski
 * @author Jens Schauder
 */
class PartTreeUnitTests {

	private String[] PREFIXES = { "find", "read", "get", "query", "search", "stream", "count", "delete", "remove",
			"exists" };

	@Test
	void rejectsNullSource() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PartTree(null, getClass()));
	}

	@Test
	void rejectsNullDomainClass() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PartTree("test", null));
	}

	@Test
	void rejectsMultipleOrderBy() {
		assertThatIllegalArgumentException().isThrownBy(() -> partTree("firstnameOrderByLastnameOrderByFirstname"));
	}

	@Test
	void parsesSimplePropertyCorrectly() throws Exception {
		PartTree partTree = partTree("firstname");
		assertPart(partTree, parts("firstname"));
	}

	@Test
	void parsesAndPropertiesCorrectly() throws Exception {
		PartTree partTree = partTree("firstnameAndLastname");
		assertPart(partTree, parts("firstname", "lastname"));
		assertThat(partTree.getSort().isSorted()).isFalse();
	}

	@Test
	void parsesOrPropertiesCorrectly() throws Exception {
		PartTree partTree = partTree("firstnameOrLastname");
		assertPart(partTree, parts("firstname"), parts("lastname"));
		assertThat(partTree.getSort().isSorted()).isFalse();
	}

	@Test
	void parsesCombinedAndAndOrPropertiesCorrectly() throws Exception {
		PartTree tree = partTree("firstnameAndLastnameOrLastname");
		assertPart(tree, parts("firstname", "lastname"), parts("lastname"));
	}

	@Test
	void hasSortIfOrderByIsGiven() throws Exception {
		assertThat(partTree("firstnameOrderByLastnameDesc").getSort()).isEqualTo(Sort.by("lastname").descending());
	}

	@Test
	void hasSortIfOrderByIsGivenWithAllIgnoreCase() throws Exception {
		hasSortIfOrderByIsGivenWithAllIgnoreCase("firstnameOrderByLastnameDescAllIgnoreCase");
		hasSortIfOrderByIsGivenWithAllIgnoreCase("firstnameOrderByLastnameDescAllIgnoringCase");
		hasSortIfOrderByIsGivenWithAllIgnoreCase("firstnameAllIgnoreCaseOrderByLastnameDesc");
	}

	private void hasSortIfOrderByIsGivenWithAllIgnoreCase(String source) throws Exception {
		PartTree partTree = partTree(source);
		assertThat(partTree.getSort()).isEqualTo(Sort.by("lastname").descending());
	}

	@Test
	void detectsDistinctCorrectly() throws Exception {
		for (String prefix : PREFIXES) {
			detectsDistinctCorrectly(prefix + "DistinctByLastname", true);
			detectsDistinctCorrectly(prefix + "UsersDistinctByLastname", true);
			detectsDistinctCorrectly(prefix + "DistinctUsersByLastname", true);
			detectsDistinctCorrectly(prefix + "UsersByLastname", false);
			detectsDistinctCorrectly(prefix + "ByLastname", false);
			// Check it's non-greedy (would strip everything until Order*By*
			// otherwise)
			PartTree tree = detectsDistinctCorrectly(prefix + "ByLastnameOrderByFirstnameDesc", false);
			assertThat(tree.getSort()).isEqualTo(Sort.by("firstname").descending());
		}
	}

	private PartTree detectsDistinctCorrectly(String source, boolean expected) {
		PartTree tree = partTree(source);
		assertThat(tree.isDistinct()).isEqualTo(expected);
		return tree;
	}

	@Test
	void parsesWithinCorrectly() {
		PartTree tree = partTree("findByLocationWithin");
		for (Part part : tree.getParts()) {
			assertThat(part.getType()).isEqualTo(Type.WITHIN);
			assertThat(part.getProperty()).isEqualTo(newProperty("location"));
		}
	}

	@Test
	void parsesNearCorrectly() {
		assertType(Collections.singletonList("locationNear"), NEAR, "location");
	}

	@Test
	void supportToStringWithoutSortOrder() throws Exception {
		assertType(Collections.singletonList("firstname"), SIMPLE_PROPERTY, "firstname");
	}

	@Test
	void supportToStringWithSortOrder() throws Exception {
		PartTree tree = partTree("firstnameOrderByLastnameDesc");
		assertThat(tree.toString()).isEqualTo("firstname SIMPLE_PROPERTY (1): [Is, Equals] NEVER Order By lastname: DESC");
	}

	@Test
	void detectsIgnoreAllCase() throws Exception {
		detectsIgnoreAllCase("firstnameOrderByLastnameDescAllIgnoreCase", IgnoreCaseType.WHEN_POSSIBLE);
		detectsIgnoreAllCase("firstnameOrderByLastnameDescAllIgnoringCase", IgnoreCaseType.WHEN_POSSIBLE);
		detectsIgnoreAllCase("firstnameAllIgnoreCaseOrderByLastnameDesc", IgnoreCaseType.WHEN_POSSIBLE);
		detectsIgnoreAllCase("getByFirstnameAllIgnoreCase", IgnoreCaseType.WHEN_POSSIBLE);
		detectsIgnoreAllCase("getByFirstname", IgnoreCaseType.NEVER);
		detectsIgnoreAllCase("firstnameOrderByLastnameDesc", IgnoreCaseType.NEVER);
	}

	private void detectsIgnoreAllCase(String source, IgnoreCaseType expected) throws Exception {
		PartTree tree = partTree(source);
		for (Part part : tree.getParts()) {
			assertThat(part.shouldIgnoreCase()).isEqualTo(expected);
		}
	}

	@Test
	void detectsSpecificIgnoreCase() throws Exception {

		PartTree tree = partTree("findByFirstnameIgnoreCaseAndLastname");

		assertPart(tree, parts("firstnameIgnoreCase", "lastname"));

		Iterator<Part> parts = tree.getParts().iterator();

		assertThat(parts.next().shouldIgnoreCase()).isEqualTo(IgnoreCaseType.ALWAYS);
		assertThat(parts.next().shouldIgnoreCase()).isEqualTo(IgnoreCaseType.NEVER);
	}

	@Test
	void detectsSpecificIgnoringCase() throws Exception {
		PartTree tree = partTree("findByFirstnameIgnoringCaseAndLastname");
		assertPart(tree, parts("firstnameIgnoreCase", "lastname"));
		Iterator<Part> parts = tree.getParts().iterator();
		assertThat(parts.next().shouldIgnoreCase()).isEqualTo(IgnoreCaseType.ALWAYS);
		assertThat(parts.next().shouldIgnoreCase()).isEqualTo(IgnoreCaseType.NEVER);
	}

	@Test // DATACMNS-78
	void parsesLessThanEqualCorrectly() {
		assertType(Arrays.asList("lastnameLessThanEqual", "lastnameIsLessThanEqual"), LESS_THAN_EQUAL, "lastname");
	}

	@Test // DATACMNS-78
	void parsesGreaterThanEqualCorrectly() {
		assertType(Arrays.asList("lastnameGreaterThanEqual", "lastnameIsGreaterThanEqual"), GREATER_THAN_EQUAL, "lastname");
	}

	@Test
	void returnsAllParts() {

		PartTree tree = partTree("findByLastnameAndFirstname");
		assertPart(tree, parts("lastname", "firstname"));
	}

	@Test
	void returnsAllPartsOfType() {

		PartTree tree = partTree("findByLastnameAndFirstnameGreaterThan");

		Collection<Part> parts = toCollection(tree.getParts(Type.SIMPLE_PROPERTY));
		assertThat(parts).containsExactly(part("lastname"));

		parts = toCollection(tree.getParts(Type.GREATER_THAN));
		assertThat(parts).containsExactly(new Part("FirstnameGreaterThan", User.class));
	}

	@Test // DATACMNS-94
	void parsesExistsKeywordCorrectly() {
		assertType(Collections.singletonList("lastnameExists"), EXISTS, "lastname", 0, false);
	}

	@Test // DATACMNS-94
	void parsesRegexKeywordCorrectly() {
		assertType(asList("lastnameRegex", "lastnameMatchesRegex", "lastnameMatches"), REGEX, "lastname");
	}

	@Test // DATACMNS-107
	void parsesTrueKeywordCorrectly() {
		assertType(asList("activeTrue", "activeIsTrue"), TRUE, "active", 0, false);
	}

	@Test // DATACMNS-107
	void parsesFalseKeywordCorrectly() {
		assertType(asList("activeFalse", "activeIsFalse"), FALSE, "active", 0, false);
	}

	@Test // DATACMNS-111
	void parsesStartingWithKeywordCorrectly() {
		assertType(asList("firstnameStartsWith", "firstnameStartingWith", "firstnameIsStartingWith"), STARTING_WITH,
				"firstname");
	}

	@Test // DATACMNS-111
	void parsesEndingWithKeywordCorrectly() {
		assertType(asList("firstnameEndsWith", "firstnameEndingWith", "firstnameIsEndingWith"), ENDING_WITH, "firstname");
	}

	@Test // DATACMNS-111
	void parsesContainingKeywordCorrectly() {
		assertType(asList("firstnameIsContaining", "firstnameContains", "firstnameContaining"), CONTAINING, "firstname");
	}

	@Test // DATACMNS-141
	void parsesAfterKeywordCorrectly() {
		assertType(asList("birthdayAfter", "birthdayIsAfter"), Type.AFTER, "birthday");
	}

	@Test // DATACMNS-141
	void parsesBeforeKeywordCorrectly() {
		assertType(Arrays.asList("birthdayBefore", "birthdayIsBefore"), Type.BEFORE, "birthday");
	}

	@Test // DATACMNS-433
	void parsesLikeKeywordCorrectly() {
		assertType(asList("activeLike", "activeIsLike"), LIKE, "active");
	}

	@Test // DATACMNS-433
	void parsesNotLikeKeywordCorrectly() {
		assertType(asList("activeNotLike", "activeIsNotLike"), NOT_LIKE, "active");
	}

	@Test // DATACMNS-182
	void parsesContainingCorrectly() {

		PartTree tree = new PartTree("findAllByLegalNameContainingOrCommonNameContainingAllIgnoringCase",
				Organization.class);
		assertPart(tree, new Part[] { new Part("legalNameContaining", Organization.class, true) },
				new Part[] { new Part("commonNameContaining", Organization.class, true) });
	}

	@Test // DATACMNS-221
	void parsesSpecialCharactersCorrectly() {

		PartTree tree = new PartTree("findByØreAndÅrOrderByÅrAsc", DomainObjectWithSpecialChars.class);

		assertPart(tree, new Part[] { new Part("øre", DomainObjectWithSpecialChars.class),
				new Part("år", DomainObjectWithSpecialChars.class) });
		assertThat(tree.getSort().getOrderFor("år").isAscending()).isTrue();
	}

	@Test // DATACMNS-363
	void parsesSpecialCharactersOnlyCorrectly_Korean() {

		PartTree tree = new PartTree("findBy이름And생일OrderBy생일Asc", DomainObjectWithSpecialChars.class);

		assertPart(tree, new Part[] { new Part("이름", DomainObjectWithSpecialChars.class),
				new Part("생일", DomainObjectWithSpecialChars.class) });
		assertThat(tree.getSort().getOrderFor("생일").isAscending()).isTrue();
	}

	@Test // DATACMNS-363
	void parsesSpecialUnicodeCharactersMixedWithRegularCharactersCorrectly_Korean() {

		PartTree tree = new PartTree("findBy이름AndOrderIdOrderBy생일Asc", DomainObjectWithSpecialChars.class);

		assertPart(tree, new Part[] { new Part("이름", DomainObjectWithSpecialChars.class),
				new Part("order.id", DomainObjectWithSpecialChars.class) });
		assertThat(tree.getSort().getOrderFor("생일").isAscending()).isTrue();
	}

	@Test // DATACMNS-363
	void parsesNestedSpecialUnicodeCharactersMixedWithRegularCharactersCorrectly_Korean() {

		PartTree tree = new PartTree( //
				"findBy" + "이름" //
						+ "And" + "OrderId" //
						+ "And" + "Nested_이름" // we use _ here to mark the beginning of a new property reference "이름"
						+ "Or" + "NestedOrderId" //
						+ "OrderBy" + "생일" + "Asc",
				DomainObjectWithSpecialChars.class);

		Iterator<OrPart> parts = tree.iterator();
		assertPartsIn(parts.next(), new Part[] { //
				new Part("이름", DomainObjectWithSpecialChars.class), //
				new Part("order.id", DomainObjectWithSpecialChars.class), //
				new Part("nested.이름", DomainObjectWithSpecialChars.class) //
		});
		assertPartsIn(parts.next(), new Part[] { //
				new Part("nested.order.id", DomainObjectWithSpecialChars.class) //
		});

		assertThat(tree.getSort().getOrderFor("생일").isAscending()).isTrue();
	}

	@Test // DATACMNS-363
	void parsesNestedSpecialUnicodeCharactersMixedWithRegularCharactersCorrectly_KoreanNumbersSymbols() {

		PartTree tree = new PartTree( //
				"findBy" + "이름" //
						+ "And" + "OrderId" //
						+ "And" + "Anders" //
						+ "And" + "Property1" //
						+ "And" + "Øre" //
						+ "And" + "År" //
						+ "Or" + "NestedOrderId" //
						+ "And" + "Nested_property1" // we use _ here to mark the beginning of a new property reference "이름"
						+ "And" + "Property1" //
						+ "OrderBy" + "생일" + "Asc",
				DomainObjectWithSpecialChars.class);

		Iterator<OrPart> parts = tree.iterator();
		assertPartsIn(parts.next(), new Part[] { //
				new Part("이름", DomainObjectWithSpecialChars.class), //
				new Part("order.id", DomainObjectWithSpecialChars.class), //
				new Part("anders", DomainObjectWithSpecialChars.class), //
				new Part("property1", DomainObjectWithSpecialChars.class), //
				new Part("øre", DomainObjectWithSpecialChars.class), //
				new Part("år", DomainObjectWithSpecialChars.class) //
		});
		assertPartsIn(parts.next(), new Part[] { //
				new Part("nested.order.id", DomainObjectWithSpecialChars.class), //
				new Part("nested.property1", DomainObjectWithSpecialChars.class), //
				new Part("property1", DomainObjectWithSpecialChars.class) //
		});

		assertThat(tree.getSort().getOrderFor("생일").isAscending()).isTrue();
	}

	@Test // DATACMNS-303
	void identifiesSimpleCountByCorrectly() {

		PartTree tree = new PartTree("countByLastname", User.class);
		assertThat(tree.isCountProjection()).isTrue();
	}

	@Test // DATACMNS-875
	void identifiesSimpleExistsByCorrectly() {

		PartTree tree = new PartTree("existsByLastname", User.class);
		assertThat(tree.isExistsProjection()).isEqualTo(true);
	}

	@Test // DATACMNS-399
	void queryPrefixShouldBeSupportedInRepositoryQueryMethods() {

		PartTree tree = new PartTree("queryByFirstnameAndLastname", User.class);
		Iterable<Part> parts = tree.getParts();

		assertThat(parts).containsExactly(part("firstname"), part("lastname"));
	}

	@Test // DATACMNS-1645
	void searchPrefixShouldBeSupportedInRepositoryQueryMethods() {

		PartTree tree = new PartTree("searchByFirstnameAndLastname", User.class);
		Iterable<Part> parts = tree.getParts();

		assertThat(parts).containsExactly(part("firstname"), part("lastname"));
	}

	@Test // DATACMNS-303
	void identifiesExtendedCountByCorrectly() {

		PartTree tree = new PartTree("countUserByLastname", User.class);
		assertThat(tree.isCountProjection()).isTrue();
	}

	@Test // DATACMNS-303
	void identifiesCountAndDistinctByCorrectly() {

		PartTree tree = new PartTree("countDistinctUserByLastname", User.class);
		assertThat(tree.isCountProjection()).isTrue();
		assertThat(tree.isDistinct()).isTrue();
	}

	@Test // DATAJPA-324
	void resolvesPropertyPathFromGettersOnInterfaces() {
		assertThat(new PartTree("findByCategoryId", Product.class)).isNotNull();
	}

	@Test // DATACMNS-368
	void detectPropertyWithOrKeywordPart() {
		assertThat(new PartTree("findByOrder", Product.class)).isNotNull();
	}

	@Test // DATACMNS-368
	void detectPropertyWithAndKeywordPart() {
		assertThat(new PartTree("findByAnders", Product.class)).isNotNull();
	}

	@Test // DATACMNS-368
	void detectPropertyPathWithOrKeywordPart() {
		assertThat(new PartTree("findByOrderId", Product.class)).isNotNull();
	}

	@Test // DATACMNS-387
	void buildsPartTreeFromEmptyPredicateCorrectly() {

		PartTree tree = new PartTree("findAllByOrderByLastnameAsc", User.class);

		assertThat(tree.getParts()).isEmpty();
		assertThat(tree.getSort()).isEqualTo(Sort.by("lastname").ascending());
	}

	@Test // DATACMNS-448
	void identifiesSimpleDeleteByCorrectly() {

		PartTree tree = new PartTree("deleteByLastname", User.class);
		assertThat(tree.isDelete()).isTrue();
	}

	@Test // DATACMNS-448
	void identifiesExtendedDeleteByCorrectly() {

		PartTree tree = new PartTree("deleteUserByLastname", User.class);
		assertThat(tree.isDelete()).isTrue();
	}

	@Test // DATACMNS-448
	void identifiesSimpleRemoveByCorrectly() {

		PartTree tree = new PartTree("removeByLastname", User.class);
		assertThat(tree.isDelete()).isTrue();
	}

	@Test // DATACMNS-448
	void identifiesExtendedRemoveByCorrectly() {

		PartTree tree = new PartTree("removeUserByLastname", User.class);
		assertThat(tree.isDelete()).isTrue();
	}

	@Test // DATACMNS-516
	void disablesFindFirstKImplicitIfNotPresent() {
		assertLimiting("findByLastname", User.class, false, null);
	}

	@Test // DATACMNS-516
	void identifiesFindFirstImplicit() {
		assertLimiting("findFirstByLastname", User.class, true, 1);
	}

	@Test // DATACMNS-516
	void identifiesFindFirst1Explicit() {
		assertLimiting("findFirstByLastname", User.class, true, 1);
	}

	@Test // DATACMNS-516
	void identifiesFindFirstKExplicit() {
		assertLimiting("findFirst10ByLastname", User.class, true, 10);
	}

	@Test // DATACMNS-516
	void identifiesFindFirstKUsersExplicit() {
		assertLimiting("findFirst10UsersByLastname", User.class, true, 10);
	}

	@Test // DATACMNS-516
	void identifiesFindFirstKDistinctUsersExplicit() {
		assertLimiting("findFirst10DistinctUsersByLastname", User.class, true, 10, true);
		assertLimiting("findDistinctFirst10UsersByLastname", User.class, true, 10, true);
		assertLimiting("findFirst10UsersDistinctByLastname", User.class, true, 10, true);
	}

	@Test // DATACMNS-516
	void identifiesFindTopImplicit() {
		assertLimiting("findTopByLastname", User.class, true, 1);
	}

	@Test // DATACMNS-516
	void identifiesFindTop1Explicit() {
		assertLimiting("findTop1ByLastname", User.class, true, 1);
	}

	@Test // DATACMNS-516
	void identifiesFindTopKExplicit() {
		assertLimiting("findTop10ByLastname", User.class, true, 10);
	}

	@Test // DATACMNS-516
	void identifiesFindTopKUsersExplicit() {
		assertLimiting("findTop10UsersByLastname", User.class, true, 10);
	}

	@Test // DATACMNS-516
	void identifiesFindTopKDistinctUsersExplicit() {
		assertLimiting("findTop10DistinctUsersByLastname", User.class, true, 10, true);
		assertLimiting("findDistinctTop10UsersByLastname", User.class, true, 10, true);
		assertLimiting("findTop10UsersDistinctByLastname", User.class, true, 10, true);
	}

	@Test
	void shouldNotSupportLimitingCountQueries() {
		assertLimiting("countFirst10DistinctUsersByLastname", User.class, false, null, true);
		assertLimiting("countTop10DistinctUsersByLastname", User.class, false, null, true);
	}

	@Test // DATACMNS-875
	void shouldNotSupportLimitingExistQueries() {

		assertLimiting("existsFirst10DistinctUsersByLastname", User.class, false, null, true);
		assertLimiting("existsTop10DistinctUsersByLastname", User.class, false, null, true);
	}

	@Test // DATACMNS-581
	void parsesIsNotContainingCorrectly() throws Exception {
		assertType(asList("firstnameIsNotContaining", "firstnameNotContaining", "firstnameNotContains"), NOT_CONTAINING,
				"firstname");
	}

	@Test // DATACMNS-581
	void buildsPartTreeForNotContainingCorrectly() throws Exception {

		PartTree tree = new PartTree("findAllByLegalNameNotContaining", Organization.class);
		assertPart(tree, new Part[] { new Part("legalNameNotContaining", Organization.class) });
	}

	@Test // DATACMNS-750
	void doesNotFailOnPropertiesContainingAKeyword() {

		PartTree partTree = new PartTree("findBySomeInfoIn", Category.class);

		Iterable<Part> parts = partTree.getParts();

		assertThat(parts).hasSize(1);

		Part part = parts.iterator().next();

		assertThat(part.getType()).isEqualTo(Type.IN);
		assertThat(part.getProperty()).isEqualTo(PropertyPath.from("someInfo", Category.class));
	}

	@Test // DATACMNS-1007
	void parsesEmptyKeywordCorrectly() {
		assertType(asList("friendsIsEmpty", "friendsEmpty"), IS_EMPTY, "friends", 0, false);
	}

	@Test // DATACMNS-1007
	void parsesNotEmptyKeywordCorrectly() {
		assertType(asList("friendsIsNotEmpty", "friendsNotEmpty"), IS_NOT_EMPTY, "friends", 0, false);
	}

	@Test // DATACMNS-1007
	void parsesEmptyAsPropertyIfDifferentKeywordIsUsed() {

		assertType(asList("emptyIsTrue"), TRUE, "empty", 0, false);
		assertType(asList("emptyIs"), SIMPLE_PROPERTY, "empty", 1, true);
	}

	@Test // DATACMNS-1129
	void emptyTreeDoesNotContainParts() {

		PartTree tree = partTree("");

		assertThat(tree).isEmpty();
		assertThat(tree.hasPredicate()).isFalse();

		tree = partTree("findByOrderByFirstname");

		assertThat(tree).isEmpty();
		assertThat(tree.hasPredicate()).isFalse();
	}

	/**
	 * This test does not verify a desired behaviour but documents a limitation. If it starts failing and everything else
	 * is green, remove the expectation to fail with an exception.
	 */
	@Test // DATACMNS-1570
	void specialCapitalizationInSubject() {

		assertThatThrownBy(() -> new PartTree("findByZIndex", SpecialCapitalization.class))
				.isInstanceOf(PropertyReferenceException.class);
	}

	/**
	 * This test does not verify a desired behaviour but documents a limitation. If it starts failing and everything else
	 * is green, remove the expectation to fail with an exception.
	 */
	@Test // DATACMNS-1570
	void specialCapitalizationInOrderBy() {

		assertThatThrownBy(() -> new PartTree("findByOrderByZIndex", SpecialCapitalization.class))
				.isInstanceOf(PropertyReferenceException.class);
	}

	@Test // DATACMNS-1570
	void allCapsInSubject() {

		PartTree tree = new PartTree("findByURL", SpecialCapitalization.class);

		assertThat(tree).hasSize(1);
	}

	@Test // DATACMNS-1570
	void allCapsInOrderBy() {

		PartTree tree = new PartTree("findByOrderByURL", SpecialCapitalization.class);

		assertThat(tree.getSort()).hasSize(1);
	}

	private static void assertLimiting(String methodName, Class<?> entityType, boolean limiting, Integer maxResults) {
		assertLimiting(methodName, entityType, limiting, maxResults, false);
	}

	private static void assertLimiting(String methodName, Class<?> entityType, boolean limiting, Integer maxResults,
			boolean distinct) {

		PartTree tree = new PartTree(methodName, entityType);

		assertThat(tree.isLimiting()).isEqualTo(limiting);
		assertThat(tree.getMaxResults()).isEqualTo(maxResults);
		assertThat(tree.isDistinct()).isEqualTo(distinct);
	}

	private static void assertType(Iterable<String> sources, Type type, String property) {
		assertType(sources, type, property, 1, true);
	}

	private static void assertType(Iterable<String> sources, Type type, String property, int numberOfArguments,
			boolean parameterRequired) {

		for (String source : sources) {
			Part part = part(source);
			assertThat(part.getType()).isEqualTo(type);
			assertThat(part.getProperty()).isEqualTo(newProperty(property));
			assertThat(part.getNumberOfArguments()).isEqualTo(numberOfArguments);
			assertThat(part.isParameterRequired()).isEqualTo(parameterRequired);
		}
	}

	private static PartTree partTree(String source) {
		return new PartTree(source, User.class);
	}

	private static Part part(String part) {
		return new Part(part, User.class);
	}

	private static Part[] parts(String... part) {
		Part[] parts = new Part[part.length];
		for (int i = 0; i < parts.length; i++) {
			parts[i] = part(part[i]);
		}
		return parts;
	}

	private static PropertyPath newProperty(String name) {
		return PropertyPath.from(name, User.class);
	}

	private void assertPart(PartTree tree, Part[]... parts) {

		Iterator<OrPart> orParts = tree.iterator();

		for (Part[] part : parts) {
			assertThat(orParts.hasNext()).isTrue();
			assertPartsIn(orParts.next(), part);
		}

		assertThat(orParts.hasNext()).isFalse();
	}

	private void assertPartsIn(OrPart orPart, Part[] part) {

		Iterator<Part> partIterator = orPart.iterator();

		for (int k = 0; k < part.length; k++) {
			assertThat(partIterator.hasNext()).as("Expected %d parts but have %d", part.length, k).isTrue();
			Part next = partIterator.next();
			assertThat(part[k]).as("Expected %s but got %s!", part[k], next).isEqualTo(next);
		}

		assertThat(partIterator.hasNext()).as("Too many parts!").isFalse();
	}

	private static <T> Collection<T> toCollection(Iterable<T> iterable) {

		List<T> result = new ArrayList<>();
		for (T element : iterable) {
			result.add(element);
		}
		return result;
	}

	class User {
		String firstname;
		String lastname;
		double[] location;
		boolean active;
		Date birthday;
		List<User> friends;
		boolean empty;
	}

	class Organization {

		String commonName;
		String legalName;
	}

	class DomainObjectWithSpecialChars {
		String øre;
		String år;

		String 생일; // Birthday
		String 이름; // Name

		int property1;

		Order order;

		Anders anders;

		DomainObjectWithSpecialChars nested;
	}

	interface Product {

		Order getOrder(); // contains Or keyword

		Anders getAnders(); // constains And keyword

		Category getCategory();
	}

	interface Category {

		Long getId();

		String getSomeInfo();
	}

	interface Order {

		Long getId();
	}

	interface Anders {

		Long getId();
	}

	static class SpecialCapitalization {
		int zIndex;
		int URL;

		int getZIndex() {
			return zIndex;
		}

		int getURL() {
			return URL;
		}
	}
}
