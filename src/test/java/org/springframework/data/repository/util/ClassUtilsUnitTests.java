/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.data.repository.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;
import org.springframework.scheduling.annotation.Async;

/**
 * Unit tests for {@link ClassUtils}.
 *
 * @author Oliver Gierke
 * @author John Blum
 */
class ClassUtilsUnitTests {

	@Test
	void rejectsInvalidReturnType() {
		assertThatIllegalStateException().isThrownBy(() -> ClassUtils.assertReturnTypeAssignable(
				SomeDao.class.getMethod("findByFirstname", Pageable.class, String.class), User.class));
	}

	@Test
	void determinesValidFieldsCorrectly() {

		assertThat(ClassUtils.hasProperty(User.class, "firstname")).isTrue();
		assertThat(ClassUtils.hasProperty(User.class, "Firstname")).isTrue();
		assertThat(ClassUtils.hasProperty(User.class, "address")).isFalse();
	}

	@Test // DATACMNS-769
	void unwrapsWrapperTypesBeforeAssignmentCheck() throws Exception {
		ClassUtils.assertReturnTypeAssignable(UserRepository.class.getMethod("findAsync", Pageable.class),
				Page.class);
	}

	@Test
	public void numberOfOccurrencesForMultipleMethodParametersOfType() throws Exception {

		Method findByAddress = AnotherDao.class.getMethod("findByAddress", Pageable.class, Pageable.class);

		assertThat(ClassUtils.getNumberOfOccurrences(findByAddress, Pageable.class)).isEqualTo(2);
	}

	@Test
	public void numberOfOccurrencesForNoMethodParameterOfType() throws Exception {

		Method findByAddress = AnotherDao.class.getMethod("findByAddress", Pageable.class, Pageable.class);

		assertThat(ClassUtils.getNumberOfOccurrences(findByAddress, Sort.class)).isZero();
		assertThat(ClassUtils.getNumberOfOccurrences(findByAddress, Page.class)).isZero();
	}

	@Test
	public void numberOfOccurrencesForSingleMethodParameterOfType() throws Exception {

		Method findByFirstname = SomeDao.class.getMethod("findByFirstname", Pageable.class, String.class);

		assertThat(ClassUtils.getNumberOfOccurrences(findByFirstname, Pageable.class)).isOne();
		assertThat(ClassUtils.getNumberOfOccurrences(findByFirstname, String.class)).isOne();
	}

	@SuppressWarnings("unused")
	private class User {

		private String firstname;

		String getAddress() {

			return null;
		}
	}

	interface UserRepository extends Repository<User, Integer> {

		@Async
		Future<Page<User>> findAsync(Pageable pageable);
	}

	interface SomeDao extends Serializable, UserRepository {

		Page<User> findByFirstname(Pageable pageable, String firstname);

		GenericType<User> someMethod();

		List<Map<String, Object>> anotherMethod();
	}

	interface AnotherDao extends Repository<User, Integer> {

		Page<User> findByAddress(Pageable pageableOne, Pageable pageableTwo);

	}

	class GenericType<T> {

	}
}
