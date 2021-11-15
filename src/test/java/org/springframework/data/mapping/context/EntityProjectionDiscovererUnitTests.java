/*
 * Copyright 2021 the original author or authors.
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

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.context.EntityProjectionDiscoverer.ReturnedTypeDescriptor;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

/**
 * Unit tests for {@link EntityProjectionDiscoverer}.
 *
 * @author Mark Paluch
 */
class EntityProjectionDiscovererUnitTests {

	SampleMappingContext mappingContext = new SampleMappingContext();
	SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();
	EntityProjectionDiscoverer.ProjectionPredicate predicate = (target,
			underlyingType) -> !SimpleTypeHolder.DEFAULT.isSimpleType(target);
	EntityProjectionDiscoverer discoverer = EntityProjectionDiscoverer.create(projectionFactory,
			predicate.and(EntityProjectionDiscoverer.ProjectionPredicate.typeHierarchy()), mappingContext);

	@Test
	void shouldDiscoverTypeHierarchy() {

		// super type
		assertThat(discoverer.introspectReturnType(Root.class, Middle.class).isProjecting()).isFalse();

		assertThat(discoverer.introspectReturnType(SuperInterface.class, Middle.class).isProjecting()).isFalse();

		// subtypes
		assertThat(discoverer.introspectReturnType(Leaf.class, Middle.class).isProjecting()).isFalse();
	}

	@Test
	void shouldConsiderTopLevelInterfaceProperties() {

		ReturnedTypeDescriptor descriptor = discoverer.introspectReturnType(DomainClassProjection.class, DomainClass.class);

		assertThat(descriptor.isProjecting()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEach(paths::add);

		assertThat(paths).hasSize(2).extracting(PropertyPath::toDotPath).containsOnly("id", "value");
	}

	@Test
	void shouldConsiderTopLevelDtoProperties() {

		ReturnedTypeDescriptor descriptor = discoverer.introspectReturnType(DomainClassDto.class, DomainClass.class);

		assertThat(descriptor.isProjecting()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEach(paths::add);

		assertThat(paths).hasSize(2).extracting(PropertyPath::toDotPath).containsOnly("id", "value");
	}

	@Test
	void shouldConsiderNestedProjectionProperties() {

		ReturnedTypeDescriptor descriptor = discoverer.introspectReturnType(WithNestedProjection.class,
				WithComplexObject.class);

		assertThat(descriptor.isProjecting()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEach(paths::add);

		assertThat(paths).hasSize(3).extracting(PropertyPath::toDotPath).containsOnly("domain.id", "domain.value",
				"domain2");
	}

	@Test
	void shouldConsiderOpenProjection() {

		ReturnedTypeDescriptor descriptor = discoverer.introspectReturnType(OpenProjection.class, DomainClass.class);

		assertThat(descriptor.isProjecting()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEach(paths::add);

		assertThat(paths).isEmpty();
	}

	@Test
	void shouldConsiderCyclicProjections() {

		ReturnedTypeDescriptor descriptor = discoverer.introspectReturnType(CyclicProjection1.class, CyclicDomain1.class);

		assertThat(descriptor.isProjecting()).isTrue();

		List<PropertyPath> paths = new ArrayList<>();
		descriptor.forEach(paths::add);

		assertThat(paths).hasSize(4).extracting(PropertyPath::toDotPath).containsOnly("name", "level1.name",
				"level1.level2.name", "level1.level2.level1");
	}

	interface SuperInterface {

	}

	static class Root implements SuperInterface {

	}

	static class Middle extends Root {

	}

	static class Leaf extends Middle {

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

	static class CyclicDomain1 {

		CyclicDomain2 level1;
		String name;
	}

	static class CyclicDomain2 {

		CyclicDomain1 level2;
		String name;
	}

	static interface CyclicProjection1 {

		CyclicProjection2 getLevel1();

		String getName();
	}

	static interface CyclicProjection2 {

		CyclicProjection1 getLevel2();

		String getName();
	}

}
