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
public class ParameterizedTypeInformationUnitTests {

	static final Map<TypeVariable<?>, Type> EMPTY_MAP = Collections.emptyMap();

	@Mock ParameterizedType one;

	@Before
	public void setUp() {
		when(one.getActualTypeArguments()).thenReturn(new Type[0]);
	}

	@Test
	public void considersTypeInformationsWithDifferingParentsNotEqual() {

		TypeDiscoverer<String> stringParent = new TypeDiscoverer<String>(String.class, EMPTY_MAP);
		TypeDiscoverer<Object> objectParent = new TypeDiscoverer<Object>(Object.class, EMPTY_MAP);

		ParameterizedTypeInformation<Object> first = new ParameterizedTypeInformation<Object>(one, stringParent);
		ParameterizedTypeInformation<Object> second = new ParameterizedTypeInformation<Object>(one, objectParent);

		assertThat(first, is(not(second)));
	}

	@Test
	public void considersTypeInformationsWithSameParentsNotEqual() {

		TypeDiscoverer<String> stringParent = new TypeDiscoverer<String>(String.class, EMPTY_MAP);

		ParameterizedTypeInformation<Object> first = new ParameterizedTypeInformation<Object>(one, stringParent);
		ParameterizedTypeInformation<Object> second = new ParameterizedTypeInformation<Object>(one, stringParent);

		assertTrue(first.equals(second));
	}

	@Test // DATACMNS-88
	public void resolvesMapValueTypeCorrectly() {

		TypeInformation<Foo> type = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> propertyType = type.getProperty("param");
		assertThat(propertyType.getProperty("value").getType(), is(typeCompatibleWith(String.class)));
		assertThat(propertyType.getMapValueType().getType(), is(typeCompatibleWith(String.class)));

		propertyType = type.getProperty("param2");
		assertThat(propertyType.getProperty("value").getType(), is(typeCompatibleWith(String.class)));
		assertThat(propertyType.getMapValueType().getType(), is(typeCompatibleWith(Locale.class)));
	}

	@Test // DATACMNS-446
	public void createsToStringRepresentation() {

		assertThat(from(Foo.class).getProperty("param").toString(),
				is("org.springframework.data.util.ParameterizedTypeInformationUnitTests$Localized<java.lang.String>"));
	}

	@Test // DATACMNS-485
	@SuppressWarnings("rawtypes")
	public void hashCodeShouldBeConsistentWithEqualsForResolvedTypes() {

		TypeInformation first = from(First.class).getProperty("property");
		TypeInformation second = from(Second.class).getProperty("property");

		assertThat(first, is(second));
		assertThat(first.hashCode(), is(second.hashCode()));
	}

	@Test // DATACMNS-485
	@SuppressWarnings("rawtypes")
	public void getActualTypeShouldNotUnwrapParameterizedTypes() {

		TypeInformation type = from(First.class).getProperty("property");
		assertThat(type.getActualType(), is(type));
	}

	@Test // DATACMNS-697
	public void usesLocalGenericInformationOfFields() {

		TypeInformation<NormalizedProfile> information = ClassTypeInformation.from(NormalizedProfile.class);
		TypeInformation<?> valueType = information.getProperty("education2.data").getComponentType();
		assertThat(valueType.getProperty("value").getType(), is(typeCompatibleWith(Education.class)));
	}

	@Test // DATACMNS-899
	public void returnsNullMapValueTypeForNonMapProperties() {

		TypeInformation<?> valueType = ClassTypeInformation.from(Bar.class).getProperty("param");
		TypeInformation<?> mapValueType = valueType.getMapValueType();

		assertThat(valueType, instanceOf(ParameterizedTypeInformation.class));
		assertThat(mapValueType, is(nullValue()));
	}

	@Test // DATACMNS-1135
	public void prefersLocalGenericsDeclarationOverParentBound() {

		ClassTypeInformation<Candidate> candidate = ClassTypeInformation.from(Candidate.class);

		TypeInformation<?> componentType = candidate.getProperty("experiences.values").getComponentType();
		componentType = componentType.getProperty("responsibilities.values").getComponentType();

		assertThat(componentType.getType(), is(typeCompatibleWith(Responsibility.class)));
	}

	@Test // DATACMNS-1196
	public void detectsNestedGenerics() {

		TypeInformation<?> myList = ClassTypeInformation.from(EnumGeneric.class).getProperty("inner.myList");

		assertThat(myList.getComponentType().getType(), is(typeCompatibleWith(MyEnum.class)));
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

	// DATACMNS-1135

	abstract class CandidateInfo {}

	class Responsibility extends CandidateInfo {}

	class Experience extends CandidateInfo {
		CandidateInfoContainer<Responsibility> responsibilities;
	}

	class CandidateInfoContainer<E extends CandidateInfo> {
		List<E> values;
	}

	class Candidate {
		CandidateInfoContainer<Experience> experiences;
	}

	// FOO

	static abstract class Generic<T> {

		Inner<T> inner;

		static class Inner<T> {
			List<T> myList;
		}
	}

	static class EnumGeneric extends Generic<MyEnum> {}

	public enum MyEnum {
		E1, E2
	}
}
