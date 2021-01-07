/*
 * Copyright 2014-2021 the original author or authors.
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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.sample.Product;
import org.springframework.data.repository.sample.ProductRepository;
import org.springframework.data.repository.sample.SampleConfiguration;
import org.springframework.data.repository.sample.User;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration tests for {@link DefaultRepositoryInvokerFactory}.
 *
 * @author Oliver Gierke
 */
@SpringJUnitConfig(classes = SampleConfiguration.class)
class DefaultRepositoryInvokerFactoryIntegrationTests {

	@Autowired ProductRepository productRepository;
	@Autowired Repositories repositories;

	RepositoryInvokerFactory factory;

	@BeforeEach
	void setUp() {
		this.factory = new DefaultRepositoryInvokerFactory(repositories);
	}

	@Test // DATACMNS-410, DATACMNS-589
	void findOneShouldDelegateToAppropriateRepository() {

		// Mockito.reset(productRepository);
		Product product = new Product();
		when(productRepository.findById(4711L)).thenReturn(product);

		Optional<Object> invokeFindOne = factory.getInvokerFor(Product.class).invokeFindById(4711L);

		assertThat(invokeFindOne).isEqualTo(Optional.of(product));
	}

	@Test // DATACMNS-374, DATACMNS-589
	void shouldThrowMeaningfulExceptionWhenTheRepositoryForAGivenDomainClassCannotBeFound() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> factory.getInvokerFor(Object.class)) //
				.withMessageContaining("No repository found for domain type: ") //
				.withMessageContaining(Object.class.getName());
	}

	@Test // DATACMNS-589
	void returnsSameInvokerInstanceForSubsequentCalls() {

		RepositoryInvoker invoker = factory.getInvokerFor(Product.class);

		assertThat(factory.getInvokerFor(Product.class)).isEqualTo(invoker);
	}

	@Test // DATACMNS-589
	void createsReflectionRepositoryInvokerForRepositoryNotExtendingADedicatedBaseRepository() {

		RepositoryInvoker invoker = factory.getInvokerFor(Product.class);

		assertThat(invoker)//
				.isInstanceOf(ReflectionRepositoryInvoker.class)//
				.isNotInstanceOf(CrudRepositoryInvoker.class);
	}

	@Test // DATACMNS-589
	void createsCrudRepositoryInvokerForRepositoryExtendingCrudRepository() {

		RepositoryInvoker invoker = factory.getInvokerFor(User.class);

		assertThat(invoker)//
				.isInstanceOf(CrudRepositoryInvoker.class)//
				.isNotInstanceOf(PagingAndSortingRepositoryInvoker.class);
	}
}
