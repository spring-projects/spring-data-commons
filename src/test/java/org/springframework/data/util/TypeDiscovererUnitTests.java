/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.core.ResolvableType;
import org.springframework.data.geo.GeoResults;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link TypeDiscoverer}.
 *
 * @author Oliver Gierke
 * @author Jürgen Diez
 */
@ExtendWith(MockitoExtension.class)
public class TypeDiscovererUnitTests {

	@Test
	void rejectsNullType() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TypeDiscoverer<>((ResolvableType) null));
	}

	@Test
	void isNotEqualIfTypesDiffer() {

		var objectTypeInfo = TypeInformation.of(Object.class);
		var stringTypeInfo = TypeInformation.of(String.class);

		assertThat(objectTypeInfo.equals(stringTypeInfo)).isFalse();
	}

	@Test
	void dealsWithTypesReferencingThemselves() {

		var information = TypeInformation.of(SelfReferencing.class);

		var first = information.getProperty("parent").getMapValueType();
		var second = first.getProperty("map").getMapValueType();

		assertThat(second).isEqualTo(first);
	}

	@Test
	void dealsWithTypesReferencingThemselvesInAMap() {

		var information = TypeInformation.of(SelfReferencingMap.class);
		var property = information.getProperty("map");

		assertThat(property.getMapValueType()).isEqualTo(information);
	}

	@Test
	void returnsComponentAndValueTypesForMapExtensions() {

		TypeInformation<?> discoverer = TypeInformation.of(CustomMap.class);

		assertThat(discoverer.getMapValueType().getType()).isEqualTo(Locale.class);
		assertThat(discoverer.getComponentType().getType()).isEqualTo(String.class);
	}

	@Test
	void returnsComponentTypeForCollectionExtension() {

		var discoverer = TypeInformation.of(CustomCollection.class);

		assertThat(discoverer.getComponentType().getType()).isEqualTo(String.class);
	}

	@Test
	void returnsComponentTypeForArrays() {

		var discoverer = TypeInformation.of(String[].class);

		assertThat(discoverer.getComponentType().getType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-57
	void discoveresConstructorParameterTypesCorrectly() throws NoSuchMethodException, SecurityException {

		var discoverer = TypeInformation.of(GenericConstructors.class);
		var constructor = GenericConstructors.class.getConstructor(List.class, Locale.class);
		var types = discoverer.getParameterTypes(constructor);

		assertThat(types).hasSize(2);
		assertThat(types.get(0).getType()).isEqualTo(List.class);
		assertThat(types.get(0).getComponentType().getType()).isEqualTo(String.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void returnsNullForComponentAndValueTypesForRawMaps() {

		var discoverer = TypeInformation.of(Map.class);

		assertThat(discoverer.getComponentType()).isNotNull();
		assertThat(discoverer.getMapValueType()).isNotNull();
	}

	@Test // DATACMNS-167
	@SuppressWarnings("rawtypes")
	void doesNotConsiderTypeImplementingIterableACollection() {

		var discoverer = TypeInformation.of(Person.class);
		TypeInformation reference = TypeInformation.of(Address.class);

		var addresses = discoverer.getProperty("addresses");

		assertThat(addresses).satisfies(it -> {
			assertThat(it.isCollectionLike()).isFalse();
			assertThat(it.getComponentType()).isEqualTo(reference);
		});

		var adressIterable = discoverer.getProperty("addressIterable");

		assertThat(adressIterable).satisfies(it -> {
			assertThat(it.isCollectionLike()).isTrue();
			assertThat(it.getComponentType()).isEqualTo(reference);
		});
	}

	@Test // DATACMNS-1342, DATACMNS-1430
	void considersStreamableToBeCollectionLike() {

		TypeInformation<SomeStreamable> type = TypeInformation.of(SomeStreamable.class);

		assertThat(type.isCollectionLike()).isTrue();
		assertThat(type.getRequiredProperty("streamable").isCollectionLike()).isTrue();
	}

	@Test // DATACMNS-1419
	void detectsSubTypes() {

		var type = TypeInformation.of(Set.class);

		assertThat(type.isSubTypeOf(Collection.class)).isTrue();
		assertThat(type.isSubTypeOf(Set.class)).isFalse();
		assertThat(type.isSubTypeOf(String.class)).isFalse();
	}

	@Test
	void isNotEqualIfFieldsDiffer() {
		// should we have something like a default TypeInformation
		// wiht static methods for forFieldOfType(), forClass(), like the
		// ones we have on resolvable type and then cache the stuff there?

		// Managed to get Stackoverflow on hashcode of Resolvable type once for caching

		// tests for fields in same class
		// tests for inherited fields
		// tests for same signature in different classes

	}

	@Test // GH-2312
	void sameFieldNoGenericsInfoShouldBeEqual() {

		var addresses = ReflectionUtils.findField(Person.class, "addresses");

		var discoverer1 = TypeInformation.of(ResolvableType.forField(addresses, Person.class));
		var discoverer2 = TypeInformation.of(ResolvableType.forField(addresses, Person.class));

		assertThat(discoverer1).isEqualTo(discoverer2);
		assertThat(discoverer1.hashCode()).isEqualTo(discoverer2.hashCode());
	}

	@Test // GH-2312
	void sameFieldNoGenericsWhenInherited() {

		var addresses = ReflectionUtils.findField(Person.class, "addresses");

		var discoverer1 = TypeInformation.of(ResolvableType.forField(addresses, Person.class));
		var discoverer2 = TypeInformation.of(ResolvableType.forField(addresses, TypeExtendingPerson.class));

		assertThat(discoverer1).isEqualTo(discoverer2);
		assertThat(discoverer1.hashCode()).isEqualTo(discoverer2.hashCode());
	}

	@Test // GH-2312
	void sameFieldNoGenericsOnDifferentTypes() {

		var addresses1 = ReflectionUtils.findField(Person.class, "addresses");
		var discoverer1 = TypeInformation.of(ResolvableType.forField(addresses1, Person.class));

		var addresses2 = ReflectionUtils.findField(OtherPerson.class, "addresses");
		var discoverer2 = TypeInformation.of(ResolvableType.forField(addresses2, OtherPerson.class));

		assertThat(discoverer1).isEqualTo(discoverer2);
		assertThat(discoverer1.hashCode()).isEqualTo(discoverer2.hashCode());
	}

	@Test // GH-2312
	void sameFieldWithGenerics() {

		var field1 = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer1 = TypeInformation.of(ResolvableType.forField(field1, GenericPerson.class));

		var field2 = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer2 = TypeInformation.of(ResolvableType.forField(field2, GenericPerson.class));

		assertThat(discoverer1).isEqualTo(discoverer2);
		assertThat(discoverer1.hashCode()).isEqualTo(discoverer2.hashCode());
	}

	@Test // GH-2312
	void sameFieldWithGenericsSet() {

		var field1 = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer1 = TypeInformation.of(ResolvableType.forField(field1, TypeExtendingGenericPersonWithObject.class));

		var field2 = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer2 = TypeInformation.of(ResolvableType.forField(field2, TypeExtendingGenericPersonWithObject.class));

		assertThat(discoverer1).isEqualTo(discoverer2);
		assertThat(discoverer1.hashCode()).isEqualTo(discoverer2.hashCode());
	}

	@Test // GH-2312
	void sameFieldWithDifferentGenericsSet() {

		var field1 = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer1 = TypeInformation.of(ResolvableType.forField(field1, TypeExtendingGenericPersonWithObject.class));

		var field2 = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer2 = TypeInformation.of(ResolvableType.forField(field2, TypeExtendingGenericPersonWithAddress.class));

		assertThat(discoverer1).isNotEqualTo(discoverer2);
		assertThat(discoverer1.hashCode()).isNotEqualTo(discoverer2.hashCode());
	}

	@Test // GH-2312
	void sameFieldWithDifferentNoGenericsAndObjectOneSet() {

		var field1 = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer1 = TypeInformation.of(ResolvableType.forField(field1, GenericPerson.class));

		var field2 = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer2 = TypeInformation.of(ResolvableType.forField(field2, TypeExtendingGenericPersonWithObject.class));

		assertThat(discoverer1).isEqualTo(discoverer2); // TODO: notEquals
		assertThat(discoverer1.hashCode()).isEqualTo(discoverer2.hashCode());
	}

	@Test // GH-2312
	void genericFieldOfType() {

		var field = ReflectionUtils.findField(GenericPerson.class, "value");
		var discoverer = TypeInformation.of(ResolvableType.forField(field, TypeExtendingGenericPersonWithAddress.class));

		assertThat(discoverer.getType()).isEqualTo(Address.class);
	}

	@Test // #2511
	void considerVavrMapToBeAMap() {

		assertThat(TypeInformation.of(io.vavr.collection.Map.class).isMap()).isTrue();
	}

	@Test // #2517
	void returnsComponentAndValueTypesForVavrMapExtensions() {

		var discoverer = TypeInformation.of(CustomVavrMap.class);

		assertThat(discoverer.getMapValueType().getType()).isEqualTo(Locale.class);
		assertThat(discoverer.getComponentType().getType()).isEqualTo(String.class);
	}

	@Test // #2511
	void considerVavrSetToBeCollectionLike() {
		assertThat(TypeInformation.of(io.vavr.collection.Set.class).isCollectionLike()).isTrue();
	}

	@Test // #2511
	void considerVavrSeqToBeCollectionLike() {
		assertThat(TypeInformation.of(io.vavr.collection.Seq.class).isCollectionLike()).isTrue();
	}

	@Test // #2511
	void considerVavrListToBeCollectionLike() {
		assertThat(TypeInformation.of(io.vavr.collection.List.class).isCollectionLike()).isTrue();
	}

	@Test
	void typeInfoShouldPreserveGenericParameter() {

		TypeInformation<Wrapper> wrapperTypeInfo = TypeInformation.of(Wrapper.class);
		TypeInformation<?> fieldTypeInfo = wrapperTypeInfo.getProperty("field");
		TypeInformation<?> valueTypeInfo = fieldTypeInfo.getProperty("value");

		assertThat(valueTypeInfo.getType()).isEqualTo(Leaf.class);
	}

	@Test
	void detectsMapKeyAndValueOnEnumMaps() {

		TypeInformation<?> map = TypeInformation.of(EnumMapWrapper.class).getProperty("map");

		assertThat(map.getComponentType().getType()).isEqualTo(Autowire.class);
		assertThat(map.getMapValueType().getType()).isEqualTo(String.class);
	}

	@Test
	void detectsComponentTypeOfTypedClass() {

		TypeInformation<?> property = TypeInformation.of(GeoResultsWrapper.class).getProperty("results");

		assertThat(property.getComponentType().getType()).isEqualTo(Leaf.class);
	}

	@Test
	void differentEqualsAndHashCodeForTypeDiscovererAndClassTypeInformation() {

		ResolvableType type = ResolvableType.forClass(Object.class);

		var discoverer = new TypeDiscoverer<>(type);
		var classTypeInformation = new ClassTypeInformation<>(type);

		assertThat(discoverer).isNotEqualTo(classTypeInformation);
		assertThat(classTypeInformation).isNotEqualTo(type);

		assertThat(discoverer.hashCode()).isNotEqualTo(classTypeInformation.hashCode());
	}

	class Person {

		Addresses addresses;
		Iterable<Address> addressIterable;
	}

	class TypeExtendingPerson {

	}

	class OtherPerson {
		Addresses addresses;
	}

	class GenericPerson<T> {
		T value;
	}

	class TypeExtendingGenericPersonWithObject extends GenericPerson<Object> {

	}

	class TypeExtendingGenericPersonWithAddress extends GenericPerson<Address> {

	}

	abstract class Addresses implements Iterable<Address> {

	}

	class Address {

	}

	class SelfReferencing {

		Map<String, SelfReferencingMap> parent;
	}

	class SelfReferencingMap {
		Map<String, SelfReferencingMap> map;
	}

	interface CustomMap extends Map<String, Locale> {

	}

	interface CustomCollection extends Collection<String> {

	}

	public static class GenericConstructors {

		public GenericConstructors(List<String> first, Locale second) {

		}
	}

	// DATACMNS-1342

	static class SomeStreamable implements Streamable<String> {

		Streamable<String> streamable;

		@Override
		public Iterator<String> iterator() {
			return Collections.emptyIterator();
		}
	}

	interface CustomVavrMap extends io.vavr.collection.Map<String, Locale> {}

	// #2312

	class SomeGeneric<T> {
		T value;
	}

	class GenericExtendingSomeGeneric<T> extends SomeGeneric<T> {}

	class Wrapper {
		GenericExtendingSomeGeneric<Leaf> field;
	}

	class Leaf {}

	class EnumMapWrapper {
		EnumMap<Autowire, String> map;
	}

	class GeoResultsWrapper {
		GeoResults<Leaf> results;
	}
}
