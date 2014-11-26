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
package org.springframework.data.keyvalue.repository;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.support.SimpleKeyValueRepository;
import org.springframework.data.repository.core.support.ReflectionEntityInformation;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicKeyValueRepositoryUnitTests {

	private SimpleKeyValueRepository<Foo, String> repo;
	private @Mock KeyValueOperations opsMock;

	@Before
	public void setUp() {

		ReflectionEntityInformation<Foo, String> ei = new ReflectionEntityInformation<Foo, String>(Foo.class);
		repo = new SimpleKeyValueRepository<Foo, String>(ei, opsMock);
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void saveNewWithNumericId() {

		ReflectionEntityInformation<WithNumericId, Integer> ei = new ReflectionEntityInformation<WithNumericId, Integer>(
				WithNumericId.class);
		SimpleKeyValueRepository<WithNumericId, Integer> temp = new SimpleKeyValueRepository<WithNumericId, Integer>(ei,
				opsMock);

		WithNumericId foo = temp.save(new WithNumericId());
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void testDoubleSave() {

		Foo foo = new Foo("one");

		repo.save(foo);

		foo.id = "1";
		repo.save(foo);
		verify(opsMock, times(1)).insert(eq(foo));
		verify(opsMock, times(1)).update(eq(foo.getId()), eq(foo));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void multipleSave() {

		Foo one = new Foo("one");
		Foo two = new Foo("one");

		repo.save(Arrays.asList(one, two));
		verify(opsMock, times(1)).insert(eq(one));
		verify(opsMock, times(1)).insert(eq(two));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void deleteEntity() {

		Foo one = repo.save(new Foo("one"));
		repo.delete(one);

		verify(opsMock, times(1)).delete(eq(one.getId()), eq(Foo.class));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void deleteById() {

		repo.delete("one");

		verify(opsMock, times(1)).delete(eq("one"), eq(Foo.class));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void deleteAll() {

		repo.deleteAll();

		verify(opsMock, times(1)).delete(eq(Foo.class));
	}

	/**
	 * @see DATACMNS-525
	 */
	@Test
	public void findAllIds() {

		repo.findAll(Arrays.asList("one", "two", "three"));

		verify(opsMock, times(3)).findById(anyString(), eq(Foo.class));
	}

	static class Foo {

		private @Id String id;
		private Long longValue;
		private String name;
		private Bar bar;

		public Foo() {

		}

		public Foo(String name) {
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Long getLongValue() {
			return longValue;
		}

		public void setLongValue(Long longValue) {
			this.longValue = longValue;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}

	}

	static class Bar {

		private String bar;

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}

	@Persistent
	static class WithNumericId {

		@Id Integer id;

	}
}
