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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import groovy.lang.MetaClass;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit test for {@link AbstractMappingContext}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
class AbstractMappingContextUnitTests {

	SampleMappingContext context;

	@BeforeEach
	void setUp() {

		context = new SampleMappingContext();
		context.setSimpleTypeHolder(new SimpleTypeHolder(Collections.singleton(LocalDateTime.class), true));
	}

	@Test // DATACMNS-92
	void doesNotAddInvalidEntity() {

		context = TypeRejectingMappingContext.rejecting(() -> new MappingException("Not supported!"), Unsupported.class);

		assertThatExceptionOfType(MappingException.class) //
				.isThrownBy(() -> context.getPersistentEntity(Unsupported.class));
	}

	@Test
	void registersEntitiesOnInitialization() {

		ApplicationContext applicationContext = mock(ApplicationContext.class);

		context.setInitialEntitySet(Collections.singleton(Person.class));
		context.setApplicationEventPublisher(applicationContext);

		verify(applicationContext, times(0)).publishEvent(any(ApplicationEvent.class));

		context.afterPropertiesSet();
		verify(applicationContext, times(1)).publishEvent(any(ApplicationEvent.class));
	}

	@Test // DATACMNS-214
	void returnsNullPersistentEntityForSimpleTypes() {

		SampleMappingContext context = new SampleMappingContext();
		assertThat(context.getPersistentEntity(String.class)).isNull();
	}

	@Test // DATACMNS-214
	void rejectsNullValueForGetPersistentEntityOfClass() {
		assertThatIllegalArgumentException().isThrownBy(() -> context.getPersistentEntity((Class<?>) null));
	}

	@Test // DATACMNS-214
	void rejectsNullValueForGetPersistentEntityOfTypeInformation() {
		assertThatIllegalArgumentException().isThrownBy(() -> context.getPersistentEntity((TypeInformation<?>) null));
	}

	@Test // DATACMNS-228
	void doesNotCreatePersistentPropertyForGroovyMetaClass() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		mappingContext.initialize();

		PersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Sample.class);
		assertThat(entity.getPersistentProperty("metaClass")).isNull();
	}

	@Test // DATACMNS-332
	void usesMostConcreteProperty() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Extension.class);

		assertThat(entity.getPersistentProperty("foo")).satisfies(it -> assertThat(it.isIdProperty()).isTrue());
	}

	@Test // DATACMNS-345
	@SuppressWarnings("rawtypes")
	void returnsEntityForComponentType() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Sample.class);

		assertThat(entity.getPersistentProperty("persons"))
				.satisfies(it -> assertThat(mappingContext.getPersistentEntity(it))
						.satisfies(inner -> assertThat(((PersistentEntity) inner).getType()).isEqualTo(Person.class)));
	}

	@Test // DATACMNS-390
	void exposesCopyOfPersistentEntitiesToAvoidConcurrentModificationException() {

		SampleMappingContext context = new SampleMappingContext();
		context.getPersistentEntity(ClassTypeInformation.MAP);

		Iterator<BasicPersistentEntity<Object, SamplePersistentProperty>> iterator = context.getPersistentEntities()
				.iterator();

		while (iterator.hasNext()) {
			context.getPersistentEntity(ClassTypeInformation.SET);
			iterator.next();
		}
	}

	@Test // DATACMNS-447
	void shouldReturnNullForSimpleTypesIfInStrictIsEnabled() {

		context.setStrict(true);
		assertThat(context.getPersistentEntity(Integer.class)).isNull();
	}

	@Test // DATACMNS-462
	void hasPersistentEntityForCollectionPropertiesAfterInitialization() {

		context.getPersistentEntity(Sample.class);
		assertHasEntityFor(Person.class, context, true);
	}

	@Test // DATACMNS-479
	void doesNotAddMapImplementationClassesAsPersistentEntity() {

		context.getPersistentEntity(Sample.class);
		assertHasEntityFor(TreeMap.class, context, false);
	}

	@Test // DATACMNS-1171
	void shouldCreateEntityForKotlinDataClass() {
		assertThat(context.getPersistentEntity(SimpleDataClass.class)).isNotNull();
	}

	@Test // DATACMNS-1171
	void shouldNotCreateEntityForSyntheticKotlinClass() {
		assertThat(context.getPersistentEntity(TypeCreatingSyntheticClass.class)).isNotNull();
	}

	@Test // DATACMNS-1208
	void ensureHasPersistentEntityReportsFalseForTypesThatShouldntBeCreated() {

		assertThat(context.hasPersistentEntityFor(String.class)).isFalse();
		assertThat(context.getPersistentEntity(String.class)).isNull();
		assertThat(context.hasPersistentEntityFor(String.class)).isFalse();
	}

	@Test // DATACMNS-1214
	void doesNotReturnPersistentEntityForCustomSimpleTypeProperty() {

		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Person.class);
		SamplePersistentProperty property = entity.getRequiredPersistentProperty("date");

		assertThat(context.getPersistentEntity(property)).isNull();
	}

	@Test // DATACMNS-1574
	void cleansUpCacheForRuntimeException() {

		TypeRejectingMappingContext context = TypeRejectingMappingContext.rejecting(() -> new RuntimeException(),
				Unsupported.class);

		assertThatExceptionOfType(RuntimeException.class) //
				.isThrownBy(() -> context.getPersistentEntity(Unsupported.class));

		// Second lookup still throws the exception as the temporarily created entity was not cached

		assertThatExceptionOfType(RuntimeException.class) //
				.isThrownBy(() -> context.getPersistentEntity(Unsupported.class));
	}

	private static void assertHasEntityFor(Class<?> type, SampleMappingContext context, boolean expected) {

		boolean found = false;

		for (BasicPersistentEntity<Object, SamplePersistentProperty> entity : context.getPersistentEntities()) {
			if (entity.getType().equals(type)) {
				found = true;
				break;
			}
		}

		if (found != expected) {
			fail(String.format("%s to find persistent entity for %s!", expected ? "Expected" : "Did not expect", type));
		}
	}

	class Person {
		String name;
		LocalDateTime date;
	}

	class Unsupported {

	}

	class Sample {

		MetaClass metaClass;
		List<Person> persons;
		TreeMap<String, Person> personMap;
	}

	static class Base {
		String foo;
	}

	static class Extension extends Base {
		@Id String foo;
	}

	/**
	 * Extension of {@link SampleMappingContext} to reject the creation of certain types with a configurable exception.
	 *
	 * @author Oliver Drotbohm
	 */
	@Value
	@EqualsAndHashCode(callSuper = false)
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static class TypeRejectingMappingContext extends SampleMappingContext {

		Supplier<? extends RuntimeException> exception;
		Collection<Class<?>> rejectedTypes;

		/**
		 * Creates a new {@link TypeRejectingMappingContext} producing the given exceptions if any of the given types is
		 * encountered.
		 *
		 * @param <T>
		 * @param exception must not be {@literal null}.
		 * @param types must not be {@literal null}.
		 * @return
		 */
		static <T extends RuntimeException> TypeRejectingMappingContext rejecting(Supplier<T> exception,
				Class<?>... types) {
			return new TypeRejectingMappingContext(exception, Arrays.asList(types));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.context.SampleMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
		 */
		@Override
		protected <S> BasicPersistentEntity<Object, SamplePersistentProperty> createPersistentEntity(
				TypeInformation<S> typeInformation) {

			return new BasicPersistentEntity<Object, SamplePersistentProperty>((TypeInformation<Object>) typeInformation) {

				/*
				 * (non-Javadoc)
				 * @see org.springframework.data.mapping.model.BasicPersistentEntity#verify()
				 */
				@Override
				public void verify() {
					if (rejectedTypes.stream().anyMatch(it -> it.isAssignableFrom(getType()))) {
						throw exception.get();
					}
				}
			};
		}
	}
}
