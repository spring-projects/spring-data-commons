/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.data.mapping.model;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.Serializable;

import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.repository.core.support.PersistentEntityInformation;

/**
 * Unit tests for {@link ClassGeneratingPropertyAccessorFactory} covering interface
 * and concrete class entity types.
 *
 * @author John Blum
 * @see <a href="https://jira.spring.io/browse/DATACMNS-809">DATACMNS-809</a>
 */
public class ClassGeneratingPropertyAccessorFactoryEntityTypeTests {

	private final SampleMappingContext mappingContext = new SampleMappingContext();

	protected PersistentEntity<Object, SamplePersistentProperty> getPersistentEntity(Object entity) {
		return getPersistentEntity(entity.getClass());
	}

	protected PersistentEntity<Object, SamplePersistentProperty> getPersistentEntity(Class<?> entityType) {
		return mappingContext.getPersistentEntity(entityType);
	}

	protected PersistentEntityInformation<Object, ?> getPersistentEntityInformation(Object entity) {
		return new PersistentEntityInformation<Object, Serializable>(getPersistentEntity(entity));
	}

	protected PersistentEntityInformation<Object, ?> getPersistentEntityInformation(Class<?> entityType) {
		return new PersistentEntityInformation<Object, Serializable>(getPersistentEntity(entityType));
	}

	@Test
	public void getIdentifierOfInterfaceBasedEntity() {
		PersistentEntityInformation<Object, ?> quickSortEntityInfo =
			getPersistentEntityInformation(Algorithm.class);

		Algorithm quickSort = new QuickSort();

		assertThat(String.valueOf(quickSortEntityInfo.getId(quickSort)), is(equalTo(quickSort.getName())));
	}

	@Test
	public void getIdentifierOfClassBasedEntity() {
		Person jonDoe = Person.newPerson("JonDoe");
		PersistentEntityInformation<Object, ?> jonDoeEntityInfo = getPersistentEntityInformation(jonDoe);

		assertThat(String.valueOf(jonDoeEntityInfo.getId(jonDoe)), is(equalTo(jonDoe.getName())));
	}

	interface Algorithm {
		@Id String getName();
	}

	class QuickSort implements Algorithm {
		@Override
		public String getName() {
			return getClass().toString();
		}
	}

	static class Person {

		@Id
		private final String name;

		static Person newPerson(String name) {
			return new Person(name);
		}

		Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return getName();
		}
	}
}
