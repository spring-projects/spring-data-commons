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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.util.ClassTypeInformation.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.OptionalAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

	@Test // DATACMNS-88
	public void resolvesMapValueTypeCorrectly() {

		TypeInformation<Foo> type = ClassTypeInformation.from(Foo.class);
		Optional<TypeInformation<?>> propertyType = type.getProperty("param");

		OptionalAssert<TypeInformation<?>> assertion = assertThat(propertyType);

		assertion.flatMap(it -> it.getProperty("value"))
				.hasValueSatisfying(it -> assertThat(it.getType()).isEqualTo(String.class));
		assertion.flatMap(TypeInformation::getMapValueType)
				.hasValueSatisfying(it -> assertThat(it.getType()).isEqualTo(String.class));

		propertyType = type.getProperty("param2");

		assertion.flatMap(it -> it.getProperty("value"))
				.hasValueSatisfying(it -> assertThat(it.getType()).isEqualTo(String.class));
		assertion.flatMap(TypeInformation::getMapValueType)
				.hasValueSatisfying(it -> assertThat(it.getType()).isEqualTo(String.class));
	}

	@Test // DATACMNS-446
	public void createsToStringRepresentation() {

		assertThat(from(Foo.class).getProperty("param")).map(Object::toString)
				.hasValue("org.springframework.data.util.ParameterizedTypeUnitTests$Localized<java.lang.String>");
	}

	@Test // DATACMNS-485
	public void hashCodeShouldBeConsistentWithEqualsForResolvedTypes() {

		Optional<TypeInformation<?>> first = from(First.class).getProperty("property");
		Optional<TypeInformation<?>> second = from(Second.class).getProperty("property");

		assertThat(first).isEqualTo(second);

		assertThat(first).hasValueSatisfying(left -> assertThat(second)
				.hasValueSatisfying(right -> assertThat(left.hashCode()).isEqualTo(right.hashCode())));
	}

	@Test // DATACMNS-485
	public void getActualTypeShouldNotUnwrapParameterizedTypes() {

		Optional<TypeInformation<?>> type = from(First.class).getProperty("property");

		assertThat(type).map(TypeInformation::getActualType).isEqualTo(type);
	}

	@Test // DATACMNS-697
	public void usesLocalGenericInformationOfFields() {

		TypeInformation<NormalizedProfile> information = ClassTypeInformation.from(NormalizedProfile.class);

		assertThat(information.getProperty("education2.data"))//
				.flatMap(TypeInformation::getComponentType)//
				.flatMap(it -> it.getProperty("value"))//
				.hasValueSatisfying(it -> assertThat(it.getType()).isEqualTo(Education.class));
	}

	@Test // DATACMNS-899
	public void returnsEmptyOptionalMapValueTypeForNonMapProperties() {

		OptionalAssert<TypeInformation<?>> assertion = assertThat(
				ClassTypeInformation.from(Bar.class).getProperty("param"));

		assertion.hasValueSatisfying(it -> assertThat(it).isInstanceOf(ParameterizedTypeInformation.class));
		assertion.flatMap(TypeInformation::getMapValueType).isEmpty();
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
