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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.convert.PropertyValueConverterFactories.CachingPropertyValueConverterFactory;
import org.springframework.data.convert.PropertyValueConverterFactories.ChainedPropertyValueConverterFactory;

/**
 * Unit tests for {@link SimplePropertyValueConversions}.
 *
 * @author Christoph Strobl
 */
class SimplePropertyValueConversionsUnitTests {

	@Test // GH-1484
	void decoratesTargetFactoryWithCacheWhenCachingIsEnabled() {

		SimplePropertyValueConversions conversions = new SimplePropertyValueConversions();
		conversions.setConverterFactory(PropertyValueConverterFactory.beanFactoryAware(mock(BeanFactory.class)));
		conversions.setConverterCacheEnabled(true);

		conversions.init();
		assertThat(conversions.getConverterFactory()).isInstanceOf(CachingPropertyValueConverterFactory.class);
	}

	@Test // GH-1484
	void doesNotDecorateTargetFactoryWithCacheWhenCachingIsDisabled() {

		PropertyValueConverterFactory factory = PropertyValueConverterFactory.beanFactoryAware(mock(BeanFactory.class));

		SimplePropertyValueConversions conversions = new SimplePropertyValueConversions();
		conversions.setConverterFactory(factory);
		conversions.setConverterCacheEnabled(false);

		conversions.init();
		assertThat(conversions.getConverterFactory()).isSameAs(factory);
	}

	@Test // GH-1484
	void chainsFactoriesIfConverterRegistryPresent() {

		ValueConverterRegistry<?> registry = mock(ValueConverterRegistry.class);
		PropertyValueConverterFactory factory = PropertyValueConverterFactory.beanFactoryAware(mock(BeanFactory.class));

		SimplePropertyValueConversions conversions = new SimplePropertyValueConversions();
		conversions.setConverterFactory(factory);
		conversions.setValueConverterRegistry(registry);
		conversions.setConverterCacheEnabled(false);

		conversions.init();
		assertThat(conversions.getConverterFactory()).isInstanceOf(ChainedPropertyValueConverterFactory.class);
	}
}
