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
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.Address;
import org.springframework.data.querydsl.QAddress;
import org.springframework.data.querydsl.User;
import org.springframework.data.util.ClassTypeInformation;

import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.StringPath;

/**
 * Unit tests for {@link QuerydslBindings}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class QuerydslBindingsUnitTests {

	private QuerydslPredicateBuilder builder;
	private QuerydslBindings typeBasedBindings;
	private QuerydslBindings pathBasedBindings;

	@Before
	public void setUp() {
		builder = new QuerydslPredicateBuilder();

		typeBasedBindings = new QuerydslBindings() {
			{
				bind(String.class, new QuerydslBinding<StringPath>() {

					@Override
					public Predicate bind(StringPath path, Object value) {
						return path.contains(value.toString());
					}
				});
			}
		};

		pathBasedBindings = new QuerydslBindings() {
			{
				bind("address.street", new QuerydslBinding<StringPath>() {

					@Override
					public Predicate bind(StringPath path, Object value) {
						return path.contains(value.toString());
					}
				});

				bind("firstname", new QuerydslBinding<StringPath>() {

					@Override
					public Predicate bind(StringPath path, Object value) {
						return path.contains(value.toString());
					}
				});
			}
		};
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getBindingForPathShouldThrowErrorWhenPathIsNull() {
		pathBasedBindings.getBindingForPath(null);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getBindingForPathShouldReturnNullWhenNoSpecifcBindingAvailable() {
		assertThat(pathBasedBindings.getBindingForPath(PropertyPath.from("lastname", User.class)), nullValue());
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getBindingForPathShouldReturnSpeficicBindingWhenAvailable() {
		assertThat(pathBasedBindings.getBindingForPath(PropertyPath.from("firstname", User.class)), notNullValue());
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getBindingForPathShouldReturnSpeficicBindingForNestedElementsWhenAvailable() {
		assertThat(pathBasedBindings.getBindingForPath(PropertyPath.from("address.street", User.class)), notNullValue());
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getBindingForPathShouldReturnSpeficicBindingForTypes() {
		assertThat(typeBasedBindings.getBindingForPath(PropertyPath.from("address.street", User.class)), notNullValue());
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getBindingForPathShouldIgnoreSpeficicBindingForTypesWhenTypesDoNotMatch() {
		assertThat(typeBasedBindings.getBindingForPath(PropertyPath.from("inceptionYear", User.class)), nullValue());
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnTrueWhenNoRestrictionDefined() {
		assertThat(typeBasedBindings.isPathVisible(PropertyPath.from("inceptionYear", User.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnTrueWhenPathContainedInIncluding() {

		assertThat(
				new QuerydslBindings().including("inceptionYear").isPathVisible(PropertyPath.from("inceptionYear", User.class)),
				is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnFalseWhenPathNotContainedInIncluding() {

		assertThat(
				new QuerydslBindings().including("inceptionYear").isPathVisible(PropertyPath.from("firstname", User.class)),
				is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnFalseWhenPathContainedInExcluding() {

		assertThat(
				new QuerydslBindings().excluding("inceptionYear").isPathVisible(PropertyPath.from("inceptionYear", User.class)),
				is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnTrueWhenPathNotContainedInExcluding() {

		assertThat(
				new QuerydslBindings().excluding("inceptionYear").isPathVisible(PropertyPath.from("firstname", User.class)),
				is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnTrueWhenPathContainedInExcludingAndIncluding() {

		assertThat(
				new QuerydslBindings().excluding("inceptionYear").isPathVisible(PropertyPath.from("firstname", User.class)),
				is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnFalseWhenPartialPathContainedInExcluding() {

		assertThat(
				new QuerydslBindings().excluding("address").isPathVisible(PropertyPath.from("address.city", User.class)),
				is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnTrueWhenPartialPathContainedInExcludingButConcretePathIsIncluded() {

		assertThat(
				new QuerydslBindings().excluding("address").including("address.city")
						.isPathVisible(PropertyPath.from("address.city", User.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnFalseWhenPartialPathContainedInExcludingAndConcretePathToDifferentPropertyIsIncluded() {

		assertThat(
				new QuerydslBindings().excluding("address").including("address.city")
						.isPathVisible(PropertyPath.from("address.street", User.class)), is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void usesTypeBasedBindingIfConfigured() {

		MutablePropertyValues values = new MutablePropertyValues(Collections.singletonMap("city", "Dresden"));

		QuerydslBindingContext context = new QuerydslBindingContext(ClassTypeInformation.from(Address.class),
				this.typeBasedBindings, null);

		assertThat(builder.getPredicate(values, context), is((Predicate) QAddress.address.city.contains("Dresden")));
	}
}
