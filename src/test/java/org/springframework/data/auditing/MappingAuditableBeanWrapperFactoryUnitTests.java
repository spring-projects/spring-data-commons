/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper;
import org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter;
import org.springframework.data.domain.Auditable;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit tests for {@link MappingAuditableBeanWrapperFactory}.
 * 
 * @author Oliver Gierke
 * @since 1.8
 */
public class MappingAuditableBeanWrapperFactoryUnitTests {

	DefaultAuditableBeanWrapperFactory factory;

	@Before
	public void setUp() {

		SampleMappingContext context = new SampleMappingContext();
		context.setInitialEntitySet(Collections.singleton(Sample.class));
		context.afterPropertiesSet();

		PersistentEntities entities = new PersistentEntities(Collections.singleton(context));
		factory = new MappingAuditableBeanWrapperFactory(entities);
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void discoversAuditingPropertyOnField() {

		Sample sample = new Sample();
		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper, is(notNullValue()));

		wrapper.setCreatedBy("Me!");
		assertThat(sample.createdBy, is(notNullValue()));
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void discoversAuditingPropertyOnAccessor() {

		Sample sample = new Sample();
		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper, is(notNullValue()));

		wrapper.setLastModifiedBy("Me, too!");
		assertThat(sample.lastModifiedBy, is(notNullValue()));
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void settingInavailablePropertyIsNoop() {

		Sample sample = new Sample();
		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(sample);

		wrapper.setLastModifiedDate(new GregorianCalendar());
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void doesNotReturnWrapperForEntityNotUsingAuditing() {
		assertThat(factory.getBeanWrapperFor(new NoAuditing()), is(nullValue()));
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void returnsAuditableWrapperForAuditable() {

		assertThat(factory.getBeanWrapperFor(mock(ExtendingAuditable.class)),
				is(instanceOf(AuditableInterfaceBeanWrapper.class)));
	}

	/**
	 * @see DATACMNS-638
	 */
	@Test
	public void returnsLastModificationCalendarAsCalendar() {

		Date reference = new Date();

		Calendar calendar = new GregorianCalendar();
		calendar.setTime(reference);

		assertLastModificationDate(calendar, reference);
	}

	/**
	 * @see DATACMNS-638
	 */
	@Test
	public void returnsLastModificationDateTimeAsCalendar() {

		DateTime reference = new DateTime();

		assertLastModificationDate(reference, reference.toDate());
	}

	/**
	 * @see DATACMNS-638
	 */
	@Test
	public void returnsLastModificationDateAsCalendar() {

		Date reference = new Date();

		assertLastModificationDate(reference, reference);
	}

	/**
	 * @see DATACMNS-638, DATACMNS-43
	 */
	@Test
	public void returnsLastModificationJsr310DateTimeAsCalendar() {

		LocalDateTime reference = LocalDateTime.now();

		assertLastModificationDate(reference, LocalDateTimeToDateConverter.INSTANCE.convert(reference));
	}

	/**
	 * @see DATACMNS-638, DATACMNS-43
	 */
	@Test
	public void returnsLastModificationThreeTenBpDateTimeAsCalendar() {

		org.threeten.bp.LocalDateTime reference = org.threeten.bp.LocalDateTime.now();

		assertLastModificationDate(reference,
				org.springframework.data.convert.ThreeTenBackPortConverters.LocalDateTimeToDateConverter.INSTANCE
						.convert(reference));
	}

	private final void assertLastModificationDate(Object source, Date expected) {

		Calendar calendar = new GregorianCalendar();
		calendar.setTime(expected);

		Sample sample = new Sample();
		sample.lastModifiedDate = source;

		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(sample);
		assertThat(wrapper.getLastModifiedDate(), is(calendar));
	}

	static class Sample {

		private @CreatedBy Object createdBy;
		private Object lastModifiedBy;
		private @LastModifiedDate Object lastModifiedDate;

		@LastModifiedBy
		public Object getLastModifiedBy() {
			return lastModifiedBy;
		}
	}

	static class NoAuditing {

	}

	@SuppressWarnings("serial")
	static abstract class ExtendingAuditable implements Auditable<Object, Long> {

	}
}
