/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.auditing;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ThreeTenBackPortConverters;
import org.springframework.data.domain.Auditable;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A factory class to {@link AuditableBeanWrapper} instances.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
class DefaultAuditableBeanWrapperFactory implements AuditableBeanWrapperFactory {

	/**
	 * Returns an {@link AuditableBeanWrapper} if the given object is capable of being equipped with auditing information.
	 * 
	 * @param source the auditing candidate.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public AuditableBeanWrapper getBeanWrapperFor(Object source) {

		if (source == null) {
			return null;
		}

		if (source instanceof Auditable) {
			return new AuditableInterfaceBeanWrapper((Auditable<Object, ?>) source);
		}

		AnnotationAuditingMetadata metadata = AnnotationAuditingMetadata.getMetadata(source.getClass());

		if (metadata.isAuditable()) {
			return new ReflectionAuditingBeanWrapper(source);
		}

		return null;
	}

	/**
	 * An {@link AuditableBeanWrapper} that works with objects implementing
	 * 
	 * @author Oliver Gierke
	 */
	static class AuditableInterfaceBeanWrapper extends DateConvertingAuditableBeanWrapper {

		private final Auditable<Object, ?> auditable;

		public AuditableInterfaceBeanWrapper(Auditable<Object, ?> auditable) {
			this.auditable = auditable;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedBy(java.lang.Object)
		 */
		public void setCreatedBy(Object value) {
			auditable.setCreatedBy(value);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedDate(org.joda.time.DateTime)
		 */
		public void setCreatedDate(Calendar value) {
			auditable.setCreatedDate(new DateTime(value));
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedBy(java.lang.Object)
		 */
		public void setLastModifiedBy(Object value) {
			auditable.setLastModifiedBy(value);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#getLastModifiedDate()
		 */
		@Override
		public Calendar getLastModifiedDate() {
			return getAsCalendar(auditable.getLastModifiedDate());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(org.joda.time.DateTime)
		 */
		public void setLastModifiedDate(Calendar value) {
			auditable.setLastModifiedDate(new DateTime(value));
		}
	}

	/**
	 * Base class for {@link AuditableBeanWrapper} implementations that might need to convert {@link Calendar} values into
	 * compatible types when setting date/time information.
	 * 
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	abstract static class DateConvertingAuditableBeanWrapper implements AuditableBeanWrapper {

		private static final boolean IS_JODA_TIME_PRESENT = ClassUtils.isPresent("org.joda.time.DateTime",
				ReflectionAuditingBeanWrapper.class.getClassLoader());

		private final ConversionService conversionService;

		/**
		 * Creates a new {@link DateConvertingAuditableBeanWrapper}.
		 */
		public DateConvertingAuditableBeanWrapper() {

			DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

			if (IS_JODA_TIME_PRESENT) {
				conversionService.addConverter(CalendarToDateTimeConverter.INSTANCE);
				conversionService.addConverter(CalendarToLocalDateTimeConverter.INSTANCE);
			}

			for (Converter<?, ?> converter : Jsr310Converters.getConvertersToRegister()) {
				conversionService.addConverter(converter);
			}

			for (Converter<?, ?> converter : ThreeTenBackPortConverters.getConvertersToRegister()) {
				conversionService.addConverter(converter);
			}

			this.conversionService = conversionService;
		}

		/**
		 * Returns the {@link Calendar} in a type, compatible to the given field.
		 * 
		 * @param value can be {@literal null}.
		 * @param targetType must not be {@literal null}.
		 * @param source must not be {@literal null}.
		 * @return
		 */
		protected Object getDateValueToSet(Calendar value, Class<?> targetType, Object source) {

			if (value == null) {
				return null;
			}

			if (Calendar.class.equals(targetType)) {
				return value;
			}

			if (conversionService.canConvert(Calendar.class, targetType)) {
				return conversionService.convert(value, targetType);
			}

			if (conversionService.canConvert(Date.class, targetType)) {

				Date date = conversionService.convert(value, Date.class);
				return conversionService.convert(date, targetType);
			}

			throw new IllegalArgumentException(String.format("Invalid date type for member %s! Supported types are %s.",
					source, AnnotationAuditingMetadata.SUPPORTED_DATE_TYPES));
		}

		/**
		 * Returns the given object as {@link Calendar}.
		 * 
		 * @param source can be {@literal null}.
		 * @return
		 */
		protected Calendar getAsCalendar(Object source) {

			if (source == null || source instanceof Calendar) {
				return (Calendar) source;
			}

			// Apply conversion to date if necessary and possible
			source = !(source instanceof Date) && conversionService.canConvert(source.getClass(), Date.class) ? conversionService
					.convert(source, Date.class) : source;

			return conversionService.convert(source, Calendar.class);
		}
	}

	/**
	 * An {@link AuditableBeanWrapper} implementation that sets values on the target object using refelction.
	 * 
	 * @author Oliver Gierke
	 */
	static class ReflectionAuditingBeanWrapper extends DateConvertingAuditableBeanWrapper {

		private final AnnotationAuditingMetadata metadata;
		private final Object target;

		/**
		 * Creates a new {@link ReflectionAuditingBeanWrapper} to set auditing data on the given target object.
		 * 
		 * @param target must not be {@literal null}.
		 */
		public ReflectionAuditingBeanWrapper(Object target) {

			Assert.notNull(target, "Target object must not be null!");

			this.metadata = AnnotationAuditingMetadata.getMetadata(target.getClass());
			this.target = target;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedBy(java.lang.Object)
		 */
		public void setCreatedBy(Object value) {
			setField(metadata.getCreatedByField(), value);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedDate(java.util.Calendar)
		 */
		public void setCreatedDate(Calendar value) {
			setDateField(metadata.getCreatedDateField(), value);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedBy(java.lang.Object)
		 */
		public void setLastModifiedBy(Object value) {
			setField(metadata.getLastModifiedByField(), value);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#getLastModifiedDate()
		 */
		@Override
		public Calendar getLastModifiedDate() {

			return getAsCalendar(org.springframework.util.ReflectionUtils.getField(metadata.getLastModifiedDateField(),
					target));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(java.util.Calendar)
		 */
		public void setLastModifiedDate(Calendar value) {
			setDateField(metadata.getLastModifiedDateField(), value);
		}

		/**
		 * Sets the given field to the given value if the field is not {@literal null}.
		 * 
		 * @param field
		 * @param value
		 */
		private void setField(Field field, Object value) {

			if (field != null) {
				ReflectionUtils.setField(field, target, value);
			}
		}

		/**
		 * Sets the given field to the given value if the field is not {@literal null}.
		 * 
		 * @param field
		 * @param value
		 */
		private void setDateField(Field field, Calendar value) {

			if (field == null) {
				return;
			}

			ReflectionUtils.setField(field, target, getDateValueToSet(value, field.getType(), field));
		}
	}

	private static enum CalendarToDateTimeConverter implements Converter<Calendar, DateTime> {

		INSTANCE;

		@Override
		public DateTime convert(Calendar source) {
			return new DateTime(source);
		}
	}

	private static enum CalendarToLocalDateTimeConverter implements Converter<Calendar, LocalDateTime> {

		INSTANCE;

		@Override
		public LocalDateTime convert(Calendar source) {
			return new LocalDateTime(source);
		}
	}
}
