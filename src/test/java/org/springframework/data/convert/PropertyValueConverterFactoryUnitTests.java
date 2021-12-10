/*
 * Copyright 2022 the original author or authors.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.convert.PropertyValueConverter.ValueConversionContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
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
		when(property.hasValueConverter()).thenReturn(true);
		when(property.getValueConverterType()).thenReturn(ConverterWithDefaultCtor.class);

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
	void compositeConverterFactoryIteratesFactories() {

		PropertyValueConverter expected = mock(PropertyValueConverter.class);

		PropertyValueConverterFactory factory = PropertyValueConverterFactory.chained(new PropertyValueConverterFactory() {
			@Nullable
			@Override
			public <S, T, C extends ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
					Class<? extends PropertyValueConverter<S, T, C>> converterType) {
				return null;
			}
		}, new PropertyValueConverterFactory() {
			@Nullable
			@Override
			public <S, T, C extends ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
					Class<? extends PropertyValueConverter<S, T, C>> converterType) {
				return expected;
			}
		});

		assertThat(factory.getConverter(ConverterWithDefaultCtor.class)).isSameAs(expected);
	}

	@Test // GH-1484
	void compositeConverterFactoryFailsOnException() {

		PropertyValueConverterFactory factory = PropertyValueConverterFactory.chained(new PropertyValueConverterFactory() {
			@Nullable
			@Override
			public <S, T, C extends ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
					Class<? extends PropertyValueConverter<S, T, C>> converterType) {
				return null;
			}
		}, new PropertyValueConverterFactory() {
			@Nullable
			@Override
			public <S, T, C extends ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
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
	void cachingConverterFactoryServesCachedInstanceForProperty() {

		PersistentProperty property = mock(PersistentProperty.class);
		when(property.hasValueConverter()).thenReturn(true);
		when(property.getValueConverterType()).thenReturn(ConverterWithDefaultCtor.class);

		PropertyValueConverterFactory factory = PropertyValueConverterFactory
				.caching(PropertyValueConverterFactory.simple());
		assertThat(factory.getConverter(property)) //
				.isSameAs(factory.getConverter(property)) //
				.isSameAs(factory.getConverter(ConverterWithDefaultCtor.class)); // TODO: is this a valid assumption?
	}

	static class ConverterWithDefaultCtor implements PropertyValueConverter<String, UUID, ValueConversionContext> {

		@Nullable
		@Override
		public String nativeToDomain(@Nullable UUID nativeValue, ValueConversionContext context) {
			return nativeValue.toString();
		}

		@Nullable
		@Override
		public UUID domainToNative(@Nullable String domainValue, ValueConversionContext context) {
			return UUID.fromString(domainValue);
		}
	}

	enum ConverterEnum implements PropertyValueConverter<String, UUID, ValueConversionContext> {

		INSTANCE;

		@Nullable
		@Override
		public String nativeToDomain(@Nullable UUID nativeValue, ValueConversionContext context) {
			return nativeValue.toString();
		}

		@Nullable
		@Override
		public UUID domainToNative(@Nullable String domainValue, ValueConversionContext context) {
			return UUID.fromString(domainValue);
		}
	}

	static class ConverterWithDependency implements PropertyValueConverter<String, UUID, ValueConversionContext> {

		private final SomeDependency someDependency;

		public ConverterWithDependency(@Autowired SomeDependency someDependency) {
			this.someDependency = someDependency;
		}

		@Nullable
		@Override
		public String nativeToDomain(@Nullable UUID nativeValue, ValueConversionContext context) {

			assertThat(someDependency).isNotNull();
			return nativeValue.toString();
		}

		@Nullable
		@Override
		public UUID domainToNative(@Nullable String domainValue, ValueConversionContext context) {

			assertThat(someDependency).isNotNull();
			return UUID.fromString(domainValue);
		}
	}

	static class SomeDependency {

	}
}
