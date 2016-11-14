/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link DefaultTypeMapper}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTypeMapperUnitTests {

	static final TypeInformation<String> STRING_TYPE_INFO = ClassTypeInformation.from(String.class);
	static final String STRING = String.class.getName();

	@Mock TypeAliasAccessor<Map<String, String>> accessor;

	@Mock TypeInformationMapper mapper;

	DefaultTypeMapper<Map<String, String>> typeMapper;
	Map<String, String> source;

	@Before
	public void setUp() {

		this.typeMapper = new DefaultTypeMapper<Map<String, String>>(accessor, Arrays.asList(mapper));
		this.source = Collections.singletonMap("key", STRING);

		doReturn(STRING).when(accessor).readAliasFrom(source);
		doReturn(STRING_TYPE_INFO).when(mapper).resolveTypeFrom(STRING);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void cachesResolvedTypeInformation() {

		TypeInformation<?> information = typeMapper.readType(source);
		assertThat(information).isEqualTo((TypeInformation) STRING_TYPE_INFO);
		verify(mapper, times(1)).resolveTypeFrom(STRING);

		typeMapper.readType(source);
		verify(mapper, times(1)).resolveTypeFrom(STRING);
	}

	/**
	 * @see DATACMNS-349
	 */
	@Test
	public void returnsTypeAliasForInformation() {

		Object alias = "alias";
		when(mapper.createAliasFor(STRING_TYPE_INFO)).thenReturn(alias);

		assertThat(this.typeMapper.getAliasFor(STRING_TYPE_INFO)).isEqualTo(alias);
	}

	/**
	 * @see DATACMNS-783
	 */
	@Test
	public void specializesRawSourceTypeUsingGenericContext() {

		ClassTypeInformation<Foo> root = ClassTypeInformation.from(Foo.class);
		TypeInformation<?> propertyType = root.getProperty("abstractBar");
		TypeInformation<?> barType = ClassTypeInformation.from(Bar.class);

		doReturn(barType).when(accessor).readAliasFrom(source);
		doReturn(barType).when(mapper).resolveTypeFrom(barType);

		TypeInformation<?> result = typeMapper.readType(source, propertyType);

		assertThat(result.getType()).isEqualTo(Bar.class);
		assertThat(result.getProperty("field").getType()).isEqualTo(Character.class);
	}

	static class TypeWithAbstractGenericType<T> {
		AbstractBar<T> abstractBar;
	}

	static class Foo extends TypeWithAbstractGenericType<Character> {}

	static abstract class AbstractBar<T> {}

	static class Bar<T> extends AbstractBar<T> {
		T field;
	}
}
