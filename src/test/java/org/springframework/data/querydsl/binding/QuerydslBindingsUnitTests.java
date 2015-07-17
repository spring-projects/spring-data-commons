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
package org.springframework.data.querydsl.binding;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.binding.MultiValueBinding;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.querydsl.binding.SingleValueBinding;
import org.springframework.test.util.ReflectionTestUtils;

import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.StringPath;

/**
 * Unit tests for {@link QuerydslBindings}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class QuerydslBindingsUnitTests {

	QuerydslPredicateBuilder builder;
	QuerydslBindings bindings;

	static final SingleValueBinding<StringPath, String> CONTAINS_BINDING = new SingleValueBinding<StringPath, String>() {

		@Override
		public Predicate bind(StringPath path, String value) {
			return path.contains(value);
		}
	};

	@Before
	public void setUp() {

		this.builder = new QuerydslPredicateBuilder(new DefaultConversionService(), SimpleEntityPathResolver.INSTANCE);
		this.bindings = new QuerydslBindings();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPath() {
		bindings.getBindingForPath(null);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void returnsNullIfNoBindingRegisteredForPath() {
		assertThat(bindings.getBindingForPath(PropertyPath.from("lastname", User.class)), nullValue());
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void returnsRegisteredBindingForSimplePath() {

		bindings.bind(QUser.user.firstname).first(CONTAINS_BINDING);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(PropertyPath.from("firstname", User.class)),
				CONTAINS_BINDING);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getBindingForPathShouldReturnSpeficicBindingForNestedElementsWhenAvailable() {

		bindings.bind(QUser.user.address.street).first(CONTAINS_BINDING);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(PropertyPath.from("address.street", User.class)),
				CONTAINS_BINDING);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void getBindingForPathShouldReturnSpeficicBindingForTypes() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(PropertyPath.from("address.street", User.class)),
				CONTAINS_BINDING);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void propertyNotExplicitlyIncludedAndWithoutTypeBindingIsInvisible() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		assertThat(bindings.getBindingForPath(PropertyPath.from("inceptionYear", User.class)), nullValue());
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void pathIsVisibleIfTypeBasedBindingWasRegistered() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		assertThat(bindings.isPathVisible(PropertyPath.from("inceptionYear", User.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void explicitlyIncludedPathIsVisible() {

		bindings.including(QUser.user.inceptionYear);

		assertThat(bindings.isPathVisible(PropertyPath.from("inceptionYear", User.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void notExplicitlyIncludedPathIsInvisible() {

		bindings.including(QUser.user.inceptionYear);

		assertThat(bindings.isPathVisible(PropertyPath.from("firstname", User.class)), is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void excludedPathIsInvisible() {

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathVisible(PropertyPath.from("inceptionYear", User.class)), is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void pathIsVisibleIfNotExplicitlyExcluded() {

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathVisible(PropertyPath.from("firstname", User.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void pathIsVisibleIfItsBothBlackAndWhitelisted() {

		bindings.excluding(QUser.user.firstname);
		bindings.including(QUser.user.firstname);

		assertThat(bindings.isPathVisible(PropertyPath.from("firstname", User.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void nestedPathIsInvisibleIfAParanetPathWasExcluded() {

		bindings.excluding(QUser.user.address);

		assertThat(bindings.isPathVisible(PropertyPath.from("address.city", User.class)), is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void pathIsVisibleIfConcretePathIsVisibleButParentExcluded() {

		bindings.excluding(QUser.user.address);
		bindings.including(QUser.user.address.city);

		assertThat(bindings.isPathVisible(PropertyPath.from("address.city", User.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathVisibleShouldReturnFalseWhenPartialPathContainedInExcludingAndConcretePathToDifferentPropertyIsIncluded() {

		bindings.excluding(QUser.user.address);
		bindings.including(QUser.user.address.city);

		assertThat(bindings.isPathVisible(PropertyPath.from("address.street", User.class)), is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void testname() {

		PropertyPath firstname = PropertyPath.from("firstname", User.class);
		PropertyPath lastname = PropertyPath.from("lastname", User.class);
		PropertyPath city = PropertyPath.from("address.city", User.class);
		PropertyPath street = PropertyPath.from("address.street", User.class);

		bindings.including(QUser.user.firstname, QUser.user.address.street);

		assertThat(bindings.isPathVisible(firstname), is(true));
		assertThat(bindings.isPathVisible(street), is(true));
		assertThat(bindings.isPathVisible(lastname), is(false));
		assertThat(bindings.isPathVisible(city), is(false));
	}

	private static <P extends Path<S>, S> void assertAdapterWithTargetBinding(MultiValueBinding<P, S> binding,
			SingleValueBinding<? extends Path<?>, ?> expected) {

		assertThat(binding, is(instanceOf(QuerydslBindings.MultiValueBindingAdapter.class)));
		assertThat(ReflectionTestUtils.getField(binding, "delegate"), is((Object) expected));
	}
}
