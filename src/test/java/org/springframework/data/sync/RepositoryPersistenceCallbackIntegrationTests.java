/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.sync;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.repository.sample.Product;
import org.springframework.data.repository.sample.ProductRepository;
import org.springframework.data.repository.sample.SampleConfiguration;
import org.springframework.sync.diffsync.PersistenceCallback;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.config.EnableDifferentialSynchronization;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link RepositoryPersistenceCallback}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RepositoryPersistenceCallbackIntegrationTests {

	@Configuration
	@Import({ SampleConfiguration.class, SpringDataSyncConfiguration.class })
	@EnableDifferentialSynchronization
	static class Config {

		@Bean
		public SampleMappingContext sampleMappingContext() {
			return new SampleMappingContext();
		}
	}

	@Autowired PersistenceCallbackRegistry registry;
	@Autowired ProductRepository productRepository;

	/**
	 * @see DATACMNS-588
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void looksUpCallbackForProductsAndInvokesFindOne() {

		PersistenceCallback<?> callback = registry.findPersistenceCallback("products");

		assertThat(callback, is(instanceOf(RepositoryPersistenceCallback.class)));

		RepositoryPersistenceCallback<Product> products = (RepositoryPersistenceCallback<Product>) callback;
		products.findOne("1");
		verify(productRepository).findOne(1L);
	}
}
