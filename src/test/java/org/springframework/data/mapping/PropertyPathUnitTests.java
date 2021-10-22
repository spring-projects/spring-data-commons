/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.mapping.PropertyPath.from;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link PropertyPath}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@SuppressWarnings("unused")
class PropertyPathUnitTests {

	@Test
	void parsesSimplePropertyCorrectly() {

		PropertyPath reference = PropertyPath.from("userName", Foo.class);

		assertThat(reference.hasNext()).isFalse();
		assertThat(reference.toDotPath()).isEqualTo("userName");
		assertThat(reference.getOwningType()).isEqualTo(ClassTypeInformation.from(Foo.class));
	}

	@Test
	void parsesPathPropertyCorrectly() {

		PropertyPath reference = PropertyPath.from("userName", Bar.class);
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
		assertThat(reference.toDotPath()).isEqualTo("user.name");
	}

	@Test
	void prefersLongerMatches() {

		PropertyPath reference = PropertyPath.from("userName", Sample.class);
		assertThat(reference.hasNext()).isFalse();
		assertThat(reference.toDotPath()).isEqualTo("userName");
	}

	@Test
	void testname() {

		PropertyPath reference = PropertyPath.from("userName", Sample2.class);
		assertThat(reference.getSegment()).isEqualTo("user");
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	void prefersExplicitPaths() {

		PropertyPath reference = PropertyPath.from("user_name", Sample.class);
		assertThat(reference.getSegment()).isEqualTo("user");
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	void handlesGenericsCorrectly() {

		PropertyPath reference = PropertyPath.from("usersName", Bar.class);
		assertThat(reference.getSegment()).isEqualTo("users");
		assertThat(reference.isCollection()).isTrue();
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	void handlesMapCorrectly() {

		PropertyPath reference = PropertyPath.from("userMapName", Bar.class);
		assertThat(reference.getSegment()).isEqualTo("userMap");
		assertThat(reference.isCollection()).isFalse();
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	void handlesArrayCorrectly() {

		PropertyPath reference = PropertyPath.from("userArrayName", Bar.class);
		assertThat(reference.getSegment()).isEqualTo("userArray");
		assertThat(reference.isCollection()).isTrue();
		assertThat(reference.hasNext()).isTrue();
		assertThat(reference.next()).isEqualTo(new PropertyPath("name", FooBar.class));
	}

	@Test
	void handlesInvalidCollectionCompountTypeProperl() {

		try {
			PropertyPath.from("usersMame", Bar.class);
			fail("Expected PropertyReferenceException!");
		} catch (PropertyReferenceException e) {
			assertThat(e.getPropertyName()).isEqualTo("mame");
			assertThat(e.getBaseProperty()).isEqualTo(PropertyPath.from("users", Bar.class));
		}
	}

	@Test
	void handlesInvalidMapValueTypeProperly() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("userMapMame", Bar.class))//
				.matches(e -> e.getPropertyName().equals("mame"))//
				.matches(e -> e.getBaseProperty().equals(PropertyPath.from("userMap", Bar.class)));
	}

	@Test
	void findsNested() {

		PropertyPath from = PropertyPath.from("barUserName", Sample.class);

		assertThat(from).isNotNull();
		assertThat(from.getLeafProperty()).isEqualTo(PropertyPath.from("name", FooBar.class));
	}

	@Test // DATACMNS-45
	void handlesEmptyUnderscoresCorrectly() {

		PropertyPath propertyPath = PropertyPath.from("_foo", Sample2.class);
		assertThat(propertyPath.getSegment()).isEqualTo("_foo");
		assertThat(propertyPath.getType()).isEqualTo(Foo.class);

		propertyPath = PropertyPath.from("_foo__email", Sample2.class);
		assertThat(propertyPath.toDotPath()).isEqualTo("_foo._email");
	}

	@Test
	void supportsDotNotationAsWell() {

		PropertyPath propertyPath = PropertyPath.from("bar.userMap.name", Sample.class);

		assertThat(propertyPath).isNotNull();
		assertThat(propertyPath.getSegment()).isEqualTo("bar");
		assertThat(propertyPath.getLeafProperty()).isEqualTo(PropertyPath.from("name", FooBar.class));
	}

	@Test
	void returnsCorrectIteratorForSingleElement() {

		PropertyPath propertyPath = PropertyPath.from("userName", Foo.class);

		Iterator<PropertyPath> iterator = propertyPath.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(propertyPath);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void returnsCorrectIteratorForMultipleElement() {

		PropertyPath propertyPath = PropertyPath.from("user.name", Bar.class);

		Iterator<PropertyPath> iterator = propertyPath.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(propertyPath);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(propertyPath.next());
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test // DATACMNS-139
	void rejectsInvalidPropertyWithLeadingUnderscore() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("_id", Foo.class))//
				.withMessageContaining("property _id");
	}

	@Test // DATACMNS-139
	void rejectsNestedInvalidPropertyWithLeadingUnderscore() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("_foo_id", Sample2.class))//
				.withMessageContaining("property id");
	}

