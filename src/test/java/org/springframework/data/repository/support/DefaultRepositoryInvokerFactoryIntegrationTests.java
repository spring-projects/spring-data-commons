/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.sample.Product;
import org.springframework.data.repository.sample.ProductRepository;
import org.springframework.data.repository.sample.SampleConfiguration;
import org.springframework.data.repository.sample.User;
import org.springframework.data.repository.support.CrudRepositoryInvoker;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.PagingAndSortingRepositoryInvoker;
import org.springframework.data.repository.support.ReflectionRepositoryInvoker;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link DefaultRepositoryInvokerFactory}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleConfiguration.class)
public class DefaultRepositoryInvokerFactoryIntegrationTests {

	@Autowired ProductRepository productRepository;
	@Autowired Repositories repositories;

	RepositoryInvokerFactory factory;

	@Rule public ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() {
		this.factory = new DefaultRepositoryInvokerFactory(repositories);
	}

	@Test // DATACMNS-410, DATACMNS-589
	public void findOneShouldDelegateToAppropriateRepository() {

		Mockito.reset(productRepository);
		Product product = new Product();
		when(productRepository.findOne(4711L)).thenReturn(product);

		assertThat(factory.getInvokerFor(Product.class).invokeFindOne(4711L), is((Object) product));
	}

	@Test // DATACMNS-374, DATACMNS-589
	public void shouldThrowMeaningfulExceptionWhenTheRepositoryForAGivenDomainClassCannotBeFound() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("No repository found for domain type: ");
		exception.expectMessage(Object.class.getName());

		factory.getInvokerFor(Object.class);
	}

	@Test // DATACMNS-589
	public void returnsSameInvokerInstanceForSubsequentCalls() {

		RepositoryInvoker invoker = factory.getInvokerFor(Product.class);

		assertThat(factory.getInvokerFor(Product.class), is(invoker));
	}

	@Test // DATACMNS-589
	public void createsReflectionRepositoryInvokerForRepositoryNotExtendingADedicatedBaseRepository() {

		RepositoryInvoker invoker = factory.getInvokerFor(Product.class);

		assertThat(invoker, is(instanceOf(ReflectionRepositoryInvoker.class)));
		assertThat(invoker, is(not(instanceOf(CrudRepositoryInvoker.class))));
	}

	@Test // DATACMNS-589
	public void createsCrudRepositoryInvokerForRepositoryExtendingCrudRepository() {

		RepositoryInvoker invoker = factory.getInvokerFor(User.class);

		assertThat(invoker, is(instanceOf(CrudRepositoryInvoker.class)));
		assertThat(invoker, is(not(instanceOf(PagingAndSortingRepositoryInvoker.class))));
	}
}
