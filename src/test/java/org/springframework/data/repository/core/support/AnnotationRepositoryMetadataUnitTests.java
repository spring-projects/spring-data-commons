/*
 * Copyright 2011-2021 the original author or authors.
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
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Unit tests for {@link DefaultRepositoryMetadata}.
 *
 * @author Oliver Gierke
 */
class AnnotationRepositoryMetadataUnitTests {

	@Test
	void handlesRepositoryProxyAnnotationCorrectly() {

		RepositoryMetadata metadata = new AnnotationRepositoryMetadata(AnnotatedRepository.class);

		assertThat(metadata.getDomainType()).isEqualTo(User.class);
		assertThat(metadata.getIdType()).isEqualTo(Integer.class);
	}

	@Test // DATACMNS-37, DATACMNS-1375
	void preventsUnannotatedInterface() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new AnnotationRepositoryMetadata(UnannotatedRepository.class)) //
				.withMessageContaining(UnannotatedRepository.class.getName());
	}

	@SuppressWarnings("unused")
	private class User {

		private String firstname;

		String getAddress() {

			return null;
		}
	}

	@RepositoryDefinition(domainClass = User.class, idClass = Integer.class)
	interface AnnotatedRepository {

	}

	interface UnannotatedRepository {

	}
}
