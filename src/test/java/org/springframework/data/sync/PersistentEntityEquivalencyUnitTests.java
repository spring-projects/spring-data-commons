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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.repository.sample.Product;
import org.springframework.data.repository.sample.User;
import org.springframework.sync.diffsync.Equivalency;

/**
 * Unit tests for {@link PersistentEntitiesEquivalency}.
 * 
 * @author Oliver Gierke
 */
public class PersistentEntityEquivalencyUnitTests {

	Equivalency equivalency;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		mappingContext.setInitialEntitySet(new HashSet<Class<?>>(Arrays.asList(User.class, Product.class)));
		mappingContext.afterPropertiesSet();

		PersistentEntities entities = new PersistentEntities(Collections.singleton(mappingContext));

		this.equivalency = new PersistentEntitiesEquivalency(entities);
	}

	/**
	 * @see DATACMNS-588
	 */
	@Test
	public void objectsOfSameTypeWithSameIdAreConsideredEquivalent() {

		Product left = new Product();
		left.id = 1L;

		Product right = new Product();
		right.id = 1L;

		assertThat(equivalency.isEquivalent(left, right), is(true));
	}
}
