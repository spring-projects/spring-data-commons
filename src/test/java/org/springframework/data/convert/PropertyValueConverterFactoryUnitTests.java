/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.lang.Nullable;

/**
 * Unit tests for {@link PropertyValueConverterFactory}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class PropertyValueConverterFactoryUnitTests {

	@Test // GH-1484
	void simpleConverterFactoryCanInstantiateFactoryWithDefaultCtor() {

		assertThat(PropertyValueConverterFactory.simple().getConverter(ConverterWithDefaultCtor.class))
				.isInstanceOf(ConverterWithDefaultCtor.class);
	}

	@Test // GH-1484
	void simpleConverterFactoryReadsConverterFromAnnotation() {

		PersistentProperty property = mock(PersistentProperty.class);
		ValueConverter annotation = mock(ValueConverter.class);
		when(annotation.value()).thenReturn((Class) ConverterWithDefaultCtor.class);
		when(property.findAnnotation(ValueConverter.class)).thenReturn(annotation);

		assertThat(PropertyValueConverterFactory.simple().getConverter(property))
				.isInstanceOf(ConverterWithDefaultCtor.class);
	}

	@Test // GH-1484
	void simpleConverterFactoryErrorsOnNullType() {

		assertThatIllegalArgumentException()
				.isThrownBy(() -> PropertyValueConverterFactory.simple().getConverter((Class) null));
	}

	@Test // GH-1484
	void simpleConverterFactoryCanExtractFactoryEnumInstance() {

		assertThat(PropertyValueConverterFactory.simple().getConverter(ConverterEnum.class))
				.isInstanceOf(ConverterEnum.class);
	}

	@Test // GH-1484
	void simpleConverterFactoryCannotInstantiateFactoryWithDependency() {

		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> PropertyValueConverterFactory.simple().getConverter(ConverterWithDependency.class));
	}

	@Test // GH-1484
	void beanFactoryAwareConverterFactoryCanInstantiateFactoryWithDefaultCtor() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		assertThat(PropertyValueConverterFactory.beanFactoryAware(beanFactory).getConverter(ConverterWithDefaultCtor.class))
				.isInstanceOf(ConverterWithDefaultCtor.class);
	}

	@Test // GH-1484
	void beanFactoryAwareConverterFactoryCanInstantiateFactoryWithBeanReference() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("someDependency",
				BeanDefinitionBuilder.rootBeanDefinition(SomeDependency.class).getBeanDefinition());

		assertThat(PropertyValueConverterFactory.beanFactoryAware(beanFactory).getConverter(ConverterWithDependency.class))
				.isInstanceOf(ConverterWithDependency.class);
	}

	@Test // GH-1484
	void beanFactoryAwareConverterFactoryCanLookupExistingBean() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("someDependency",
				BeanDefinitionBuilder.rootBeanDefinition(SomeDependency.class).getBeanDefinition());
		beanFactory.registerBeanDefinition("theMightyConverter",
				BeanDefinitionBuilder.rootBeanDefinition(ConverterWithDependency.class)
						.addConstructorArgReference("someDependency").getBeanDefinition());

		assertThat(PropertyValueConverterFactory.beanFactoryAware(beanFactory).getConverter(ConverterWithDependency.class))
				.isSameAs(beanFactory.getBean("theMightyConverter"));
	}

	@Test // GH-1484
	void chainedConverterFactoryIteratesFactories() {

		PropertyValueConverter expected = mock(PropertyValueConverter.class);

		PropertyValueConverterFactory factory = PropertyValueConverterFactory.chained(new PropertyValueConverterFactory() {
			@Nullable
			@Override
			public <S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> getConverter(
					Class<? extends PropertyValueConverter<S, T, C>> converterType) {
				return null;
			}
		}, new PropertyValueConverterFactory() {
			@Nullable
			@Override
			public <S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> getConverter(
					Class<? extends PropertyValueConverter<S, T, C>> converterType) {
				return expected;
			}
		});

		assertThat(factory.getConverter(ConverterWithDefaultCtor.class)).isSameAs(expected);
	}

	@Test // GH-1484
	void chainedConverterFactoryFailsOnException() {

		PropertyValueConverterFactory factory = PropertyValueConverterFactory.chained(new PropertyValueConverterFactory() {
			@Nullable
			@Override
			public <S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> getConverter(
					Class<? extends PropertyValueConverter<S, T, C>> converterType) {
				return null;
			}
		}, new PropertyValueConverterFactory() {
			@Nullable
			@Override
			public <S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> getConverter(
					Class<? extends PropertyValueConverter<S, T, C>> converterType) {
				throw new RuntimeException("can't touch this!");
			}
		});

		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> factory.getConverter(ConverterWithDefaultCtor.class));
	}

	@Test // GH-1484
	void cachingConverterFactoryServesCachedInstance() {

		PropertyValueConverterFactory factory = PropertyValueConverterFactory
				.caching(PropertyValueConverterFactory.simple());
		assertThat(factory.getConverter(ConverterWithDefaultCtor.class))
				.isSameAs(factory.getConverter(ConverterWithDefaultCtor.class));
	}

	@Test // GH-1484
	void cachingConverterFactoryAlsoCachesAbsenceOfConverter() {

		PropertyValueConverterFactory source = Mockito.spy(PropertyValueConverterFactory.simple());
		PropertyValueConverterFactory factory = PropertyValueConverterFactory.caching(source);

		PersistentEntity entity = mock(PersistentEntity.class);
		PersistentProperty property = mock(PersistentProperty.class);
		when(property.getOwner()).thenReturn(entity);
		when(entity.getType()).thenReturn(Person.class);
		when(property.getName()).thenReturn("firstname");

		// fill the cache
		assertThat(factory.getConverter(property)).isNull();
		verify(source).getConverter(any(PersistentProperty.class));

		// now get the cached null value
		assertThat(factory.getConverter(property)).isNull();
		verify(source).getConverter(any(PersistentProperty.class));
	}

	@Test // GH-1484
	void cachingConverterFactoryServesCachedInstanceForProperty() {

		PersistentProperty property = mock(PersistentProperty.class);
		ValueConverter annotation = mock(ValueConverter.class);
		when(annotation.value()).thenReturn((Class) ConverterWithDefaultCtor.class);
		when(property.findAnnotation(ValueConverter.class)).thenReturn(annotation);

		PropertyValueConverterFactory factory = PropertyValueConverterFactory
				.caching(PropertyValueConverterFactory.simple());
		assertThat(factory.getConverter(property)) //
				.isSameAs(factory.getConverter(property)) //
				.isSameAs(factory.getConverter(ConverterWithDefaultCtor.class)); // TODO: is this a valid assumption?
	}

	static class ConverterWithDefaultCtor
			implements PropertyValueConverter<String, UUID, ValueConversionContext<SamplePersistentProperty>> {

		@Nullable
		@Override
		public String read(@Nullable UUID value, ValueConversionContext<SamplePersistentProperty> context) {
			return value.toString();
		}

		@Nullable
		@Override
		public UUID write(@Nullable String value, ValueConversionContext<SamplePersistentProperty> context) {
			return UUID.fromString(value);
		}
	}

	enum ConverterEnum implements PropertyValueConverter<String, UUID, ValueConversionContext<SamplePersistentProperty>> {

		INSTANCE;

		@Nullable
		@Override
		public String read(UUID value, ValueConversionContext context) {
			return value.toString();
		}

		@Nullable
		@Override
		public UUID write(String value, ValueConversionContext context) {
			return UUID.fromString(value);
		}
	}

	static class ConverterWithDependency
			implements PropertyValueConverter<String, UUID, ValueConversionContext<SamplePersistentProperty>> {

		private final SomeDependency someDependency;

		public ConverterWithDependency(@Autowired SomeDependency someDependency) {
			this.someDependency = someDependency;
		}

		@Nullable
		@Override
		public String read(UUID value, ValueConversionContext<SamplePersistentProperty> context) {

			assertThat(someDependency).isNotNull();
			return value.toString();
		}

		@Nullable
		@Override
		public UUID write(String value, ValueConversionContext<SamplePersistentProperty> context) {

			assertThat(someDependency).isNotNull();
			return UUID.fromString(value);
		}
	}

	static class SomeDependency {

	}

	static class Person {
		String name;
		Address address;

		public String getName() {
			return name;
		}

		public Address getAddress() {
			return address;
		}
	}

	static class Address {
		String street;
		ZipCode zipCode;
	}

	static class ZipCode {

	}
}
