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
package org.springframework.data.repository.inmemory;

import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.support.ReflectionEntityInformation;

@RunWith(MockitoJUnitRunner.class)
public class BasicInMemoryRepositoryUnitTests {

	private BasicInMemoryRepository<Foo, String> repo;
	private @Mock InMemoryOperations opsMock;

	@Before
	public void setUp() {

		ReflectionEntityInformation<Foo, String> ei = new ReflectionEntityInformation<BasicInMemoryRepositoryUnitTests.Foo, String>(
				Foo.class);
		repo = new BasicInMemoryRepository<Foo, String>(ei, opsMock);
	}

	@Test
	public void testSaveNew() {

		Foo foo = new Foo("one");

		repo.save(foo);
		assertThat(foo.getId(), notNullValue());
		verify(opsMock, times(1)).create(eq(foo.getId()), eq(foo));
	}

	@Test
	public void testDoubleSave() {

		Foo foo = new Foo("one");

		repo.save(foo);
		repo.save(foo);
		verify(opsMock, times(1)).create(eq(foo.getId()), eq(foo));
		verify(opsMock, times(1)).update(eq(foo.getId()), eq(foo));
	}

	@Test
	public void multipleSave() {

		Foo one = new Foo("one");
		Foo two = new Foo("one");

		repo.save(Arrays.asList(one, two));
		verify(opsMock, times(1)).create(eq(one.getId()), eq(one));
		verify(opsMock, times(1)).create(eq(two.getId()), eq(two));
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

}
