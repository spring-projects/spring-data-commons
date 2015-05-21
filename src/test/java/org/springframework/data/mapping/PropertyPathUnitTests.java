/*
 * Copyright 2011-2015 the original author or authors.
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
import static org.springframework.data.mapping.PropertyPath.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link PropertyPath}.
 * 
 * @author Oliver Gierke
 */
@SuppressWarnings("unused")
public class PropertyPathUnitTests {

	@Rule public ExpectedException exception = ExpectedException.none();

	@Test
	@SuppressWarnings("rawtypes")
	public void parsesSimplePropertyCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Foo.class);
		assertThat(reference.hasNext(), is(false));
		assertThat(reference.toDotPath(), is("userName"));
		assertThat(reference.getOwningType(), is((TypeInformation) ClassTypeInformation.from(Foo.class)));
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

	@Test
	public void handlesInvalidCollectionCompountTypeProperl() {

		try {
			PropertyPath.from("usersMame", Bar.class);
			fail("Expected PropertyReferenceException!");
		} catch (PropertyReferenceException e) {
			assertThat(e.getPropertyName(), is("mame"));
			assertThat(e.getBaseProperty(), is(PropertyPath.from("users", Bar.class)));
		}
	}

	@Test
	public void handlesInvalidMapValueTypeProperly() {

		try {
			PropertyPath.from("userMapMame", Bar.class);
			fail();
		} catch (PropertyReferenceException e) {
			assertThat(e.getPropertyName(), is("mame"));
			assertThat(e.getBaseProperty(), is(PropertyPath.from("userMap", Bar.class)));
		}
	}

	@Test
	public void findsNested() {

		PropertyPath from = PropertyPath.from("barUserName", Sample.class);

		assertThat(from, is(notNullValue()));
		assertThat(from.getLeafProperty(), is(PropertyPath.from("name", FooBar.class)));
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

		PropertyPath propertyPath = PropertyPath.from("bar.userMap.name", Sample.class);

		assertThat(propertyPath, is(notNullValue()));
		assertThat(propertyPath.getSegment(), is("bar"));
		assertThat(propertyPath.getLeafProperty(), is(PropertyPath.from("name", FooBar.class)));
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

	/**
	 * @see DATACMNS-139
	 */
	@Test
	public void rejectsInvalidPropertyWithLeadingUnderscore() {
		try {
			PropertyPath.from("_id", Foo.class);
			fail();
		} catch (PropertyReferenceException e) {
			assertThat(e.getMessage(), containsString("property _id"));
		}
	}

	/**
	 * @see DATACMNS-139
	 */
	@Test
	public void rejectsNestedInvalidPropertyWithLeadingUnderscore() {
		try {
			PropertyPath.from("_foo_id", Sample2.class);
			fail();
		} catch (PropertyReferenceException e) {
			assertThat(e.getMessage(), containsString("property id"));
		}
	}

	/**
	 * @see DATACMNS-139
	 */
	@Test
	public void rejectsNestedInvalidPropertyExplictlySplitWithLeadingUnderscore() {
		try {
			PropertyPath.from("_foo__id", Sample2.class);
			fail();
		} catch (PropertyReferenceException e) {
			assertThat(e.getMessage(), containsString("property _id"));
		}
	}

	/**
	 * @see DATACMNS 158
	 */
	@Test(expected = PropertyReferenceException.class)
	public void rejectsInvalidPathsContainingDigits() {
		PropertyPath.from("PropertyThatWillFail4Sure", Foo.class);
	}

	@Test
	public void rejectsInvalidProperty() {

		try {
			PropertyPath.from("bar", Foo.class);
			fail();
		} catch (PropertyReferenceException e) {
			assertThat(e.getBaseProperty(), is(nullValue()));
		}
	}

	@Test
	public void samePathsEqual() {

		PropertyPath left = PropertyPath.from("user.name", Bar.class);
		PropertyPath right = PropertyPath.from("user.name", Bar.class);

		PropertyPath shortPath = PropertyPath.from("user", Bar.class);

		assertThat(left, is(right));
		assertThat(right, is(left));
		assertThat(left, is(not(shortPath)));
		assertThat(shortPath, is(not(left)));

		assertThat(left, is(not(new Object())));
	}

	@Test
	public void hashCodeTests() {

		PropertyPath left = PropertyPath.from("user.name", Bar.class);
		PropertyPath right = PropertyPath.from("user.name", Bar.class);

		PropertyPath shortPath = PropertyPath.from("user", Bar.class);

		assertThat(left.hashCode(), is(right.hashCode()));
		assertThat(left.hashCode(), is(not(shortPath.hashCode())));
	}

	/**
	 * @see DATACMNS-257
	 */
	@Test
	public void findsAllUppercaseProperty() {

		PropertyPath path = PropertyPath.from("UUID", Foo.class);

		assertThat(path, is(notNullValue()));
		assertThat(path.getSegment(), is("UUID"));
	}

	/**
	 * @see DATACMNS-257
	 */
	@Test
	public void findsNestedAllUppercaseProperty() {

		PropertyPath path = PropertyPath.from("_fooUUID", Sample2.class);

		assertThat(path, is(notNullValue()));
		assertThat(path.getSegment(), is("_foo"));
		assertThat(path.hasNext(), is(true));
		assertThat(path.next().getSegment(), is("UUID"));
	}

	/**
	 * @see DATACMNS-381
	 */
	@Test
	public void exposesPreviouslyReferencedPathInExceptionMessage() {

		exception.expect(PropertyReferenceException.class);
		exception.expectMessage("bar"); // missing variable
		exception.expectMessage("String"); // type
		exception.expectMessage("Bar.user.name"); // previously referenced path

		PropertyPath.from("userNameBar", Bar.class);
	}

	/**
	 * @see DATACMNS-387
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullSource() {
		from(null, Foo.class);
	}

	/**
	 * @see DATACMNS-387
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptySource() {
		from("", Foo.class);
	}

	/**
	 * @see DATACMNS-387
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullClass() {
		from("foo", (Class<?>) null);
	}

	/**
	 * @see DATACMNS-387
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTypeInformation() {
		from("foo", (TypeInformation<?>) null);
	}

	/**
	 * @see DATACMNS-546
	 */
	@Test
	public void returnsCompletePathIfResolutionFailedCompletely() {

		exception.expect(PropertyReferenceException.class);
		exception.expectMessage("somethingDifferent");

		from("somethingDifferent", Foo.class);
	}

	/**
	 * @see DATACMNS-546
	 */
	@Test
	public void includesResolvedPathInExceptionMessage() {

		exception.expect(PropertyReferenceException.class);
		exception.expectMessage("fooName");
		exception.expectMessage(FooBar.class.getSimpleName());
		exception.expectMessage("Bar.user");

		from("userFooName", Bar.class);
	}

	/**
	 * @see DATACMNS-703
	 */
	@Test
	public void includesPropertyHintsOnTypos() {

		exception.expect(PropertyReferenceException.class);
		exception.expectMessage("userName");

		from("userAme", Foo.class);
	}

	private class Foo {

		String userName;
		String _email;
		String UUID;
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
