/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PersistentPropertyPaths;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.Streamable;
import org.springframework.util.StringUtils;

/**
 * Unit tests for {@link PersistentPropertyPathFactory}.
 *
 * @author Oliver Gierke
 * @soundtrack Cypress Hill - Illusions (Q-Tip Remix, Unreleased & Revamped)
 */
class PersistentPropertyPathFactoryUnitTests {

	PersistentPropertyPathFactory<BasicPersistentEntity<Object, SamplePersistentProperty>, SamplePersistentProperty> factory = //
			new PersistentPropertyPathFactory<>(new SampleMappingContext());

	@Test
	void doesNotTryToLookupPersistentEntityForLeafProperty() {
		assertThat(factory.from(Person.class, "name")).isNotNull();
	}

	@Test // DATACMNS-380
	void returnsPersistentPropertyPathForDotPath() {

		PersistentPropertyPath<SamplePersistentProperty> path = factory.from(PersonSample.class, "persons.name");

		assertThat(path.getLength()).isEqualTo(2);
		assertThat(path.getBaseProperty().getName()).isEqualTo("persons");
		assertThat(path.getLeafProperty().getName()).isEqualTo("name");
	}

	@Test // DATACMNS-380
	void rejectsInvalidPropertyReferenceWithMappingException() {
		assertThatExceptionOfType(MappingException.class).isThrownBy(() -> factory.from(PersonSample.class, "foo"));
	}

	@Test // DATACMNS-695
	void persistentPropertyPathTraversesGenericTypesCorrectly() {
		assertThat(factory.from(Outer.class, "field.wrapped.field")).hasSize(3);
	}

	@Test // DATACMNS-727
	void exposesContextForFailingPropertyPathLookup() {

		assertThatExceptionOfType(InvalidPersistentPropertyPath.class)//
				.isThrownBy(() -> factory.from(PersonSample.class, "persons.firstname"))//
				.matches(e -> StringUtils.hasText(e.getMessage()))//
				.matches(e -> e.getResolvedPath().equals("persons"))//
				.matches(e -> e.getUnresolvableSegment().equals("firstname"))//
				.matches(e -> factory.from(PersonSample.class, e.getResolvedPath()) != null);
	}

	@Test // DATACMNS-1116
	void cachesPersistentPropertyPaths() {

		assertThat(factory.from(PersonSample.class, "persons.name")) //
				.isSameAs(factory.from(PersonSample.class, "persons.name"));
	}

	@Test // DATACMNS-1275
	void findsNestedPropertyByFilter() {

		PersistentPropertyPaths<?, SamplePersistentProperty> paths = factory.from(Sample.class,
				property -> property.findAnnotation(Inject.class) != null);

		assertThat(paths).hasSize(1).anySatisfy(it -> it.toDotPath().equals("inner.annotatedField"));
	}

	@Test // DATACMNS-1275
	void findsNestedPropertiesByFilter() {

		PersistentPropertyPaths<?, SamplePersistentProperty> paths = factory.from(Wrapper.class,
				property -> property.findAnnotation(Inject.class) != null);

		assertThat(paths).hasSize(2);//
		assertThat(paths).element(0).satisfies(it -> it.toDotPath().equals("first.inner.annotatedField"));
		assertThat(paths).element(1).satisfies(it -> it.toDotPath().equals("second.inner.annotatedField"));
	}

	@Test // DATACMNS-1275
	void createsEmptyPropertyPathCorrectly() {
		assertThat(factory.from(Wrapper.class, "")).isEmpty();
	}

	@Test // DATACMNS-1275
	void returnsShortestsPathsFirst() {

		Streamable<String> paths = factory.from(First.class, it -> true, it -> true) //
				.map(PersistentPropertyPath::toDotPath);

		assertThat(paths).containsExactly("third", "second", "third.lastname", "second.firstname");
	}

	@Test // DATACMNS-1275
	void doesNotTraverseAssociationsByDefault() {

		Streamable<String> paths = factory.from(First.class, it -> true) //
				.map(PersistentPropertyPath::toDotPath);

		assertThat(paths) //
				.contains("third", "second", "second.firstname")//
				.doesNotContain("third.lastname");
	}

	@Test // DATACMNS-1275
	void traversesAssociationsIfTraversalGuardAllowsIt() {

		PersistentPropertyPaths<First, SamplePersistentProperty> paths = //
				factory.from(First.class, it -> true, it -> true);

		assertThat(paths.contains("third.lastname")).isTrue();
		assertThat(paths.contains(PropertyPath.from("third.lastname", First.class)));
	}

	@Test // DATACMNS-1275
	void returnsEmptyPropertyPathsIfNoneSelected() {

		PersistentPropertyPaths<Third, SamplePersistentProperty> paths = factory.from(Third.class, it -> false);

		assertThat(paths).isEmpty();
		assertThat(paths.getFirst()).isEmpty();
	}

	@Test // DATACMNS-1275
	void returnsShortestPathFirst() {

		PersistentPropertyPaths<First, SamplePersistentProperty> paths = factory.from(First.class, it -> !it.isEntity(),
				it -> true);

		assertThat(paths.contains("second.firstname")).isTrue();
		assertThat(paths.getFirst()) //
				.hasValueSatisfying(it -> assertThat(it.toDotPath()).isEqualTo("third.lastname"));
	}

	static class PersonSample {
		List<Person> persons;
	}

	class Person {
		String name;
	}

	static class Wrapper {
		Sample first;
		Sample second;
	}

	static class Sample {
		Inner inner;
	}

	static class Inner {
		@Inject String annotatedField;
		Sample cyclic;
	}

	static class Outer {
		Generic<Nested> field;
	}

	static class Generic<T> {
		T wrapped;
	}

	static class Nested {
		String field;
	}

	static class First {
		Second second;
		@Reference Third third;
	}

	static class Second {
		String firstname;
	}

	static class Third {
		String lastname;
	}
}
