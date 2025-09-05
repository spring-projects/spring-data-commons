/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.data.mapping.Alias;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link DefaultTypeMapper}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultTypeMapperUnitTests {

	static final TypeInformation<String> STRING_TYPE_INFO = TypeInformation.of(String.class);
	static final Alias ALIAS = Alias.of(String.class.getName());

	@Mock TypeAliasAccessor<Map<String, String>> accessor;
	@Mock(extraInterfaces = BeanClassLoaderAware.class) TypeInformationMapper mapper;

	DefaultTypeMapper<Map<String, String>> typeMapper;
	Map<String, String> source;

	@BeforeEach
	void setUp() {

		this.typeMapper = new DefaultTypeMapper<>(accessor, Collections.singletonList(mapper));
		this.source = Collections.singletonMap("key", ALIAS.toString());

		doReturn(ALIAS).when(accessor).readAliasFrom(source);
		doReturn(STRING_TYPE_INFO).when(mapper).resolveTypeFrom(ALIAS);
	}

	@Test
	void cachesResolvedTypeInformation() {

		var information = typeMapper.readType(source);
		assertThat(information).isEqualTo(STRING_TYPE_INFO);
		verify(mapper, times(1)).resolveTypeFrom(ALIAS);

		typeMapper.readType(source);
		verify(mapper, times(1)).resolveTypeFrom(ALIAS);
	}

	@Test // DATACMNS-349
	void returnsTypeAliasForInformation() {

		var alias = Alias.of("alias");
		doReturn(alias).when(mapper).createAliasFor(STRING_TYPE_INFO);

		assertThat(this.typeMapper.getAliasFor(STRING_TYPE_INFO)).isEqualTo(alias);
	}

	@Test // DATACMNS-783
	void specializesRawSourceTypeUsingGenericContext() {

		var root = TypeInformation.of(Foo.class);
		var propertyType = root.getProperty("abstractBar");
		TypeInformation<?> barType = TypeInformation.of(Bar.class);

		doReturn(Alias.of(barType)).when(accessor).readAliasFrom(source);
		doReturn(barType).when(mapper).resolveTypeFrom(Alias.of(barType));

		var result = typeMapper.readType(source, propertyType);

		assertThat(result).isInstanceOf(TypeInformation.class);

		TypeInformation<?> typeInformation = TypeInformation.class.cast(result);

		assertThat(typeInformation.getType()).isEqualTo(Bar.class);
		assertThat(typeInformation.getProperty("field").getType()).isEqualTo(Character.class);
	}

	@Test // GH-2508
	void configuresClassLoaderOnTypeInformationMapper() {

		ClassLoader loader = new URLClassLoader(new URL[0]);
		typeMapper.setBeanClassLoader(loader);

		verify((BeanClassLoaderAware) mapper).setBeanClassLoader(loader);
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
