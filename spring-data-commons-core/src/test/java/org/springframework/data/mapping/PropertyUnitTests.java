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
package org.springframework.data.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.data.mapping.PropertyPath;

/**
 * Unit tests for {@link PropertyPath}.
 * 
 * @author Oliver Gierke
 */
@SuppressWarnings("unused")
public class PropertyUnitTests {

	@Test
	public void parsesSimplePropertyCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Foo.class);
		assertThat(reference.hasNext(), is(false));
		assertThat(reference.toDotPath(), is("userName"));
	}

	@Test
	public void parsesPathPropertyCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Bar.class);
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new PropertyPath("name", FooBar.class)));
		assertThat(reference.toDotPath(), is("user.name"));
	}

	@Test
	public void prefersLongerMatches() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Sample.class);
		assertThat(reference.hasNext(), is(false));
		assertThat(reference.toDotPath(), is("userName"));
	}

	@Test
	public void testname() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Sample2.class);
		assertThat(reference.getSegment(), is("user"));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new PropertyPath("name", FooBar.class)));
	}

	@Test
	public void prefersExplicitPaths() throws Exception {

		PropertyPath reference = PropertyPath.from("user_name", Sample.class);
		assertThat(reference.getSegment(), is("user"));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new PropertyPath("name", FooBar.class)));
	}

	@Test
	public void handlesGenericsCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("usersName", Bar.class);
		assertThat(reference.getSegment(), is("users"));
		assertThat(reference.isCollection(), is(true));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new PropertyPath("name", FooBar.class)));
	}

	@Test
	public void handlesMapCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userMapName", Bar.class);
		assertThat(reference.getSegment(), is("userMap"));
		assertThat(reference.isCollection(), is(false));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new PropertyPath("name", FooBar.class)));
	}

	@Test
	public void handlesArrayCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userArrayName", Bar.class);
		assertThat(reference.getSegment(), is("userArray"));
		assertThat(reference.isCollection(), is(true));
		assertThat(reference.hasNext(), is(true));
		assertThat(reference.next(), is(new PropertyPath("name", FooBar.class)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void handlesInvalidCollectionCompountTypeProperl() {

		PropertyPath.from("usersMame", Bar.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void handlesInvalidMapValueTypeProperl() {

		PropertyPath.from("userMapMame", Bar.class);
	}

	@Test
	public void findsNested() {

		PropertyPath from = PropertyPath.from("barUserName", Sample.class);
	}

	/**
	 * @see DATACMNS-45
	 */
	@Test
	public void handlesEmptyUnderscoresCorrectly() {

		PropertyPath propertyPath = PropertyPath.from("_foo", Sample2.class);
		assertThat(propertyPath.getSegment(), is("_foo"));
		assertThat(propertyPath.getType(), is(typeCompatibleWith(Foo.class)));

		propertyPath = PropertyPath.from("_foo__email", Sample2.class);
		assertThat(propertyPath.toDotPath(), is("_foo._email"));
	}

	@Test
	public void supportsDotNotationAsWell() {
		PropertyPath.from("bar.userMap.name", Sample.class);
	}

	@Test
	public void returnsCorrectIteratorForSingleElement() {

		PropertyPath propertyPath = PropertyPath.from("userName", Foo.class);

		Iterator<PropertyPath> iterator = propertyPath.iterator();
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(propertyPath));
		assertThat(iterator.hasNext(), is(false));
	}

	@Test
	public void returnsCorrectIteratorForMultipleElement() {

		PropertyPath propertyPath = PropertyPath.from("user.name", Bar.class);

		Iterator<PropertyPath> iterator = propertyPath.iterator();
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(propertyPath));
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is(propertyPath.next()));
		assertThat(iterator.hasNext(), is(false));
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
