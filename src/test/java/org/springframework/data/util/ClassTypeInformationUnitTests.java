/*
 * Copyright 2011-2014 the original author or authors.
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
import static org.springframework.data.util.ClassTypeInformation.*;

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
		TypeInformation<?> wrapper = discoverer.getProperty("wrapped");
		assertThat(wrapper.getType()).isEqualTo(GenericType.class);
		TypeInformation<?> content = wrapper.getProperty("content");

		assertThat(content.getType()).isEqualTo(String.class);
		assertThat(discoverer.getProperty("wrapped").getProperty("content").getType()).isEqualTo(String.class);
		assertThat(discoverer.getProperty("wrapped.content").getType()).isEqualTo(String.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundType() {

		TypeInformation<GenericTypeWithBound> information = ClassTypeInformation.from(GenericTypeWithBound.class);
		assertThat(information.getProperty("person").getType()).isEqualTo(Person.class);
	}

	@Test
	public void discoversBoundTypeForSpecialization() {

		TypeInformation<SpecialGenericTypeWithBound> information = ClassTypeInformation
				.from(SpecialGenericTypeWithBound.class);
		assertThat(information.getProperty("person").getType()).isEqualTo(SpecialPerson.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundTypeForNested() {

		TypeInformation<AnotherGenericType> information = ClassTypeInformation.from(AnotherGenericType.class);
		assertThat(information.getProperty("nested").getType()).isEqualTo(GenericTypeWithBound.class);
		assertThat(information.getProperty("nested.person").getType()).isEqualTo(Person.class);
	}

	@Test
	public void discoversArraysAndCollections() {
		TypeInformation<StringCollectionContainer> information = ClassTypeInformation.from(StringCollectionContainer.class);

		TypeInformation<?> property = information.getProperty("array");
		assertThat(property.getComponentType().getType()).isEqualTo(String.class);

		Class<?> type = property.getType();
		assertThat(type).isEqualTo(String[].class);
		assertThat(type.isArray()).isTrue();

		property = information.getProperty("foo");
		assertThat(property.getType()).isEqualTo(Collection[].class);
		assertThat(property.getComponentType().getType()).isEqualTo(Collection.class);
		assertThat(property.getComponentType().getComponentType().getType()).isEqualTo(String.class);

		property = information.getProperty("rawSet");
		assertThat(property.getType()).isEqualTo(Set.class);
		assertThat(property.getComponentType().getType()).isEqualTo(Object.class);
		assertThat(property.getMapValueType()).isNull();
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

		TypeInformation<?> property = from.getProperty("_name");
		assertThat(property).isNotNull();
		assertThat(property.getType()).isEqualTo(String.class);

		property = from.getProperty("name");
		assertThat(property).isNotNull();
		assertThat(property.getType()).isEqualTo(byte[].class);
	}

	/**
	 * @see DATACMNS-77
	 */
	@Test
	public void returnsSameInstanceForCachedClass() {

		TypeInformation<PropertyGetter> info = ClassTypeInformation.from(PropertyGetter.class);
		assertThat(ClassTypeInformation.from(PropertyGetter.class)).isSameAs(info);
	}

	/**
	 * @see DATACMNS-39
	 */
	@Test
	public void resolvesWildCardTypeCorrectly() {

		TypeInformation<ClassWithWildCardBound> information = ClassTypeInformation.from(ClassWithWildCardBound.class);

		TypeInformation<?> property = information.getProperty("wildcard");
		assertThat(property.isCollectionLike()).isTrue();
		assertThat(property.getComponentType().getType()).isEqualTo(String.class);

		property = information.getProperty("complexWildcard");
		assertThat(property.isCollectionLike()).isTrue();

		TypeInformation<?> component = property.getComponentType();
		assertThat(component.isCollectionLike()).isTrue();
		assertThat(component.getComponentType().getType()).isEqualTo(String.class);
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
		assertThat(information.getComponentType().getType()).isEqualTo(String[].class);
		assertThat(information.getActualType().getActualType().getType()).isEqualTo(String.class);
	}

	/**
	 * @see DATACMNS-309
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void findsGetterOnInterface() {

		TypeInformation<Product> information = from(Product.class);
		TypeInformation<?> categoryIdInfo = information.getProperty("category.id");

		assertThat(categoryIdInfo).isNotNull();
		assertThat(categoryIdInfo).isEqualTo((TypeInformation) from(Long.class));
	}

	/**
	 * @see DATACMNS-387
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullClass() {
		from(null);
	}

	/**
	 * @see DATACMNS-422
	 */
	@Test
	public void returnsNullForRawTypesOnly() {

		assertThat(from(MyRawIterable.class).getComponentType()).isNull();
		assertThat(from(MyIterable.class).getComponentType()).isNotNull();
	}

	/**
	 * @see DATACMNS-440
	 */
	@Test
	public void detectsSpecialMapAsMapValueType() {

		TypeInformation<SuperGenerics> information = ClassTypeInformation.from(SuperGenerics.class);

		TypeInformation<?> propertyInformation = information.getProperty("seriously");
		assertThat(propertyInformation.getType()).isEqualTo(SortedMap.class);

		TypeInformation<?> mapValueType = propertyInformation.getMapValueType();
		assertThat(mapValueType.getType()).isEqualTo(SortedMap.class);
		assertThat(mapValueType.getComponentType().getType()).isEqualTo(String.class);

		TypeInformation<?> nestedValueType = mapValueType.getMapValueType();
		assertThat(nestedValueType.getType()).isEqualTo(List.class);
		assertThat(nestedValueType.getComponentType().getType()).isEqualTo(Person.class);
	}

	/**
	 * @see DATACMNS-446
	 */
	@Test
	public void createsToStringRepresentation() {
		assertThat(from(SpecialPerson.class).toString())
				.isEqualTo("org.springframework.data.util.ClassTypeInformationUnitTests$SpecialPerson");
	}

	/**
	 * @see DATACMNS-590
	 */
	@Test
	public void resolvesNestedGenericsToConcreteType() {

		ClassTypeInformation<ConcreteRoot> rootType = from(ConcreteRoot.class);
		TypeInformation<?> subsPropertyType = rootType.getProperty("subs");
		TypeInformation<?> subsElementType = subsPropertyType.getActualType();
		TypeInformation<?> subSubType = subsElementType.getProperty("subSub");

		assertThat(subSubType.getType()).isEqualTo(ConcreteSubSub.class);
	}

	/**
	 * @see DATACMNS-594
	 */
	@Test
	public void considersGenericsOfTypeBounds() {

		ClassTypeInformation<ConcreteRootIntermediate> customer = ClassTypeInformation.from(ConcreteRootIntermediate.class);
		TypeInformation<?> leafType = customer.getProperty("intermediate.content.intermediate.content");

		assertThat(leafType.getType()).isEqualTo(Leaf.class);
	}

	/**
	 * @see DATACMNS-783
	 * @see DATACMNS-853
	 */
	@Test
	public void specializesTypeUsingTypeVariableContext() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> property = root.getProperty("abstractBar");

		TypeInformation<?> specialized = property.specialize(ClassTypeInformation.from(Bar.class));

		assertThat(specialized.getType()).isEqualTo(Bar.class);
		assertThat(specialized.getProperty("field").getType()).isEqualTo(Character.class);
		assertThat(specialized.getProperty("anotherField").getType()).isEqualTo(Integer.class);
	}

	/**
	 * @see DATACMNS-783
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void usesTargetTypeDirectlyIfNoGenericsAreInvolved() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> property = root.getProperty("object");

		ClassTypeInformation<?> from = ClassTypeInformation.from(Bar.class);
		assertThat(property.specialize(from)).isEqualTo((TypeInformation) from);
	}

	/**
	 * @see DATACMNS-855
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void specializedTypeEqualsAndHashCode() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> property = root.getProperty("abstractBar");

		TypeInformation left = property.specialize(ClassTypeInformation.from(Bar.class));
		TypeInformation right = property.specialize(ClassTypeInformation.from(Bar.class));

		assertThat(left).isEqualTo(right);
		assertThat(right).isEqualTo(left);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	/**
	 * @see DATACMNS-896
	 */
	@Test
	public void prefersLocalTypeMappingOverNestedWithSameGenericType() {

		ClassTypeInformation<Concrete> information = ClassTypeInformation.from(Concrete.class);

		assertThat(information.getProperty("field").getType()).isEqualTo(Nested.class);
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
}
