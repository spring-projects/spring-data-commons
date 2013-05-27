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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
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

	static final String STRING = String.class.getName();

	@Mock
	TypeAliasAccessor<Map<String, String>> accessor;

	@Mock
	TypeInformationMapper mapper;

	TypeMapper<Map<String, String>> typeMapper;
	Map<String, String> source;

	@Before
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setUp() {

		this.typeMapper = new DefaultTypeMapper<Map<String, String>>(accessor, Arrays.asList(mapper));
		this.source = Collections.singletonMap("key", STRING);

		when(accessor.readAliasFrom(source)).thenReturn(STRING);
		when(mapper.resolveTypeFrom(STRING)).thenReturn((TypeInformation) ClassTypeInformation.from(String.class));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void cachesResolvedTypeInformation() {

		TypeInformation<?> information = typeMapper.readType(source);
		assertThat(information, is((TypeInformation) ClassTypeInformation.from(String.class)));
		verify(mapper, times(1)).resolveTypeFrom(STRING);

		typeMapper.readType(source);
		verify(mapper, times(1)).resolveTypeFrom(STRING);
	}
}
