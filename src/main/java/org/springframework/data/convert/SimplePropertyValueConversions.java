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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.convert.PropertyValueConverterFactories.ChainedPropertyValueConverterFactory;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * {@link PropertyValueConversions} implementation that allows to pick a {@link PropertyValueConverterFactory} serving
 * {@link PropertyValueConverter converters}. Activating {@link #setConverterCacheEnabled(boolean) cahing} allows to
 * reuse converters. Providing a {@link SimplePropertyValueConverterRegistry} adds path configured converter instances.
 * <p>
 * Should be {@link #afterPropertiesSet() initialized}. If not, {@link #init()} will be called of fist attempt of
 * {@link PropertyValueConverter converter} retrieval.
 *
 * @author Christoph Strobl
 * @since 2.7
 */
public class SimplePropertyValueConversions implements PropertyValueConversions, InitializingBean {

	private @Nullable PropertyValueConverterFactory converterFactory;
	private @Nullable ValueConverterRegistry<?> valueConverterRegistry;
	private boolean converterCacheEnabled = true;
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * Set the {@link PropertyValueConverterFactory factory} responsible for creating the actual
	 * {@link PropertyValueConverter converter}.
	 *
	 * @param converterFactory must not be {@literal null}.
	 */
	public void setConverterFactory(PropertyValueConverterFactory converterFactory) {
		this.converterFactory = converterFactory;
	}

	@Nullable
	public PropertyValueConverterFactory getConverterFactory() {
		return converterFactory;
	}

	/**
	 * Set the {@link ValueConverterRegistry converter registry} for path configured converters. This is short for adding
	 * a
	 * {@link org.springframework.data.convert.PropertyValueConverterFactories.ConfiguredInstanceServingValueConverterFactory}
	 * at the end of a {@link ChainedPropertyValueConverterFactory}.
	 *
	 * @param valueConverterRegistry must not be {@literal null}.
	 */
	public void setValueConverterRegistry(ValueConverterRegistry<?> valueConverterRegistry) {
		this.valueConverterRegistry = valueConverterRegistry;
	}

	/**
	 * Get the {@link ValueConverterRegistry} used for path configured converters.
	 *
	 * @return can be {@literal null}.
	 */
	@Nullable
	public ValueConverterRegistry<?> getValueConverterRegistry() {
		return valueConverterRegistry;
	}

	/**
	 * Dis-/Enable caching. Enabled by default.
	 *
	 * @param converterCacheEnabled set to {@literal true} to enable caching of {@link PropertyValueConverter converter}
	 *          instances.
	 */
	public void setConverterCacheEnabled(boolean converterCacheEnabled) {
		this.converterCacheEnabled = converterCacheEnabled;
	}

	@Override
	public boolean hasValueConverter(PersistentProperty<?> property) {

		if (!initialized.get()) {
			init();
		}

		return this.converterFactory.getConverter(property) != null;
	}

	@Nullable
	@Override
	public <DV, SV, C extends PersistentProperty<C>, D extends ValueConversionContext<C>> PropertyValueConverter<DV, SV, D> getValueConverter(
			C property) {

		if (!initialized.get()) {
			init();
		}

		return this.converterFactory.getConverter(property);
	}

	/**
	 * May be called just once to initialize the underlying factory with its values.
	 */
	public void init() {

		if (initialized.compareAndSet(false, true)) {

			List<PropertyValueConverterFactory> factoryList = new ArrayList<>(3);

			if (converterFactory != null) {
				factoryList.add(converterFactory);
			} else {
				factoryList.add(PropertyValueConverterFactory.simple());
			}

			if ((valueConverterRegistry != null) && !valueConverterRegistry.isEmpty()) {
				factoryList.add(PropertyValueConverterFactory.configuredInstance(valueConverterRegistry));
			}

			PropertyValueConverterFactory targetFactory = factoryList.size() > 1
					? PropertyValueConverterFactory.chained(factoryList)
					: factoryList.iterator().next();

			this.converterFactory = converterCacheEnabled ? PropertyValueConverterFactory.caching(targetFactory)
					: targetFactory;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		init();
	}
}
