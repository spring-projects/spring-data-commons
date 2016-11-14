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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
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
 * Unit tests for {@link MappingAuditableBeanWrapperFactory}. TODO: Which date types to support?
 * 
 * @author Oliver Gierke
 * @since 1.8
 */
@Ignore
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

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(Optional.of(sample));

		assertThat(wrapper).hasValueSatisfying(it -> {

			it.setCreatedBy(Optional.of("Me!"));
			assertThat(sample.createdBy).isNotNull();
		});
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void discoversAuditingPropertyOnAccessor() {

		Sample sample = new Sample();

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(Optional.of(sample));

		assertThat(wrapper).hasValueSatisfying(it -> {

			it.setLastModifiedBy(Optional.of("Me, too!"));
			assertThat(sample.lastModifiedBy).isNotNull();
		});
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void settingInavailablePropertyIsNoop() {

		Sample sample = new Sample();

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(Optional.of(sample));

		assertThat(wrapper).hasValueSatisfying(it -> {
			it.setLastModifiedDate(Optional.of(Instant.now()));
		});
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void doesNotReturnWrapperForEntityNotUsingAuditing() {
		assertThat(factory.getBeanWrapperFor(Optional.of(new NoAuditing()))).isNotPresent();
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void returnsAuditableWrapperForAuditable() {

		assertThat(factory.getBeanWrapperFor(Optional.of(mock(ExtendingAuditable.class)))).hasValueSatisfying(it -> {
			assertThat(it).isInstanceOf(AuditableInterfaceBeanWrapper.class);
		});
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

	private void assertLastModificationDate(Object source, Object expected) {

		Sample sample = new Sample();
		sample.lastModifiedDate = source;

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(Optional.of(sample));

		assertThat(wrapper).hasValueSatisfying(it -> {
			assertThat(it.getLastModifiedDate()).isEqualTo(expected);
		});
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
	static abstract class ExtendingAuditable implements Auditable<Object, Long, LocalDateTime> {

	}
}
