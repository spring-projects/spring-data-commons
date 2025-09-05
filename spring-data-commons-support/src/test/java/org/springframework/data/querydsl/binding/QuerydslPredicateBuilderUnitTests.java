/*
 * Copyright 2015-2025 the original author or authors.
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
import static org.assertj.core.api.Assumptions.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.querydsl.Address;
import org.springframework.data.querydsl.QSpecialUser;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.QUserWrapper;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.UserWrapper;
import org.springframework.data.querydsl.Users;
import org.springframework.data.util.TypeInformation;
import org.springframework.data.util.Version;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.querydsl.collections.CollQueryFactory;
import com.querydsl.core.types.Constant;
import com.querydsl.core.types.dsl.StringPath;

/**
 * Unit tests for {@link QuerydslPredicateBuilder}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class QuerydslPredicateBuilderUnitTests {

	static final TypeInformation<User> USER_TYPE = TypeInformation.of(User.class);
	static final QuerydslBindings DEFAULT_BINDINGS = new QuerydslBindings();
	static final SingleValueBinding<StringPath, String> CONTAINS_BINDING = (path, value) -> path.contains(value);

	QuerydslPredicateBuilder builder;
	MultiValueMap<String, String> values;

	@BeforeEach
	void setUp() {
		this.builder = new QuerydslPredicateBuilder(new DefaultFormattingConversionService(),
				SimpleEntityPathResolver.INSTANCE);
		this.values = new LinkedMultiValueMap<>();
	}

	@Test // DATACMNS-669
	void rejectsNullConversionService() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new QuerydslPredicateBuilder(null, SimpleEntityPathResolver.INSTANCE));
	}

	@Test // DATACMNS-669
	void getPredicateShouldThrowErrorWhenBindingContextIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> builder.getPredicate(null, values, null));
	}

	@Test // DATACMNS-669, DATACMNS-1168
	void getPredicateShouldReturnEmptyWhenPropertiesAreEmpty() {
		assertThat(
				QuerydslPredicateBuilder
						.isEmpty(builder.getPredicate(TypeInformation.of(Object.class), values, DEFAULT_BINDINGS)))
								.isTrue();
	}

	@Test // GH-2418
	void shouldLookupCorrectPath() {

		DEFAULT_BINDINGS.bind(QUser.user.description).first(CONTAINS_BINDING);

		values.add("description", "Linz");
		var predicate = this.builder.getPredicate(TypeInformation.of(User.class), values, DEFAULT_BINDINGS);

		assertThat(predicate).hasToString("contains(user.description,Linz)");

		predicate = this.builder.getPredicate(TypeInformation.of(Address.class), values, DEFAULT_BINDINGS);

		assertThat(predicate).hasToString("address.description = Linz");
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		assumeThat(Version.javaVersion().toString())
				.as("QueryDSL isn't Java 11 ready https://github.com/querydsl/querydsl/issues/2151").startsWith("1.8");

		values.add("firstname", "Oliver");

		var predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("Oliver"));

		var result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).fetchResults().getResults();

		assertThat(result).containsExactly(Users.OLIVER);
	}

	@Test // DATACMNS-669
	void resolveArgumentShouldCreateNestedStringParameterPredicateCorrectly() throws Exception {

		assumeThat(Version.javaVersion().toString())
				.as("QueryDSL isn't Java 11 ready https://github.com/querydsl/querydsl/issues/2151").startsWith("1.8");

		values.add("address.city", "Linz");

		var predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.address.city.eq("Linz"));

		var result = CollQueryFactory.from(QUser.user, Users.USERS).where(predicate).fetchResults().getResults();

		assertThat(result).containsExactly(Users.CHRISTOPH);
	}

	@Test // DATACMNS-669
	void ignoresNonDomainTypeProperties() {

		values.add("firstname", "rand");
		values.add("lastname".toUpperCase(), "al'thor");

		var predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand"));
	}

	@Test // DATACMNS-669
	void forwardsNullForEmptyParameterToSingleValueBinder() {

		values.add("lastname", null);

		var bindings = new QuerydslBindings();
		bindings.bind(QUser.user.lastname).firstOptional((path, value) -> value.map(path::contains));

		builder.getPredicate(USER_TYPE, values, bindings);
	}

	@Test // DATACMNS-734
	@SuppressWarnings("unchecked")
	void resolvesCommaSeparatedArgumentToArrayCorrectly() {

		values.add("address.lonLat", "40.740337,-73.995146");

		var predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		var constant = (Constant<Object>) ((List<?>) getField(getField(predicate, "mixin"), "args")).get(1);

		assertThat(constant.getConstant()).isEqualTo(new Double[] { 40.740337D, -73.995146D });
	}

	@Test // DATACMNS-734
	@SuppressWarnings("unchecked")
	void leavesCommaSeparatedArgumentUntouchedWhenTargetIsNotAnArray() {

		values.add("address.city", "rivers,two");

		var predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		var constant = (Constant<Object>) ((List<?>) getField(getField(predicate, "mixin"), "args")).get(1);

		assertThat(constant.getConstant()).isEqualTo("rivers,two");
	}

	@Test // GH-2649
	void resolvesCommaSeparatedArgumentToListCorrectly() {

		values.add("nickNames", "Walt,Heisenberg");

		var predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).hasToString("Walt in user.nickNames && Heisenberg in user.nickNames");
	}

	@Test // GH-2649
	void resolvesCommaSeparatedArgumentToListCorrectlyForNestedPath() {

		values.add("user.nickNames", "Walt,Heisenberg");

		var predicate = builder.getPredicate(TypeInformation.of(UserWrapper.class), values, DEFAULT_BINDINGS);

		assertThat(predicate).hasToString("Walt in userWrapper.user.nickNames && Heisenberg in userWrapper.user.nickNames");
	}

	@Test // DATACMNS-883
	void automaticallyInsertsAnyStepInCollectionReference() {

		values.add("addresses.street", "VALUE");

		var predicate = builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS);

		assertThat(predicate).isEqualTo(QUser.user.addresses.any().street.eq("VALUE"));
	}

	@Test // DATACMNS-941
	void buildsPredicateForBindingUsingDowncast() {

		values.add("specialProperty", "VALUE");

		var bindings = new QuerydslBindings();
		bindings.bind(QUser.user.as(QSpecialUser.class).specialProperty)//
				.first(QuerydslBindingsUnitTests.ContainsBinding.INSTANCE);

		assertThat(builder.getPredicate(USER_TYPE, values, bindings))//
				.isEqualTo(QUser.user.as(QSpecialUser.class).specialProperty.contains("VALUE"));
	}

	@Test // DATACMNS-941
	void buildsPredicateForBindingUsingNestedDowncast() {

		values.add("user.specialProperty", "VALUE");

		var wrapper = QUserWrapper.userWrapper;

		var bindings = new QuerydslBindings();
		bindings.bind(wrapper.user.as(QSpecialUser.class).specialProperty)//
				.first(QuerydslBindingsUnitTests.ContainsBinding.INSTANCE);

		assertThat(builder.getPredicate(TypeInformation.of(UserWrapper.class), values, bindings))//
				.isEqualTo(wrapper.user.as(QSpecialUser.class).specialProperty.contains("VALUE"));
	}

	@Test // DATACMNS-1443
	void doesNotDropValuesContainingABlank() {

		values.add("firstname", " ");

		assertThat(builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS)) //
				.isEqualTo(QUser.user.firstname.eq(" "));
	}

	@Test // DATACMNS-1443
	void dropsValuesContainingAnEmptyString() {

		values.add("firstname", "");

		assertThat(QuerydslPredicateBuilder.isEmpty(builder.getPredicate(USER_TYPE, values, DEFAULT_BINDINGS))).isTrue();
	}
}
