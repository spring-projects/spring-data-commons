/*
 * Copyright 2011-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.util.ClassTypeInformation.*;

import javaslang.collection.Traversable;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.data.mapping.Person;

/**
 * Unit tests for {@link ClassTypeInformation}.
 * 
 * @author Oliver Gierke
 */
public class ClassTypeInformationUnitTests {

	@Test
	public void discoversTypeForSimpleGenericField() {

		TypeInformation<ConcreteType> discoverer = ClassTypeInformation.from(ConcreteType.class);
		assertEquals(ConcreteType.class, discoverer.getType());
		TypeInformation<?> content = discoverer.getProperty("content");
		assertEquals(String.class, content.getType());
		assertNull(content.getComponentType());
		assertNull(content.getMapValueType());
	}

	@Test
	public void discoversTypeForNestedGenericField() {

		TypeInformation<ConcreteWrapper> discoverer = ClassTypeInformation.from(ConcreteWrapper.class);
		assertEquals(ConcreteWrapper.class, discoverer.getType());
		TypeInformation<?> wrapper = discoverer.getProperty("wrapped");
		assertEquals(GenericType.class, wrapper.getType());
		TypeInformation<?> content = wrapper.getProperty("content");

		assertEquals(String.class, content.getType());
		assertEquals(String.class, discoverer.getProperty("wrapped").getProperty("content").getType());
		assertEquals(String.class, discoverer.getProperty("wrapped.content").getType());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundType() {

		TypeInformation<GenericTypeWithBound> information = ClassTypeInformation.from(GenericTypeWithBound.class);
		assertEquals(Person.class, information.getProperty("person").getType());
	}

	@Test
	public void discoversBoundTypeForSpecialization() {

		TypeInformation<SpecialGenericTypeWithBound> information = ClassTypeInformation
				.from(SpecialGenericTypeWithBound.class);
		assertEquals(SpecialPerson.class, information.getProperty("person").getType());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundTypeForNested() {

		TypeInformation<AnotherGenericType> information = ClassTypeInformation.from(AnotherGenericType.class);
		assertEquals(GenericTypeWithBound.class, information.getProperty("nested").getType());
		assertEquals(Person.class, information.getProperty("nested.person").getType());
	}

	@Test
	public void discoversArraysAndCollections() {
		TypeInformation<StringCollectionContainer> information = ClassTypeInformation.from(StringCollectionContainer.class);

		TypeInformation<?> property = information.getProperty("array");
		assertThat(property.getComponentType().getType(), is((Object) String.class));

		Class<?> type = property.getType();
		assertEquals(String[].class, type);
		assertThat(type.isArray(), is(true));

		property = information.getProperty("foo");
		assertEquals(Collection[].class, property.getType());
		assertEquals(Collection.class, property.getComponentType().getType());
		assertEquals(String.class, property.getComponentType().getComponentType().getType());

		property = information.getProperty("rawSet");
		assertEquals(Set.class, property.getType());
		assertThat(property.getComponentType().getType(), is(Matchers.<Class<?>> equalTo(Object.class)));
		assertNull(property.getMapValueType());
	}

	@Test
	public void discoversMapValueType() {

		TypeInformation<StringMapContainer> information = ClassTypeInformation.from(StringMapContainer.class);
		TypeInformation<?> genericMap = information.getProperty("genericMap");
		assertEquals(Map.class, genericMap.getType());
		assertEquals(String.class, genericMap.getMapValueType().getType());

		TypeInformation<?> map = information.getProperty("map");
		assertEquals(Map.class, map.getType());
		assertEquals(Calendar.class, map.getMapValueType().getType());
	}

	@Test
	public void typeInfoDoesNotEqualForGenericTypesWithDifferentParent() {

		TypeInformation<ConcreteWrapper> first = ClassTypeInformation.from(ConcreteWrapper.class);
		TypeInformation<AnotherConcreteWrapper> second = ClassTypeInformation.from(AnotherConcreteWrapper.class);

		assertFalse(first.getProperty("wrapped").equals(second.getProperty("wrapped")));
	}

	@Test
	public void handlesPropertyFieldMismatchCorrectly() {

		TypeInformation<PropertyGetter> from = ClassTypeInformation.from(PropertyGetter.class);

		TypeInformation<?> property = from.getProperty("_name");
		assertThat(property, is(notNullValue()));
		assertThat(property.getType(), is(typeCompatibleWith(String.class)));

		property = from.getProperty("name");
		assertThat(property, is(notNullValue()));
		assertThat(property.getType(), is(typeCompatibleWith(byte[].class)));
	}

	@Test // DATACMNS-77
	public void returnsSameInstanceForCachedClass() {

		TypeInformation<PropertyGetter> info = ClassTypeInformation.from(PropertyGetter.class);
		assertThat(ClassTypeInformation.from(PropertyGetter.class), is(sameInstance(info)));
	}

	@Test // DATACMNS-39
	public void resolvesWildCardTypeCorrectly() {

		TypeInformation<ClassWithWildCardBound> information = ClassTypeInformation.from(ClassWithWildCardBound.class);

		TypeInformation<?> property = information.getProperty("wildcard");
		assertThat(property.isCollectionLike(), is(true));
		assertThat(property.getComponentType().getType(), is(typeCompatibleWith(String.class)));

		property = information.getProperty("complexWildcard");
		assertThat(property.isCollectionLike(), is(true));

		TypeInformation<?> component = property.getComponentType();
		assertThat(component.isCollectionLike(), is(true));
		assertThat(component.getComponentType().getType(), is(typeCompatibleWith(String.class)));
	}

	@Test
	public void resolvesTypeParametersCorrectly() {

		TypeInformation<ConcreteType> information = ClassTypeInformation.from(ConcreteType.class);
		TypeInformation<?> superTypeInformation = information.getSuperTypeInformation(GenericType.class);

		List<TypeInformation<?>> parameters = superTypeInformation.getTypeArguments();
		assertThat(parameters, hasSize(2));
		assertThat(parameters.get(0).getType(), is((Object) String.class));
		assertThat(parameters.get(1).getType(), is((Object) Object.class));
	}

	@Test
	public void resolvesNestedInheritedTypeParameters() {

		TypeInformation<SecondExtension> information = ClassTypeInformation.from(SecondExtension.class);
		TypeInformation<?> superTypeInformation = information.getSuperTypeInformation(Base.class);

		List<TypeInformation<?>> parameters = superTypeInformation.getTypeArguments();
		assertThat(parameters, hasSize(1));
		assertThat(parameters.get(0).getType(), is((Object) String.class));
	}

	@Test
	public void discoveresMethodParameterTypesCorrectly() throws Exception {

		TypeInformation<SecondExtension> information = ClassTypeInformation.from(SecondExtension.class);
		Method method = SecondExtension.class.getMethod("foo", Base.class);
		List<TypeInformation<?>> informations = information.getParameterTypes(method);
		TypeInformation<?> returnTypeInformation = information.getReturnType(method);

		assertThat(informations, hasSize(1));
		assertThat(informations.get(0).getType(), is((Object) Base.class));
		assertThat(informations.get(0), is((Object) returnTypeInformation));
	}

	@Test
	public void discoversImplementationBindingCorrectlyForString() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("stringMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		TypeInformation<StringImplementation> stringInfo = from(StringImplementation.class);
		assertThat(parameterType.isAssignableFrom(stringInfo), is(true));
		assertThat(stringInfo.getSuperTypeInformation(GenericInterface.class), is((Object) parameterType));
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class)), is(false));
		assertThat(parameterType
				.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(GenericInterface.class)), is(true));
	}

	@Test
	public void discoversImplementationBindingCorrectlyForLong() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("longMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class)), is(false));
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class)), is(true));
		assertThat(parameterType
				.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(GenericInterface.class)), is(false));
	}

	@Test
	public void discoversImplementationBindingCorrectlyForNumber() throws Exception {

		TypeInformation<TypedClient> information = from(TypedClient.class);
		Method method = TypedClient.class.getMethod("boundToNumberMethod", GenericInterface.class);

		TypeInformation<?> parameterType = information.getParameterTypes(method).get(0);

		assertThat(parameterType.isAssignableFrom(from(StringImplementation.class)), is(false));
		assertThat(parameterType.isAssignableFrom(from(LongImplementation.class)), is(true));
		assertThat(parameterType
				.isAssignableFrom(from(StringImplementation.class).getSuperTypeInformation(GenericInterface.class)), is(false));
	}

	@Test
	public void returnsComponentTypeForMultiDimensionalArrayCorrectly() {

		TypeInformation<?> information = from(String[][].class);
		assertThat(information.getType(), is((Object) String[][].class));
		assertThat(information.getComponentType().getType(), is((Object) String[].class));
		assertThat(information.getActualType().getActualType().getType(), is((Object) String.class));
	}

	@Test // DATACMNS-309
	@SuppressWarnings("rawtypes")
	public void findsGetterOnInterface() {

		TypeInformation<Product> information = from(Product.class);
		TypeInformation<?> categoryIdInfo = information.getProperty("category.id");

		assertThat(categoryIdInfo, is(notNullValue()));
		assertThat(categoryIdInfo, is((TypeInformation) from(Long.class)));
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-387
	public void rejectsNullClass() {
		from(null);
	}

	@Test // DATACMNS-422
	public void returnsNullForRawTypesOnly() {

		assertThat(from(MyRawIterable.class).getComponentType(), is(nullValue()));
		assertThat(from(MyIterable.class).getComponentType(), is(notNullValue()));
	}

	@Test // DATACMNS-440
	public void detectsSpecialMapAsMapValueType() {

		TypeInformation<SuperGenerics> information = ClassTypeInformation.from(SuperGenerics.class);

		TypeInformation<?> propertyInformation = information.getProperty("seriously");
		assertThat(propertyInformation.getType(), is((Object) SortedMap.class));

		TypeInformation<?> mapValueType = propertyInformation.getMapValueType();
		assertThat(mapValueType.getType(), is((Object) SortedMap.class));
		assertThat(mapValueType.getComponentType().getType(), is((Object) String.class));

		TypeInformation<?> nestedValueType = mapValueType.getMapValueType();
		assertThat(nestedValueType.getType(), is((Object) List.class));
		assertThat(nestedValueType.getComponentType().getType(), is((Object) Person.class));
	}

	@Test // DATACMNS-446
	public void createsToStringRepresentation() {

		assertThat(from(SpecialPerson.class).toString(),
				is("org.springframework.data.util.ClassTypeInformationUnitTests$SpecialPerson"));
	}

	@Test // DATACMNS-590
	public void resolvesNestedGenericsToConcreteType() {

		ClassTypeInformation<ConcreteRoot> rootType = from(ConcreteRoot.class);
		TypeInformation<?> subsPropertyType = rootType.getProperty("subs");
		TypeInformation<?> subsElementType = subsPropertyType.getActualType();
		TypeInformation<?> subSubType = subsElementType.getProperty("subSub");

		assertThat(subSubType.getType(), is((Object) ConcreteSubSub.class));
	}

	@Test // DATACMNS-594
	public void considersGenericsOfTypeBounds() {

		ClassTypeInformation<ConcreteRootIntermediate> customer = ClassTypeInformation.from(ConcreteRootIntermediate.class);
		TypeInformation<?> leafType = customer.getProperty("intermediate.content.intermediate.content");

		assertThat(leafType.getType(), is((Object) Leaf.class));
	}

	@Test // DATACMNS-783, DATACMNS-853
	public void specializesTypeUsingTypeVariableContext() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> property = root.getProperty("abstractBar");

		TypeInformation<?> specialized = property.specialize(ClassTypeInformation.from(Bar.class));

		assertThat(specialized.getType(), is((Object) Bar.class));
		assertThat(specialized.getProperty("field").getType(), is((Object) Character.class));
		assertThat(specialized.getProperty("anotherField").getType(), is((Object) Integer.class));
	}

	@Test // DATACMNS-783
	@SuppressWarnings("rawtypes")
	public void usesTargetTypeDirectlyIfNoGenericsAreInvolved() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> property = root.getProperty("object");

		ClassTypeInformation<?> from = ClassTypeInformation.from(Bar.class);
		assertThat(property.specialize(from), is((TypeInformation) from));
	}

	@Test // DATACMNS-855
	@SuppressWarnings("rawtypes")
	public void specializedTypeEqualsAndHashCode() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> property = root.getProperty("abstractBar");

		TypeInformation left = property.specialize(ClassTypeInformation.from(Bar.class));
		TypeInformation right = property.specialize(ClassTypeInformation.from(Bar.class));

		assertThat(left, is(right));
		assertThat(right, is(left));
		assertThat(left.hashCode(), is(right.hashCode()));
	}

	@Test // DATACMNS-896
	public void prefersLocalTypeMappingOverNestedWithSameGenericType() {

		ClassTypeInformation<Concrete> information = ClassTypeInformation.from(Concrete.class);

		assertThat(information.getProperty("field").getType(), is(typeCompatibleWith(Nested.class)));
	}

	@Test // DATACMNS-940
	public void detectsJavaslangTraversableComponentType() {

		ClassTypeInformation<SampleTraversable> information = ClassTypeInformation.from(SampleTraversable.class);

		assertThat(information.getComponentType().getType(), is(typeCompatibleWith(Integer.class)));
	}

	@Test // DATACMNS-940
	public void detectsJavaslangMapComponentAndValueType() {

		ClassTypeInformation<SampleMap> information = ClassTypeInformation.from(SampleMap.class);

		assertThat(information.getComponentType().getType(), is(typeCompatibleWith(String.class)));
		assertThat(information.getMapValueType().getType(), is(typeCompatibleWith(Integer.class)));
	}

	@Test // DATACMNS-1138
	@SuppressWarnings("rawtypes")
	public void usesTargetTypeForWildcardedBaseOnSpecialization() {

		ClassTypeInformation<WildcardedWrapper> wrapper = ClassTypeInformation.from(WildcardedWrapper.class);
		ClassTypeInformation<SomeConcrete> concrete = ClassTypeInformation.from(SomeConcrete.class);

		TypeInformation<?> property = wrapper.getProperty("wildcarded");

		assertThat(property.specialize(concrete), is((TypeInformation) concrete));
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
