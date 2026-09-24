/*
 * Copyright 2015-present the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.querydsl.Address;
import org.springframework.data.querydsl.QAddress;
import org.springframework.data.querydsl.QSpecialUser;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;

/**
 * Unit tests for {@link QuerydslBindings}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class QuerydslBindingsUnitTests {

	QuerydslPredicateBuilder builder;
	QuerydslBindings bindings;

	static final SingleValueBinding<StringPath, String> CONTAINS_BINDING = StringExpression::contains;

	@BeforeEach
	void setUp() {

		this.builder = new QuerydslPredicateBuilder(new DefaultConversionService(), SimpleEntityPathResolver.INSTANCE);
		this.bindings = new QuerydslBindings();
	}

	@Test // DATACMNS-669
	void rejectsNullPath() {
		assertThatIllegalArgumentException().isThrownBy(() -> bindings.getBindingForPath(null));
	}

	@Test // GH-3547
	void explicitBindNotHiddenByDefaultExclusion() {

		bindings.excludeUnlistedProperties(true);
		bindings.bind(QUser.user.firstname).first(CONTAINS_BINDING);

		assertThat(bindings.isPathAvailable("User.firstname", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("firstname", User.class)).isTrue();
	}

	@Test // DATACMNS-669
	void returnsEmptyOptionalIfNoBindingRegisteredForPath() {

		PathInformation path = PropertyPathInformation.of("lastname", User.class);

		assertThat(bindings.getBindingForPath(path)).isEmpty();
	}

	@Test // GH-2418
	void shouldConsiderOwningTypeWhenObtainingBindings() {

		bindings.bind(QUser.user.description).first(CONTAINS_BINDING);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(PropertyPathInformation.of("description", User.class)),
				CONTAINS_BINDING);
		assertThat(bindings.getBindingForPath(PropertyPathInformation.of("address.description", User.class))).isEmpty();
		assertThat(bindings.getBindingForPath(PropertyPathInformation.of("description", Address.class))).isEmpty();
	}

	@Test // GH-2418
	void shouldConsiderOwningTypeWhenObtainingBindingsWithQuerydslPathInformation() {

		bindings.bind(QUser.user.description).first(CONTAINS_BINDING);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(QuerydslPathInformation.of(QUser.user.description)),
				CONTAINS_BINDING);
		assertThat(bindings.getBindingForPath(QuerydslPathInformation.of(QAddress.address.description))).isEmpty();
	}

	@Test // DATACMNS-669
	void returnsRegisteredBindingForSimplePath() {

		bindings.bind(QUser.user.firstname).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("firstname", User.class);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	void getBindingForPathShouldReturnSpecificBindingForNestedElementsWhenAvailable() {

		bindings.bind(QUser.user.address.street).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("address.street", User.class);

		assertAdapterWithTargetBinding(bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	void getBindingForPathShouldReturnSpeficicBindingForTypes() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("address.street", User.class);
		assertAdapterWithTargetBinding(bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	void propertyNotExplicitlyIncludedAndWithoutTypeBindingIsNotAvailable() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("inceptionYear", User.class);

		assertThat(bindings.getBindingForPath(path)).isEmpty();
	}

	@Test // DATACMNS-669
	void pathIsAvailableIfTypeBasedBindingWasRegistered() {

		bindings.bind(String.class).first(CONTAINS_BINDING);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class)).isTrue();
	}

	@Test // DATACMNS-669
	void explicitlyIncludedPathIsAvailable() {

		bindings.including(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class)).isTrue();
	}

	@Test // DATACMNS-669
	void notExplicitlyIncludedPathIsNotAvailable() {

		bindings.including(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("firstname", User.class)).isFalse();
	}

	@Test // DATACMNS-669
	void excludedPathIsNotAvailable() {

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class)).isFalse();
	}

	@Test // DATACMNS-669
	void pathIsAvailableIfNotExplicitlyExcluded() {

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("firstname", User.class)).isTrue();
	}

	@Test // DATACMNS-669, DATACMNS-1744
	void pathIsAvailableIfItsBothDeniedAndAllowed() {

		bindings.excluding(QUser.user.firstname);
		bindings.including(QUser.user.firstname);

		assertThat(bindings.isPathAvailable("firstname", User.class)).isTrue();
	}

	@Test // DATACMNS-669, GH-3547
	void nestedPathIsNotAvailableIfParentPathWasExcluded() {

		bindings.excluding(QUser.user.address);

		assertThat(bindings.isPathAvailable("address.city", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("address_city", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("addressCity", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("User.address.city", User.class)).isFalse();
	}

	@Test // GH-3547
	void nestedPathIsNotAvailableUsingNonCanonicalNotationIfExcluded() {

		bindings.excluding(QUser.user.address.city);

		assertThat(bindings.isPathAvailable("address_city", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("addressCity", User.class)).isFalse();
	}

	@Test // GH-3547
	void aliasingHidesOriginalPathUsingNonCanonicalNotation() {

		bindings.bind(QUser.user.address.city).as("city").withDefaultBinding();

		assertThat(bindings.isPathAvailable("city", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("address.city", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("address_city", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("addressCity", User.class)).isFalse();
	}

	@Test // DATACMNS-669
	void pathIsAvailableIfConcretePathIsAvailableButParentExcluded() {

		bindings.excluding(QUser.user.address);
		bindings.including(QUser.user.address.city);

		assertThat(bindings.isPathAvailable("address.city", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("address_city", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("addressCity", User.class)).isTrue();
	}

	@Test // DATACMNS-669, GH-3547
	void isPathAvailableShouldReturnFalseWhenPartialPathContainedInExcludingAndConcretePathToDifferentPropertyIsIncluded() {

		bindings.excluding(QUser.user.address);
		bindings.including(QUser.user.address.city);

		assertThat(bindings.isPathAvailable("address.street", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("address.city", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("address_city", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("addressCity", User.class)).isTrue();
	}

	@Test // GH-3547
	void excludesAliasAfterBinding() {

		bindings.bind(QUser.user.inceptionYear).first(SimpleExpression::eq);

		bindings.excluding(QUser.user.inceptionYear);

		assertThat(bindings.isPathAvailable("inceptionYear", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("User.inceptionYear", User.class)).isFalse();
	}

	@Test // GH-3547
	void considersAliasProperly() {

		bindings.bind(QUser.user.firstname).first(CONTAINS_BINDING);
		bindings.bind(QUser.user.lastname).as("User.firstname").first(CONTAINS_BINDING);

		assertThat(bindings.getPropertyPath("User.firstname", TypeInformation.of(String.class)).getLeafProperty())
				.isEqualTo("lastname");

		assertThat(bindings.getPropertyPath("firstname", TypeInformation.of(User.class)).getLeafProperty())
				.isEqualTo("lastname");
	}

	@Test // GH-3547
	void considersShadowedAliasProperly() {

		bindings.bind(QUser.user.address.description).as("description").first(CONTAINS_BINDING);
		bindings.bind(QUser.user.description).first(CONTAINS_BINDING);

		PathInformation path = bindings.getPropertyPath("description", TypeInformation.of(User.class));

		assertThat(path).isNotNull();
		assertThat(path.toDotPath()).isEqualTo("description");
	}

	@Test // GH-3547
	void excludesParentAfterBinding() {

		bindings.bind(QUser.user.address.city).first(SimpleExpression::eq);
		bindings.excluding(QUser.user.address);

		assertThat(bindings.isPathAvailable("User.address.city", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	@Test // DATACMNS-669, DATACMNS-1744
	void allowsPropertiesCorrectly() {

		bindings.including(QUser.user.firstname, QUser.user.address.street);

		assertThat(bindings.isPathAvailable("firstname", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("address.street", User.class)).isTrue();
		assertThat(bindings.isPathAvailable("lastname", User.class)).isFalse();
		assertThat(bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	@Test // DATACMNS-787
	void rejectsNullAlias() {
		assertThatIllegalArgumentException().isThrownBy(() -> bindings.bind(QUser.user.address).as(null));
	}

	@Test // DATACMNS-787
	void rejectsEmptyAlias() {
		assertThatIllegalArgumentException().isThrownBy(() -> bindings.bind(QUser.user.address).as(""));
	}

	@Test // DATACMNS-787
	void aliasesBinding() {

		bindings.bind(QUser.user.address.city).as("city").first(CONTAINS_BINDING);

		var path = bindings.getPropertyPath("city", TypeInformation.of(User.class));

		assertThat(path).isNotNull();
		assertThat(bindings.isPathAvailable("city", User.class)).isTrue();

		// Aliasing implicitly denies original path
		assertThat(bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	@Test // DATACMNS-787
	void explicitlyIncludesOriginalBindingDespiteAlias() {

		bindings.including(QUser.user.address.city);
		bindings.bind(QUser.user.address.city).as("city").first(CONTAINS_BINDING);

		var path = bindings.getPropertyPath("city", TypeInformation.of(User.class));

		assertThat(path).isNotNull();
		assertThat(bindings.isPathAvailable("city", User.class)).isTrue();

		assertThat(bindings.isPathAvailable("address.city", User.class)).isTrue();

		var propertyPath = bindings.getPropertyPath("address.city", TypeInformation.of(User.class));
		assertThat(propertyPath).isNotNull();

		assertAdapterWithTargetBinding(bindings.getBindingForPath(propertyPath), CONTAINS_BINDING);
	}

	@Test // DATACMNS-787
	void registeredAliasWithNullBinding() {

		bindings.bind(QUser.user.address.city).as("city").withDefaultBinding();

		var path = bindings.getPropertyPath("city", TypeInformation.of(User.class));
		assertThat(path).isNotNull();

		assertThat(bindings.getBindingForPath(path)).isNotPresent();
	}

	@Test // DATACMNS-941
	void registersBindingForPropertyOfSubtype() {

		bindings.bind(QUser.user.as(QSpecialUser.class).specialProperty).first(ContainsBinding.INSTANCE);

		assertThat(bindings.isPathAvailable("specialProperty", User.class)).isTrue();
	}

	private static void assertAdapterWithTargetBinding(Optional<MultiValueBinding<StringPath, String>> binding,
			SingleValueBinding<StringPath, String> expected) {

		assertThat(binding).hasValueSatisfying(it -> {
			assertThat(it.bind(QUser.user.firstname, Arrays.asList("Walter", "White")))
					.contains(expected.bind(QUser.user.firstname, "Walter"));
			assertThat(it.bind(QUser.user.firstname, Collections.emptyList())).isEmpty();
		});
	}

	enum ContainsBinding implements SingleValueBinding<StringPath, String> {

		INSTANCE;

		@Override
		public Predicate bind(StringPath path, String value) {
			return path.contains(value);
		}
	}

}
