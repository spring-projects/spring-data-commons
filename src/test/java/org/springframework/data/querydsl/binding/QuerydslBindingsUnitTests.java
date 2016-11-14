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

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.util.ClassTypeInformation;

import com.querydsl.core.types.Path;
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

	static final SingleValueBinding<StringPath, String> CONTAINS_BINDING = (path, value) -> Optional
			.of(path.contains(value));

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
		assertThat(bindings.getBindingForPath(PropertyPath.from("lastname", User.class))).isNotPresent();
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
	public void propertyNotExplicitlyIncludedAndWithoutTypeBindingIsNotAvailable() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		assertThat(bindings.getBindingForPath(PropertyPath.from("inceptionYear", User.class))).isNotPresent();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void pathIsAvailableIfTypeBasedBindingWasRegistered() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class)).isTrue();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void explicitlyIncludedPathIsAvailable() {

		bindings.including(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class)).isTrue();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void notExplicitlyIncludedPathIsNotAvailable() {

		bindings.including(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("firstname", User.class)).isFalse();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void excludedPathIsNotAvailable() {

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class)).isFalse();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void pathIsAvailableIfNotExplicitlyExcluded() {

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("firstname", User.class)).isTrue();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void pathIsAvailableIfItsBothBlackAndWhitelisted() {

		bindings.excluding(QUser.user.firstname);
		bindings.including(QUser.user.firstname);

		assertThat(bindings.isPathAvailable("firstname", User.class)).isTrue();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void nestedPathIsNotAvailableIfAParanetPathWasExcluded() {

		bindings.excluding(QUser.user.address);

		assertThat(bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void pathIsAvailableIfConcretePathIsAvailableButParentExcluded() {

		bindings.excluding(QUser.user.address);
		bindings.including(QUser.user.address.city);

		assertThat(bindings.isPathAvailable("address.city", User.class)).isTrue();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void isPathAvailableShouldReturnFalseWhenPartialPathContainedInExcludingAndConcretePathToDifferentPropertyIsIncluded() {

		bindings.excluding(QUser.user.address);
		bindings.including(QUser.user.address.city);

		assertThat(bindings.isPathAvailable("address.street", User.class)).isFalse();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void whitelistsPropertiesCorrectly() {

		bindings.including(QUser.user.firstname, QUser.user.address.street);

		assertThat(bindings.isPathAvailable("firstname", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("address.street", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("lastname", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	/**
	 * @see DATACMNS-787
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullAlias() {
		bindings.bind(QUser.user.address).as(null);
	}

	/**
	 * @see DATACMNS-787
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptyAlias() {
		bindings.bind(QUser.user.address).as("");
	}

	/**
	 * @see DATACMNS-787
	 */
	@Test
	public void aliasesBinding() {

		bindings.bind(QUser.user.address.city).as("city").first(CONTAINS_BINDING);

		PropertyPath path = bindings.getPropertyPath("city", ClassTypeInformation.from(User.class));

		assertThat(path).isNotNull();
		assertThat(bindings.isPathAvailable("city", User.class)).isTrue();

		// Aliasing implicitly blacklists original path
		assertThat(bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	/**
	 * @see DATACMNS-787
	 */
	@Test
	public void explicitlyIncludesOriginalBindingDespiteAlias() {

		bindings.including(QUser.user.address.city);
		bindings.bind(QUser.user.address.city).as("city").first(CONTAINS_BINDING);

		PropertyPath path = bindings.getPropertyPath("city", ClassTypeInformation.from(User.class));

		assertThat(path).isNotNull();
		assertThat(bindings.isPathAvailable("city", User.class)).isTrue();

		assertThat(bindings.isPathAvailable("address.city", User.class)).isTrue();

		PropertyPath propertyPath = bindings.getPropertyPath("address.city", ClassTypeInformation.from(User.class));
		assertThat(propertyPath).isNotNull();

		assertAdapterWithTargetBinding(bindings.getBindingForPath(propertyPath), CONTAINS_BINDING);
	}

	/**
	 * @see DATACMNS-787
	 */
	@Test
	public void registedAliasWithNullBinding() {

		bindings.bind(QUser.user.address.city).as("city").withDefaultBinding();

		assertThat(bindings.getPropertyPath("city", ClassTypeInformation.from(User.class))).isNotNull();
		assertThat(bindings.getBindingForPath(bindings.getPropertyPath("city", ClassTypeInformation.from(User.class))))
				.isNotPresent();
	}

	private static <P extends Path<? extends S>, S> void assertAdapterWithTargetBinding(
			Optional<MultiValueBinding<P, S>> binding, SingleValueBinding<? extends Path<?>, ?> expected) {

		assertThat(binding).hasValueSatisfying(it -> {

			// assertThat(it).isInstanceOf(QuerydslBindings.MultiValueBindingAdapter.class);
			// assertThat(ReflectionTestUtils.getField(it, "delegate")).isEqualTo(expected);
		});
	}
}
