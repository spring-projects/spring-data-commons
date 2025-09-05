/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProviderUnitTests.Outer.Inner;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link PersistentEntityParameterValueProvider}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class PersistentEntityParameterValueProviderUnitTests<P extends PersistentProperty<P>> {

	@Mock PropertyValueProvider<P> propertyValueProvider;
	@Mock P property;

	@Test // DATACMNS-134
	void usesParentObjectAsImplicitFirstConstructorArgument() {

		Object outer = new Outer();

		PersistentEntity<Inner, P> entity = new BasicPersistentEntity<Inner, P>(TypeInformation.of(Inner.class)) {

			@Override
			public P getPersistentProperty(String name) {
				return property;
			}
		};

		assertThat(entity.getInstanceCreatorMetadata()).satisfies(constructor -> {

			var iterator = constructor.getParameters().iterator();
			ParameterValueProvider<P> provider = new PersistentEntityParameterValueProvider<>(entity, propertyValueProvider,
					outer);

			assertThat(provider.getParameterValue(iterator.next())).isEqualTo(outer);
			assertThat(provider.getParameterValue(iterator.next())).isNull();
			assertThat(iterator.hasNext()).isFalse();
		});
	}

	@Test
	void rejectsPropertyIfNameDoesNotMatch() {

		PersistentEntity<Entity, P> entity = new BasicPersistentEntity<>(TypeInformation.of(Entity.class));
		ParameterValueProvider<P> provider = new PersistentEntityParameterValueProvider<>(entity, propertyValueProvider,
				Optional.of(property));

		assertThat(entity.getInstanceCreatorMetadata())
				.satisfies(constructor -> assertThatExceptionOfType(MappingException.class)//
						.isThrownBy(() -> provider.getParameterValue(constructor.getParameters().iterator().next()))//
						.withMessageContaining("bar")//
						.withMessageContaining(Entity.class.getName()));
	}

	static class Outer {

		class Inner {

			Object myObject;

			Inner(Object myObject) {

			}
		}
	}

	static class Entity {

		String foo;

		public Entity(String bar) {

		}
	}
}
