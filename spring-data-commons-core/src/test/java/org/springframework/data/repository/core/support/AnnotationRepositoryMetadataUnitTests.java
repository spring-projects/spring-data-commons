/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AnnotationRepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * Unit tests for {@link DefaultRepositoryMetadata}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationRepositoryMetadataUnitTests {

	@Test
	public void handlesRepositoryProxyAnnotationCorrectly() {

		RepositoryMetadata metadata = new AnnotationRepositoryMetadata(AnnotatedRepository.class);
		assertEquals(User.class, metadata.getDomainType());
		assertEquals(Integer.class, metadata.getIdType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsUnannotatedInterface() {

		new AnnotationRepositoryMetadata(UnannotatedRepository.class);
	}

	@SuppressWarnings("unused")
	private class User {

		private String firstname;

		public String getAddress() {

			return null;
		}
	}

	@RepositoryDefinition(domainClass = User.class, idClass = Integer.class)
	interface AnnotatedRepository {

	}

	interface UnannotatedRepository {

	}
}
