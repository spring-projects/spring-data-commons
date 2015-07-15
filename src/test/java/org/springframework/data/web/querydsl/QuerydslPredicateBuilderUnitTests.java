/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.web.querydsl;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.Users;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mysema.query.collections.CollQueryFactory;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.StringPath;

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
		this.builder = new QuerydslPredicateBuilder(new DefaultConversionService());
		this.values = new LinkedMultiValueMap<String, String>();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullConversionService() {
		new QuerydslPredicateBuilder(null);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getPredicateShouldThrowErrorWhenBindingContextIsNull() {
		builder.getPredicate(values, null, null);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getPredicateShouldReturnEmptyPredicateWhenPropertiesAreEmpty() {

		assertThat(builder.getPredicate(values, DEFAULT_BINDINGS, ClassTypeInformation.OBJECT), is(nullValue()));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		values.add("firstname", "Oliver");

		Predicate predicate = builder.getPredicate(values, DEFAULT_BINDINGS, USER_TYPE);

		assertThat(predicate, is((Predicate) QUser.user.firstname.eq("Oliver")));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).list(QUser.user);

		assertThat(result, hasSize(1));
		assertThat(result, hasItem(Users.OLIVER));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateNestedStringParameterPredicateCorrectly() throws Exception {

		values.add("address.city", "Linz");

		Predicate predicate = builder.getPredicate(values, DEFAULT_BINDINGS, USER_TYPE);

		assertThat(predicate, is((Predicate) QUser.user.address.city.eq("Linz")));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).list(QUser.user);

		assertThat(result, hasSize(1));
		assertThat(result, hasItem(Users.CHRISTOPH));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void ignoresNonDomainTypeProperties() {

		values.add("firstname", "rand");
		values.add("lastname".toUpperCase(), "al'thor");

		Predicate predicate = builder.getPredicate(values, new QuerydslBindings(), USER_TYPE);

		assertThat(predicate, is((Predicate) QUser.user.firstname.eq("rand")));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void forwardsNullForEmptyParameterToSingleValueBinder() {

		values.add("lastname", null);

		QuerydslBindings bindings = new QuerydslBindings();
		bindings.bind(QUser.user.lastname).single(new SingleValueBinding<StringPath, String>() {

			@Override
			public Predicate bind(StringPath path, String value) {
				return value == null ? null : path.contains(value);
			}
		});

		builder.getPredicate(values, bindings, USER_TYPE);
	}
}
