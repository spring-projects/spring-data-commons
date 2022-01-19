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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link PropertyValueConverterFactories} provides a collection of predefined {@link PropertyValueConverterFactory}
 * implementations. Depending on the applications need {@link PropertyValueConverterFactory factories} can be
 * {@link CompositePropertyValueConverterFactory chained} and the created {@link PropertyValueConverter converter}
 * {@link CachingPropertyValueConverterFactory cached}.
 *
 * @author Christoph Strobl
 * @since ?
 */
final class PropertyValueConverterFactories {

	/**
	 * @author Christoph Strobl
	 * @since ?
	 */
	static class CompositePropertyValueConverterFactory implements PropertyValueConverterFactory {

		private List<PropertyValueConverterFactory> delegates;

		CompositePropertyValueConverterFactory(PropertyValueConverterFactory... delegates) {
			this(Arrays.asList(delegates));
		}

		CompositePropertyValueConverterFactory(List<PropertyValueConverterFactory> delegates) {
			this.delegates = delegates;
		}

		@Nullable
		@Override
		public <A, B, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<A, B, C> getConverter(
				PersistentProperty<?> property) {
			return delegates.stream().map(it -> (PropertyValueConverter<A, B, C>) it.getConverter(property))
					.filter(Objects::nonNull).findFirst().orElse(null);
		}

		@Override
		public <S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
				Class<? extends PropertyValueConverter<S, T, C>> converterType) {
			return delegates.stream().filter(it -> it.getConverter(converterType) != null).findFirst()
					.map(it -> it.getConverter(converterType)).orElse(null);
		}
	}

	/**
	 * Trivial implementation of {@link PropertyValueConverter}.
	 *
	 * @author Christoph Strobl
	 * @since ?
	 */
	static class SimplePropertyConverterFactory implements PropertyValueConverterFactory {

		@Override
		public <S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
				Class<? extends PropertyValueConverter<S, T, C>> converterType) {

			Assert.notNull(converterType, "ConverterType must not be null!");

			if (converterType.isEnum()) {
				return (PropertyValueConverter<S, T, C>) EnumSet.allOf((Class) converterType).iterator().next();
			}
			return BeanUtils.instantiateClass(converterType);
		}
	}

	/**
	 * {@link PropertyValueConverterFactory} implementation that leverages the {@link BeanFactory} to create the desired
	 * {@link PropertyValueConverter}. This allows the {@link PropertyValueConverter} to make use of DI.
	 *
	 * @author Christoph Strobl
	 * @since ?
	 */
	static class BeanFactoryAwarePropertyValueConverterFactory implements PropertyValueConverterFactory {

		private final BeanFactory beanFactory;

		public BeanFactoryAwarePropertyValueConverterFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public <S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
				Class<? extends PropertyValueConverter<S, T, C>> converterType) {

			Assert.state(beanFactory != null, "BeanFactory must not be null. Did you forget to set it!");
			Assert.notNull(converterType, "ConverterType must not be null!");

			try {
				return beanFactory.getBean(converterType);
			} catch (NoSuchBeanDefinitionException exception) {

				if (beanFactory instanceof AutowireCapableBeanFactory) {
					return (PropertyValueConverter<S, T, C>) ((AutowireCapableBeanFactory) beanFactory).createBean(converterType,
							AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
				}
			}
			return null;
		}
	}

	/**
	 * @author Christoph Strobl
	 * @since ?
	 */
	static class ConfiguredInstanceServingValueConverterFactory implements PropertyValueConverterFactory {

		private final PropertyValueConverterRegistrar conversionsRegistrar;

		public ConfiguredInstanceServingValueConverterFactory(PropertyValueConverterRegistrar conversionsRegistrar) {

			Assert.notNull(conversionsRegistrar, "ConversionsRegistrar must not be null!");
			this.conversionsRegistrar = conversionsRegistrar;
		}

		@Nullable
		@Override
		public <A, B, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<A, B, C> getConverter(
				PersistentProperty<?> property) {
			return (PropertyValueConverter<A, B, C>) conversionsRegistrar.getConverter(property.getOwner().getType(),
					property.getName());
		}

		@Override
		public <S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
				Class<? extends PropertyValueConverter<S, T, C>> converterType) {
			return null;
		}
	}

	/**
	 * TODO: This would be easier if we'd get rid of {@link PropertyValueConverterFactory#getConverter(Class)}.
	 *
	 * @author Christoph Strobl
	 * @since ?
	 */
	static class CachingPropertyValueConverterFactory implements PropertyValueConverterFactory {

		private final PropertyValueConverterFactory delegate;
		private final Cache cache = new Cache();

		public CachingPropertyValueConverterFactory(PropertyValueConverterFactory delegate) {
			
			Assert.notNull(delegate, "Delegate must not be null!");
			this.delegate = delegate;
		}

		@Nullable
		@Override
		public <S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
				PersistentProperty<?> property) {

			PropertyValueConverter converter = cache.get(property);
			if (converter != null) {
				return converter;
			}
			return cache.cache(property, delegate.getConverter(property));
		}

		@Override
		public <S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
				Class<? extends PropertyValueConverter<S, T, C>> converterType) {

			PropertyValueConverter converter = cache.get(converterType);
			if (converter != null) {
				return converter;
			}
			return cache.cache(converterType, delegate.getConverter(converterType));
		}

		static class Cache {

			Map<PersistentProperty<?>, PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext>> perPropertyCache = new HashMap<>();
			Map<Class<?>, PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext>> typeCache = new HashMap<>();

			PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext> get(PersistentProperty<?> property) {
				return perPropertyCache.get(property);
			}

			PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext> get(Class<?> type) {
				return typeCache.get(type);
			}

			<S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> cache(PersistentProperty<?> property,
					PropertyValueConverter<S, T, C> converter) {
				perPropertyCache.putIfAbsent(property, converter);
				cache(property.getValueConverterType(), converter);
				return converter;
			}

			<S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> cache(Class<?> type,
					PropertyValueConverter<S, T, C> converter) {
				typeCache.putIfAbsent(type, converter);
				return converter;
			}
		}
	}
}
