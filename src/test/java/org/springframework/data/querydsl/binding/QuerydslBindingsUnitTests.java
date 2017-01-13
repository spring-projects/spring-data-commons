/*
 * Copyright 2015-2017 the original author or authors.
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
import org.springframework.data.querydsl.QSpecialUser;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.test.util.ReflectionTestUtils;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;

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

	@Test(expected = IllegalArgumentException.class) // DATACMNS-669
	public void rejectsNullPath() {
		bindings.getBindingForPath(null);
	}

	@Test // DATACMNS-669
	public void returnsNullIfNoBindingRegisteredForPath() {

		PathInformation path = PropertyPathInformation.of("lastname", User.class);

		assertThat(bindings.getBindingForPath(path), nullValue());
	}

	@Test // DATACMNS-669
	public void returnsRegisteredBindingForSimplePath() {

		bindings.bind(QUser.user.firstname).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("firstname", User.class);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	public void getBindingForPathShouldReturnSpeficicBindingForNestedElementsWhenAvailable() {

		bindings.bind(QUser.user.address.street).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("address.street", User.class);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	public void getBindingForPathShouldReturnSpeficicBindingForTypes() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("address.street", User.class);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	public void propertyNotExplicitlyIncludedAndWithoutTypeBindingIsInvisible() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("inceptionYear", User.class);

		assertThat(bindings.getBindingForPath(path), nullValue());
	}

	@Test // DATACMNS-669
	public void pathIsVisibleIfTypeBasedBindingWasRegistered() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class), is(true));
	}

	@Test // DATACMNS-669
	public void explicitlyIncludedPathIsVisible() {

		bindings.including(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class), is(true));
	}

	@Test // DATACMNS-669
	public void notExplicitlyIncludedPathIsInvisible() {

		bindings.including(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("firstname", User.class), is(false));
	}

	@Test // DATACMNS-669
	public void excludedPathIsInvisible() {

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class), is(false));
	}

	@Test // DATACMNS-669
	public void pathIsVisibleIfNotExplicitlyExcluded() {

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("firstname", User.class), is(true));
	}

	@Test // DATACMNS-669
	public void pathIsVisibleIfItsBothBlackAndWhitelisted() {

		bindings.excluding(QUser.user.firstname);
		bindings.including(QUser.user.firstname);

		assertThat(bindings.isPathAvailable("firstname", User.class), is(true));
	}

	@Test // DATACMNS-669
	public void nestedPathIsInvisibleIfAParanetPathWasExcluded() {

		bindings.excluding(QUser.user.address);

		assertThat(bindings.isPathAvailable("address.city", User.class), is(false));
	}

	@Test // DATACMNS-669
	public void pathIsVisibleIfConcretePathIsVisibleButParentExcluded() {

		bindings.excluding(QUser.user.address);
		bindings.including(QUser.user.address.city);

		assertThat(bindings.isPathAvailable("address.city", User.class), is(true));
	}

	@Test // DATACMNS-669
	public void isPathVisibleShouldReturnFalseWhenPartialPathContainedInExcludingAndConcretePathToDifferentPropertyIsIncluded() {

		bindings.excluding(QUser.user.address);
		bindings.including(QUser.user.address.city);

		assertThat(bindings.isPathAvailable("address.street", User.class), is(false));
	}

	@Test // DATACMNS-669
	public void whitelistsPropertiesCorrectly() {

		bindings.including(QUser.user.firstname, QUser.user.address.street);

		assertThat(bindings.isPathAvailable("firstname", User.class), is(true));
		assertThat(bindings.isPathAvailable("address.street", User.class), is(true));
		assertThat(bindings.isPathAvailable("lastname", User.class), is(false));
		assertThat(bindings.isPathAvailable("address.city", User.class), is(false));
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-787
	public void rejectsNullAlias() {
		bindings.bind(QUser.user.address).as(null);
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-787
	public void rejectsEmptyAlias() {
		bindings.bind(QUser.user.address).as("");
	}

	@Test // DATACMNS-787
	public void aliasesBinding() {

		bindings.bind(QUser.user.address.city).as("city").first(CONTAINS_BINDING);

		PathInformation path = bindings.getPropertyPath("city", ClassTypeInformation.from(User.class));

		assertThat(path, is(notNullValue()));
		assertThat(bindings.isPathAvailable("city", User.class), is(true));

		// Aliasing implicitly blacklists original path
		assertThat(bindings.isPathAvailable("address.city", User.class), is(false));
	}

	@Test // DATACMNS-787
	public void explicitlyIncludesOriginalBindingDespiteAlias() {

		bindings.including(QUser.user.address.city);
		bindings.bind(QUser.user.address.city).as("city").first(CONTAINS_BINDING);

		PathInformation path = bindings.getPropertyPath("city", ClassTypeInformation.from(User.class));

		assertThat(path, is(notNullValue()));
		assertThat(bindings.isPathAvailable("city", User.class), is(true));

		assertThat(bindings.isPathAvailable("address.city", User.class), is(true));

		PathInformation propertyPath = bindings.getPropertyPath("address.city", ClassTypeInformation.from(User.class));
		assertThat(propertyPath, is(notNullValue()));

		assertAdapterWithTargetBinding(bindings.getBindingForPath(propertyPath), CONTAINS_BINDING);
	}

	@Test // DATACMNS-787
	public void registedAliasWithNullBinding() {

		bindings.bind(QUser.user.address.city).as("city").withDefaultBinding();

		PathInformation path = bindings.getPropertyPath("city", ClassTypeInformation.from(User.class));
		assertThat(path, is(notNullValue()));

		MultiValueBinding<Path<? extends Object>, Object> binding = bindings.getBindingForPath(path);
		assertThat(binding, is(nullValue()));
	}

	@Test // DATACMNS-941
	public void registersBindingForPropertyOfSubtype() {

		bindings.bind(QUser.user.as(QSpecialUser.class).specialProperty).first(ContainsBinding.INSTANCE);

		assertThat(bindings.isPathAvailable("specialProperty", User.class), is(true));
	}

	private static <P extends Path<? extends S>, S> void assertAdapterWithTargetBinding(MultiValueBinding<P, S> binding,
			SingleValueBinding<? extends Path<?>, ?> expected) {

		assertThat(binding, is(instanceOf(QuerydslBindings.MultiValueBindingAdapter.class)));
		assertThat(ReflectionTestUtils.getField(binding, "delegate"), is((Object) expected));
	}

	enum ContainsBinding implements SingleValueBinding<StringPath, String> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.querydsl.binding.SingleValueBinding#bind(com.querydsl.core.types.Path, java.lang.Object)
		 */
		@Override
		public Predicate bind(StringPath path, String value) {
			return path.contains(value);
		}
	}
}
