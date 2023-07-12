/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.data.auditing;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.domain.Auditable;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A factory class to {@link AuditableBeanWrapper} instances.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Pavel Horal
 * @author Johannes Englmeier
 * @since 1.5
 */
class DefaultAuditableBeanWrapperFactory implements AuditableBeanWrapperFactory {

	private final ConversionService conversionService;

	public DefaultAuditableBeanWrapperFactory() {

		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

		Jsr310Converters.getConvertersToRegister().forEach(conversionService::addConverter);

		this.conversionService = conversionService;
	}

	ConversionService getConversionService() {
		return conversionService;
	}

	/**
	 * Returns an {@link AuditableBeanWrapper} if the given object is capable of being equipped with auditing information.
	 *
	 * @param source the auditing candidate.
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<AuditableBeanWrapper<T>> getBeanWrapperFor(T source) {

		Assert.notNull(source, "Source must not be null");

		return Optional.of(source).map(it -> {

			if (it instanceof Auditable) {
				return (AuditableBeanWrapper<T>) new AuditableInterfaceBeanWrapper(conversionService,
						(Auditable<Object, ?, TemporalAccessor>) it);
			}

			AnnotationAuditingMetadata metadata = AnnotationAuditingMetadata.getMetadata(it.getClass());

			if (metadata.isAuditable()) {
				return new ReflectionAuditingBeanWrapper<T>(conversionService, it);
			}

			return null;
		});
	}

	/**
	 * An {@link AuditableBeanWrapper} that works with objects implementing
	 *
	 * @author Oliver Gierke
	 */
	static class AuditableInterfaceBeanWrapper
			extends DateConvertingAuditableBeanWrapper<Auditable<Object, ?, TemporalAccessor>> {

		private final Auditable<Object, ?, TemporalAccessor> auditable;
		private final Class<? extends TemporalAccessor> type;

		@SuppressWarnings("unchecked")
		public AuditableInterfaceBeanWrapper(ConversionService conversionService,
				Auditable<Object, ?, TemporalAccessor> auditable) {

			super(conversionService);

			this.auditable = auditable;
			this.type = (Class<? extends TemporalAccessor>) ResolvableType.forClass(Auditable.class, auditable.getClass())
					.getGeneric(2).resolve(TemporalAccessor.class);
		}

		@Override
		public Object setCreatedBy(Object value) {

			auditable.setCreatedBy(value);
			return value;
		}

		@Override
		public TemporalAccessor setCreatedDate(TemporalAccessor value) {

			auditable.setCreatedDate(getAsTemporalAccessor(Optional.of(value), type).orElseThrow(IllegalStateException::new));

			return value;
		}

		@Override
		public Object setLastModifiedBy(Object value) {

			auditable.setLastModifiedBy(value);

			return value;
		}

		@Override
		public Optional<TemporalAccessor> getLastModifiedDate() {
			return getAsTemporalAccessor(auditable.getLastModifiedDate(), TemporalAccessor.class);
		}

		@Override
		public TemporalAccessor setLastModifiedDate(TemporalAccessor value) {

			auditable
					.setLastModifiedDate(getAsTemporalAccessor(Optional.of(value), type).orElseThrow(IllegalStateException::new));

			return value;
		}

		@Override
		public Auditable<Object, ?, TemporalAccessor> getBean() {
			return auditable;
		}
	}

	/**
	 * Base class for {@link AuditableBeanWrapper} implementations that might need to convert {@link TemporalAccessor}
	 * values into compatible types when setting date/time information.
	 *
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	abstract static class DateConvertingAuditableBeanWrapper<T> implements AuditableBeanWrapper<T> {

		private final ConversionService conversionService;

		DateConvertingAuditableBeanWrapper(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		/**
		 * Returns the {@link TemporalAccessor} in a type, compatible to the given field.
		 *
		 * @param value must not be {@literal null}.
		 * @param targetType must not be {@literal null}.
		 * @param source must not be {@literal null}.
		 * @return
		 */
		@Nullable
		protected Object getDateValueToSet(TemporalAccessor value, Class<?> targetType, Object source) {

			if (targetType.isInstance(value)) {
				return value;
			}

			if (conversionService.canConvert(value.getClass(), targetType)) {
				return conversionService.convert(value, targetType);
			}

			if (conversionService.canConvert(Date.class, targetType)) {

				if (!conversionService.canConvert(value.getClass(), Date.class)) {
					throw new IllegalArgumentException(
							String.format("Cannot convert date type for %s; From %s to java.util.Date to %s", source,
									value.getClass(), targetType));
				}

				Date date = conversionService.convert(value, Date.class);
				return conversionService.convert(date, targetType);
			}

			throw rejectUnsupportedType(value.getClass(), targetType);
		}

