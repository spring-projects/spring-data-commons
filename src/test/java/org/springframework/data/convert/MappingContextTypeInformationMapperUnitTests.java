/*
 * Copyright 2012 the original author or authors.
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
import static org.springframework.data.util.ClassTypeInformation.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit tests for {@link MappingContextTypeInformationMapper}.
 * 
 * @author Oliver Gierke
 */
public class MappingContextTypeInformationMapperUnitTests {

	SampleMappingContext mappingContext;
	TypeInformationMapper mapper;

	@Before
	public void setUp() {
		mappingContext = new SampleMappingContext();
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMappingContext() {
		new MappingContextTypeInformationMapper(null);
	}

	@Test
	public void extractsAliasInfoFromMappingContext() {

		mappingContext.setInitialEntitySet(Collections.singleton(Entity.class));
		mappingContext.initialize();

		mapper = new MappingContextTypeInformationMapper(mappingContext);

		assertThat(mapper.createAliasFor(ClassTypeInformation.from(Entity.class)).hasValue("foo")).isTrue();
	}

	@Test
	public void extractsAliasForUnknownType() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		mappingContext.initialize();

		mapper = new MappingContextTypeInformationMapper(mappingContext);

		assertThat(mapper.createAliasFor(from(Entity.class)).hasValue("foo")).isTrue();
	}

	@Test
	public void doesNotReturnTypeAliasForSimpleType() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		mappingContext.initialize();

		mapper = new MappingContextTypeInformationMapper(mappingContext);
		assertThat(mapper.createAliasFor(from(String.class)).isPresent()).isFalse();
	}

	@Test
	public void detectsTypeForUnknownEntity() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		mappingContext.initialize();

		mapper = new MappingContextTypeInformationMapper(mappingContext);
		assertThat(mapper.resolveTypeFrom(Alias.of("foo"))).isEmpty();

		PersistentEntity<?, SamplePersistentProperty> entity = mappingContext.getRequiredPersistentEntity(Entity.class);

		assertThat(entity).isNotNull();
		assertThat(mapper.resolveTypeFrom(Alias.of("foo"))).hasValue(from(Entity.class));
	}

	/**
	 * @see DATACMNS-485
	 */
	@Test
	public void createsTypeMapperForGenericTypesWithDifferentBindings() {

		AnnotatedTypeScanner scanner = new AnnotatedTypeScanner(TypeAlias.class);

		SampleMappingContext context = new SampleMappingContext();
		context.setInitialEntitySet(scanner.findTypes(getClass().getPackage().getName()));
		context.initialize();

		new MappingContextTypeInformationMapper(context);
	}

	@TypeAlias("foo")
	static class Entity {

	}

	@TypeAlias("genericType")
	static class GenericType<T> {

	}

	@TypeAlias("concreteWrapper")
	static class ConcreteWrapper {

		GenericType<String> stringGeneric;
		GenericType<Integer> integerGeneric;
	}

	@TypeAlias("genericWrapper")
	static class GenericWrapper<T> {

		GenericType<T> genericGeneric;
	}
}
