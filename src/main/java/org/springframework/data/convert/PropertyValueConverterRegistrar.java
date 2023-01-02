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

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.data.convert.PropertyValueConverter.FunctionPropertyValueConverter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.MethodInvocationRecorder;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Configuration class used to register a {@link PropertyValueConverter} with
 * a {@link SimplePropertyValueConverterRegistry} that can be used in {@link PropertyValueConversions}.
 * <p>
 * It is possible to register type safe converters via {@link #registerConverter(Class, Function)}
 *
 * <pre class="code">
 * registrar.registerConverter(Person.class, Person::getName) //
 * 		.writing(StringConverter::encrypt) //
 * 		.reading(StringConverter::decrypt);
 * </pre>
 *
 * @author Christoph Strobl
 * @author Oliver Drotbohm
 * @since 2.7
 */
public class PropertyValueConverterRegistrar<P extends PersistentProperty<P>> {

	private final SimplePropertyValueConverterRegistry<P> registry = new SimplePropertyValueConverterRegistry<>();

	/**
	 * Starts a converter registration by pointing to a property of a domain type.
	 *
	 * @param <T> the domain type
	 * @param <S> the property type
	 * @param type the domain type to obtain the property from
	 * @param property a {@link Function} to describe the property to be referenced.
	 * Usually a method handle to a getter.
	 * @return will never be {@literal null}.
	 */
	public <T, S> WritingConverterRegistrationBuilder<T, S, P> registerConverter(Class<T> type, Function<T, S> property) {

		String propertyName = MethodInvocationRecorder.forProxyOf(type).record(property).getPropertyPath()
				.orElseThrow(() -> new IllegalArgumentException("Cannot obtain property name"));

		return new WritingConverterRegistrationBuilder<>(type, propertyName, this);
	}

	/**
	 * Starts a converter registration by pointing to a property of a domain type.
	 *
	 * @param <T> the domain type
	 * @param <S> the property type
	 * @param type the domain type to obtain the property from
	 * @param propertyName a {@link Function} to describe the property to be referenced.
	 * Usually a method handle to a getter.
	 * @return will never be {@literal null}.
	 */
	public <T, S> WritingConverterRegistrationBuilder<T, S, P> registerConverter(Class<T> type, String propertyName,
			@SuppressWarnings("unused") Class<S> propertyType) {

		return new WritingConverterRegistrationBuilder<>(type, propertyName, this);
	}

	/**
	 * Register the given {@link PropertyValueConverter converter} for the given type and property identified by
	 * its name.
	 *
	 * @param type the domain type to obtain the property from
	 * @param path the property name.
	 * @param converter the {@link PropertyValueConverter converter} to apply.
	 * @return this.
	 */
	public PropertyValueConverterRegistrar<P> registerConverter(Class<?> type, String path,
			PropertyValueConverter<?, ?, ? extends ValueConversionContext<?>> converter) {

		registry.registerConverter(type, path,
				(PropertyValueConverter<?, ?, ? extends ValueConversionContext<P>>) converter);

		return this;
	}

	/**
	 * Register collected {@link PropertyValueConverter converters} within the given
	 * {@link ValueConverterRegistry registry}.
	 *
	 * @param target {@link ValueConverterRegistry} from which to register {@link PropertyValueConverter converters};
	 * must not be {@literal null}.
	 * @throws IllegalArgumentException if the {@link ValueConverterRegistry} is {@literal null}.
	 * @see ValueConverterRegistry
	 */
	public void registerConvertersIn(@NonNull ValueConverterRegistry<P> target) {

		Assert.notNull(target, "Target registry must not be null");

		registry.getConverterRegistrationMap().forEach((key, value) ->
				target.registerConverter(key.type, key.path, value));
	}

	/**
	 * Obtain the {@link SimplePropertyValueConverterRegistry}.
	 *
	 * @return new instance of {@link SimplePropertyValueConverterRegistry}.
	 */
	@NonNull
	public ValueConverterRegistry<P> buildRegistry() {
		return new SimplePropertyValueConverterRegistry<>(registry);
	}

	/**
	 * Helper class used to build up a fluent registration API starting with writing.
	 *
	 * @author Oliver Drotbohm
	 */
	public static class WritingConverterRegistrationBuilder<T, S, P extends PersistentProperty<P>> {

		private final Consumer<PropertyValueConverter<T, S, ValueConversionContext<P>>> registration;
		private final PropertyValueConverterRegistrar<P> config;

		WritingConverterRegistrationBuilder(Class<T> type, String property,
				@NonNull PropertyValueConverterRegistrar<P> config) {

			this.config = config;
			this.registration = converter -> config.registerConverter(type, property, converter);
		}

		public ReadingConverterRegistrationBuilder<T, S, S, P> writingAsIs() {
			return writing((source, context) -> source);
		}

		public <R> ReadingConverterRegistrationBuilder<T, S, R, P> writing(Function<S, R> writer) {
			return writing((source, context) -> writer.apply(source));
		}

		/**
		 * Describes how to convert the domain property value into the database native property.
		 *
		 * @param <R> the type to be written to the database
		 * @param writer the property conversion to extract a value written to the database
		 * @return will never be {@literal null}.
		 */
		public <R> ReadingConverterRegistrationBuilder<T, S, R, P> writing(
				BiFunction<S, ValueConversionContext<P>, R> writer) {

			return new ReadingConverterRegistrationBuilder<>(this, writer);
		}
	}

	/**
	 * Helper class used to build a fluent API to register how to read a database value into a domain object property.
	 *
	 * @author Oliver Drotbohm
	 */
	public static class ReadingConverterRegistrationBuilder<T, S, R, P extends PersistentProperty<P>> {

		private final WritingConverterRegistrationBuilder<T, S, P> origin;
		private final BiFunction<S, ValueConversionContext<P>, R> writer;

		ReadingConverterRegistrationBuilder(@NonNull WritingConverterRegistrationBuilder<T, S, P> origin,
				@NonNull BiFunction<S, ValueConversionContext<P>, R> writer) {

			this.origin = origin;
			this.writer = writer;
		}

		@SuppressWarnings("unchecked")
		public PropertyValueConverterRegistrar<P> readingAsIs() {
			return reading((source, context) -> (S) source);
		}

		public PropertyValueConverterRegistrar<P> reading(Function<R, S> reader) {
			return reading((source, context) -> reader.apply(source));
		}

		/**
		 * Describes how to read a database value into a domain object's property value.
		 *
		 * @param reader must not be {@literal null}.
		 * @return the confiured {@link PropertyValueConverterRegistrar}.
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public PropertyValueConverterRegistrar<P> reading(BiFunction<R, ValueConversionContext<P>, S> reader) {

			origin.registration.accept(new FunctionPropertyValueConverter(writer, reader));

			return origin.config;
		}
	}
}
