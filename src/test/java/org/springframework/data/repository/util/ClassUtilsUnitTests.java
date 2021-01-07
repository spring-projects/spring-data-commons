/*
 * Copyright 2008-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.repository.util.ClassUtils.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.scheduling.annotation.Async;

/**
 * Unit test for {@link ClassUtils}.
 *
 * @author Oliver Gierke
 */
class ClassUtilsUnitTests {

	@Test
	void rejectsInvalidReturnType() {
		assertThatIllegalStateException().isThrownBy(() -> assertReturnTypeAssignable(
				SomeDao.class.getMethod("findByFirstname", Pageable.class, String.class), User.class));
	}

	@Test
	void determinesValidFieldsCorrectly() {

		assertThat(hasProperty(User.class, "firstname")).isTrue();
		assertThat(hasProperty(User.class, "Firstname")).isTrue();
		assertThat(hasProperty(User.class, "address")).isFalse();
	}

	@Test // DATACMNS-769
	void unwrapsWrapperTypesBeforeAssignmentCheck() throws Exception {
		assertReturnTypeAssignable(UserRepository.class.getMethod("findAsync", Pageable.class), Page.class);
	}

	@SuppressWarnings("unused")
	private class User {

		private String firstname;

		String getAddress() {

			return null;
		}
	}

	static interface UserRepository extends Repository<User, Integer> {

		@Async
		Future<Page<User>> findAsync(Pageable pageable);
	}

	interface SomeDao extends Serializable, UserRepository {

		Page<User> findByFirstname(Pageable pageable, String firstname);

		GenericType<User> someMethod();

		List<Map<String, Object>> anotherMethod();
	}

	class GenericType<T> {

	}
}
