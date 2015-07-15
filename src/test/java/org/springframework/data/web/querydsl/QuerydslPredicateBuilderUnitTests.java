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

import static java.util.Collections.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.Users;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.collections.CollQueryFactory;
import com.mysema.query.types.Predicate;

/**
 * Unit tests for {@link QuerydslPredicateBuilder}.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
public class QuerydslPredicateBuilderUnitTests {

	static final QuerydslBindings DEFAULT_BINDINGS = new QuerydslBindings();

	QuerydslPredicateBuilder builder;

	@Before
	public void setUp() {
		builder = new QuerydslPredicateBuilder(new DefaultConversionService());
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
		builder.getPredicate(new MutablePropertyValues(), null, null);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getPredicateShouldReturnEmptyPredicateWhenPropertiesAreEmpty() {

		assertThat(builder.getPredicate(new MutablePropertyValues(), DEFAULT_BINDINGS, ClassTypeInformation.OBJECT),
				is((Predicate) new BooleanBuilder()));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		Predicate predicate = builder.getPredicate(new MutablePropertyValues(singletonMap("firstname", "Oliver")),
				DEFAULT_BINDINGS, ClassTypeInformation.from(User.class));

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

		Predicate predicate = builder.getPredicate(new MutablePropertyValues(singletonMap("address.city", "Linz")),
				DEFAULT_BINDINGS, ClassTypeInformation.from(User.class));

		assertThat(predicate, is((Predicate) QUser.user.address.city.eq("Linz")));

		List<User> result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).list(QUser.user);

		assertThat(result, hasSize(1));
		assertThat(result, hasItem(Users.CHRISTOPH));
	}

	@Test
	public void ignoresNonDomainTypeProperties() {

		MultiValueMap<String, String> values = new LinkedMultiValueMap<String, String>();
		values.add("firstname", "rand");
		values.add("lastname".toUpperCase(), "al'thor");

		Predicate predicate = builder.getPredicate(new MutablePropertyValues(values), new QuerydslBindings(),
				ClassTypeInformation.from(User.class));

		assertThat(predicate, is((Predicate) QUser.user.firstname.eq("rand")));
	}
}
