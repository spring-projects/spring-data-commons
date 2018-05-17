/*
 * Copyright 2014-2018 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit tests for {@link PersistentEntities}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntitiesUnitTests {

	@Mock SampleMappingContext first;
	@Mock SampleMappingContext second;

	@Test(expected = IllegalArgumentException.class) // DATACMNS-458
	public void rejectsNullMappingContexts() {
		new PersistentEntities(null);
	}

	@Test // DATACMNS-458
	public void returnsPersistentEntitiesFromMappingContexts() {

		when(first.hasPersistentEntityFor(Sample.class)).thenReturn(false);
		when(second.hasPersistentEntityFor(Sample.class)).thenReturn(true);

		PersistentEntities.of(first, second).getPersistentEntity(Sample.class);

		verify(first, times(1)).hasPersistentEntityFor(Sample.class);
		verify(first, times(0)).getRequiredPersistentEntity(Sample.class);

		verify(second, times(1)).hasPersistentEntityFor(Sample.class);
		verify(second, times(1)).getRequiredPersistentEntity(Sample.class);
	}

	@Test // DATACMNS-458
	public void indicatesManagedType() {

		SampleMappingContext context = new SampleMappingContext();
		context.setInitialEntitySet(Collections.singleton(Sample.class));
		context.initialize();

		PersistentEntities entities = PersistentEntities.of(context);

		assertThat(entities.getPersistentEntity(Sample.class)).isPresent();
		assertThat(entities.getPersistentEntity(Object.class)).isNotPresent();
		assertThat(entities.getManagedTypes()).contains(ClassTypeInformation.from(Sample.class));

		assertThat(entities.getPersistentEntity(Sample.class)).hasValueSatisfying(it -> assertThat(entities).contains(it));
	}

	@Test // DATACMNS-1318
	public void detectsReferredToEntity() {

		SampleMappingContext context = new SampleMappingContext();
		context.getPersistentEntity(Sample.class);

		SamplePersistentProperty property = context.getRequiredPersistentEntity(WithReference.class)//
				.getPersistentProperty("sampleId");

		PersistentEntity<?, ?> referredToEntity = PersistentEntities.of(context).getEntityUltimatelyReferredToBy(property);

		assertThat(referredToEntity).isNotNull();
		assertThat(referredToEntity.getType()).isEqualTo(Sample.class);
	}

	@Test // DATACMNS-1318
	public void rejectsAmbiguousIdentifierType() {

		SampleMappingContext context = new SampleMappingContext();
		context.getPersistentEntity(FirstWithLongId.class);
		context.getPersistentEntity(SecondWithLongId.class);

		SamplePersistentProperty property = context.getRequiredPersistentEntity(WithReference.class) //
				.getPersistentProperty("longId");

		PersistentEntities entities = PersistentEntities.of(context);

		assertThatExceptionOfType(IllegalStateException.class)//
				.isThrownBy(() -> entities.getEntityUltimatelyReferredToBy(property)) //
				.withMessageContaining(FirstWithLongId.class.getName()) //
				.withMessageContaining(SecondWithLongId.class.getName()) //
				.withMessageContaining(Reference.class.getSimpleName());
	}

	@Test // DATACMNS-1318
	public void allowsExplicitlyQualifiedReference() {

		SampleMappingContext context = new SampleMappingContext();
		context.getPersistentEntity(FirstWithLongId.class);
		context.getPersistentEntity(SecondWithLongId.class);

		SamplePersistentProperty property = context.getRequiredPersistentEntity(WithReference.class) //
				.getPersistentProperty("qualifiedLongId");

		PersistentEntity<?, ?> entity = PersistentEntities.of(context).getEntityUltimatelyReferredToBy(property);

		assertThat(entity).isNotNull();
		assertThat(entity.getType()).isEqualTo(FirstWithLongId.class);
	}

	@Test // DATACMNS-1318
	public void allowsGenericReference() {

		SampleMappingContext context = new SampleMappingContext();
		context.getPersistentEntity(FirstWithGenericId.class);
		context.getPersistentEntity(SecondWithGenericId.class);

		SamplePersistentProperty property = context.getRequiredPersistentEntity(WithReference.class) //
				.getPersistentProperty("generic");

		PersistentEntity<?, ?> entity = PersistentEntities.of(context).getEntityUltimatelyReferredToBy(property);

		assertThat(entity).isNotNull();
		assertThat(entity.getType()).isEqualTo(SecondWithGenericId.class);
	}

	static class Sample {
		@Id String id;
	}

	static class WithReference {
		@Reference String sampleId;
		@Reference Long longId;
		@Reference(FirstWithLongId.class) Long qualifiedLongId;
		@Reference Identifier<SecondWithGenericId> generic;
	}

	static class FirstWithLongId {
		@Id Long id;
	}

	static class SecondWithLongId {
		@Id Long id;
	}

	static class FirstWithGenericId {
		@Id Identifier<FirstWithGenericId> id;
	}

	static class SecondWithGenericId {
		@Id Identifier<SecondWithGenericId> id;
	}

	interface Identifier<T> {}
}
