/*
 * Copyright 2015-2016 the original author or authors.
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
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.Users;
import org.springframework.data.util.ClassTypeInformation;
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
		this.values = new LinkedMultiValueMap<String, String>();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullConversionService() {
		new QuerydslPredicateBuilder(null, SimpleEntityPathResolver.INSTANCE);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getPredicateShouldThrowErrorWhenBindingContextIsNull() {
		builder.getPredicate(null, values, null);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getPredicateShouldReturnEmptyPredicateWhenPropertiesAreEmpty() {
		assertThat(builder.getPredicate(ClassTypeInformation.OBJECT, values, DEFAULT_BINDINGS)).isNull();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		values.add("firstname", "Oliver");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo((Predicate) QUser.user.firstname.eq("Oliver"));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).fetchResults().getResults();

		assertThat(result).containsExactly(Users.OLIVER);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateNestedStringParameterPredicateCorrectly() throws Exception {

		values.add("address.city", "Linz");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.address.city.eq("Linz"));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).fetchResults().getResults();

		assertThat(result).containsExactly(Users.CHRISTOPH);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void ignoresNonDomainTypeProperties() {

		values.add("firstname", "rand");
		values.add("lastname".toUpperCase(), "al'thor");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand"));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void forwardsNullForEmptyParameterToSingleValueBinder() {

		values.add("lastname", null);

		QuerydslBindings bindings = new QuerydslBindings();
		bindings.bind(QUser.user.lastname).firstOptional((path, value) -> value.map(it -> path.contains(it)));

		builder.getPredicate(USER_TYPE, values, bindings);
	}

	/**
	 * @see DATACMNS-734
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void resolvesCommaSeparatedArgumentToArrayCorrectly() {

		values.add("address.lonLat", "40.740337,-73.995146");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		Constant<Object> constant = (Constant<Object>) ((List<?>) getField(getField(predicate, "mixin"), "args")).get(1);

		assertThat(constant.getConstant()).isEqualTo(new Double[] { 40.740337D, -73.995146D });
	}

	/**
	 * @see DATACMNS-734
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void leavesCommaSeparatedArgumentUntouchedWhenTargetIsNotAnArray() {

		values.add("address.city", "rivers,two");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		Constant<Object> constant = (Constant<Object>) ((List<?>) getField(getField(predicate, "mixin"), "args")).get(1);

		assertThat(constant.getConstant()).isEqualTo("rivers,two");
	}

	/**
	 * @see DATACMNS-734
	 */
	@Test
	public void bindsDateCorrectly() throws ParseException {

		DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
		String date = format.print(new DateTime());

		values.add("dateOfBirth", format.print(new DateTime()));

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.dateOfBirth.eq(format.parseDateTime(date).toDate()));
	}

	/**
	 * @see DATACMNS-883
	 */
	@Test
	public void automaticallyInsertsAnyStepInCollectionReference() {

		values.add("addresses.street", "VALUE");

		Predicate predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.addresses.any().street.eq("VALUE"));
	}
}
