/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.text.ParseException;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.querydsl.QSpecialUser;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.QUserWrapper;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.Users;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.querydsl.collections.CollQueryFactory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Constant;
import com.querydsl.core.types.Predicate;

/**
 * Unit tests for {@link QuerydslPredicateBuilder}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
public class QuerydslPredicateBuilderUnitTests {

	static final ClassTypeInformation<User> USER_TYPE = ClassTypeInformation.from(User.class);
	static final QuerydslBindings DEFAULT_BINDINGS = new QuerydslBindings();

	QuerydslPredicateBuilder builder;
	MultiValueMap<String, String> values;

	@Before
	public void setUp() {
		this.builder = new QuerydslPredicateBuilder(new DefaultFormattingConversionService(),
				SimpleEntityPathResolver.INSTANCE);
		this.values = new LinkedMultiValueMap<>();
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-669
	public void rejectsNullConversionService() {
		new QuerydslPredicateBuilder(null, SimpleEntityPathResolver.INSTANCE);
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-669
	public void getPredicateShouldThrowErrorWhenBindingContextIsNull() {
		builder.getPredicate(null, values, null);
	}

	@Test // DATACMNS-669, DATACMNS-1168
	public void getPredicateShouldReturnEmptyPredicateWhenPropertiesAreEmpty() {
		assertThat(builder.getPredicate(ClassTypeInformation.OBJECT, values, DEFAULT_BINDINGS))
				.isEqualTo(new BooleanBuilder());
	}

	@Test // DATACMNS-669
	public void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		values.add("firstname", "Oliver");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo((Predicate) QUser.user.firstname.eq("Oliver"));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).fetchResults().getResults();

		assertThat(result).containsExactly(Users.OLIVER);
	}

	@Test // DATACMNS-669
	public void resolveArgumentShouldCreateNestedStringParameterPredicateCorrectly() throws Exception {

		values.add("address.city", "Linz");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.address.city.eq("Linz"));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).fetchResults().getResults();

		assertThat(result).containsExactly(Users.CHRISTOPH);
	}

	@Test // DATACMNS-669
	public void ignoresNonDomainTypeProperties() {

		values.add("firstname", "rand");
		values.add("lastname".toUpperCase(), "al'thor");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand"));
	}

	@Test // DATACMNS-669
	public void forwardsNullForEmptyParameterToSingleValueBinder() {

		values.add("lastname", null);

		QuerydslBindings bindings = new QuerydslBindings();
		bindings.bind(QUser.user.lastname).firstOptional((path, value) -> value.map(path::contains));

		builder.getPredicate(USER_TYPE, values, bindings);
	}

	@Test // DATACMNS-734
	@SuppressWarnings("unchecked")
	public void resolvesCommaSeparatedArgumentToArrayCorrectly() {

		values.add("address.lonLat", "40.740337,-73.995146");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		Constant<Object> constant = (Constant<Object>) ((List<?>) getField(getField(predicate, "mixin"), "args")).get(1);

		assertThat(constant.getConstant()).isEqualTo(new Double[] { 40.740337D, -73.995146D });
	}

	@Test // DATACMNS-734
	@SuppressWarnings("unchecked")
	public void leavesCommaSeparatedArgumentUntouchedWhenTargetIsNotAnArray() {

		values.add("address.city", "rivers,two");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		Constant<Object> constant = (Constant<Object>) ((List<?>) getField(getField(predicate, "mixin"), "args")).get(1);

		assertThat(constant.getConstant()).isEqualTo("rivers,two");
	}

	@Test // DATACMNS-734
	public void bindsDateCorrectly() throws ParseException {

		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		String date = format.print(new DateTime());

		values.add("dateOfBirth", format.print(new DateTime()));

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.dateOfBirth.eq(format.parseDateTime(date).toDate()));
	}

	@Test // DATACMNS-883
	public void automaticallyInsertsAnyStepInCollectionReference() {

		values.add("addresses.street", "VALUE");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.addresses.any().street.eq("VALUE"));
	}

	@Test // DATACMNS-941
	public void buildsPredicateForBindingUsingDowncast() {

		values.add("specialProperty", "VALUE");

		QuerydslBindings bindings = new QuerydslBindings();
		bindings.bind(QUser.user.as(QSpecialUser.class).specialProperty)//
				.first(QuerydslBindingsUnitTests.ContainsBinding.INSTANCE);

		assertThat(builder.getPredicate(USER_TYPE, values, bindings))//
				.isEqualTo(QUser.user.as(QSpecialUser.class).specialProperty.contains("VALUE"));
	}

	@Test // DATACMNS-941
	public void buildsPredicateForBindingUsingNestedDowncast() {

		values.add("user.specialProperty", "VALUE");

		QUserWrapper $ = QUserWrapper.userWrapper;

		QuerydslBindings bindings = new QuerydslBindings();
		bindings.bind($.user.as(QSpecialUser.class).specialProperty)//
				.first(QuerydslBindingsUnitTests.ContainsBinding.INSTANCE);

		assertThat(builder.getPredicate(USER_TYPE, values, bindings))//
				.isEqualTo($.user.as(QSpecialUser.class).specialProperty.contains("VALUE"));
	}
}
