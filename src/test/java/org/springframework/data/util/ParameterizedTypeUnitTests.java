/*
 * Copyright 2011-2016 the original author or authors.
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
import static org.mockito.Mockito.*;
import static org.springframework.data.util.ClassTypeInformation.*;
import static org.springframework.data.util.OptionalAssert.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link ParameterizedTypeInformation}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ParameterizedTypeUnitTests {

	static final Map<TypeVariable<?>, Type> EMPTY_MAP = Collections.emptyMap();

	@Mock ParameterizedType one;

	@Before
	public void setUp() {
		when(one.getActualTypeArguments()).thenReturn(new Type[0]);
	}

	@Test
	public void considersTypeInformationsWithDifferingParentsNotEqual() {

		TypeDiscoverer<String> stringParent = new TypeDiscoverer<>(String.class, EMPTY_MAP);
		TypeDiscoverer<Object> objectParent = new TypeDiscoverer<>(Object.class, EMPTY_MAP);

		ParameterizedTypeInformation<Object> first = new ParameterizedTypeInformation<>(one, stringParent, EMPTY_MAP);
		ParameterizedTypeInformation<Object> second = new ParameterizedTypeInformation<>(one, objectParent, EMPTY_MAP);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	public void considersTypeInformationsWithSameParentsNotEqual() {

		TypeDiscoverer<String> stringParent = new TypeDiscoverer<>(String.class, EMPTY_MAP);

		ParameterizedTypeInformation<Object> first = new ParameterizedTypeInformation<>(one, stringParent, EMPTY_MAP);
		ParameterizedTypeInformation<Object> second = new ParameterizedTypeInformation<>(one, stringParent, EMPTY_MAP);

		assertThat(first.equals(second)).isTrue();
	}

	/**
	 * @see DATACMNS-88
	 */
	@Test
	public void resolvesMapValueTypeCorrectly() {

		TypeInformation<Foo> type = ClassTypeInformation.from(Foo.class);
		Optional<TypeInformation<?>> propertyType = type.getProperty("param");

		OptionalAssert<TypeInformation<?>> assertion = assertOptional(propertyType);

		assertion.flatMap(it -> it.getProperty("value")).value(it -> it.getType()).isEqualTo(String.class);
		assertion.flatMap(it -> it.getMapValueType()).value(it -> it.getType()).isEqualTo(String.class);

		propertyType = type.getProperty("param2");

		assertion.flatMap(it -> it.getProperty("value")).value(it -> it.getType()).isEqualTo(String.class);
		assertion.flatMap(it -> it.getMapValueType()).value(it -> it.getType()).isEqualTo(String.class);
	}

	/**
	 * @see DATACMNS-446
	 */
	@Test
	public void createsToStringRepresentation() {

		assertOptional(from(Foo.class).getProperty("param")).value(it -> it.toString())
				.isEqualTo("org.springframework.data.util.ParameterizedTypeUnitTests$Localized<java.lang.String>");
	}

	/**
	 * @see DATACMNS-485
	 */
	@Test
	public void hashCodeShouldBeConsistentWithEqualsForResolvedTypes() {

		Optional<TypeInformation<?>> first = from(First.class).getProperty("property");
		Optional<TypeInformation<?>> second = from(Second.class).getProperty("property");

		assertThat(first).isEqualTo(second);

		assertThat(first).hasValueSatisfying(left -> {
			assertThat(second).hasValueSatisfying(right -> {
				assertThat(left.hashCode()).isEqualTo(right.hashCode());
			});
		});
	}

	/**
	 * @see DATACMNS-485
	 */
	@Test
	public void getActualTypeShouldNotUnwrapParameterizedTypes() {

		Optional<TypeInformation<?>> type = from(First.class).getProperty("property");

		assertOptional(type).map(it -> it.getActualType()).isEqualTo(type);
	}

	/**
	 * @see DATACMNS-697
	 */
	@Test
	public void usesLocalGenericInformationOfFields() {

		TypeInformation<NormalizedProfile> information = ClassTypeInformation.from(NormalizedProfile.class);

		assertOptional(information.getProperty("education2.data"))//
				.flatMap(it -> it.getComponentType())//
				.flatMap(it -> it.getProperty("value"))//
				.value(it -> it.getType())//
				.isEqualTo(Education.class);
	}

	/**
	 * @see DATACMNS-899
	 */
	@Test
	public void returnsEmptyOptionalMapValueTypeForNonMapProperties() {

		OptionalAssert<TypeInformation<?>> assertion = assertOptional(
				ClassTypeInformation.from(Bar.class).getProperty("param"));

		assertion.hasValueSatisfying(it -> assertThat(it).isInstanceOf(ParameterizedTypeInformation.class));
		assertion.flatMap(it -> it.getMapValueType()).isEmpty();
	}

	@SuppressWarnings("serial")
	class Localized<S> extends HashMap<Locale, S> {
		S value;
	}

	@SuppressWarnings("serial")
	class Localized2<S> extends HashMap<S, Locale> {
		S value;
	}

	class Foo {
		Localized<String> param;
		Localized2<String> param2;
	}

	class Bar {
		List<String> param;
	}

	class Parameterized<T> {
		T property;
	}

	class First {
		Parameterized<String> property;
	}

	class Second {
		Parameterized<String> property;
	}

	// see DATACMNS-697

	class NormalizedProfile {

		ListField<Education> education2;
	}

	class ListField<L> {
		List<Value<L>> data;
	}

	class Value<T> {
		T value;
	}

	class Education {}
}
