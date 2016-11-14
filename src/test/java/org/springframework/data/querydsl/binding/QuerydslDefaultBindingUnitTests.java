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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.querydsl.QUser;

import com.querydsl.core.types.Predicate;

/**
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
public class QuerydslDefaultBindingUnitTests {

	QuerydslDefaultBinding binding;

	@Before
	public void setUp() {
		binding = new QuerydslDefaultBinding();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldCreatePredicateCorrectlyWhenPropertyIsInRoot() {

		Optional<Predicate> predicate = binding.bind(QUser.user.firstname, Collections.singleton("tam"));

		assertThat(predicate).hasValue(QUser.user.firstname.eq("tam"));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldCreatePredicateCorrectlyWhenPropertyIsInNestedElement() {

		Optional<Predicate> predicate = binding.bind(QUser.user.address.city, Collections.singleton("two rivers"));

		assertThat(predicate).hasValueSatisfying(it -> {
			assertThat(it.toString()).isEqualTo(QUser.user.address.city.eq("two rivers").toString());
		});
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldCreatePredicateWithContainingWhenPropertyIsCollectionLikeAndValueIsObject() {

		Optional<Predicate> predicate = binding.bind(QUser.user.nickNames, Collections.singleton("dragon reborn"));

		assertThat(predicate).hasValue(QUser.user.nickNames.contains("dragon reborn"));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldCreatePredicateWithInWhenPropertyIsAnObjectAndValueIsACollection() {

		Optional<Predicate> predicate = binding.bind(QUser.user.firstname, Arrays.asList("dragon reborn", "shadowkiller"));

		assertThat(predicate).hasValue(QUser.user.firstname.in(Arrays.asList("dragon reborn", "shadowkiller")));
	}

	@Test
	public void testname() {
		assertThat(binding.bind(QUser.user.lastname, Collections.emptySet())).isNotPresent();
	}
}
