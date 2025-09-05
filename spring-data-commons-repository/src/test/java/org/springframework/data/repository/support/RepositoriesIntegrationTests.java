/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.sample.Product;
import org.springframework.data.repository.sample.ProductRepository;
import org.springframework.data.repository.sample.SampleConfiguration;
import org.springframework.data.repository.sample.User;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration tests for {@link Repositories}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@SpringJUnitConfig(classes = SampleConfiguration.class)
class RepositoriesIntegrationTests {

	@Autowired Repositories repositories;
	@Autowired ProductRepository productRepository;

	@Test
	void detectsRepositories() {

		assertThat(repositories).isNotNull();
		assertThat(repositories.hasRepositoryFor(User.class)).isTrue();
		assertThat(repositories.hasRepositoryFor(Product.class)).isTrue();
	}

	@Test // DATACMNS-376
	void returnsPersistentEntityForProxiedClass() {

		var user = mock(User.class);
		assertThat(repositories.getPersistentEntity(user.getClass())).isNotNull();
	}
}
