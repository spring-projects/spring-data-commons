/*
 * Copyright 2011-2018 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.util.ClassTypeInformation.from;

import javaslang.collection.Traversable;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.junit.Test;
import org.springframework.data.mapping.Person;

/**
 * Unit tests for {@link ClassTypeInformation}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ClassTypeInformationUnitTests {

	@Test
	public void discoversTypeForSimpleGenericField() {

		TypeInformation<ConcreteType> discoverer = ClassTypeInformation.from(ConcreteType.class);

		assertThat(discoverer.getType()).isEqualTo(ConcreteType.class);

		TypeInformation<?> content = discoverer.getProperty("content");

		assertThat(content.getType()).isEqualTo(String.class);
		assertThat(content.getComponentType()).isNull();
		assertThat(content.getMapValueType()).isNull();
	}

	@Test
	public void discoversTypeForNestedGenericField() {

		TypeInformation<ConcreteWrapper> discoverer = ClassTypeInformation.from(ConcreteWrapper.class);
		assertThat(discoverer.getType()).isEqualTo(ConcreteWrapper.class);

		assertThat(discoverer.getProperty("wrapped")).satisfies(it -> {
			assertThat(it.getType()).isEqualTo(GenericType.class);
			assertThat(it.getProperty("content")).satisfies(nested -> assertThat(nested.getType()).isEqualTo(String.class));
		});

		assertThat(discoverer.getProperty("wrapped.content"))
				.satisfies(it -> assertThat(it.getType()).isEqualTo(String.class));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundType() {

		TypeInformation<GenericTypeWithBound> information = ClassTypeInformation.from(GenericTypeWithBound.class);
		assertThat(information.getProperty("person")).satisfies(it -> assertThat(it.getType()).isEqualTo(Person.class));
	}

	@Test
	public void discoversBoundTypeForSpecialization() {

		TypeInformation<SpecialGenericTypeWithBound> information = ClassTypeInformation
				.from(SpecialGenericTypeWithBound.class);
		assertThat(information.getProperty("person"))
				.satisfies(it -> assertThat(it.getType()).isEqualTo(SpecialPerson.class));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundTypeForNested() {

		TypeInformation<AnotherGenericType> information = ClassTypeInformation.from(AnotherGenericType.class);

		assertThat(information.getProperty("nested"))
				.satisfies(it -> assertThat(it.getType()).isEqualTo(GenericTypeWithBound.class));
		assertThat(information.getProperty("nested.person"))
				.satisfies(it -> assertThat(it.getType()).isEqualTo(Person.class));
	}

	@Test
	public void discoversArraysAndCollections() {

		TypeInformation<StringCollectionContainer> information = ClassTypeInformation.from(StringCollectionContainer.class);

		TypeInformation<?> array = information.getProperty("array");

		assertThat(array.getComponentType().getType()).isEqualTo(String.class);
		assertThat(array.getType()).isEqualTo(String[].class);
		assertThat(array.getType().isArray()).isTrue();

		TypeInformation<?> foo = information.getProperty("foo");

		assertThat(foo.getType()).isEqualTo(Collection[].class);
		assertThat(foo.getComponentType()).satisfies(it -> {
			assertThat(it.getType()).isEqualTo(Collection.class);
			assertThat(it.getComponentType()).satisfies(nested -> assertThat(nested.getType()).isEqualTo(String.class));
		});

		TypeInformation<?> rawSet = information.getProperty("rawSet");

		assertThat(rawSet.getType()).isEqualTo(Set.class);
		assertThat(rawSet.getComponentType().getType()).isEqualTo(Object.class);
		assertThat(rawSet.getMapValueType()).isNull();
	}

	@Test
	public void discoversMapValueType() {

		TypeInformation<StringMapContainer> information = ClassTypeInformation.from(StringMapContainer.class);

		TypeInformation<?> genericMap = information.getProperty("genericMap");

		assertThat(genericMap.getType()).isEqualTo(Map.class);
		assertThat(genericMap.getMapValueType().getType()).isEqualTo(String.class);

		TypeInformation<?> map = information.getProperty("map");

		assertThat(map.getType()).isEqualTo(Map.class);
		assertThat(map.getMapValueType().getType()).isEqualTo(Calendar.class);
	}

	@Test
	public void typeInfoDoesNotEqualForGenericTypesWithDifferentParent() {

		TypeInformation<ConcreteWrapper> first = ClassTypeInformation.from(ConcreteWrapper.class);
		TypeInformation<AnotherConcreteWrapper> second = ClassTypeInformation.from(AnotherConcreteWrapper.class);

		assertThat(first.getProperty("wrapped").equals(second.getProperty("wrapped"))).isFalse();
	}

	@Test
	public void handlesPropertyFieldMismatchCorrectly() {

		TypeInformation<PropertyGetter> from = ClassTypeInformation.from(PropertyGetter.class);

		assertThat(from.getProperty("_name")).satisfies(it -> assertThat(it.getType()).isEqualTo(String.class));
		assertThat(from.getProperty("name")).satisfies(it -> assertThat(it.getType()).isEqualTo(byte[].class));
	}

	@Test // DATACMNS-77
	public void returnsSameInstanceForCachedClass() {

		TypeInformation<PropertyGetter> info = ClassTypeInformation.from(PropertyGetter.class);
		assertThat(ClassTypeInformation.from(PropertyGetter.class)).isSameAs(info);
	}

	@Test // DATACMNS-39
	public void resolvesWildCardTypeCorrectly() {

		TypeInformation<ClassWithWildCardBound> information = ClassTypeInformation.from(ClassWithWildCardBound.class);

		TypeInformation<?> wildcard = information.getProperty("wildcard");

		assertThat(wildcard.isCollectionLike()).isTrue();
		assertThat(wildcard.getComponentType().getType()).isEqualTo(String.class);

		TypeInformation<?> complexWildcard = information.getProperty("complexWildcard");

		assertThat(complexWildcard.isCollectionLike()).isTrue();
		assertThat(complexWildcard.getComponentType()).satisfies(it -> {
			assertThat(it.isCollectionLike()).isEqualTo(true);
			assertThat(it.getComponentType()).satisfies(nested -> assertThat(nested.getType()).isEqualTo(String.class));
		});
	}

	@Test
	public void resolvesTypeParametersCorrectly() {

		TypeInformation<ConcreteType> information = ClassTypeInformation.from(ConcreteType.class);
		TypeInformation<?> superTypeInformation = information.getSuperTypeInformation(GenericType.class);

		List<TypeInformation<?>> parameters = superTypeInformation.getTypeArguments();
		assertThat(parameters).hasSize(2);
		assertThat(parameters.get(0).getType()).isEqualTo(String.class);
		assertThat(parameters.get(1).getType()).isEqualTo(Object.class);
	}

	@Test
	public void resolvesNestedInheritedTypeParameters() {

		TypeInformation<SecondExtension> information = ClassTypeInformation.from(SecondExtension.class);
		TypeInformation<?> superTypeInformation = information.getSuperTypeInformation(Base.class);

		List<TypeInformation<?>> parameters = superTypeInformation.getTypeArguments();
		assertThat(parameters).hasSize(1);
		assertThat(parameters.get(0).getType()).isEqualTo(String.class);
	}

	@Test
	public void discoveresMethodParameterTypesCorrectly() throws Exception {

		TypeInformation<SecondExtension> information = ClassTypeInformation.from(SecondExtension.class);
		Method method = SecondExtension.class.getMethod("foo", Base.class);
		List<TypeInformation<?>> informations = information.getParameterTypes(method);
		TypeInformation<?> returnTypeInformation = information.getReturnType(method);

		assertThat(informations).hasSize(1);
		assertThat(informations.get(0).getType()).isEqualTo(Base.class);
		assertThat(informations.get(0)).isEqualTo(returnTypeInformation);
	}

	@Test
	public void discoversImplementationBindingCorrectlyForString() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("stringMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		TypeInformation<StringImplementation> stringInfo = from(StringImplementation.class);
		assertThat(parameterType.isAssignableFrom(stringInfo)).isTrue();
		assertThat(stringInfo.getSuperTypeInformation(GenericInterface.class)).isEqualTo(parameterType);
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class))).isFalse();
		assertThat(parameterType
				.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(GenericInterface.class))).isTrue();
	}

	@Test
	public void discoversImplementationBindingCorrectlyForLong() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("longMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class))).isFalse();
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class))).isTrue();
		assertThat(parameterType
				.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(GenericInterface.class))).isFalse();
	}

	@Test
	public void discoversImplementationBindingCorrectlyForNumber() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("boundToNumberMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class))).isFalse();
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class))).isTrue();
		assertThat(parameterType
				.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(GenericInterface.class))).isFalse();
	}

	@Test
	public void returnsComponentTypeForMultiDimensionalArrayCorrectly() {

		TypeInformation<?> information = from(String[][].class);

		assertThat(information.getType()).isEqualTo(String[][].class);
		assertThat(information.getComponentType()).satisfies(it -> assertThat(it.getType()).isEqualTo(String[].class));
		assertThat(information.getActualType().getActualType().getType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-309
	public void findsGetterOnInterface() {

		TypeInformation<Product> information = from(Product.class);

		assertThat(information.getProperty("category.id")).isEqualTo(from(Long.class));
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-387
	public void rejectsNullClass() {
		ClassTypeInformation.from(null);
	}

	@Test // DATACMNS-422
	public void returnsEmptyOptionalForRawTypesOnly() {

		assertThat(from(MyRawIterable.class).getComponentType()).isNull();
		assertThat(from(MyIterable.class).getComponentType()).isNotNull();
	}

	@Test // DATACMNS-440
	public void detectsSpecialMapAsMapValueType() {

		TypeInformation<?> seriously = ClassTypeInformation.from(SuperGenerics.class).getProperty("seriously");

		// Type
		assertThat(seriously.getType()).isEqualTo(SortedMap.class);

		// Map value type
		assertThat(seriously.getMapValueType()).satisfies(value -> {
			assertThat(value.getType()).isEqualTo(SortedMap.class);
			assertThat(value.getComponentType()).satisfies(it -> assertThat(it.getType()).isEqualTo(String.class));
		});

		assertThat(seriously.getMapValueType().getMapValueType()).satisfies(nestedValue -> {
			assertThat(nestedValue.getType()).isEqualTo(List.class);
			assertThat(nestedValue.getComponentType()).satisfies(it -> assertThat(it.getType()).isEqualTo(Person.class));
		});
	}

	@Test // DATACMNS-446
	public void createsToStringRepresentation() {
		assertThat(from(SpecialPerson.class).toString())
				.isEqualTo("org.springframework.data.util.ClassTypeInformationUnitTests$SpecialPerson");
	}

	@Test // DATACMNS-590
	public void resolvesNestedGenericsToConcreteType() {

		ClassTypeInformation<ConcreteRoot> rootType = from(ConcreteRoot.class);

		assertThat(rootType.getProperty("subs").getActualType().getProperty("subSub").getType())//
				.isEqualTo(ConcreteSubSub.class);
	}

	@Test // DATACMNS-594
	public void considersGenericsOfTypeBounds() {

		assertThat(ClassTypeInformation.from(ConcreteRootIntermediate.class)
				.getProperty("intermediate.content.intermediate.content").getType())//
						.isEqualTo(Leaf.class);
	}

	@Test // DATACMNS-783, DATACMNS-853
	public void specializesTypeUsingTypeVariableContext() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);

		assertThat(root.getProperty("abstractBar").specialize(ClassTypeInformation.from(Bar.class)))//
				.satisfies(it -> {
					assertThat(it.getType()).isEqualTo(Bar.class);
					assertThat(it.getProperty("field").getType()).isEqualTo(Character.class);
					assertThat(it.getProperty("anotherField").getType()).isEqualTo(Integer.class);
				});
	}

	@Test // DATACMNS-783
	public void usesTargetTypeDirectlyIfNoGenericsAreInvolved() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);
		ClassTypeInformation<?> from = ClassTypeInformation.from(Bar.class);

		assertThat(root.getProperty("object").specialize(from)).isEqualTo(from);
	}

	@Test // DATACMNS-855
	public void specializedTypeEqualsAndHashCode() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);

		TypeInformation<?> abstractBar = root.getProperty("abstractBar");

		assertThat(Pair.of(abstractBar.specialize(ClassTypeInformation.from(Bar.class)),
				abstractBar.specialize(ClassTypeInformation.from(Bar.class)))).satisfies(pair -> {
					assertThat(pair.getFirst()).isEqualTo(pair.getSecond());
					assertThat(pair.getSecond()).isEqualTo(pair.getFirst());
					assertThat(pair.getFirst().hashCode()).isEqualTo(pair.getSecond().hashCode());
				});
	}

	@Test // DATACMNS-896
	public void prefersLocalTypeMappingOverNestedWithSameGenericType() {

		ClassTypeInformation<Concrete> information = ClassTypeInformation.from(Concrete.class);

		assertThat(information.getProperty("field").getType()).isEqualTo(Nested.class);
	}

	@Test // DATACMNS-940
	public void detectsJavaslangTraversableComponentType() {

		ClassTypeInformation<SampleTraversable> information = ClassTypeInformation.from(SampleTraversable.class);

		assertThat(information.getComponentType().getType()).isAssignableFrom(Integer.class);
	}

	@Test // DATACMNS-940
	public void detectsJavaslangMapComponentAndValueType() {

		ClassTypeInformation<SampleMap> information = ClassTypeInformation.from(SampleMap.class);

		assertThat(information.getComponentType().getType()).isAssignableFrom(String.class);

		assertThat(information.getMapValueType().getType()).isAssignableFrom(Integer.class);
	}

	@Test // DATACMNS-1138
	public void usesTargetTypeForWildcardedBaseOnSpecialization() {

		ClassTypeInformation<WildcardedWrapper> wrapper = ClassTypeInformation.from(WildcardedWrapper.class);
		ClassTypeInformation<SomeConcrete> concrete = ClassTypeInformation.from(SomeConcrete.class);

		TypeInformation<?> property = wrapper.getRequiredProperty("wildcarded");

		assertThat(property.specialize(concrete)).isEqualTo(concrete);
	}

	static class StringMapContainer extends MapContainer<String> {

	}

	static class MapContainer<T> {
		Map<String, T> genericMap;
		Map<String, Calendar> map;
	}

	static class StringCollectionContainer extends CollectionContainer<String> {

	}

	@SuppressWarnings("rawtypes")
	static class CollectionContainer<T> {

		T[] array;
		Collection<T>[] foo;
		Set<String> set;
		Set rawSet;
	}

	static class GenericTypeWithBound<T extends Person> {

		T person;
	}

	static class AnotherGenericType<T extends Person, S extends GenericTypeWithBound<T>> {
		S nested;
	}

	static class SpecialGenericTypeWithBound extends GenericTypeWithBound<SpecialPerson> {

	}

	static abstract class SpecialPerson extends Person {
		protected SpecialPerson(Integer ssn, String firstName, String lastName) {
			super(ssn, firstName, lastName);
		}
	}

	static class GenericType<T, S> {

		Long index;
		T content;
	}

	static class ConcreteType extends GenericType<String, Object> {

	}

	static class GenericWrapper<S> {

		GenericType<S, Object> wrapped;
	}

	static class ConcreteWrapper extends GenericWrapper<String> {

	}

	static class AnotherConcreteWrapper extends GenericWrapper<Long> {

	}

	static class PropertyGetter {
		private String _name;

		public byte[] getName() {
			return _name.getBytes();
		}
	}

	static class ClassWithWildCardBound {
		List<? extends String> wildcard;
		List<? extends Collection<? extends String>> complexWildcard;
	}

	static class Base<T> {

	}

	static class FirstExtension<T> extends Base<String> {

		public Base<GenericWrapper<T>> foo(Base<GenericWrapper<T>> param) {
			return null;
		}
	}

	static class SecondExtension extends FirstExtension<Long> {

	}

	interface GenericInterface<T> {

	}

	interface TypedClient {

		void stringMethod(GenericInterface<String> param);

		void longMethod(GenericInterface<Long> param);

		void boundToNumberMethod(GenericInterface<? extends Number> param);
	}

	class StringImplementation implements GenericInterface<String> {

	}

	class LongImplementation implements GenericInterface<Long> {

	}

	interface Product {
		Category getCategory();
	}

	interface Category extends Identifiable {

	}

	interface Identifiable {
		Long getId();
	}

	@SuppressWarnings("rawtypes")
	interface MyRawIterable extends Iterable {}

	interface MyIterable<T> extends Iterable<T> {}

	static class SuperGenerics {

		SortedMap<String, ? extends SortedMap<String, List<Person>>> seriously;
	}

	// DATACMNS-590

	static abstract class GenericRoot<T extends GenericSub<?>> {
		List<T> subs;
	}

	static abstract class GenericSub<T extends GenericSubSub> {
		T subSub;
	}

	static abstract class GenericSubSub {}

	static class ConcreteRoot extends GenericRoot<ConcreteSub> {}

	static class ConcreteSub extends GenericSub<ConcreteSubSub> {}

	static class ConcreteSubSub extends GenericSubSub {
		String content;
	}

	// DATACMNS-594

	static class Intermediate<T> {
		T content;
	}

	static abstract class GenericRootIntermediate<T> {
		Intermediate<T> intermediate;
	}

	static abstract class GenericInnerIntermediate<T> {
		Intermediate<T> intermediate;
	}

	static class ConcreteRootIntermediate extends GenericRootIntermediate<ConcreteInnerIntermediate> {}

	static class ConcreteInnerIntermediate extends GenericInnerIntermediate<Leaf> {}

	static class Leaf {}

	static class TypeWithAbstractGenericType<T, S> {
		AbstractBar<T, S> abstractBar;
		Object object;
	}

	static class Foo extends TypeWithAbstractGenericType<Character, Integer> {}

	static abstract class AbstractBar<T, S> {}

	static class Bar<T, S> extends AbstractBar<T, S> {
		T field;
		S anotherField;
	}

	// DATACMNS-896

	static class SomeType<T> {
		T field;
	}

	static class Nested extends SomeType<String> {}

	static class Concrete extends SomeType<Nested> {}

	static interface SampleTraversable extends Traversable<Integer> {}

	static interface SampleMap extends javaslang.collection.Map<String, Integer> {}

	// DATACMNS-1138

	static class SomeGeneric<T> {
		T value;
	}

	static class SomeConcrete extends SomeGeneric<String> {}

	static class WildcardedWrapper {
		SomeGeneric<?> wildcarded;
	}
}
