/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RepositoryFragment}.
 *
 * @author Mark Paluch
 */
class RepositoryFragmentUnitTests {

	@Test // DATACMNS-1289
	@SuppressWarnings("unchecked")
	void fragmentCreationFromUnrelatedTypesShouldFail() {

		assertThatThrownBy(() -> RepositoryFragment.implemented((Class) CustomFragment.class, new UnrelatedImpl()))
				.hasMessageMatching("Fragment implementation .*UnrelatedImpl does not implement .*")
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATACMNS-1289
	void fragmentCreationFromRelatedTypesShouldCreateNewFragment() {
		assertThat(RepositoryFragment.implemented(CustomFragment.class, new CustomFragmentImpl())).isNotNull();
	}

	interface CustomFragment {}

	private static class CustomFragmentImpl implements CustomFragment {}

	private static class UnrelatedImpl {}
}
