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

import static org.assertj.core.api.Assertions.*;
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
	public void parsesSimplePropertyCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Foo.class);

		assertThat(reference.hasNext()).isFalse();
		assertThat(reference.toDotPath()).isEqualTo("userName");
		assertThat(reference.getOwningType()).isEqualTo(ClassTypeInformation.from(Foo.class));
	}

	@Test
	public void parsesPathPropertyCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Bar.class);
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
		assertThat(reference.toDotPath()).isEqualTo("user.name");
	}

	@Test
	public void prefersLongerMatches() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Sample.class);
		assertThat(reference.hasNext()).isFalse();
		assertThat(reference.toDotPath()).isEqualTo("userName");
	}

	@Test
	public void testname() throws Exception {

		PropertyPath reference = PropertyPath.from("userName", Sample2.class);
		assertThat(reference.getSegment()).isEqualTo("user");
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	public void prefersExplicitPaths() throws Exception {

		PropertyPath reference = PropertyPath.from("user_name", Sample.class);
		assertThat(reference.getSegment()).isEqualTo("user");
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	public void handlesGenericsCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("usersName", Bar.class);
		assertThat(reference.getSegment()).isEqualTo("users");
		assertThat(reference.isCollection()).isTrue();
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	public void handlesMapCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userMapName", Bar.class);
		assertThat(reference.getSegment()).isEqualTo("userMap");
		assertThat(reference.isCollection()).isFalse();
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	public void handlesArrayCorrectly() throws Exception {

		PropertyPath reference = PropertyPath.from("userArrayName", Bar.class);
		assertThat(reference.getSegment()).isEqualTo("userArray");
		assertThat(reference.isCollection()).isTrue();
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	public void handlesInvalidCollectionCompountTypeProperl() {

		try {
			PropertyPath.from("usersMame", Bar.class);
			fail("Expected PropertyReferenceException!");
		} catch (PropertyReferenceException e) {
			assertThat(e.getPropertyName()).isEqualTo("mame");
			assertThat(e.getBaseProperty()).isEqualTo(PropertyPath.from("users", Bar.class));
		}
	}

	@Test
	public void handlesInvalidMapValueTypeProperly() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("userMapMame", Bar.class))//
				.matches(e -> e.getPropertyName().equals("mame"))//
				.matches(e -> e.getBaseProperty().equals(PropertyPath.from("userMap", Bar.class)));
	}

	@Test
	public void findsNested() {

		PropertyPath from = PropertyPath.from("barUserName", Sample.class);

		assertThat(from).isNotNull();
		assertThat(from.getLeafProperty()).isEqualTo(PropertyPath.from("name", FooBar.class));
	}

	/**
	 * @see DATACMNS-45
	 */
	@Test
	public void handlesEmptyUnderscoresCorrectly() {

		PropertyPath propertyPath = PropertyPath.from("_foo", Sample2.class);
		assertThat(propertyPath.getSegment()).isEqualTo("_foo");
		assertThat(propertyPath.getType()).isEqualTo(Foo.class);

		propertyPath = PropertyPath.from("_foo__email", Sample2.class);
		assertThat(propertyPath.toDotPath()).isEqualTo("_foo._email");
	}

	@Test
	public void supportsDotNotationAsWell() {

		PropertyPath propertyPath = PropertyPath.from("bar.userMap.name", Sample.class);

		assertThat(propertyPath).isNotNull();
		assertThat(propertyPath.getSegment()).isEqualTo("bar");
		assertThat(propertyPath.getLeafProperty()).isEqualTo(PropertyPath.from("name", FooBar.class));
	}

	@Test
	public void returnsCorrectIteratorForSingleElement() {

		PropertyPath propertyPath = PropertyPath.from("userName", Foo.class);

		Iterator<PropertyPath> iterator = propertyPath.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(propertyPath);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void returnsCorrectIteratorForMultipleElement() {

		PropertyPath propertyPath = PropertyPath.from("user.name", Bar.class);

		Iterator<PropertyPath> iterator = propertyPath.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(propertyPath);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(propertyPath.next());
		assertThat(iterator.hasNext()).isFalse();
	}

	/**
	 * @see DATACMNS-139
	 */
	@Test
	public void rejectsInvalidPropertyWithLeadingUnderscore() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("_id", Foo.class))//
				.withMessageContaining("property _id");
	}

	/**
	 * @see DATACMNS-139
	 */
	@Test
	public void rejectsNestedInvalidPropertyWithLeadingUnderscore() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("_foo_id", Sample2.class))//
				.withMessageContaining("property id");
	}

	/**
	 * @see DATACMNS-139
	 */
	@Test
	public void rejectsNestedInvalidPropertyExplictlySplitWithLeadingUnderscore() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("_foo__id", Sample2.class))//
				.withMessageContaining("property _id");
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

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("_foo_id", Sample2.class))//
				.matches(e -> e.getBaseProperty().getSegment().equals("_foo"));
	}

	@Test
	public void samePathsEqual() {

		PropertyPath left = PropertyPath.from("user.name", Bar.class);
		PropertyPath right = PropertyPath.from("user.name", Bar.class);

		PropertyPath shortPath = PropertyPath.from("user", Bar.class);

		assertThat(left).isEqualTo(right);
		assertThat(right).isEqualTo(left);
		assertThat(left).isNotEqualTo(shortPath);
		assertThat(shortPath).isNotEqualTo(left);

		assertThat(left).isNotEqualTo(new Object());
	}

	@Test
	public void hashCodeTests() {

		PropertyPath left = PropertyPath.from("user.name", Bar.class);
		PropertyPath right = PropertyPath.from("user.name", Bar.class);

		PropertyPath shortPath = PropertyPath.from("user", Bar.class);

		assertThat(left.hashCode()).isEqualTo(right.hashCode());
		assertThat(left.hashCode()).isNotEqualTo(shortPath.hashCode());
	}

	/**
	 * @see DATACMNS-257
	 */
	@Test
	public void findsAllUppercaseProperty() {

		PropertyPath path = PropertyPath.from("UUID", Foo.class);

		assertThat(path).isNotNull();
		assertThat(path.getSegment()).isEqualTo("UUID");
	}

	/**
	 * @see DATACMNS-257
	 */
	@Test
	public void findsNestedAllUppercaseProperty() {

		PropertyPath path = PropertyPath.from("_fooUUID", Sample2.class);

		assertThat(path).isNotNull();
		assertThat(path.getSegment()).isEqualTo("_foo");
		assertThat(path.hasNext()).isTrue();
		assertThat(path.next().getSegment()).isEqualTo("UUID");
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
