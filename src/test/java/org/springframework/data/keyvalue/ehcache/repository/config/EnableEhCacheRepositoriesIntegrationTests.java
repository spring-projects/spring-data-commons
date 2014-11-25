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
package org.springframework.data.keyvalue.ehcache.repository.config;

import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.List;

import net.sf.ehcache.CacheManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.keyvalue.ehcache.EhCacheKeyValueAdapter;
import org.springframework.data.keyvalue.ehcache.EhCacheQueryEngine;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnableEhCacheRepositoriesIntegrationTests {

	@Configuration
	@EnableEhCacheRepositories(considerNestedRepositories = true)
	static class Config {

		@Bean
		public KeyValueOperations keyValueTemplate() {
			return new KeyValueTemplate(new EhCacheKeyValueAdapter(new EhCacheQueryEngine(), CacheManager.create()));
		}
	}

	@Autowired PersonRepository repo;

	@Test
	public void shouldEnableKeyValueRepositoryCorrectly() {
		assertThat(repo, notNullValue());

		Person person = new Person();
		person.setFirstname("foo");
		repo.save(person);

		List<Person> result = repo.findByFirstname("foo");
		assertThat(result, hasSize(1));
		assertThat(result.get(0).firstname, is("foo"));
	}

	static class Person implements Serializable {

		private static final long serialVersionUID = -1654603912377346292L;

		@Id String id;
		String firstname;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

	}

	static interface PersonRepository extends CrudRepository<Person, String> {

		List<Person> findByFirstname(String firstname);

	}

}
