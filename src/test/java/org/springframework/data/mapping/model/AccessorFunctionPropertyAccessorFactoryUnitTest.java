/*
 * Copyright 2020 the original author or authors.
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
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PersonWithId;
import org.springframework.data.mapping.PersonWithIdTypeInformation;
import org.springframework.data.util.ClassTypeInformation;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
@ExtendWith(MockitoExtension.class)
class AccessorFunctionPropertyAccessorFactoryUnitTest<P extends PersistentProperty<P>> {

	@Mock PersistentEntity<PersonWithId, P> entity;
	@Mock P property;
	AccessorFunctionPropertyAccessorFactory factory = new AccessorFunctionPropertyAccessorFactory();

	@Test // DATACMNS-???
	void refusesNonDomainTypeInformation() {

		when(entity.getTypeInformation()).thenReturn(ClassTypeInformation.from(PersonWithId.class));

		assertThat(factory.isSupported(entity)).isFalse();
	}

	@Test // DATACMNS-???
	void acceptsDomainTypeInformation() {

		when(entity.getTypeInformation()).thenReturn(PersonWithIdTypeInformation.instance());

		assertThat(factory.isSupported(entity)).isTrue();
	}

	@Test // DATACMNS-???
	void accessorCallsGetterFunction() {

		when(entity.getTypeInformation()).thenReturn(PersonWithIdTypeInformation.instance());
		when(property.getName()).thenReturn("firstName");

		PersistentPropertyAccessor<PersonWithId> accessor = factory.getPropertyAccessor(entity,
				new PersonWithId(1, "fn", "ln"));

		assertThat(accessor.getProperty(property)).isEqualTo("fn");
	}

	@Test // DATACMNS-???
	void accessorCallsSetterFunction() {

		when(entity.getTypeInformation()).thenReturn(PersonWithIdTypeInformation.instance());
		when(property.getName()).thenReturn("firstName");

		PersistentPropertyAccessor<PersonWithId> accessor = factory.getPropertyAccessor(entity,
				new PersonWithId(1, "fn", "ln"));

		accessor.setProperty(property, "edited");
		assertThat(accessor.getBean().getFirstName()).isEqualTo("edited");
	}

	// TODO: ignore non existing properties?

}
