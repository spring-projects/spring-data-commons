/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.core;

import static org.assertj.core.api.Assertions.*;

import io.vavr.collection.Traversable;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.aopalliance.aop.Advice;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.core.ResolvableType;
import org.springframework.data.mapping.Person;
import org.springframework.data.util.Pair;

/**
 * Unit tests for {@link ClassTypeInformation}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ClassTypeInformationUnitTests {

	@Test // GH-3340
	void typeInformationConstantsShouldNotBeNull() {

		assertThat(ClassTypeInformation.COLLECTION).isNotNull();
		assertThat(TypeInformation.COLLECTION).isNotNull();
		assertThat(TypeInformation.LIST).isNotNull();
		assertThat(TypeInformation.SET).isNotNull();
		assertThat(TypeInformation.MAP).isNotNull();
		assertThat(TypeInformation.OBJECT).isNotNull();
		assertThat(ClassTypeInformation.OBJECT).isNotNull();

		assertThat(TypeInformation.COLLECTION).isEqualTo(ClassTypeInformation.COLLECTION);
		assertThat(TypeInformation.LIST).isEqualTo(ClassTypeInformation.LIST);
		assertThat(TypeInformation.SET).isEqualTo(ClassTypeInformation.SET);
		assertThat(TypeInformation.MAP).isEqualTo(ClassTypeInformation.MAP);
		assertThat(TypeInformation.OBJECT).isEqualTo(ClassTypeInformation.OBJECT);
	}

	@Test
	public void discoversTypeForSimpleGenericField() {

		TypeInformation<ConcreteType> discoverer = TypeInformation.of(ConcreteType.class);

		assertThat(discoverer.getType()).isEqualTo(ConcreteType.class);

		var content = discoverer.getProperty("content");

		assertThat(content.getType()).isEqualTo(String.class);
		assertThat(content.getComponentType()).isNull();
		assertThat(content.getMapValueType()).isNull();
	}

	@Test
	public void discoversTypeForNestedGenericField() {

		TypeInformation<ConcreteWrapper> discoverer = TypeInformation.of(ConcreteWrapper.class);
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

		TypeInformation<GenericTypeWithBound> information = TypeInformation.of(GenericTypeWithBound.class);
		assertThat(information.getProperty("person")).satisfies(it -> assertThat(it.getType()).isEqualTo(Person.class));
	}

	@Test
	public void discoversBoundTypeForSpecialization() {

		TypeInformation<SpecialGenericTypeWithBound> information = TypeInformation.of(SpecialGenericTypeWithBound.class);
		assertThat(information.getProperty("person"))
				.satisfies(it -> assertThat(it.getType()).isEqualTo(SpecialPerson.class));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void discoversBoundTypeForNested() {

		TypeInformation<AnotherGenericType> information = TypeInformation.of(AnotherGenericType.class);

		assertThat(information.getProperty("nested"))
				.satisfies(it -> assertThat(it.getType()).isEqualTo(GenericTypeWithBound.class));
		assertThat(information.getProperty("nested.person"))
				.satisfies(it -> assertThat(it.getType()).isEqualTo(Person.class));
	}

	@Test
	public void discoversArraysAndCollections() {

		TypeInformation<StringCollectionContainer> information = TypeInformation.of(StringCollectionContainer.class);

		var array = information.getProperty("array");

		assertThat(array.getComponentType().getType()).isEqualTo(String.class);
		assertThat(array.getType()).isEqualTo(String[].class);
		assertThat(array.getType().isArray()).isTrue();

		var foo = information.getProperty("foo");

		assertThat(foo.getType()).isEqualTo(Collection[].class);
		assertThat(foo.getComponentType()).satisfies(it -> {
			assertThat(it.getType()).isEqualTo(Collection.class);
			assertThat(it.getComponentType()).satisfies(nested -> assertThat(nested.getType()).isEqualTo(String.class));
		});

		var rawSet = information.getProperty("rawSet");

		assertThat(rawSet.getType()).isEqualTo(Set.class);
		assertThat(rawSet.getComponentType().getType()).isEqualTo(Object.class);
		assertThat(rawSet.getMapValueType()).isNull();
	}

	@Test
	public void discoversMapValueType() {

		TypeInformation<StringMapContainer> information = TypeInformation.of(StringMapContainer.class);

		var genericMap = information.getProperty("genericMap");

		assertThat(genericMap.getType()).isEqualTo(Map.class);
		assertThat(genericMap.getMapValueType().getType()).isEqualTo(String.class);

		var map = information.getProperty("map");

		assertThat(map.getType()).isEqualTo(Map.class);
		assertThat(map.getMapValueType().getType()).isEqualTo(Calendar.class);
	}

	@Test
	public void typeInfoDoesNotEqualForGenericTypesWithDifferentParent() {

		TypeInformation<ConcreteWrapper> first = TypeInformation.of(ConcreteWrapper.class);
		TypeInformation<AnotherConcreteWrapper> second = TypeInformation.of(AnotherConcreteWrapper.class);

		assertThat(first.getProperty("wrapped").equals(second.getProperty("wrapped"))).isFalse();
	}

	@Test
	public void handlesPropertyFieldMismatchCorrectly() {

		TypeInformation<PropertyGetter> from = TypeInformation.of(PropertyGetter.class);

		assertThat(from.getProperty("_name")).satisfies(it -> assertThat(it.getType()).isEqualTo(String.class));
		assertThat(from.getProperty("name")).satisfies(it -> assertThat(it.getType()).isEqualTo(byte[].class));
	}

	@Test // DATACMNS-77
	public void returnsSameInstanceForCachedClass() {

		TypeInformation<PropertyGetter> info = TypeInformation.of(PropertyGetter.class);
		assertThat(TypeInformation.of(PropertyGetter.class)).isSameAs(info);
	}

	@Test // DATACMNS-39
	public void resolvesWildCardTypeCorrectly() {

		TypeInformation<ClassWithWildCardBound> information = TypeInformation.of(ClassWithWildCardBound.class);

		var wildcard = information.getProperty("wildcard");

		assertThat(wildcard.isCollectionLike()).isTrue();
		assertThat(wildcard.getComponentType().getType()).isEqualTo(String.class);

		var complexWildcard = information.getProperty("complexWildcard");

		assertThat(complexWildcard.isCollectionLike()).isTrue();
		assertThat(complexWildcard.getComponentType()).satisfies(it -> {
			assertThat(it.isCollectionLike()).isEqualTo(true);
			assertThat(it.getComponentType()).satisfies(nested -> assertThat(nested.getType()).isEqualTo(String.class));
		});
	}

	@Test
	public void resolvesTypeParametersCorrectly() {

		TypeInformation<ConcreteType> information = TypeInformation.of(ConcreteType.class);
		var superTypeInformation = information.getSuperTypeInformation(GenericType.class);

		var parameters = superTypeInformation.getTypeArguments();
		assertThat(parameters).hasSize(2);
		assertThat(parameters.get(0).getType()).isEqualTo(String.class);
		assertThat(parameters.get(1).getType()).isEqualTo(Object.class);
	}

	@Test
	public void resolvesNestedInheritedTypeParameters() {

		TypeInformation<SecondExtension> information = TypeInformation.of(SecondExtension.class);
		var superTypeInformation = information.getSuperTypeInformation(Base.class);

		var parameters = superTypeInformation.getTypeArguments();
		assertThat(parameters).hasSize(1);
		assertThat(parameters.get(0).getType()).isEqualTo(String.class);
	}

	@Test
	public void discoveresMethodParameterTypesCorrectly() throws Exception {

		TypeInformation<SecondExtension> information = TypeInformation.of(SecondExtension.class);
		var method = SecondExtension.class.getMethod("foo", Base.class);
		var informations = information.getParameterTypes(method);
		var returnTypeInformation = information.getReturnType(method);

		assertThat(informations).hasSize(1);
		assertThat(informations.get(0).getType()).isEqualTo(Base.class);
		assertThat(informations.get(0)).isEqualTo(returnTypeInformation);
	}

	@Test
	public void discoversImplementationBindingCorrectlyForString() throws Exception {

		TypeInformation<TypedClient> information = TypeInformation.of(TypedClient.class);
		var method = TypedClient.class.getMethod("stringMethod", GenericInterface.class);

		var parameterType = information.getParameterTypes(method).get(0);

		TypeInformation<StringImplementation> stringInfo = TypeInformation.of(StringImplementation.class);
		assertThat(parameterType.isAssignableFrom(stringInfo)).isTrue();
		assertThat(stringInfo.getSuperTypeInformation(GenericInterface.class)).isEqualTo(parameterType);
		assertThat(parameterType.isAssignableFrom(TypeInformation.of(LongImplementation.class))).isFalse();
		assertThat(parameterType
				.isAssignableFrom(
						TypeInformation.of(StringImplementation.class).getSuperTypeInformation(GenericInterface.class))).isTrue();
	}

	@Test
	public void discoversImplementationBindingCorrectlyForLong() throws Exception {

		TypeInformation<TypedClient> information = TypeInformation.of(TypedClient.class);
		var method = TypedClient.class.getMethod("longMethod", GenericInterface.class);

		var parameterType = information.getParameterTypes(method).get(0);

		assertThat(parameterType.isAssignableFrom(TypeInformation.of(StringImplementation.class))).isFalse();
		assertThat(parameterType.isAssignableFrom(TypeInformation.of(LongImplementation.class))).isTrue();
		assertThat(parameterType
				.isAssignableFrom(
						TypeInformation.of(StringImplementation.class).getSuperTypeInformation(GenericInterface.class))).isFalse();
	}

	@Test
	public void discoversImplementationBindingCorrectlyForNumber() throws Exception {

		TypeInformation<TypedClient> information = TypeInformation.of(TypedClient.class);
		var method = TypedClient.class.getMethod("boundToNumberMethod", GenericInterface.class);

		var parameterType = information.getParameterTypes(method).get(0);

		assertThat(parameterType.isAssignableFrom(TypeInformation.of(StringImplementation.class))).isFalse();
		assertThat(parameterType.isAssignableFrom(TypeInformation.of(LongImplementation.class))).isTrue();
		assertThat(parameterType
				.isAssignableFrom(
						TypeInformation.of(StringImplementation.class).getSuperTypeInformation(GenericInterface.class))).isFalse();
	}

	@Test
	public void returnsComponentTypeForMultiDimensionalArrayCorrectly() {

		TypeInformation<?> information = TypeInformation.of(String[][].class);

		assertThat(information.getType()).isEqualTo(String[][].class);
		assertThat(information.getComponentType()).satisfies(it -> assertThat(it.getType()).isEqualTo(String[].class));
		assertThat(information.getActualType().getActualType().getType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-309
	public void findsGetterOnInterface() {

		TypeInformation<Product> information = TypeInformation.of(Product.class);

		assertThat(information.getProperty("category.id")).isEqualTo(TypeInformation.of(Long.class));
	}

	@Test // DATACMNS-387
	public void rejectsNullClass() {
		assertThatIllegalArgumentException().isThrownBy(() -> TypeInformation.of((Class<?>) null));
	}

	@Test // DATACMNS-387
	public void rejectsNullResolvableType() {
		assertThatIllegalArgumentException().isThrownBy(() -> TypeInformation.of((ResolvableType) null));
	}

	@Test // DATACMNS-422
	public void returnsEmptyOptionalForRawTypesOnly() {

		assertThat(TypeInformation.of(MyRawIterable.class).getComponentType()).isNull();
		assertThat(TypeInformation.of(MyIterable.class).getComponentType()).isNotNull();
	}

	@Test // DATACMNS-440
	public void detectsSpecialMapAsMapValueType() {

		var seriously = TypeInformation.of(SuperGenerics.class).getProperty("seriously");

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
		assertThat(TypeInformation.of(SpecialPerson.class).toString())
				.isEqualTo("org.springframework.data.core.ClassTypeInformationUnitTests$SpecialPerson");
	}

	@Test // DATACMNS-590
	public void resolvesNestedGenericsToConcreteType() {

		var rootType = TypeInformation.of(ConcreteRoot.class);

		assertThat(rootType.getProperty("subs").getActualType().getProperty("subSub").getType())//
				.isEqualTo(ConcreteSubSub.class);
	}

	@Test // DATACMNS-594
	public void considersGenericsOfTypeBounds() {

		assertThat(TypeInformation.of(ConcreteRootIntermediate.class)
				.getProperty("intermediate.content.intermediate.content").getType())//
						.isEqualTo(Leaf.class);
	}

	@Test // DATACMNS-783, DATACMNS-853
	public void specializesTypeUsingTypeVariableContext() {

		var root = TypeInformation.of(Foo.class);

		assertThat(root.getProperty("abstractBar").specialize(TypeInformation.of(Bar.class)))//
				.satisfies(it -> {
					assertThat(it.getType()).isEqualTo(Bar.class);
					assertThat(it.getProperty("field").getType()).isEqualTo(Character.class);
					assertThat(it.getProperty("anotherField").getType()).isEqualTo(Integer.class);
				});
	}

	@Test // DATACMNS-783
	public void usesTargetTypeDirectlyIfNoGenericsAreInvolved() {

		var root = TypeInformation.of(Foo.class);
		TypeInformation<?> from = TypeInformation.of(Bar.class);

		assertThat(root.getProperty("object").specialize(from)).isEqualTo(from);
	}

	@Test // DATACMNS-855
	public void specializedTypeEqualsAndHashCode() {

		var root = TypeInformation.of(Foo.class);

		var abstractBar = root.getProperty("abstractBar");

		Assertions.assertThat(Pair.of(abstractBar.specialize(TypeInformation.of(Bar.class)),
				abstractBar.specialize(TypeInformation.of(Bar.class)))).satisfies(pair -> {
					assertThat(pair.getFirst()).isEqualTo(pair.getSecond());
					assertThat(pair.getSecond()).isEqualTo(pair.getFirst());
					assertThat(pair.getFirst().hashCode()).isEqualTo(pair.getSecond().hashCode());
				});
	}

	@Test // DATACMNS-896
	public void prefersLocalTypeMappingOverNestedWithSameGenericType() {

		var information = TypeInformation.of(Concrete.class);

		assertThat(information.getProperty("field").getType()).isEqualTo(Nested.class);
	}

	@Test // DATACMNS-940
	public void detectsVavrTraversableComponentType() {

		var information = TypeInformation.of(SampleTraversable.class);

		assertThat(information.getComponentType().getType()).isAssignableFrom(Integer.class);
	}

	@Test // DATACMNS-940
	public void detectsVavrMapComponentAndValueType() {

		var information = TypeInformation.of(SampleMap.class);

		assertThat(information.getComponentType().getType()).isAssignableFrom(String.class);

		assertThat(information.getMapValueType().getType()).isAssignableFrom(Integer.class);
	}

	@Test // DATACMNS-1138
	public void usesTargetTypeForWildcardedBaseOnSpecialization() {

		var wrapper = TypeInformation.of(WildcardedWrapper.class);
		var concrete = TypeInformation.of(SomeConcrete.class);

		var property = wrapper.getRequiredProperty("wildcarded");

		assertThat(property.specialize(concrete)).isEqualTo(concrete);
	}

	@Test // DATACMNS-1571
	public void considersGenericsOfTypeToSpecializeToIfFullyResolved() {

		TypeInformation<StoredEvent> storeEvent = TypeInformation.of(StoredEvent.class);
		assertThat(storeEvent.getType()).isEqualTo(StoredEvent.class);

		var domainEvent = (TypeInformation<DomainEvent>) storeEvent.getProperty("event");
		assertThat(domainEvent.getType()).isEqualTo(DomainEvent.class);

		var specialized = domainEvent
				.specialize(TypeInformation.of(OfferCreated.class));

		assertThat(specialized.getType()).isEqualTo(OfferCreated.class);
		assertThat(specialized.getProperty("aggregateId").getType()).isEqualTo(Long.class);
		assertThat(specialized.getProperty("root").getType()).isEqualTo(OfferDetails.class);
	}

	@Test // DATACMNS-1571
	public void mergesGenericsFromContextAndProvidedDefaultOnSpecialization() {

		TypeInformation<StoredEvent> storeEvent = TypeInformation.of(StoredEvent.class);
		assertThat(storeEvent.getType()).isEqualTo(StoredEvent.class);

		var domainEvent = (TypeInformation<DomainEvent>) storeEvent.getProperty("event");
		assertThat(domainEvent.getType()).isEqualTo(DomainEvent.class);

		var specialized = domainEvent
				.specialize(TypeInformation.of(GenericEvent.class));

		assertThat(specialized.getType()).isEqualTo(GenericEvent.class);
		assertThat(specialized.getProperty("aggregateId").getType()).isEqualTo(Long.class);
		assertThat(specialized.getProperty("root").getType()).isEqualTo(Aggregate.class);
	}

	@Test // DATACMNS-1828
	void discoversMapKeyAndValueTypeFromTypedMap() {

		TypeInformation<TypeWithTypedMap> information = TypeInformation.of(TypeWithTypedMap.class);

		var typedMap = information.getProperty("typedMap");

		assertThat(typedMap.getType()).isEqualTo(StringKeyMap.class);
		assertThat(typedMap.isMap()).isTrue();
		assertThat(typedMap.getRequiredComponentType().getType()).isEqualTo(String.class);
		assertThat(typedMap.getMapValueType().getType()).isEqualTo(Long.class);

		var longMultiValueMap = information.getProperty("longMultiValueMap");

		assertThat(longMultiValueMap.getType()).isEqualTo(MultiValueMap.class);
		assertThat(longMultiValueMap.getRequiredComponentType().getType()).isEqualTo(String.class);
		assertThat(longMultiValueMap.getMapValueType().getType()).isEqualTo(List.class);
		assertThat(longMultiValueMap.getMapValueType().getRequiredActualType().getType()).isEqualTo(Long.class);

		var justMap = information.getProperty("justMap");

		assertThat(justMap.getType()).isEqualTo(Map.class);
		assertThat(justMap.getRequiredComponentType().getType()).isEqualTo(String.class);
		assertThat(justMap.getMapValueType().getType()).isEqualTo(Long.class);
	}

	@Test // GH-2485
	public void proxyTypeInformationShouldNotEqualUserClassTypeInfo() {

		var typeInfoLeaf = TypeInformation.of(Leaf.class);
		var typeInformationLeafProxy = TypeInformation
				.of(Leaf$$SpringProxy$873fa2e.class);

		assertThat(typeInfoLeaf).isNotEqualTo(typeInformationLeafProxy);
	}

	@Test // GH-2312
	void typeInfoShouldPreserveGenericParameter() {

		var wrapperTypeInfo = TypeInformation.of(Wrapper.class);
		var fieldTypeInfo = wrapperTypeInfo.getProperty("field");
		var valueTypeInfo = fieldTypeInfo.getProperty("value");

		assertThat(valueTypeInfo.getType()).isEqualTo(Leaf.class);
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

	static interface SampleMap extends io.vavr.collection.Map<String, Integer> {}

	// DATACMNS-1138

	static class SomeGeneric<T> {
		T value;
	}

	static class SomeConcrete extends SomeGeneric<String> {}

	static class GenericExtendingSomeGeneric<T> extends SomeGeneric<T> {}

	static class Wrapper {
		GenericExtendingSomeGeneric<Leaf> field;
	}

	static class WildcardedWrapper {
		SomeGeneric<?> wildcarded;
	}

	// DATACMNS-1571

	interface Aggregate {}

	static class StoredEvent<A extends Aggregate, ID> {
		DomainEvent<A, ID> event;
	}

	static abstract class DomainEvent<T extends Aggregate, ID> {

		ID aggregateId;
		T root;
	}

	static class OfferDetails implements Aggregate {
		String name;
	}

	// A domain type fully binding all generics
	static class OfferCreated extends DomainEvent<OfferDetails, Long> {}

	// A domain type partially binding generics
	static class GenericEvent<T extends Aggregate> extends DomainEvent<T, Long> {}

	// DATACMNS-1828
	interface StringKeyMap<T> extends Map<String, T> {}

	interface MultiValueMap<T> extends Map<String, List<T>> {}

	static class TypeWithTypedMap {
		StringKeyMap<Long> typedMap;
		MultiValueMap<Long> longMultiValueMap;
		Map<String, Long> justMap;
	}

	static class Leaf$$SpringProxy$873fa2e extends Leaf implements SpringProxy, Advised {

		@Override
		public boolean isFrozen() {
			return false;
		}

		@Override
		public boolean isProxyTargetClass() {
			return false;
		}

		@Override
		public Class<?>[] getProxiedInterfaces() {
			return new Class[0];
		}

		@Override
		public boolean isInterfaceProxied(Class<?> intf) {
			return false;
		}

		@Override
		public void setTargetSource(TargetSource targetSource) {

		}

		@Override
		public TargetSource getTargetSource() {
			return null;
		}

		@Override
		public void setExposeProxy(boolean exposeProxy) {

		}

		@Override
		public boolean isExposeProxy() {
			return false;
		}

		@Override
		public void setPreFiltered(boolean preFiltered) {

		}

		@Override
		public boolean isPreFiltered() {
			return false;
		}

		@Override
		public Advisor[] getAdvisors() {
			return new Advisor[0];
		}

		@Override
		public void addAdvisor(Advisor advisor) throws AopConfigException {

		}

		@Override
		public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {

		}

		@Override
		public boolean removeAdvisor(Advisor advisor) {
			return false;
		}

		@Override
		public void removeAdvisor(int index) throws AopConfigException {

		}

		@Override
		public int indexOf(Advisor advisor) {
			return 0;
		}

		@Override
		public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
			return false;
		}

		@Override
		public void addAdvice(Advice advice) throws AopConfigException {

		}

		@Override
		public void addAdvice(int pos, Advice advice) throws AopConfigException {

		}

		@Override
		public boolean removeAdvice(Advice advice) {
			return false;
		}

		@Override
		public int indexOf(Advice advice) {
			return 0;
		}

		@Override
		public String toProxyConfigString() {
			return null;
		}

		@Nullable
		@Override
		public Class<?> getTargetClass() {
			return null;
		}
	}
}
