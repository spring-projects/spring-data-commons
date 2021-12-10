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
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since ?
 */
public class SimplePropertyValueConversions implements PropertyValueConversions, InitializingBean {

	private @Nullable PropertyValueConverterFactory converterFactory;
	private @Nullable PropertyValueConverterRegistrar converterRegistrar;
	private boolean converterCacheEnabled = true;
	private AtomicBoolean initialized = new AtomicBoolean(false);

	public void setConverterFactory(PropertyValueConverterFactory converterFactory) {
		this.converterFactory = converterFactory;
	}

	public void setConverterRegistrar(PropertyValueConverterRegistrar converterRegistrar) {
		this.converterRegistrar = converterRegistrar;
	}

	public void setConverterCacheEnabled(boolean converterCacheEnabled) {
		this.converterCacheEnabled = converterCacheEnabled;
	}

	@Override
	public <A, B, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<A, B, C> getValueConverter(
			PersistentProperty<?> property) {

		if (!initialized.get()) {
			init();
		}

		return this.converterFactory.getConverter(property);
	}

	public void init() {

		if (initialized.compareAndSet(false, true)) {
			List<PropertyValueConverterFactory> factoryList = new ArrayList<>(3);

			if (converterFactory != null) {
				factoryList.add(converterFactory);
			} else {
				factoryList.add(PropertyValueConverterFactory.simple());
			}

			if (converterRegistrar != null && !converterRegistrar.isEmpty()) {
				factoryList.add(PropertyValueConverterFactory.configuredInstance(converterRegistrar));
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
