/*
 * Copyright 2022. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.convert;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2022/01
 */
public class WhatWeWant {

	@Test
	void converterConfig() {

		ConverterConfig converterConfig = null;

		converterConfig.registerConverter(Foo.class, "value", new PropertyValueConverter() {

			@Nullable
			@Override
			public Object nativeToDomain(@Nullable Object nativeValue, ValueConversionContext context) {
				return null;
			}

			@Nullable
			@Override
			public Object domainToNative(@Nullable Object domainValue, ValueConversionContext context) {
				return null;
			}
		});

	}

	static class ConverterConfig {

		ConverterConfig registerConverter(Predicate<PersistentProperty<?>> filter, PropertyValueConverter<?,?, ? extends PropertyValueConverter.ValueConversionContext> converter) {
			return this;
		}

		ConverterConfig registerConverter(Class type, String property, PropertyValueConverter<?,?, ? extends PropertyValueConverter.ValueConversionContext> converter) {
			PropertyPath.from(property, type);
			return this;
		}
	}

	static class Foo {
		String value;
	}

	interface SpecificValueConversionContext extends PropertyValueConverter.ValueConversionContext {

	}

	interface SpecificPropertyValueConverter<S,T> extends PropertyValueConverter<S,T,SpecificValueConversionContext> {}
}
