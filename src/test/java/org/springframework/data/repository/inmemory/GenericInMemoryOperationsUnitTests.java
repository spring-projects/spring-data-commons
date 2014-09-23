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

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import java.util.List;

import net.sf.ehcache.search.expression.EqualTo;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.repository.inmemory.ehcache.EhCacheOperations;
import org.springframework.data.repository.inmemory.ehcache.EhCacheQuery;
import org.springframework.data.repository.inmemory.map.MapOperations;
import org.springframework.data.repository.inmemory.map.MapQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Christoph Strobl
 */
public abstract class GenericInMemoryOperationsUnitTests {

	private static final Foo FOO1 = new Foo("one");
	private static final Foo FOO2 = new Foo("two");

	private InMemoryOperations operations;

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException {
		this.operations = getInMemoryOperations();
	}

	@After
	public void tearDown() throws Exception {
		this.operations.destroy();
	}

	@Test
	public void foo() {
		operations.create("1", FOO1);
	}

	@Test
	public void getNothing() {
		assertNull(operations.read("1", Foo.class));
	}

	@Test
	public void getOne() {
		operations.create("1", FOO1);
		assertThat(operations.read("1", Foo.class), is(FOO1));
	}

	@Test
	public void getOneNoId() {
		operations.create("1", FOO1);
		assertThat(operations.read("2", Foo.class), nullValue());
	}

	@Test
	public void getOneDifferntType() {
		operations.create("1", FOO1);
		assertThat(operations.read("1", Bar.class), nullValue());
	}

	@Test
	public void update() {

		operations.create("1", FOO1);
		operations.update("1", FOO2);
		assertThat(operations.read("1", Foo.class), is(FOO2));
	}

	@Test
	public void delete() {
		operations.create("1", FOO1);
		operations.delete("1", Foo.class);
		assertThat(operations.read("1", Foo.class), nullValue());
	}

	@Test
	public void deleteReturnsNullWhenNotExisting() {
		operations.create("1", FOO1);
		assertThat(operations.delete("2", Foo.class), nullValue());
	}

	@Test
	public void returnsDeleted() {
		operations.create("1", FOO1);
		assertThat(operations.delete("1", Foo.class), is(FOO1));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void throwsErrorOnDuplicateInsert() {
		operations.create("1", FOO1);
		operations.create("1", FOO2);
	}

	@Test
	public void readMatching() {

		operations.create("1", FOO1);
		operations.create("2", FOO2);

		SpelExpressionParser parser = new SpelExpressionParser();

		InMemoryQuery q = null;

		if (operations instanceof MapOperations) {
			q = new MapQuery(parser.parseExpression("foo == 'two'"));
		}
		if (operations instanceof EhCacheOperations) {
			q = new EhCacheQuery(new EqualTo("foo", "two"));
		}

		List<Foo> result = (List<Foo>) operations.read(q, Foo.class);
		assertThat(result, IsCollectionWithSize.hasSize(1));
		assertThat(result.get(0), is(FOO2));
	}

	protected abstract InMemoryOperations getInMemoryOperations();

	static class Foo {

		@Persistent String foo;

		public Foo(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return foo;
		}

	}

	static class Bar {
		String bar;

		public Bar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return bar;
		}

	}

	static class FooBar {

		Foo foo;
		Bar bar;

		public Foo getFoo() {
			return foo;
		}

		public void setFoo(Foo foo) {
			this.foo = foo;
		}

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}

	}
}
