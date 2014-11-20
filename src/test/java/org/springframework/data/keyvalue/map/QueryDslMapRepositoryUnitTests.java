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

import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.*;
import static org.hamcrest.collection.IsIterableContainingInOrder.*;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.keyvalue.Person;
import org.springframework.data.keyvalue.QPerson;
import org.springframework.data.querydsl.QSort;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

/**
 * @author Christoph Strobl
 */
public class QueryDslMapRepositoryUnitTests extends MapBackedKeyValueRepositoryUnitTests {

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findOneIsExecutedCorrectly() {

		repository.save(LENNISTERS);

		Person result = getQPersonRepo().findOne(QPerson.person.firstname.eq(CERSEI.getFirstname()));
		assertThat(result, is(CERSEI));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findAllIsExecutedCorrectly() {

		repository.save(LENNISTERS);

		Iterable<Person> result = getQPersonRepo().findAll(QPerson.person.age.eq(CERSEI.getAge()));
		assertThat(result, containsInAnyOrder(CERSEI, JAIME));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findWithPaginationWorksCorrectly() {

		repository.save(LENNISTERS);
		Page<Person> page1 = getQPersonRepo().findAll(QPerson.person.age.eq(CERSEI.getAge()), new PageRequest(0, 1));

		assertThat(page1.getTotalElements(), is(2L));
		assertThat(page1.getContent(), hasSize(1));
		assertThat(page1.hasNext(), is(true));

		Page<Person> page2 = ((QPersonRepository) repository).findAll(QPerson.person.age.eq(CERSEI.getAge()),
				page1.nextPageable());

		assertThat(page2.getTotalElements(), is(2L));
		assertThat(page2.getContent(), hasSize(1));
		assertThat(page2.hasNext(), is(false));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findAllUsingOrderSpecifierWorksCorrectly() {

		repository.save(LENNISTERS);

		Iterable<Person> result = getQPersonRepo().findAll(QPerson.person.age.eq(CERSEI.getAge()),
				QPerson.person.firstname.desc());

		assertThat(result, contains(JAIME, CERSEI));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findAllUsingPageableWithSortWorksCorrectly() {

		repository.save(LENNISTERS);

		Iterable<Person> result = getQPersonRepo().findAll(QPerson.person.age.eq(CERSEI.getAge()),
				new PageRequest(0, 10, Direction.DESC, "firstname"));

		assertThat(result, contains(JAIME, CERSEI));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findAllUsingPagableWithQSortWorksCorrectly() {

		repository.save(LENNISTERS);

		Iterable<Person> result = getQPersonRepo().findAll(QPerson.person.age.eq(CERSEI.getAge()),
				new PageRequest(0, 10, new QSort(QPerson.person.firstname.desc())));

		assertThat(result, contains(JAIME, CERSEI));
	}

	@Override
	protected Class<? extends PersonRepository> getRepositoryClass() {
		return QPersonRepository.class;
	}

	QPersonRepository getQPersonRepo() {
		return ((QPersonRepository) repository);
	}

	static interface QPersonRepository extends PersonRepository, QueryDslPredicateExecutor<Person> {

	}

}
