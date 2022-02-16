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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.springframework.data.convert.PropertyValueConverter.ValueConversionContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.util.MethodInvocationRecorder;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2022/01
 */
public class WhatWeWant {

	@Test
	void converterConfig() {

		ConverterConfig<SamplePersistentProperty> converterConfig = new ConverterConfig();

		// Arbitrary translations
		converterConfig.registerConverter(Foo.class, Foo::getValue)
				.writing((source, context) -> WhatWeWant.reverse(source)) // store reversed
				.reading((source, context) -> WhatWeWant.reverse(source)); // restore order

		converterConfig.registerConverter(it -> it.findAnnotation(Deprecated.class) != null)
				.writing((source, context) -> WhatWeWant.reverse(source.toString()))
				.reading((source, context) -> {
					return context.read(WhatWeWant.reverse(source));
				});

		// Clean up on read
		converterConfig.registerConverter(Foo.class, Foo::getValue)
				.writingAsIs()
				.reading((source, context) -> source.trim());

		converterConfig.registerConverter(Foo.class, Foo::getAddress)
				.writing((source, context) -> {

					Map<Object, Object> map = new HashMap<>();
					map.put("STREET", source.street.toUpperCase());
					map.put("ZIP", context.write(source.zipCode));

					return map;
				});

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

	static String reverse(String source) {
		return new StringBuilder(source).reverse().toString();
	}

	static class ConverterConfig<P extends PersistentProperty<P>> {

		ConverterConfig registerConverter(Predicate<P> filter,
				PropertyValueConverter<?, ?, ? extends ValueConversionContext<P>> converter) {
			return this;
		}

		ConverterConfig registerConverter(Class type, String property,
				PropertyValueConverter<?, ?, ? extends ValueConversionContext<P>> converter) {
			PropertyPath.from(property, type);
			return this;
		}

		/**
		 * Starts a converter registration by pointing to a property of a domain type.
		 *
		 * @param <T> the domain type
		 * @param <S> the property type
		 * @param type the domain type to obtain the property from
		 * @param property a function to describe the property to be referenced. Usually a method handle to a getter.
		 * @return will never be {@literal null}.
		 */
		<T, S> WritingConverterRegistrationBuilder<T, S, P> registerConverter(
				Class<T> type, Function<T, S> property) {

			String propertyName = MethodInvocationRecorder.forProxyOf(type)
					.record(property)
					.getPropertyPath()
					.orElseThrow(() -> new IllegalArgumentException("Cannot obtain property name!"));

			return new WritingConverterRegistrationBuilder<T, S, P>(type, propertyName, this);
		}

		<T, S> WritingConverterRegistrationBuilder<T, S, P> registerConverter(Predicate<P> predicate) {
			return new WritingConverterRegistrationBuilder<T, S, P>(predicate, this);
		}

		/**
		 * Helper to build up a fluent registration API starting on
		 * {@link ConverterConfig#registerConverter(Class, Function)}.
		 *
		 * @author Oliver Drotbohm
		 */
		static class WritingConverterRegistrationBuilder<T, S, P extends PersistentProperty<P>> {

			private final Consumer<PropertyValueConverter> registration;
			private final ConverterConfig config;

			public WritingConverterRegistrationBuilder(Class<T> type, String property, ConverterConfig config) {

				this.config = config;
				this.registration = (converter) -> config.registerConverter(type, property, converter);
			}

			public WritingConverterRegistrationBuilder(Predicate<P> predicate, ConverterConfig config) {

				this.config = config;
				this.registration = (converter) -> config.registerConverter(predicate, converter);
			}

			<R> ReadingConverterRegistrationBuilder<T, S, S, P> writingAsIs() {
				return writing((source, context) -> source);
			}

			<R> ReadingConverterRegistrationBuilder<T, S, R, P> writing(Function<S, R> writer) {
				return writing((source, context) -> writer.apply(source));
			}

			/**
			 * Describes how to convert the domain property value into the database native property.
			 *
			 * @param <R> the type to be written to the database
			 * @param writer the property conversion to extract a value written to the database
			 * @return will never be {@literal null}.
			 */
			<R> ReadingConverterRegistrationBuilder<T, S, R, P> writing(BiFunction<S, ValueConversionContext<P>, R> writer) {
				return new ReadingConverterRegistrationBuilder<>(this, writer);
			}
		}

		/**
		 * A helper to build a fluent API to register how to read a database value into a domain object property.
		 *
		 * @author Oliver Drotbohm
		 */
		static class ReadingConverterRegistrationBuilder<T, S, R, P extends PersistentProperty<P>> {

			private WritingConverterRegistrationBuilder<T, S, P> origin;
			private BiFunction<S, ValueConversionContext<P>, R> writer;

			public ReadingConverterRegistrationBuilder(WritingConverterRegistrationBuilder<T, S, P> origin,
					BiFunction<S, ValueConversionContext<P>, R> writer) {
				this.origin = origin;
				this.writer = writer;
			}

			ConverterConfig<P> readingAsIs() {
				return reading((source, context) -> (S) source);
			}

			ConverterConfig<P> reading(Function<R, S> reader) {
				return reading((source, context) -> reader.apply(source));
			}

			/**
			 * Describes how to read a database value into a domain object's property value.
			 *
			 * @param reader must not be {@literal null}.
			 * @return
			 */
			ConverterConfig<P> reading(BiFunction<R, ValueConversionContext<P>, S> reader) {

				origin.registration.accept(new FunctionPropertyValueConverter(writer, reader));

				return origin.config;
			}

			/**
			 * A {@link PropertyValueConverter} that delegates conversion to the given {@link BiFunction}s.
			 *
			 * @author Oliver Drotbohm
			 */
			static class FunctionPropertyValueConverter<A, B, P extends PersistentProperty<P>>
					implements PropertyValueConverter<A, B, ValueConversionContext<P>> {

				private final BiFunction<A, ValueConversionContext<P>, B> writer;
				private final BiFunction<B, ValueConversionContext<P>, A> reader;

				public FunctionPropertyValueConverter(BiFunction<A, ValueConversionContext<P>, B> writer,
						BiFunction<B, ValueConversionContext<P>, A> reader) {
					this.writer = writer;
					this.reader = reader;
				}

				@Override
				public B domainToNative(A domainValue, ValueConversionContext<P> context) {
					return writer.apply(domainValue, context);
				}

				@Override
				public A nativeToDomain(B nativeValue, ValueConversionContext<P> context) {
					return reader.apply(nativeValue, context);
				}
			}
		}
	}

	static class Foo {
		String value;
		Address address;

		public String getValue() {
			return value;
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

	interface SpecificValueConversionContext<P extends PersistentProperty<P>> extends ValueConversionContext<P> {

	}

	interface SpecificPropertyValueConverter<S, T>
			extends PropertyValueConverter<S, T, SpecificValueConversionContext<SamplePersistentProperty>> {}
}
