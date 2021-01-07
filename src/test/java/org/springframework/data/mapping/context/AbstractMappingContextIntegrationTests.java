/*
 * Copyright 2011-2021 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Integration tests for {@link AbstractMappingContext}.
 *
 * @author Oliver Gierke
 */
class AbstractMappingContextIntegrationTests<T extends PersistentProperty<T>> {

	@Test // DATACMNS-457
	void returnsManagedType() {

		SampleMappingContext context = new SampleMappingContext();
		context.setInitialEntitySet(Collections.singleton(Person.class));
		context.initialize();

		assertThat(context.getManagedTypes()).contains(ClassTypeInformation.from(Person.class));
	}

	@Test // DATACMNS-457
	void indicatesManagedType() {

		SampleMappingContext context = new SampleMappingContext();
		context.setInitialEntitySet(Collections.singleton(Person.class));
		context.initialize();

		assertThat(context.hasPersistentEntityFor(Person.class)).isTrue();
	}

	@Test // DATACMNS-243
	void createsPersistentEntityForInterfaceCorrectly() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context
				.getRequiredPersistentEntity(InterfaceOnly.class);

		assertThat(entity.getIdProperty()).isNotNull();
	}

	@Test // DATACMNS-65
	void foo() throws InterruptedException {

		final DummyMappingContext context = new DummyMappingContext();

		Thread a = new Thread(() -> context.getPersistentEntity(Person.class));

		Thread b = new Thread(() -> {

			PersistentEntity<Object, T> entity = context.getRequiredPersistentEntity(Person.class);

			entity.doWithProperties((PropertyHandler<T>) persistentProperty -> {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
		});

		a.start();
		Thread.sleep(700);
		b.start();

		a.join();
		b.join();
	}

	class DummyMappingContext extends AbstractMappingContext<BasicPersistentEntity<Object, T>, T> {

		@Override
		@SuppressWarnings("unchecked")
		protected <S> BasicPersistentEntity<Object, T> createPersistentEntity(TypeInformation<S> typeInformation) {
			return (BasicPersistentEntity<Object, T>) new BasicPersistentEntity<S, T>(typeInformation);
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		protected T createPersistentProperty(Property property, BasicPersistentEntity<Object, T> owner,
				SimpleTypeHolder simpleTypeHolder) {

			PersistentProperty prop = mock(PersistentProperty.class);

			when(prop.getTypeInformation()).thenReturn(owner.getTypeInformation());
			when(prop.getName()).thenReturn(property.getName());
			when(prop.getPersistentEntityTypes()).thenReturn(Collections.EMPTY_SET);

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			return (T) prop;
		}
	}

	class Person {

		String firstname;
		String lastname;
		String email;
	}

	interface InterfaceOnly {

		@Id
		String getId();
	}
}
