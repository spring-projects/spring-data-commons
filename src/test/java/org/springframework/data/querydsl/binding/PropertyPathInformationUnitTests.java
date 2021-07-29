/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.User;

/**
 * Unit tests for {@link PropertyPathInformation}.
 *
 * @author Mark Paluch
 */
class PropertyPathInformationUnitTests {

	@Test // GH-2418
	void shouldEqualsCorrectly() {

		PropertyPathInformation information = PropertyPathInformation.of("address.description", User.class);

		QuerydslPathInformation querydslPathInformation = QuerydslPathInformation.of(QUser.user.address.description);

		assertThat(information).isEqualTo(querydslPathInformation);
		assertThat(querydslPathInformation).isEqualTo(information);
	}

	@Test // GH-2418
	void shouldHashCodeCorrectly() {

		PropertyPathInformation information = PropertyPathInformation.of("address.description", User.class);

		QuerydslPathInformation querydslPathInformation = QuerydslPathInformation.of(QUser.user.address.description);

		assertThat(information).hasSameHashCodeAs(querydslPathInformation);
		assertThat(querydslPathInformation).hasSameHashCodeAs(information);
	}
}
