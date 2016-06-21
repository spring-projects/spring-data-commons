/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.repository.init;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.sample.Product;
import org.springframework.data.repository.sample.ProductRepository;
import org.springframework.data.repository.sample.SampleConfiguration;
import org.springframework.data.repository.sample.User;
import org.springframework.data.repository.support.Repositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for {@link UnmarshallingRepositoryInitializer}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleConfiguration.class)
public class ResourceReaderRepositoryInitializerUnitTests {

	@Autowired ProductRepository productRepository;
	@Autowired Repositories repositories;

	ApplicationEventPublisher publisher;
	ResourceReader reader;
	Resource resource;

	@Before
	public void setUp() {

		this.reader = mock(ResourceReader.class);
		this.publisher = mock(ApplicationEventPublisher.class);
		this.resource = mock(Resource.class);
	}

	@Test
	public void storesSingleObjectCorrectly() throws Exception {

		Product reference = new Product();
		setUpReferenceAndInititalize(reference);

		verify(productRepository).save(reference);
	}

	@Test
	public void storesCollectionOfObjectsCorrectly() throws Exception {

		Product product = new Product();
		Collection<Product> reference = Collections.singletonList(product);

		setUpReferenceAndInititalize(reference);

		verify(productRepository, times(1)).save(product);
	}

	/**
	 * @see DATACMNS-224
	 */
	@Test
	public void emitsRepositoriesPopulatedEventIfPublisherConfigured() throws Exception {

		RepositoryPopulator populator = setUpReferenceAndInititalize(new User(), publisher);

		ApplicationEvent event = new RepositoriesPopulatedEvent(populator, repositories);
		verify(publisher, times(1)).publishEvent(event);
	}

	private RepositoryPopulator setUpReferenceAndInititalize(Object reference, ApplicationEventPublisher publish)
			throws Exception {

		when(reader.readFrom(any(), any())).thenReturn(reference);

		ResourceReaderRepositoryPopulator populator = new ResourceReaderRepositoryPopulator(reader);
		populator.setResources(resource);
		populator.setApplicationEventPublisher(publisher);
		populator.populate(repositories);

		return populator;
	}

	private RepositoryPopulator setUpReferenceAndInititalize(Object reference) throws Exception {
		return setUpReferenceAndInititalize(reference, null);
	}
}
