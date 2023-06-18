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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link PropertyValueConverterFactories} provides a collection of predefined {@link PropertyValueConverterFactory}
 * implementations. Depending on the applications need {@link PropertyValueConverterFactory factories} can be
 * {@link ChainedPropertyValueConverterFactory chained} and the created {@link PropertyValueConverter converter}
 * {@link CachingPropertyValueConverterFactory cached}.
 *
 * @author Christoph Strobl
 * @since 2.7
 */
final class PropertyValueConverterFactories {

	/**
	 * {@link PropertyValueConverterFactory} implementation that returns the first no null {@link PropertyValueConverter}
	 * by asking given {@link PropertyValueConverterFactory factories} one by one.
	 *
	 * @author Christoph Strobl
	 * @since 2.7
	 */
	static class ChainedPropertyValueConverterFactory implements PropertyValueConverterFactory {

		private final List<PropertyValueConverterFactory> delegates;

		ChainedPropertyValueConverterFactory(List<PropertyValueConverterFactory> delegates) {
			this.delegates = Collections.unmodifiableList(delegates);
		}

		@Nullable
		@Override
		@SuppressWarnings("unchecked")
		public <A, B, C extends ValueConversionContext<?>> PropertyValueConverter<A, B, C> getConverter(
				PersistentProperty<?> property) {

			return delegates.stream().map(it -> (PropertyValueConverter<A, B, C>) it.getConverter(property))
					.filter(Objects::nonNull).findFirst().orElse(null);
		}

		@Override
		public <S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> getConverter(
				Class<? extends PropertyValueConverter<S, T, C>> converterType) {

			return delegates.stream().filter(it -> it.getConverter(converterType) != null).findFirst()
					.map(it -> it.getConverter(converterType)).orElse(null);
		}
	}

	/**
	 * Trivial implementation of {@link PropertyValueConverterFactory} capable of instantiating a
	 * {@link PropertyValueConverter} via default constructor or in case of {@link Enum} returning the first value.
	 *
	 * @author Christoph Strobl
	 * @since 2.7
	 */
	static class SimplePropertyConverterFactory implements PropertyValueConverterFactory {

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public <S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> getConverter(
				Class<? extends PropertyValueConverter<S, T, C>> converterType) {

			Assert.notNull(converterType, "ConverterType must not be null");

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
	 * @since 2.7
	 */
	static class BeanFactoryAwarePropertyValueConverterFactory implements PropertyValueConverterFactory {

		private final BeanFactory beanFactory;

		BeanFactoryAwarePropertyValueConverterFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <DV, SV, C extends ValueConversionContext<?>> PropertyValueConverter<DV, SV, C> getConverter(
				Class<? extends PropertyValueConverter<DV, SV, C>> converterType) {

			Assert.notNull(converterType, "ConverterType must not be null");

			PropertyValueConverter<DV, SV, C> converter = beanFactory.getBeanProvider(converterType).getIfAvailable();

			if (converter == null && beanFactory instanceof AutowireCapableBeanFactory) {
				return (PropertyValueConverter<DV, SV, C>) ((AutowireCapableBeanFactory) beanFactory).createBean(converterType,
						AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
			}

			return converter;
		}
	}

	/**
	 * {@link PropertyValueConverterFactory} implementation that serves {@link PropertyValueConverter} from a given
	 * {@link ValueConverterRegistry registry}.
	 *
	 * @author Christoph Strobl
	 * @since 2.7
	 */
	static class ConfiguredInstanceServingValueConverterFactory implements PropertyValueConverterFactory {

		private final ValueConverterRegistry<?> converterRegistry;

		ConfiguredInstanceServingValueConverterFactory(ValueConverterRegistry<?> converterRegistry) {

			Assert.notNull(converterRegistry, "ConversionsRegistrar must not be null");

			this.converterRegistry = converterRegistry;
		}

		@Nullable
		@Override
		@SuppressWarnings("unchecked")
		public <DV, SV, C extends ValueConversionContext<?>> PropertyValueConverter<DV, SV, C> getConverter(
				PersistentProperty<?> property) {

			return (PropertyValueConverter<DV, SV, C>) converterRegistry.getConverter(property.getOwner().getType(),
					property.getName());
		}

		@Override
		public <S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> getConverter(
				Class<? extends PropertyValueConverter<S, T, C>> converterType) {

			return null;
		}
	}

	/**
	 * {@link PropertyValueConverterFactory} implementation that caches converters provided by an underlying
	 * {@link PropertyValueConverterFactory factory}.
	 *
	 * @author Christoph Strobl
	 * @since 2.7
	 */
	static class CachingPropertyValueConverterFactory implements PropertyValueConverterFactory {

		private final PropertyValueConverterFactory delegate;
		private final Cache cache = new Cache();

		CachingPropertyValueConverterFactory(PropertyValueConverterFactory delegate) {

			Assert.notNull(delegate, "Delegate must not be null");

			this.delegate = delegate;
		}

		@Nullable
		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public <DV, SV, C extends ValueConversionContext<?>> PropertyValueConverter<DV, SV, C> getConverter(
				PersistentProperty<?> property) {

			Optional<PropertyValueConverter<?, ?, ? extends ValueConversionContext<?>>> converter = cache.get(property);

			return converter != null ? (PropertyValueConverter) converter.orElse(null)
					: cache.cache(property, delegate.getConverter(property));
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public <DV, SV, C extends ValueConversionContext<?>> PropertyValueConverter<DV, SV, C> getConverter(
				Class<? extends PropertyValueConverter<DV, SV, C>> converterType) {

			Optional<PropertyValueConverter<?, ?, ? extends ValueConversionContext<?>>> converter = cache.get(converterType);

			return converter != null ? (PropertyValueConverter) converter.orElse(null)
					: cache.cache(converterType, delegate.getConverter(converterType));
		}

		static class Cache {

			Map<PersistentProperty<?>, Optional<PropertyValueConverter<?, ?, ? extends ValueConversionContext<?>>>> perPropertyCache = new ConcurrentHashMap<>();
			Map<Class<?>, Optional<PropertyValueConverter<?, ?, ? extends ValueConversionContext<?>>>> typeCache = new ConcurrentHashMap<>();

			Optional<PropertyValueConverter<?, ?, ? extends ValueConversionContext<?>>> get(PersistentProperty<?> property) {
				return perPropertyCache.get(property);
			}

			Optional<PropertyValueConverter<?, ?, ? extends ValueConversionContext<?>>> get(Class<?> type) {
				return typeCache.get(type);
			}

			<S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> cache(PersistentProperty<?> property,
					@Nullable PropertyValueConverter<S, T, C> converter) {

				perPropertyCache.putIfAbsent(property, Optional.ofNullable(converter));

				AnnotatedPropertyValueConverterAccessor accessor = new AnnotatedPropertyValueConverterAccessor(property);
				Class<? extends PropertyValueConverter<?, ?, ? extends ValueConversionContext<? extends PersistentProperty<?>>>> valueConverterType = accessor
						.getValueConverterType();

				if (valueConverterType != null) {
					cache(valueConverterType, converter);
				}

				return converter;
			}

			<S, T, C extends ValueConversionContext<?>> PropertyValueConverter<S, T, C> cache(Class<?> type,
					@Nullable PropertyValueConverter<S, T, C> converter) {

				typeCache.putIfAbsent(type, Optional.ofNullable(converter));

				return converter;
			}
		}
	}
}
