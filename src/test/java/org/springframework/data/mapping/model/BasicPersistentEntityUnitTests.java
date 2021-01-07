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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.Document;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentEntitySpec;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for {@link BasicPersistentEntity}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class BasicPersistentEntityUnitTests<T extends PersistentProperty<T>> {

	@Mock T property, anotherProperty;

	@Test
	void assertInvariants() {
		PersistentEntitySpec.assertInvariants(createEntity(Person.class));
	}

	@Test
	void rejectsNullTypeInformation() {
		assertThatIllegalArgumentException().isThrownBy(() -> new BasicPersistentEntity<Object, T>(null));
	}

	@Test
	void rejectsNullProperty() {
		assertThatIllegalArgumentException().isThrownBy(() -> createEntity(Person.class, null).addPersistentProperty(null));
	}

	@Test
	void returnsNullForTypeAliasIfNoneConfigured() {

		PersistentEntity<Entity, T> entity = createEntity(Entity.class);
		assertThat(entity.getTypeAlias()).isEqualTo(Alias.NONE);
	}

	@Test
	void returnsTypeAliasIfAnnotated() {

		PersistentEntity<AliasedEntity, T> entity = createEntity(AliasedEntity.class);
		assertThat(entity.getTypeAlias()).isEqualTo(Alias.of("foo"));
	}

	@Test // DATACMNS-50
	@SuppressWarnings("unchecked")
	void considersComparatorForPropertyOrder() {

		BasicPersistentEntity<Person, T> entity = createEntity(Person.class,
				Comparator.comparing(PersistentProperty::getName));

		T lastName = (T) Mockito.mock(PersistentProperty.class);
		when(lastName.getName()).thenReturn("lastName");

		T firstName = (T) Mockito.mock(PersistentProperty.class);
		when(firstName.getName()).thenReturn("firstName");

		T ssn = (T) Mockito.mock(PersistentProperty.class);
		when(ssn.getName()).thenReturn("ssn");

		entity.addPersistentProperty(lastName);
		entity.addPersistentProperty(firstName);
		entity.addPersistentProperty(ssn);
		entity.verify();

		List<T> properties = (List<T>) ReflectionTestUtils.getField(entity, "properties");

		assertThat(properties).hasSize(3);
		Iterator<T> iterator = properties.iterator();

		assertThat(entity.getPersistentProperty("firstName")).isEqualTo(iterator.next());
		assertThat(entity.getPersistentProperty("lastName")).isEqualTo(iterator.next());
		assertThat(entity.getPersistentProperty("ssn")).isEqualTo(iterator.next());
	}

	@Test // DATACMNS-18, DATACMNS-1364
	void addingAndIdPropertySetsIdPropertyInternally() {

		MutablePersistentEntity<Person, T> entity = createEntity(Person.class);
		assertThat(entity.getIdProperty()).isNull();

		when(property.getName()).thenReturn("id");
		when(property.isIdProperty()).thenReturn(true);
		entity.addPersistentProperty(property);
		assertThat(entity.getIdProperty()).isEqualTo(property);
	}

	@Test // DATACMNS-186, DATACMNS-1364
	void rejectsIdPropertyIfAlreadySet() {

		MutablePersistentEntity<Person, T> entity = createEntity(Person.class);

		when(property.getName()).thenReturn("id");
		when(property.isIdProperty()).thenReturn(true);
		when(anotherProperty.isIdProperty()).thenReturn(true);
		when(anotherProperty.getName()).thenReturn("another");

		entity.addPersistentProperty(property);
		assertThatExceptionOfType(MappingException.class).isThrownBy(() -> entity.addPersistentProperty(anotherProperty));
	}

	@Test // DATACMNS-365
	void detectsPropertyWithAnnotation() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		SamplePersistentProperty property = entity.getPersistentProperty(LastModifiedBy.class);

		assertThat(property.getName()).isEqualTo("field");

		property = entity.getPersistentProperty(CreatedBy.class);

		assertThat(property.getName()).isEqualTo("property");

		assertThat(entity.getPersistentProperty(CreatedDate.class)).isNull();
	}

	@Test // DATACMNS-1122
	void reportsRequiredPropertyName() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		assertThatThrownBy(() -> entity.getRequiredPersistentProperty("foo"))
				.hasMessageContaining("Required property foo not found");
	}

	@Test // DATACMNS-809, DATACMNS-1768
	@SuppressWarnings("rawtypes")
	void returnsGeneratedPropertyAccessorForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		Entity value = new Entity();
		PersistentPropertyAccessor accessor = entity.getPropertyAccessor(value);

		assertThat(accessor).isNotInstanceOf(BeanWrapper.class);
		assertThat(accessor).isInstanceOfSatisfying(InstantiationAwarePropertyAccessor.class, it -> {

			Function<Object, PersistentPropertyAccessor<Object>> delegateFunction = (Function<Object, PersistentPropertyAccessor<Object>>) ReflectionTestUtils
					.getField(it, "delegateFunction");

			PersistentPropertyAccessor<Object> delegate = delegateFunction.apply(value);
			assertThat(delegate.getClass().getName()).contains("_Accessor_");
			assertThat(delegate.getBean()).isEqualTo(value);
		});
	}

	@Test // DATACMNS-596
	void rejectsNullBeanForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		assertThatIllegalArgumentException().isThrownBy(() -> entity.getPropertyAccessor(null));
	}

	@Test // DATACMNS-596
	void rejectsNonMatchingBeanForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		assertThatIllegalArgumentException().isThrownBy(() -> entity.getPropertyAccessor("foo"));
	}

	@Test // DATACMNS-597
	void supportsSubtypeInstancesOnPropertyAccessorLookup() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		assertThat(entity.getPropertyAccessor(new Subtype())).isNotNull();
	}

	@Test // DATACMNS-825
	void returnsTypeAliasIfAnnotatedUsingComposedAnnotation() {

		PersistentEntity<AliasEntityUsingComposedAnnotation, T> entity = createEntity(
				AliasEntityUsingComposedAnnotation.class);
		assertThat(entity.getTypeAlias()).isEqualTo(Alias.of("bar"));
	}

	@Test // DATACMNS-866
	void invalidBeanAccessCreatesDescriptiveErrorMessage() {

		PersistentEntity<Entity, T> entity = createEntity(Entity.class);

		assertThatThrownBy(() -> entity.getPropertyAccessor(new Object())).hasMessageContaining(Entity.class.getName())
				.hasMessageContaining(Object.class.getName());
	}

	@Test // DATACMNS-934, DATACMNS-867
	void rejectsNullAssociation() {

		MutablePersistentEntity<Entity, T> entity = createEntity(Entity.class);

		assertThatIllegalArgumentException().isThrownBy(() -> entity.addAssociation(null));
	}

	@Test // DATACMNS-1141
	void getRequiredAnnotationReturnsAnnotation() {

		PersistentEntity<AliasEntityUsingComposedAnnotation, T> entity = createEntity(
				AliasEntityUsingComposedAnnotation.class);
		assertThat(entity.getRequiredAnnotation(TypeAlias.class).value()).isEqualTo("bar");
	}

	@Test // DATACMNS-1141
	void getRequiredAnnotationThrowsException() {

		PersistentEntity<AliasEntityUsingComposedAnnotation, T> entity = createEntity(
				AliasEntityUsingComposedAnnotation.class);

		assertThatThrownBy(() -> entity.getRequiredAnnotation(Document.class)).isInstanceOf(IllegalStateException.class);
	}

	@Test // DATACMNS-1210
	void findAnnotationShouldBeThreadSafe() throws InterruptedException {

		CountDownLatch latch = new CountDownLatch(2);
		CountDownLatch syncLatch = new CountDownLatch(1);
		AtomicBoolean failed = new AtomicBoolean(false);

		PersistentEntity<EntityWithAnnotation, T> entity = new BasicPersistentEntity<EntityWithAnnotation, T>(
				ClassTypeInformation.from(EntityWithAnnotation.class), null) {

			@Nullable
			@Override
			public <A extends Annotation> A findAnnotation(Class<A> annotationType) {

				try {
					syncLatch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				return super.findAnnotation(annotationType);
			}
		};

		Runnable findAccessType = () -> {

			try {
				entity.findAnnotation(AccessType.class);
			} catch (Exception e) {
				failed.set(true);
			} finally {
				latch.countDown();
			}
		};

		Runnable findPersistent = () -> {

			try {
				entity.findAnnotation(Persistent.class);
			} catch (Exception e) {
				failed.set(true);
			} finally {
				latch.countDown();
			}
		};

		new Thread(findAccessType).start();
		new Thread(findPersistent).start();

		syncLatch.countDown();
		latch.await();

		assertThat(failed.get()).isFalse();
	}

	@Test // DATACMNS-1325
	void supportsPersistableViaIdentifierAccessor() {

		PersistentEntity<PersistableEntity, T> entity = createEntity(PersistableEntity.class);

		assertThat(entity.getIdentifierAccessor(new PersistableEntity()).getRequiredIdentifier()).isEqualTo(4711L);
	}

	@Test // DATACMNS-1322
	void detectsImmutableEntity() {

		assertThat(createEntity(SomeValue.class).isImmutable()).isTrue();
		assertThat(createEntity(Entity.class).isImmutable()).isFalse();
	}

	@Test // DATACMNS-1366
	void exposesPropertyPopulationRequired() {

		assertThat(createPopulatedPersistentEntity(PropertyPopulationRequired.class).requiresPropertyPopulation()).isTrue();
	}

	@Test // DATACMNS-1366
	void exposesPropertyPopulationNotRequired() {

		Stream.of(PropertyPopulationNotRequired.class, PropertyPopulationNotRequiredWithTransient.class) //
				.forEach(it -> assertThat(createPopulatedPersistentEntity(it).requiresPropertyPopulation()).isFalse());
	}

	private <S> BasicPersistentEntity<S, T> createEntity(Class<S> type) {
		return createEntity(type, null);
	}

	private <S> BasicPersistentEntity<S, T> createEntity(Class<S> type, Comparator<T> comparator) {
		return new BasicPersistentEntity<>(ClassTypeInformation.from(type), comparator);
	}

	private static PersistentEntity<Object, ?> createPopulatedPersistentEntity(Class<?> type) {

		SampleMappingContext context = new SampleMappingContext();
		return context.getRequiredPersistentEntity(type);
	}

	@TypeAlias("foo")
	private static class AliasedEntity {

	}

	static class Entity {

		@LastModifiedBy String field;
		String property;

		/**
		 * @return the property
		 */
		@CreatedBy
		public String getProperty() {
			return property;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@TypeAlias("foo")
	static @interface ComposedTypeAlias {

		@AliasFor(annotation = TypeAlias.class, attribute = "value")
		String name() default "bar";
	}

	@ComposedTypeAlias
	private static class AliasEntityUsingComposedAnnotation {}

	private static class Subtype extends Entity {}

	@AccessType(Type.PROPERTY)
	private static class EntityWithAnnotation {

	}

	// DATACMNS-1325

	static class PersistableEntity implements Persistable<Long> {

		private final Long id = 42L;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.domain.Persistable#getId()
		 */
		@Override
		public Long getId() {
			return 4711L;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.domain.Persistable#isNew()
		 */
		@Override
		public boolean isNew() {
			return false;
		}
	}

	@Immutable
	private static class SomeValue {}

	// DATACMNS-1366

	@RequiredArgsConstructor
	private static class PropertyPopulationRequired {

		private final String firstname, lastname;
		private String email;
	}

	@RequiredArgsConstructor
	private static class PropertyPopulationNotRequired {

		private final String firstname, lastname;
	}

	@RequiredArgsConstructor
	private static class PropertyPopulationNotRequiredWithTransient {

		private final String firstname, lastname;
		private @Transient String email;
	}
}
