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
package org.springframework.data.repository.query.parser;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.typeCompatibleWith;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link Property}.
 *
 * @author Oliver Gierke
 */
@SuppressWarnings("unused")
public class PropertyUnitTests {

	@Test
	public void parsesSimplePropertyCorrectly() throws Exception {

		Property reference = Property.from("userName", Foo.class);
		assertThat(reference.hasNext(), is(false));
		assertThat(reference.toDotPath(), is("userName"));
	}

	@Test
	public void parsesPathPropertyCorrectly() throws Exception {

		Property reference = Property.from("userName", Bar.class);
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new Property("name", FooBar.class)));
		assertThat(reference.toDotPath(), is("user.name"));
	}

	@Test
	public void prefersLongerMatches() throws Exception {

		Property reference = Property.from("userName", Sample.class);
		assertThat(reference.hasNext(), is(false));
		assertThat(reference.toDotPath(), is("userName"));
	}

	@Test
	public void testname() throws Exception {

		Property reference = Property.from("userName", Sample2.class);
		assertThat(reference.getName(), is("user"));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new Property("name", FooBar.class)));
	}

	@Test
	public void prefersExplicitPaths() throws Exception {

		Property reference = Property.from("user_name", Sample.class);
		assertThat(reference.getName(), is("user"));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new Property("name", FooBar.class)));
	}

	@Test
	public void handlesGenericsCorrectly() throws Exception {

		Property reference = Property.from("usersName", Bar.class);
		assertThat(reference.getName(), is("users"));
		assertThat(reference.isCollection(), is(true));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new Property("name", FooBar.class)));
	}

	@Test
	public void handlesMapCorrectly() throws Exception {

		Property reference = Property.from("userMapName", Bar.class);
		assertThat(reference.getName(), is("userMap"));
		assertThat(reference.isCollection(), is(false));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new Property("name", FooBar.class)));
	}

	@Test
	public void handlesArrayCorrectly() throws Exception {

		Property reference = Property.from("userArrayName", Bar.class);
		assertThat(reference.getName(), is("userArray"));
		assertThat(reference.isCollection(), is(true));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new Property("name", FooBar.class)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void handlesInvalidCollectionCompountTypeProperl() {

		Property.from("usersMame", Bar.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void handlesInvalidMapValueTypeProperl() {

		Property.from("userMapMame", Bar.class);
	}

	@Test
	public void findsNested() {

		Property from = Property.from("barUserName", Sample.class);
	}

	/**
	 * @see DATACMNS-45
	 */
	@Test
	public void handlesEmptyUnderscoresCorrectly() {

		Property property = Property.from("_foo", Sample2.class);
		assertThat(property.getName(), is("_foo"));
		assertThat(property.getType(), is(typeCompatibleWith(Foo.class)));

		property = Property.from("_foo__email", Sample2.class);
		assertThat(property.toDotPath(), is("_foo._email"));
	}

	@Test
	public void supportsDotNotationAsWell() {
		Property.from("bar.userMap.name", Sample.class);
	}

	private class Foo {

		String userName;
		String _email;
	}

	private class Bar {

		private FooBar user;
		private Set<FooBar> users;
		private Map<String, FooBar> userMap;
		private FooBar[] userArray;
	}

	private class FooBar {

		private String name;
	}

	private class Sample {

		private String userName;
		private FooBar user;
		private Bar bar;
	}

	private class Sample2 {

		private String userNameWhatever;
		private FooBar user;
		private Foo _foo;
	}
}