	@Test // DATACMNS-139
	void rejectsNestedInvalidPropertyExplictlySplitWithLeadingUnderscore() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> PropertyPath.from("_foo__id", Sample2.class))//
				.withMessageContaining("property _id");
	}

	@Test // DATACMNS 158
	void rejectsInvalidPathsContainingDigits() {
		assertThatExceptionOfType(PropertyReferenceException.class)
				.isThrownBy(() -> from("PropertyThatWillFail4Sure", Foo.class));
	}

	@Test // GH-2472
	void acceptsValidPathWithDigits() {
		assertThat(from("bar1", Sample.class)).isNotNull();
		assertThat(from("bar1foo", Sample.class)).isNotNull();
	}

	@Test // GH-2472
	void acceptsValidNestedPathWithDigits() {
		assertThat(from("sample.bar1", SampleHolder.class)).isNotNull();
		assertThat(from("sample.bar1foo", SampleHolder.class)).isNotNull();
		assertThat(from("sampleBar1", SampleHolder.class)).isNotNull();
		assertThat(from("sampleBar1foo", SampleHolder.class)).isNotNull();
	}

	@Test
	void rejectsInvalidProperty() {

		assertThatExceptionOfType(PropertyReferenceException.class)//
				.isThrownBy(() -> from("_foo_id", Sample2.class))//
				.matches(e -> e.getBaseProperty().getSegment().equals("_foo"));
	}

	@Test
	void samePathsEqual() {

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
	void hashCodeTests() {

		PropertyPath left = PropertyPath.from("user.name", Bar.class);
		PropertyPath right = PropertyPath.from("user.name", Bar.class);

		PropertyPath shortPath = PropertyPath.from("user", Bar.class);

		assertThat(left.hashCode()).isEqualTo(right.hashCode());
		assertThat(left.hashCode()).isNotEqualTo(shortPath.hashCode());
	}

	@Test // DATACMNS-257
	void findsAllUppercaseProperty() {

		PropertyPath path = PropertyPath.from("UUID", Foo.class);

		assertThat(path).isNotNull();
		assertThat(path.getSegment()).isEqualTo("UUID");
	}

	@Test // DATACMNS-257
	void findsNestedAllUppercaseProperty() {

		PropertyPath path = PropertyPath.from("_fooUUID", Sample2.class);

		assertThat(path).isNotNull();
		assertThat(path.getSegment()).isEqualTo("_foo");
		assertThat(path.hasNext()).isTrue();
		assertThat(path.next().getSegment()).isEqualTo("UUID");
	}

	@Test // DATACMNS-381
	void exposesPreviouslyReferencedPathInExceptionMessage() {

		assertThatExceptionOfType(PropertyReferenceException.class).isThrownBy(() -> from("userNameBar", Bar.class)) //
				.withMessageContaining("bar") // missing variable
				.withMessageContaining("String") // type
				.withMessageContaining("Bar.user.name"); // previously referenced path
	}

	@Test // DATACMNS-387
	void rejectsNullSource() {
		assertThatIllegalArgumentException().isThrownBy(() -> from(null, Foo.class));
	}

	@Test // DATACMNS-387
	void rejectsEmptySource() {
		assertThatIllegalArgumentException().isThrownBy(() -> from("", Foo.class));
	}

	@Test // DATACMNS-387
	void rejectsNullClass() {
		assertThatIllegalArgumentException().isThrownBy(() -> from("foo", (Class<?>) null));
	}

	@Test // DATACMNS-387
	void rejectsNullTypeInformation() {
		assertThatIllegalArgumentException().isThrownBy(() -> from("foo", (TypeInformation<?>) null));
	}

	@Test // DATACMNS-546
	void returnsCompletePathIfResolutionFailedCompletely() {

		assertThatExceptionOfType(PropertyReferenceException.class) //
				.isThrownBy(() -> from("somethingDifferent", Foo.class)).withMessageContaining("somethingDifferent");
	}

	@Test // DATACMNS-546
	void includesResolvedPathInExceptionMessage() {

		assertThatExceptionOfType(PropertyReferenceException.class) //
				.isThrownBy(() -> from("userFooName", Bar.class)) //
				.withMessageContaining("fooName") // missing variable
				.withMessageContaining(FooBar.class.getSimpleName()) // type
				.withMessageContaining("Bar.user"); // previously referenced path
	}

	@Test // DATACMNS-703
	void includesPropertyHintsOnTypos() {

		assertThatExceptionOfType(PropertyReferenceException.class) //
				.isThrownBy(() -> from("userAme", Foo.class)).withMessageContaining("userName");
	}

	@Test // DATACMNS-867
	void preservesUnderscoresForQuotedNames() {

		PropertyPath path = from(Pattern.quote("var_name_with_underscore"), Foo.class);

		assertThat(path).isNotNull();
		assertThat(path.getSegment()).isEqualTo("var_name_with_underscore");
		assertThat(path.hasNext()).isFalse();
	}

	@Test // DATACMNS-1120
	void cachesPropertyPathsByPathAndType() {
		assertThat(from("userName", Foo.class)).isSameAs(from("userName", Foo.class));
	}

	@Test // DATACMNS-1198
	void exposesLeafPropertyType() {
		assertThat(from("user.name", Bar.class).getLeafType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-1199
	void createsNestedPropertyPath() {
		assertThat(from("user", Bar.class).nested("name")).isEqualTo(from("user.name", Bar.class));
	}

	@Test // DATACMNS-1199
	void rejectsNonExistentNestedPath() {

		assertThatExceptionOfType(PropertyReferenceException.class) //
				.isThrownBy(() -> from("user", Bar.class).nested("nonexistant")) //
				.withMessageContaining("nonexistant") //
				.withMessageContaining("Bar.user");
	}

	@Test // DATACMNS-1285
	void rejectsTooLongPath() {

		String source = "foo.bar";

		for (int i = 0; i < 9; i++) {
			source = source + "." + source;
		}

		assertThat(source.split("\\.").length).isGreaterThan(1000);

		final String path = source;

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> PropertyPath.from(path, Left.class));
	}

	@Test // DATACMNS-1304
	void resolvesPropertyPathWithSingleUppercaseLetterPropertyEnding() {
		assertThat(from("categoryB", Product.class).toDotPath()).isEqualTo("categoryB");
	}

	@Test // DATACMNS-1304
	void resolvesPropertyPathWithUppercaseLettersPropertyEnding() {
		assertThat(from("categoryABId", Product.class).toDotPath()).isEqualTo("categoryAB.id");
	}

	@Test // DATACMNS-1304
	void detectsNestedSingleCharacterProperty() {
		assertThat(from("category_B", Product.class).toDotPath()).isEqualTo("category.b");
	}

	private class Foo {

		String userName;
		String _email;
		String UUID;
		String var_name_with_underscore;
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
		private Bar bar1;
		private Bar bar1foo;
	}

	private class SampleHolder {

		private Sample sample;
	}

	private class Sample2 {

		private String userNameWhatever;
		private FooBar user;
		private Foo _foo;
	}

	// DATACMNS-1285

	private class Left {
		Right foo;
	}

	private class Right {
		Left bar;
	}

	// DATACMNS-1304
	private class Product {
		Category category;
		Category categoryB;
		Category categoryAB;
	}

	private class Category {
		B b;
		String id;
	}

	private class B {}
}
