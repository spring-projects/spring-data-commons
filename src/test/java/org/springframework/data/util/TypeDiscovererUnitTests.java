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

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link TypeDiscoverer}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class TypeDiscovererUnitTests {

	static final Map<TypeVariable<?>, Type> EMPTY_MAP = Collections.emptyMap();

	@Mock Map<TypeVariable<?>, Type> firstMap;
	@Mock Map<TypeVariable<?>, Type> secondMap;

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullType() {
		new TypeDiscoverer<>(null, null);
	}

	@Test
	public void isNotEqualIfTypesDiffer() {

		TypeDiscoverer<Object> objectTypeInfo = new TypeDiscoverer<>(Object.class, EMPTY_MAP);
		TypeDiscoverer<String> stringTypeInfo = new TypeDiscoverer<>(String.class, EMPTY_MAP);

		assertThat(objectTypeInfo.equals(stringTypeInfo)).isFalse();
	}

	@Test
	public void isNotEqualIfTypeVariableMapsDiffer() {

		assertThat(firstMap.equals(secondMap)).isFalse();

		TypeDiscoverer<Object> first = new TypeDiscoverer<>(Object.class, firstMap);
		TypeDiscoverer<Object> second = new TypeDiscoverer<>(Object.class, secondMap);

		assertThat(first.equals(second)).isFalse();
	}

	@Test
	public void dealsWithTypesReferencingThemselves() {

		TypeInformation<SelfReferencing> information = from(SelfReferencing.class);
		Optional<TypeInformation<?>> first = information.getProperty("parent").flatMap(it -> it.getMapValueType());
		Optional<TypeInformation<?>> second = first.flatMap(it -> it.getProperty("map"))
				.flatMap(it -> it.getMapValueType());

		assertThat(second).isEqualTo(first);
	}

	@Test
	public void dealsWithTypesReferencingThemselvesInAMap() {

		TypeInformation<SelfReferencingMap> information = from(SelfReferencingMap.class);
		Optional<TypeInformation<?>> property = information.getProperty("map");

		assertThat(property).hasValueSatisfying(it -> {
			assertThat(it.getMapValueType()).hasValue(information);
		});
	}

	@Test
	public void returnsComponentAndValueTypesForMapExtensions() {

		TypeInformation<?> discoverer = new TypeDiscoverer<>(CustomMap.class, EMPTY_MAP);

		assertThat(discoverer.getMapValueType()).hasValueSatisfying(it -> {
			assertThat(it.getType()).isEqualTo(Locale.class);
		});

		assertThat(discoverer.getComponentType()).hasValueSatisfying(it -> {
			assertThat(it.getType()).isEqualTo(String.class);
		});
	}

	@Test
	public void returnsComponentTypeForCollectionExtension() {

		TypeDiscoverer<CustomCollection> discoverer = new TypeDiscoverer<>(CustomCollection.class, firstMap);

		assertThat(discoverer.getComponentType()).hasValueSatisfying(it -> {
			assertThat(it.getType()).isEqualTo(String.class);
		});
	}

	@Test
	public void returnsComponentTypeForArrays() {

		TypeDiscoverer<String[]> discoverer = new TypeDiscoverer<String[]>(String[].class, EMPTY_MAP);

		assertThat(discoverer.getComponentType()).hasValueSatisfying(it -> {
			assertThat(it.getType()).isEqualTo(String.class);
		});
	}

	/**
	 * @see DATACMNS-57
	 */
	@Test
	public void discoveresConstructorParameterTypesCorrectly() throws NoSuchMethodException, SecurityException {

		TypeDiscoverer<GenericConstructors> discoverer = new TypeDiscoverer<>(GenericConstructors.class, firstMap);
		Constructor<GenericConstructors> constructor = GenericConstructors.class.getConstructor(List.class, Locale.class);
		List<TypeInformation<?>> types = discoverer.getParameterTypes(constructor);

		assertThat(types).hasSize(2);
		assertThat(types.get(0).getType()).isEqualTo(List.class);
		assertThat(types.get(0).getComponentType()).hasValueSatisfying(it -> {
			assertThat(it.getType()).isEqualTo(String.class);
		});
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void returnsNullForComponentAndValueTypesForRawMaps() {

		TypeDiscoverer<Map> discoverer = new TypeDiscoverer<>(Map.class, EMPTY_MAP);

		assertThat(discoverer.getComponentType()).isEmpty();
		assertThat(discoverer.getMapValueType()).isEmpty();
	}

	/**
	 * @see DATACMNS-167
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void doesNotConsiderTypeImplementingIterableACollection() {

		TypeDiscoverer<Person> discoverer = new TypeDiscoverer<>(Person.class, EMPTY_MAP);
		TypeInformation reference = from(Address.class);

		Optional<TypeInformation<?>> addresses = discoverer.getProperty("addresses");

		assertThat(addresses).hasValueSatisfying(it -> {
			assertThat(it.isCollectionLike()).isFalse();
			assertThat(it.getComponentType()).hasValue(reference);
		});

		Optional<TypeInformation<?>> adressIterable = discoverer.getProperty("addressIterable");

		assertThat(adressIterable).hasValueSatisfying(it -> {
			assertThat(it.isCollectionLike()).isTrue();
			assertThat(it.getComponentType()).hasValue(reference);
		});
	}

	class Person {

		Addresses addresses;
		Iterable<Address> addressIterable;
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
}
