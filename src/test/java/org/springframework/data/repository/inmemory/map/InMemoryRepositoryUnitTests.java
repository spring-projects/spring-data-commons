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
package org.springframework.data.repository.inmemory.map;

import static org.hamcrest.collection.IsIterableContainingInOrder.*;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.inmemory.InMemoryRepository;

/**
 * @author Christoph Strobl
 */
public class InMemoryRepositoryUnitTests {

	private static final Person CERSEI = new Person("cersei", 19);
	private static final Person JAIME = new Person("jaime", 19);
	private static final Person TYRION = new Person("tyrion", 17);

	private PersonRepository repository;

	@Before
	public void setup() {
		this.repository = new MapBackedRepositoryFactory<Person, String>(new MapOperations())
				.getRepository(PersonRepository.class);
	}

	@Test
	public void findBy() {

		this.repository.save(Arrays.asList(CERSEI, JAIME, TYRION));
		assertThat(this.repository.findByAge(19), contains(CERSEI, JAIME));
	}

	@Test
	public void combindedFindUsingAnd() {

		this.repository.save(Arrays.asList(CERSEI, JAIME, TYRION));

		assertThat(this.repository.findByFirstnameAndAge(JAIME.firstname, 19), contains(JAIME));
	}

	@Test
	public void findPage() {

		this.repository.save(Arrays.asList(CERSEI, JAIME, TYRION));

		Page<Person> page = this.repository.findByAge(19, new PageRequest(0, 1));
		assertThat(page.hasNext(), is(true));
		assertThat(page.getTotalElements(), is(2L));
		assertThat(page.getContent(), contains(CERSEI));

		assertThat(this.repository.findByAge(19, page.nextPageable()), contains(JAIME));
	}

	@Test
	public void findByConnectingOr() {

		this.repository.save(Arrays.asList(CERSEI, JAIME, TYRION));

		assertThat(this.repository.findByAgeOrFirstname(19, TYRION.firstname), contains(CERSEI, JAIME, TYRION));
	}

	static class Person {

		@Id String id;
		private String firstname;
		int age;

		public Person(String firstname, int age) {
			super();
			this.firstname = firstname;
			this.age = age;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		@Override
		public String toString() {
			return "Person [id=" + id + ", firstname=" + firstname + ", age=" + age + "]";
		}

	}

	static interface PersonRepository extends CrudRepository<Person, String>, InMemoryRepository<Person, String> {

		List<Person> findByAge(int age);

		List<Person> findByFirstname(String firstname);

		List<Person> findByFirstnameAndAge(String firstname, int age);

		Page<Person> findByAge(int age, Pageable page);

		List<Person> findByAgeOrFirstname(int age, String firstname);

	}

}
