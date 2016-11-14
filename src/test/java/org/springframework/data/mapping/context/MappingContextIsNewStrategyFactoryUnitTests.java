/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mapping.context.MappingContextIsNewStrategyFactory.PropertyIsNullIsNewStrategy;
import org.springframework.data.mapping.context.MappingContextIsNewStrategyFactory.PropertyIsNullOrZeroNumberIsNewStrategy;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.support.IsNewStrategyFactory;

/**
 * Unit tests for {@link MappingContextIsNewStrategyFactory}.
 * 
 * @author Oliver Gierke
 */
public class MappingContextIsNewStrategyFactoryUnitTests {

	IsNewStrategyFactory factory;

	@Before
	public void setUp() {

		SampleMappingContext context = new SampleMappingContext();
		context.setInitialEntitySet(new HashSet<>(Arrays.asList(Entity.class, VersionedEntity.class)));
		context.afterPropertiesSet();

		factory = new MappingContextIsNewStrategyFactory(new PersistentEntities(Collections.singleton(context)));
	}

	@Test
	public void returnsPropertyIsNullOrZeroIsNewStrategyForVersionedEntity() {

		IsNewStrategy strategy = factory.getIsNewStrategy(VersionedEntity.class);
		assertThat(strategy).isInstanceOf(PropertyIsNullOrZeroNumberIsNewStrategy.class);

		Optional<VersionedEntity> entity = Optional.of(new VersionedEntity());
		assertThat(strategy.isNew(entity)).isTrue();

		entity.get().id = 1L;
		assertThat(strategy.isNew(entity)).isTrue();

		entity.get().version = 0L;
		assertThat(strategy.isNew(entity)).isTrue();

		entity.get().version = 1L;
		assertThat(strategy.isNew(entity)).isFalse();
	}

	@Test
	public void returnsPropertyIsNullOrZeroIsNewStrategyForPrimitiveVersionedEntity() {

		IsNewStrategy strategy = factory.getIsNewStrategy(VersionedEntity.class);
		assertThat(strategy).isInstanceOf(PropertyIsNullOrZeroNumberIsNewStrategy.class);

		Optional<VersionedEntity> entity = Optional.of(new VersionedEntity());
		assertThat(strategy.isNew(entity)).isTrue();

		entity.get().id = 1L;
		assertThat(strategy.isNew(entity)).isTrue();

		entity.get().version = 1L;
		assertThat(strategy.isNew(entity)).isFalse();
	}

	@Test
	public void returnsPropertyIsNullIsNewStrategyForEntity() {

		IsNewStrategy strategy = factory.getIsNewStrategy(Entity.class);
		assertThat(strategy).isInstanceOf(PropertyIsNullIsNewStrategy.class);

		Optional<Entity> entity = Optional.of(new Entity());
		assertThat(strategy.isNew(entity)).isTrue();

		entity.get().id = 1L;
		assertThat(strategy.isNew(entity)).isFalse();
	}

	@SuppressWarnings("serial")
	static class PersistableEntity implements Persistable<Long> {

		@Version Long version;

		@Id Long id;

		boolean isNew = true;

		public Long getId() {
			return id;
		}

		public boolean isNew() {
			return isNew;
		}
	}

	static class VersionedEntity {

		@Version Long version;

		@Id Long id;
	}

	static class PrimitveVersionedEntity {

		@Version long version = 0;

		@Id Long id;
	}

	static class Entity {

		@Id Long id;
	}
}