		/**
		 * Returns the given object as {@link TemporalAccessor}.
		 *
		 * @param source can be {@literal null}.
		 * @param target must not be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		protected <S extends TemporalAccessor> Optional<S> getAsTemporalAccessor(Optional<?> source,
				Class<? extends S> target) {

			return source.map(it -> {

				if (target.isInstance(it)) {
					return (S) it;
				}

				Class<?> typeToConvertTo = Stream.of(target, Instant.class)//
						.filter(target::isAssignableFrom)//
						.filter(type -> conversionService.canConvert(it.getClass(), type))//
						.findFirst() //
						.orElseThrow(() -> rejectUnsupportedType(it.getClass(), target));

				return (S) conversionService.convert(it, typeToConvertTo);
			});
		}
	}

	private static IllegalArgumentException rejectUnsupportedType(Class<?> sourceType, Class<?> targetType) {
		return new IllegalArgumentException(
				String.format("Cannot convert unsupported date type %s to %s; Supported types are %s", sourceType.getName(),
						targetType.getName(), AnnotationAuditingMetadata.SUPPORTED_DATE_TYPES));
	}

	/**
	 * An {@link AuditableBeanWrapper} implementation that sets values on the target object using reflection.
	 *
	 * @author Oliver Gierke
	 */
	static class ReflectionAuditingBeanWrapper<T> extends DateConvertingAuditableBeanWrapper<T> {

		private final AnnotationAuditingMetadata metadata;
		private final T target;

		/**
		 * Creates a new {@link ReflectionAuditingBeanWrapper} to set auditing data on the given target object.
		 *
		 * @param conversionService conversion service for date value type conversions
		 * @param target must not be {@literal null}.
		 */
		public ReflectionAuditingBeanWrapper(ConversionService conversionService, T target) {
			super(conversionService);

			Assert.notNull(target, "Target object must not be null");

			this.metadata = AnnotationAuditingMetadata.getMetadata(target.getClass());
			this.target = target;
		}

		@Override
		public Object setCreatedBy(Object value) {
			return setField(metadata.getCreatedByField(), value);
		}

		@Override
		public TemporalAccessor setCreatedDate(TemporalAccessor value) {
			return setDateField(metadata.getCreatedDateField(), value);
		}

		@Override
		public Object setLastModifiedBy(Object value) {
			return setField(metadata.getLastModifiedByField(), value);
		}

		@Override
		public Optional<TemporalAccessor> getLastModifiedDate() {

			return getAsTemporalAccessor(metadata.getLastModifiedDateField().map(field -> {

				Object value = org.springframework.util.ReflectionUtils.getField(field, target);
				return value instanceof Optional ? ((Optional<?>) value).orElse(null) : value;

			}), TemporalAccessor.class);
		}

		@Override
		public TemporalAccessor setLastModifiedDate(TemporalAccessor value) {
			return setDateField(metadata.getLastModifiedDateField(), value);
		}

		@Override
		public T getBean() {
			return target;
		}

		/**
		 * Sets the given field to the given value if present.
		 *
		 * @param field
		 * @param value
		 */
		private <S> S setField(Optional<Field> field, S value) {

			field.ifPresent(it -> ReflectionUtils.setField(it, target, value));

			return value;
		}

		/**
		 * Sets the given field to the given value if the field is not {@literal null}.
		 *
		 * @param field
		 * @param value
		 */
		private TemporalAccessor setDateField(Optional<Field> field, TemporalAccessor value) {

			field.ifPresent(it -> ReflectionUtils.setField(it, target, getDateValueToSet(value, it.getType(), it)));

			return value;
		}
	}
}
