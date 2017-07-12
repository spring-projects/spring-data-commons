/*
 * Copyright 2014-2017 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper;
import org.springframework.data.convert.JodaTimeConverters;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ThreeTenBackPortConverters;
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
		context.getPersistentEntity(Sample.class);
		context.getPersistentEntity(SampleWithInstant.class);

		PersistentEntities entities = new PersistentEntities(Collections.singleton(context));
		factory = new MappingAuditableBeanWrapperFactory(entities);
	}

	@Test // DATACMNS-365
	public void discoversAuditingPropertyOnField() {

		Sample sample = new Sample();

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper).hasValueSatisfying(it -> {

			it.setCreatedBy(Optional.of("Me!"));
			assertThat(sample.createdBy).isNotNull();
		});
	}

	@Test // DATACMNS-365
	public void discoversAuditingPropertyOnAccessor() {

		Sample sample = new Sample();

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper).hasValueSatisfying(it -> {

			it.setLastModifiedBy(Optional.of("Me, too!"));
			assertThat(sample.lastModifiedBy).isNotNull();
		});
	}

	@Test // DATACMNS-365
	public void settingInavailablePropertyIsNoop() {

		Sample sample = new Sample();

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper).hasValueSatisfying(it -> it.setLastModifiedDate(Instant.now()));
	}

	@Test // DATACMNS-365
	public void doesNotReturnWrapperForEntityNotUsingAuditing() {
		assertThat(factory.getBeanWrapperFor(new NoAuditing())).isNotPresent();
	}

	@Test // DATACMNS-365
	public void returnsAuditableWrapperForAuditable() {

		assertThat(factory.getBeanWrapperFor(mock(ExtendingAuditable.class)))
				.hasValueSatisfying(it -> assertThat(it).isInstanceOf(AuditableInterfaceBeanWrapper.class));
	}

	@Test // DATACMNS-638
	public void returnsLastModificationCalendarAsCalendar() {

		Date reference = new Date();

		Calendar calendar = new GregorianCalendar();
		calendar.setTime(reference);

		assertLastModificationDate(calendar, //
				Jsr310Converters.DateToLocalDateTimeConverter.INSTANCE.convert(reference));
	}

	@Test // DATACMNS-638
	public void returnsLastModificationDateTimeAsCalendar() {

		org.joda.time.LocalDateTime reference = new org.joda.time.LocalDateTime();

		assertLastModificationDate(reference,
				JodaTimeConverters.LocalDateTimeToJsr310Converter.INSTANCE.convert(reference));
	}

	@Test // DATACMNS-638
	public void returnsLastModificationDateAsCalendar() {

		Date reference = new Date();

		assertLastModificationDate(reference, //
				Jsr310Converters.DateToLocalDateTimeConverter.INSTANCE.convert(reference));
	}

	@Test // DATACMNS-638, DATACMNS-43
	public void returnsLastModificationJsr310DateTimeAsCalendar() {

		LocalDateTime reference = LocalDateTime.now();

		assertLastModificationDate(reference, reference);
	}

	@Test // DATACMNS-638, DATACMNS-43
	public void returnsLastModificationThreeTenBpDateTimeAsCalendar() {

		org.threeten.bp.LocalDateTime reference = org.threeten.bp.LocalDateTime.now();

		assertLastModificationDate(reference,
				ThreeTenBackPortConverters.LocalDateTimeToJsr310LocalDateTimeConverter.INSTANCE.convert(reference));
	}

	@Test
	public void exposesInstantAsModificationDate() {

		SampleWithInstant sample = new SampleWithInstant();
		sample.modified = Instant.now();

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(sample);
		assertThat(wrapper.flatMap(it -> it.getLastModifiedDate())).hasValue(sample.modified);
	}

	private void assertLastModificationDate(Object source, TemporalAccessor expected) {

		Sample sample = new Sample();
		sample.lastModifiedDate = source;

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper.flatMap(it -> it.getLastModifiedDate())).hasValue(expected);
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

	static class SampleWithInstant {

		@CreatedDate Instant created;
		@LastModifiedDate Instant modified;
	}

	static class NoAuditing {}

	static abstract class ExtendingAuditable implements Auditable<Object, Long, LocalDateTime> {}
}
