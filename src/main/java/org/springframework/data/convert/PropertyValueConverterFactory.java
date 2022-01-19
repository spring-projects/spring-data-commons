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
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.convert.PropertyValueConverterFactories.BeanFactoryAwarePropertyValueConverterFactory;
import org.springframework.data.convert.PropertyValueConverterFactories.CachingPropertyValueConverterFactory;
import org.springframework.data.convert.PropertyValueConverterFactories.CompositePropertyValueConverterFactory;
import org.springframework.data.convert.PropertyValueConverterFactories.ConfiguredInstanceServingValueConverterFactory;
import org.springframework.data.convert.PropertyValueConverterFactories.SimplePropertyConverterFactory;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A factory that provides {@link PropertyValueConverter value converters}.
 * <p>
 * Depending on the applications need {@link PropertyValueConverterFactory factories} can be {@link #chained(List)
 * chained} and the resulting {@link PropertyValueConverter converter} may be
 * {@link #caching(PropertyValueConverterFactory) cached}.
 *
 * @author Christoph Strobl
 * @since ?
 */
public interface PropertyValueConverterFactory {

	/**
	 * Get the {@link PropertyValueConverter} applicable for the given {@link PersistentProperty}.
	 *
	 * @param property must not be {@literal null}.
	 * @param <A> domain specific type
	 * @param <B> store native type
	 * @return can be {@literal null}.
	 */
	@Nullable
	default <A, B, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<A, B, C> getConverter(
			PersistentProperty<?> property) {

		if (!property.hasValueConverter()) {
			return null;
		}
		return getConverter((Class<PropertyValueConverter<A, B, C>>) property.getValueConverterType());
	}

	@Nullable
	<S, T, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<S, T, C> getConverter(
			Class<? extends PropertyValueConverter<S, T, C>> converterType);

	/**
	 * Obtain a simple {@link PropertyValueConverterFactory} capable of instantiating {@link PropertyValueConverter}
	 * implementations via their default {@link java.lang.reflect.Constructor} or in case of an {@link Enum} accessing the
	 * first enum value.
	 *
	 * @return new instance of {@link PropertyValueConverterFactory}.
	 */
	static PropertyValueConverterFactory simple() {
		return new SimplePropertyConverterFactory();
	}

	/**
	 * Obtain a {@link PropertyValueConverterFactory} capable of looking up/creating the {@link PropertyValueConverter}
	 * via the given {@link BeanFactory}.
	 *
	 * @param beanFactory must not be {@literal null}.
	 * @return new instance of {@link PropertyValueConverterFactory}.
	 */
	static PropertyValueConverterFactory beanFactoryAware(BeanFactory beanFactory) {
		return new BeanFactoryAwarePropertyValueConverterFactory(beanFactory);
	}

	/**
	 * Obtain a {@link PropertyValueConverterFactory} capable of looking up the {@link PropertyValueConverter} in the
	 * given {@link PropertyValueConverterRegistrar}.
	 *
	 * @param registrar must not be {@literal null}.
	 * @return new instance of {@link PropertyValueConverterFactory}.
	 */
	static PropertyValueConverterFactory configuredInstance(PropertyValueConverterRegistrar registrar) {
		return new ConfiguredInstanceServingValueConverterFactory(registrar);
	}

	/**
	 * Obtain a {@link PropertyValueConverterFactory} that will try to obtain a {@link PropertyValueConverter} from the
	 * given array of {@link PropertyValueConverterFactory factories} by returning the first non {@literal null} one.
	 *
	 * @param factories must not be {@literal null} nor contain {@literal null} values.
	 * @return new instance of {@link PropertyValueConverterFactory}.
	 */
	static PropertyValueConverterFactory chained(PropertyValueConverterFactory... factories) {
		return chained(Arrays.asList(factories));
	}

	/**
	 * Obtain a {@link PropertyValueConverterFactory} that will try to obtain a {@link PropertyValueConverter} from the
	 * given list of {@link PropertyValueConverterFactory factories} by returning the first non {@literal null} one.
	 *
	 * @param factoryList must not be {@literal null} nor contain {@literal null} values.
	 * @return new instance of {@link PropertyValueConverterFactory}.
	 */
	static PropertyValueConverterFactory chained(List<PropertyValueConverterFactory> factoryList) {

		Assert.noNullElements(factoryList, "FactoryList must not contain null elements.");

		if (factoryList.size() == 1) {
			return factoryList.iterator().next();
		}

		return new CompositePropertyValueConverterFactory(factoryList);
	}

	/**
	 * Obtain a {@link PropertyValueConverterFactory} that will cache {@link PropertyValueConverter} instances per
	 * {@link PersistentProperty}.
	 *
	 * @param factory
	 * @return
	 */
	static PropertyValueConverterFactory caching(PropertyValueConverterFactory factory) {
		return new CachingPropertyValueConverterFactory(factory);
	}
}
