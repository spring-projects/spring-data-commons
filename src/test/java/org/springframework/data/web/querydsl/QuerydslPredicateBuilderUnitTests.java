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

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.User;
import org.springframework.data.util.ClassTypeInformation;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.Predicate;

/**
 * @author Christoph Strobl
 */
public class QuerydslPredicateBuilderUnitTests {

	QuerydslPredicateBuilder builder;

	@Before
	public void setUp() {
		builder = new QuerydslPredicateBuilder();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getPredicateShouldThrowErrorWhenBindingContextIsNull() {
		builder.getPredicate(new MutablePropertyValues(), null);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getPredicateShouldReturnEmptyPredicateWhenPropertiesAreEmpty() {

		assertThat(builder.getPredicate(new MutablePropertyValues(), new QuerydslBindingContext(
				ClassTypeInformation.OBJECT, null, null)), is((Predicate) new BooleanBuilder()));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		Predicate predicate = builder.getPredicate(
				new MutablePropertyValues(Collections.singletonMap("firstname", new String[] { "rand" })),
				new QuerydslBindingContext(ClassTypeInformation.from(User.class), null, null));

		assertThat(predicate, Is.<Object> is(QUser.user.firstname.eq("rand")));
	}
}
