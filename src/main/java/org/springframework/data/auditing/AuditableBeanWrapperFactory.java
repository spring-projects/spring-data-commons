/*
 * Copyright 2012 the original author or authors.
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
import java.util.Date;

import org.joda.time.DateTime;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Auditable;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;

/**
 * A factory class to {@link AuditableBeanWrapper} instances.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
class AuditableBeanWrapperFactory {

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
	static class AuditableInterfaceBeanWrapper implements AuditableBeanWrapper {

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
		public void setCreatedDate(DateTime value) {
			auditable.setCreatedDate(value);
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
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(org.joda.time.DateTime)
		 */
		public void setLastModifiedDate(DateTime value) {
			auditable.setLastModifiedDate(value);
		}
	}

	/**
	 * An {@link AuditableBeanWrapper} implementation that sets values on the target object using refelction.
	 * 
	 * @author Oliver Gierke
	 */
	static class ReflectionAuditingBeanWrapper implements AuditableBeanWrapper {

		private final ConversionService conversionService;
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

			DefaultConversionService conversionService = new DefaultConversionService();
			conversionService.addConverter(DateTimeToLongConverter.INSTANCE);
			conversionService.addConverter(DateTimeToDateConverter.INSTANCE);

			this.conversionService = conversionService;
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
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedDate(org.joda.time.DateTime)
		 */
		public void setCreatedDate(DateTime value) {
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
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(org.joda.time.DateTime)
		 */
		public void setLastModifiedDate(DateTime value) {
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
		private void setDateField(Field field, DateTime value) {

			if (field == null) {
				return;
			}

			ReflectionUtils.setField(field, target, getDateValueToSet(value, field));
		}

		/**
		 * Returns the {@link DateTime} in a type compatible to the given field.
		 * 
		 * @param value
		 * @param field must not be {@literal null}.
		 * @return
		 */
		private Object getDateValueToSet(DateTime value, Field field) {

			if (value == null) {
				return null;
			}

			Class<?> targetType = field.getType();

			if (DateTime.class.equals(targetType)) {
				return value;
			}

			if (conversionService.canConvert(DateTime.class, targetType)) {
				return conversionService.convert(value, targetType);
			}

			throw new IllegalArgumentException(String.format("Invalid date type for field %s! Supported types are %s.",
					field, AnnotationAuditingMetadata.SUPPORTED_DATE_TYPES));
		}
	}

	static enum DateTimeToLongConverter implements Converter<DateTime, Long> {

		INSTANCE;

		@Override
		public Long convert(DateTime source) {
			return source.getMillis();
		}
	}

	static enum DateTimeToDateConverter implements Converter<DateTime, Date> {

		INSTANCE;

		@Override
		public Date convert(DateTime source) {
			return source.toDate();
		}
	}
}
