/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.assertj.core.api.Assertions.*;

import lombok.Getter;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.projection.EntityProjection;
import org.springframework.data.projection.EntityProjectionIntrospector;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

/**
 * Unit tests for {@link EntityProjectionIntrospector}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class EntityProjectionIntrospectorUnitTests {

	SampleMappingContext mappingContext = new SampleMappingContext();
	SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();
	EntityProjectionIntrospector.ProjectionPredicate predicate = (target,
			underlyingType) -> !SimpleTypeHolder.DEFAULT.isSimpleType(target);
	EntityProjectionIntrospector discoverer = EntityProjectionIntrospector.create(projectionFactory,
			predicate.and(EntityProjectionIntrospector.ProjectionPredicate.typeHierarchy()), mappingContext);

	@Test // GH-2420
	void shouldDiscoverTypeHierarchy() {

		// super type
		assertThat(discoverer.introspect(Root.class, Middle.class).isProjection()).isFalse();

		assertThat(discoverer.introspect(SuperInterface.class, Middle.class).isProjection()).isFalse();

		// subtypes
		assertThat(discoverer.introspect(Leaf.class, Middle.class).isProjection()).isFalse();
	}

	@Test // GH-2420
	void shouldConsiderTopLevelInterfaceProperties() {

		EntityProjection<?, ?> descriptor = discoverer.introspect(DomainClassProjection.class, DomainClass.class);

		assertThat(descriptor.isProjection()).isTrue();
		assertThat(descriptor.isClosedProjection()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEachRecursive(it -> paths.add(it.getPropertyPath()));

		assertThat(paths).hasSize(2).extracting(PropertyPath::toDotPath).containsOnly("id", "value");
	}

	@Test // GH-2420
	void shouldConsiderTopLevelDtoProperties() {

		EntityProjection<?, ?> descriptor = discoverer.introspect(DomainClassDto.class, DomainClass.class);

		assertThat(descriptor.isProjection()).isTrue();
		assertThat(descriptor.isClosedProjection()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEachRecursive(it -> paths.add(it.getPropertyPath()));

		assertThat(paths).hasSize(2).extracting(PropertyPath::toDotPath).containsOnly("id", "value");
	}

	@Test // GH-2420
	void shouldConsiderNestedProjectionProperties() {

		EntityProjection<?, ?> descriptor = discoverer.introspect(WithNestedProjection.class,
				WithComplexObject.class);

		assertThat(descriptor.isProjection()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEachRecursive(it -> paths.add(it.getPropertyPath()));

		assertThat(paths).hasSize(3).extracting(PropertyPath::toDotPath).containsOnly("domain.id", "domain.value",
				"domain2");
	}

	@Test // GH-2420
	void shouldConsiderOpenProjection() {

		EntityProjection<?, ?> descriptor = discoverer.introspect(OpenProjection.class, DomainClass.class);

		assertThat(descriptor.isProjection()).isTrue();
		assertThat(descriptor.isClosedProjection()).isFalse();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEachRecursive(it -> paths.add(it.getPropertyPath()));

		assertThat(paths).isEmpty();
	}

	@Test // GH-2420
	void shouldConsiderCyclicPaths() {

		EntityProjection<?, ?> descriptor = discoverer.introspect(PersonProjection.class, Person.class);

		assertThat(descriptor.isProjection()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEachRecursive(it -> paths.add(it.getPropertyPath()));

		// cycles are tracked on a per-property root basis. Global tracking would not expand "secondaryAddress" into its
		// components.
		assertThat(paths).extracting(PropertyPath::toDotPath).containsOnly("primaryAddress.owner.primaryAddress",
				"primaryAddress.owner.secondaryAddress.owner", "secondaryAddress.owner.primaryAddress.owner",
				"secondaryAddress.owner.secondaryAddress");
	}

	@Test // GH-2420
	void shouldConsiderCollectionProjection() {

		EntityProjection<?, ?> descriptor = discoverer.introspect(WithCollectionProjection.class, WithCollection.class);

		assertThat(descriptor.isProjection()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEachRecursive(it -> paths.add(it.getPropertyPath()));

		assertThat(paths).hasSize(2).extracting(PropertyPath::toDotPath).containsOnly("domains.id", "domains.value");
	}

	@Test // GH-2420
	void considersPropertiesWithinContainers() {

		EntityProjection<?, ?> descriptor = discoverer.introspect(WithMapOfCollectionProjection.class,
				WithMapOfCollection.class);

		assertThat(descriptor.isProjection()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEachRecursive(it -> {
			paths.add(it.getPropertyPath());
		});

		assertThat(paths).hasSize(3).extracting(PropertyPath::toDotPath).containsOnly("domains", "id", "value");
	}

	interface SuperInterface {

	}

	static class Root implements SuperInterface {

	}

	static class Middle extends Root {

	}

	static class Leaf extends Middle {

	}

	static class WithCollection {

		List<DomainClass> domains;
	}

	static class WithMapOfCollection {

		Map<String, List<DomainClass>> domains;
	}

	@Getter
	static class WithMapOfCollectionProjection {

		Map<String, List<DomainClassProjection>> domains;
	}

	interface WithCollectionProjection {

		List<DomainClassProjection> getDomains();
	}

	static class DomainClass {

		String id;
		long value;
		String name;
	}

	static class WithComplexObject {

		DomainClass domain;
		DomainClass domain2;
	}

	interface WithNestedProjection {

		DomainClassProjection getDomain();

		DomainClass getDomain2();
	}

	interface DomainClassProjection {

		String getId();

		long getValue();

		String getFoo();
	}

	interface OpenProjection {

		@org.springframework.beans.factory.annotation.Value("#{target.foo}")
		String getFoo();
	}

	@Value
	static class DomainClassDto {

		String id;
		long value;

		public DomainClassDto(String id, long value) {
			this.id = id;
			this.value = value;
		}
	}

	static class Person {

		Address primaryAddress;

		Address secondaryAddress;
	}

	static class Address {

		Person owner;
	}

	interface PersonProjection {

		AddressProjection getPrimaryAddress();

		AddressProjection getSecondaryAddress();
	}

	interface AddressProjection {

		PersonProjection getOwner();
	}
}
