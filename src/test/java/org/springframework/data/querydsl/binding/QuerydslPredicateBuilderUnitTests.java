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
package org.springframework.data.querydsl.binding;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.text.ParseException;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.querydsl.QSpecialUser;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.QUserWrapper;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.Users;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Version;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.querydsl.collections.CollQueryFactory;
import com.querydsl.core.types.Constant;
import com.querydsl.core.types.Predicate;

/**
 * Unit tests for {@link QuerydslPredicateBuilder}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class QuerydslPredicateBuilderUnitTests {

	static final ClassTypeInformation<User> USER_TYPE = ClassTypeInformation.from(User.class);
	static final QuerydslBindings DEFAULT_BINDINGS = new QuerydslBindings();

	QuerydslPredicateBuilder builder;
	MultiValueMap<String, String> values;

	@BeforeEach
	void setUp() {
		this.builder = new QuerydslPredicateBuilder(new DefaultFormattingConversionService(),
				SimpleEntityPathResolver.INSTANCE);
		this.values = new LinkedMultiValueMap<>();
	}

	@Test // DATACMNS-669
	void rejectsNullConversionService() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new QuerydslPredicateBuilder(null, SimpleEntityPathResolver.INSTANCE));
	}

	@Test // DATACMNS-669
	void getPredicateShouldThrowErrorWhenBindingContextIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> builder.getPredicate(null, values, null));
	}

	@Test // DATACMNS-669, DATACMNS-1168
	void getPredicateShouldReturnNullWhenPropertiesAreEmpty() {
		assertThat(builder.getPredicate(ClassTypeInformation.OBJECT, values, DEFAULT_BINDINGS)).isNull();
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		assumeThat(Version.javaVersion().toString())
				.as("QueryDSL isn't Java 11 ready https://github.com/querydsl/querydsl/issues/2151").startsWith("1.8");

		values.add("firstname", "Oliver");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("Oliver"));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).fetchResults().getResults();

		assertThat(result).containsExactly(Users.OLIVER);
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldCreateNestedStringParameterPredicateCorrectly() throws Exception {

		assumeThat(Version.javaVersion().toString())
				.as("QueryDSL isn't Java 11 ready https://github.com/querydsl/querydsl/issues/2151").startsWith("1.8");

		values.add("address.city", "Linz");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.address.city.eq("Linz"));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).fetchResults().getResults();

		assertThat(result).containsExactly(Users.CHRISTOPH);
	}

	@Test // DATACMNS-669
	void ignoresNonDomainTypeProperties() {

		values.add("firstname", "rand");
		values.add("lastname".toUpperCase(), "al'thor");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand"));
	}

	@Test // DATACMNS-669
	void forwardsNullForEmptyParameterToSingleValueBinder() {

		values.add("lastname", null);

		QuerydslBindings bindings = new QuerydslBindings();
		bindings.bind(QUser.user.lastname).firstOptional((path, value) -> value.map(path::contains));

		builder.getPredicate(USER_TYPE, values, bindings);
	}

	@Test // DATACMNS-734
	@SuppressWarnings("unchecked")
	void resolvesCommaSeparatedArgumentToArrayCorrectly() {

		values.add("address.lonLat", "40.740337,-73.995146");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		Constant<Object> constant = (Constant<Object>) ((List<?>) getField(getField(predicate, "mixin"), "args")).get(1);

		assertThat(constant.getConstant()).isEqualTo(new Double[] { 40.740337D, -73.995146D });
	}

	@Test // DATACMNS-734
	@SuppressWarnings("unchecked")
	void leavesCommaSeparatedArgumentUntouchedWhenTargetIsNotAnArray() {

		values.add("address.city", "rivers,two");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		Constant<Object> constant = (Constant<Object>) ((List<?>) getField(getField(predicate, "mixin"), "args")).get(1);

		assertThat(constant.getConstant()).isEqualTo("rivers,two");
	}

	@Test // DATACMNS-734
	void bindsDateCorrectly() throws ParseException {

		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		String date = format.print(new DateTime());

		values.add("dateOfBirth", format.print(new DateTime()));

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.dateOfBirth.eq(format.parseDateTime(date).toDate()));
	}

	@Test // DATACMNS-883
	void automaticallyInsertsAnyStepInCollectionReference() {

		values.add("addresses.street", "VALUE");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.addresses.any().street.eq("VALUE"));
	}

	@Test // DATACMNS-941
	void buildsPredicateForBindingUsingDowncast() {

		values.add("specialProperty", "VALUE");

		QuerydslBindings bindings = new QuerydslBindings();
		bindings.bind(QUser.user.as(QSpecialUser.class).specialProperty)//
				.first(QuerydslBindingsUnitTests.ContainsBinding.INSTANCE);

		assertThat(builder.getPredicate(USER_TYPE, values, bindings))//
				.isEqualTo(QUser.user.as(QSpecialUser.class).specialProperty.contains("VALUE"));
	}

	@Test // DATACMNS-941
	void buildsPredicateForBindingUsingNestedDowncast() {

		values.add("user.specialProperty", "VALUE");

		QUserWrapper $ = QUserWrapper.userWrapper;

		QuerydslBindings bindings = new QuerydslBindings();
		bindings.bind($.user.as(QSpecialUser.class).specialProperty)//
				.first(QuerydslBindingsUnitTests.ContainsBinding.INSTANCE);

		assertThat(builder.getPredicate(USER_TYPE, values, bindings))//
				.isEqualTo($.user.as(QSpecialUser.class).specialProperty.contains("VALUE"));
	}

	@Test // DATACMNS-1443
	void doesNotDropValuesContainingABlank() {

		values.add("firstname", " ");

		assertThat(builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS)) //
				.isEqualTo(QUser.user.firstname.eq(" "));
	}

	@Test // DATACMNS-1443
	void dropsValuesContainingAnEmptyString() {

		values.add("firstname", "");

		assertThat(builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS)).isNull();
	}
}
