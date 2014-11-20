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
package org.springframework.data.keyvalue.map;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;
import static org.hamcrest.collection.IsIterableContainingInOrder.*;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.keyvalue.Person;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Christoph Strobl
 */
public class MapBackedKeyValueRepositoryUnitTests {

	protected static final Person CERSEI = new Person("cersei", 19);
	protected static final Person JAIME = new Person("jaime", 19);
	protected static final Person TYRION = new Person("tyrion", 17);

	protected static List<Person> LENNISTERS = Arrays.asList(CERSEI, JAIME, TYRION);

	protected PersonRepository repository;
	protected KeyValueTemplate template = new KeyValueTemplate(new MapKeyValueAdapter());

	@Before
	public void setup() {
		this.repository = new KeyValueRepositoryFactory(template).getRepository(getRepositoryClass());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findBy() {

		this.repository.save(LENNISTERS);
		assertThat(this.repository.findByAge(19), containsInAnyOrder(CERSEI, JAIME));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void combindedFindUsingAnd() {

		this.repository.save(LENNISTERS);

		assertThat(this.repository.findByFirstnameAndAge(JAIME.getFirstname(), 19), containsInAnyOrder(JAIME));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findPage() {

		this.repository.save(LENNISTERS);

		Page<Person> page = this.repository.findByAge(19, new PageRequest(0, 1));
		assertThat(page.hasNext(), is(true));
		assertThat(page.getTotalElements(), is(2L));
		assertThat(page.getContent(), IsCollectionWithSize.hasSize(1));

		Page<Person> next = this.repository.findByAge(19, page.nextPageable());
		assertThat(next.hasNext(), is(false));
		assertThat(next.getTotalElements(), is(2L));
		assertThat(next.getContent(), IsCollectionWithSize.hasSize(1));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findByConnectingOr() {

		this.repository.save(LENNISTERS);

		assertThat(this.repository.findByAgeOrFirstname(19, TYRION.getFirstname()),
				containsInAnyOrder(CERSEI, JAIME, TYRION));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void singleEntityExecution() {

		this.repository.save(LENNISTERS);

		assertThat(this.repository.findByAgeAndFirstname(TYRION.getAge(), TYRION.getFirstname()), is(TYRION));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findAllShouldRespectSort() {

		this.repository.save(LENNISTERS);

		assertThat(this.repository.findAll(new Sort(new Sort.Order(Direction.ASC, "age"), new Sort.Order(Direction.DESC,
				"firstname"))), IsIterableContainingInOrder.contains(TYRION, JAIME, CERSEI));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void derivedFinderShouldRespectSort() {

		repository.save(LENNISTERS);

		List<Person> result = repository.findByAgeGreaterThanOrderByAgeAscFirstnameDesc(2);

		assertThat(result, contains(TYRION, JAIME, CERSEI));
	}

	protected Class<? extends PersonRepository> getRepositoryClass() {
		return PersonRepository.class;
	}

	public static interface PersonRepository extends CrudRepository<Person, String>, KeyValueRepository<Person, String> {

		List<Person> findByAge(int age);

		List<Person> findByFirstname(String firstname);

		List<Person> findByFirstnameAndAge(String firstname, int age);

		Page<Person> findByAge(int age, Pageable page);

		List<Person> findByAgeOrFirstname(int age, String firstname);

		Person findByAgeAndFirstname(int age, String firstname);

		List<Person> findByAgeGreaterThanOrderByAgeAscFirstnameDesc(int age);

	}

}
