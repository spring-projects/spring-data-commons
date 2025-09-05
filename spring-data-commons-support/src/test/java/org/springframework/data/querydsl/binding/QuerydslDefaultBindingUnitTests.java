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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.querydsl.QUser;

/**
 * Unit tests for {@link QuerydslDefaultBinding}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Colin Gao
 * @author Mark Paluch
 */
class QuerydslDefaultBindingUnitTests {

	QuerydslDefaultBinding binding;

	@BeforeEach
	void setUp() {
		binding = new QuerydslDefaultBinding();
	}

	@Test // DATACMNS-669
	void shouldCreatePredicateCorrectlyWhenPropertyIsInRoot() {

		var predicate = binding.bind(QUser.user.firstname, Collections.singleton("tam"));

		assertThat(predicate).hasValue(QUser.user.firstname.eq("tam"));
	}

	@Test // DATACMNS-669
	void shouldCreatePredicateCorrectlyWhenPropertyIsInNestedElement() {

		var predicate = binding.bind(QUser.user.address.city, Collections.singleton("two rivers"));

		assertThat(predicate).hasValueSatisfying(
				it -> assertThat(it.toString()).isEqualTo(QUser.user.address.city.eq("two rivers").toString()));
	}

	@Test // DATACMNS-669
	void shouldCreatePredicateWithContainingWhenPropertyIsCollectionLikeAndValueIsObject() {

		var predicate = binding.bind(QUser.user.nickNames, Collections.singleton("dragon reborn"));

		assertThat(predicate).hasValue(QUser.user.nickNames.contains("dragon reborn"));
	}

	@Test // GH-2834
	void shouldUnwrapNestedCollectionsWithContainingWhenPropertyIsCollectionLike() {

		var predicate = binding.bind(QUser.user.nickNames, Collections.singleton(Collections.singleton("dragon reborn")));

		assertThat(predicate).hasValue(QUser.user.nickNames.contains("dragon reborn"));
	}

	@Test // DATACMNS-669
	void shouldCreatePredicateWithInWhenPropertyIsAnObjectAndValueIsACollection() {

		var predicate = binding.bind(QUser.user.firstname, Arrays.asList("dragon reborn", "shadowkiller"));

		assertThat(predicate).hasValue(QUser.user.firstname.in(Arrays.asList("dragon reborn", "shadowkiller")));
	}

	@Test
	void shouldNotBindEmptyParameterCollection() {
		assertThat(binding.bind(QUser.user.lastname, Collections.emptySet())).isNotPresent();
	}

	@Test // DATACMNS-1578
	void shouldCreatePredicateWithIsNullWhenPropertyIsANestedObjectAndValueIsNull() {

		var predicate = binding.bind(QUser.user.address.city, Collections.singleton(null));

		assertThat(predicate).hasValueSatisfying(it -> {
			assertThat(it.toString()).isEqualTo(QUser.user.address.city.isNull().toString());
		});
	}
}
