/*
 * Copyright 2014-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.AbstractLongAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.domain.Auditable;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit tests for {@link MappingAuditableBeanWrapperFactory}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @since 1.8
 */
class MappingAuditableBeanWrapperFactoryUnitTests {

	DefaultAuditableBeanWrapperFactory factory;

	@BeforeEach
	void setUp() {

		var context = new SampleMappingContext();
		context.getPersistentEntity(Sample.class);
		context.getPersistentEntity(SampleWithInstant.class);
		context.getPersistentEntity(WithEmbedded.class);

		var entities = PersistentEntities.of(context);
		factory = new MappingAuditableBeanWrapperFactory(entities);
	}

	@Test // DATACMNS-365
	void discoversAuditingPropertyOnField() {

		var sample = new Sample();

		var wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper).hasValueSatisfying(it -> {

			it.setCreatedBy(Optional.of("Me!"));
			assertThat(it.getBean().createdBy).isNotNull();
		});
	}

	@Test // DATACMNS-365
	void discoversAuditingPropertyOnAccessor() {

		var sample = new Sample();

		var wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper).hasValueSatisfying(it -> {

			it.setLastModifiedBy(Optional.of("Me, too!"));
			assertThat(it.getBean().lastModifiedBy).isNotNull();
		});
	}

	@Test // DATACMNS-365
	void settingInavailablePropertyIsNoop() {

		var sample = new Sample();

		var wrapper = factory.getBeanWrapperFor(sample);

		assertThat(wrapper).hasValueSatisfying(it -> it.setLastModifiedDate(Instant.now()));
	}

	@Test // DATACMNS-365
	void doesNotReturnWrapperForEntityNotUsingAuditing() {
		assertThat(factory.getBeanWrapperFor(new NoAuditing())).isNotPresent();
	}

	@Test // DATACMNS-365
	void returnsAuditableWrapperForAuditable() {

		assertThat(factory.getBeanWrapperFor(mock(ExtendingAuditable.class)))
				.hasValueSatisfying(it -> assertThat(it).isInstanceOf(AuditableInterfaceBeanWrapper.class));
	}

	@Test // DATACMNS-638
	void returnsLastModificationCalendarAsCalendar() {

		var reference = new Date();

		Calendar calendar = new GregorianCalendar();
		calendar.setTime(reference);

		assertLastModificationDate(calendar, //
				Jsr310Converters.DateToLocalDateTimeConverter.INSTANCE.convert(reference));
	}

	@Test // DATACMNS-638
	void returnsLastModificationDateAsCalendar() {

		var reference = new Date();

		assertLastModificationDate(reference, //
				Jsr310Converters.DateToLocalDateTimeConverter.INSTANCE.convert(reference));
	}

	@Test // DATACMNS-638, DATACMNS-43
	void returnsLastModificationJsr310DateTimeAsCalendar() {

		var reference = LocalDateTime.now();

		assertLastModificationDate(reference, reference);
	}

	@Test // DATACMNS-1109
	void exposesInstantAsModificationDate() {

		var sample = new SampleWithInstant();
		sample.modified = Instant.now();

		var result = factory.getBeanWrapperFor(sample) //
				.flatMap(it -> it.getLastModifiedDate());

		assertThat(result).hasValue(sample.modified);
	}

	@Test // DATACMNS-1259
	void exposesLongAsModificationDate() {

		Long reference = new Date().getTime();

		assertLastModificationDate(reference, Instant.ofEpochMilli(reference));
	}

	@Test // DATACMNS-1274
	void writesNestedAuditingData() {

		var target = new WithEmbedded();
		target.embedded = new Embedded();

		var wrapper = factory.getBeanWrapperFor(target);

		assertThat(wrapper).hasValueSatisfying(it -> {

			var now = Instant.now();
			var user = "user";

			it.setCreatedBy(user);
			it.setLastModifiedBy(user);
			it.setLastModifiedDate(now);
			it.setCreatedDate(now);

			var embedded = it.getBean().embedded;

			assertThat(embedded.created).isEqualTo(now);
			assertThat(embedded.creator).isEqualTo(user);
			assertThat(embedded.modified).isEqualTo(now);
			assertThat(embedded.modifier).isEqualTo(user);

			assertThat(it.getLastModifiedDate()).hasValue(now);
		});
	}

	@Test // DATACMNS-1461, DATACMNS-1671
	void skipsNullIntermediatesWhenSettingProperties() {

		var withEmbedded = new WithEmbedded();

		assertThat(factory.getBeanWrapperFor(withEmbedded)).hasValueSatisfying(it -> {
			assertThatCode(() -> it.setCreatedBy("user")).doesNotThrowAnyException();
			assertThatCode(() -> it.setLastModifiedDate(Instant.now())).doesNotThrowAnyException();
		});
	}

	@Test // DATACMNS-1438
	void skipsCollectionPropertiesWhenSettingProperties() {

		var withEmbedded = new WithEmbedded();
		withEmbedded.embedded = new Embedded();
		withEmbedded.embeddeds = Arrays.asList(new Embedded());
		withEmbedded.embeddedMap = new HashMap<>();
		withEmbedded.embeddedMap.put("key", new Embedded());

		assertThat(factory.getBeanWrapperFor(withEmbedded)).hasValueSatisfying(it -> {

			var user = "user";
			var now = Instant.now();

			it.setCreatedBy(user);
			it.setLastModifiedBy(user);
			it.setLastModifiedDate(now);
			it.setCreatedDate(now);

			var embedded = withEmbedded.embeddeds.iterator().next();

			assertThat(embedded.created).isNull();
			assertThat(embedded.creator).isNull();
			assertThat(embedded.modified).isNull();
			assertThat(embedded.modifier).isNull();

			embedded = withEmbedded.embeddedMap.get("key");

			assertThat(embedded.created).isNull();
			assertThat(embedded.creator).isNull();
			assertThat(embedded.modified).isNull();
			assertThat(embedded.modifier).isNull();
		});
	}

	@Test // GH-2719
	void shouldRejectUnsupportedTemporalConversion() {

		var source = new WithZonedDateTime();
		AuditableBeanWrapper<WithZonedDateTime> wrapper = factory.getBeanWrapperFor(source).get();

		assertThatIllegalArgumentException().isThrownBy(() -> wrapper.setCreatedDate(LocalDateTime.now()))
				.withMessageContaining(
						"Cannot convert unsupported date type java.time.LocalDateTime to java.time.ZonedDateTime");
	}

	@Test // GH-2719
	void shouldPassthruTemporalValue() {

		var source = new WithZonedDateTime();
		var now = ZonedDateTime.now();
		AuditableBeanWrapper<WithZonedDateTime> wrapper = factory.getBeanWrapperFor(source).get();

		wrapper.setCreatedDate(now);

		assertThat(source.created).isEqualTo(now);
	}

	private void assertLastModificationDate(Object source, TemporalAccessor expected) {

		var sample = new Sample();
		sample.lastModifiedDate = source;

		var result = factory.getBeanWrapperFor(sample) //
				.flatMap(it -> it.getLastModifiedDate());

		assertThat(result).hasValueSatisfying(ta -> compareTemporalAccessors(expected, ta));
	}

	private static AbstractLongAssert<?> compareTemporalAccessors(TemporalAccessor expected, TemporalAccessor actual) {

		var actualSeconds = getInstantSeconds(actual);
		var expectedSeconds = getInstantSeconds(expected);

		return assertThat(actualSeconds) //
				.describedAs("Difference is %s", actualSeconds - expectedSeconds) //
				.isEqualTo(expectedSeconds);
	}

	private static long getInstantSeconds(TemporalAccessor actual) {

		return actual instanceof LocalDateTime //
				? getInstantSeconds(((LocalDateTime) actual).atZone(ZoneOffset.systemDefault())) //
				: actual.getLong(ChronoField.INSTANT_SECONDS);
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

	// DATACMNS-1274

	static class Embedded {

		@CreatedDate Instant created;
		@CreatedBy String creator;
		@LastModifiedDate Instant modified;
		@LastModifiedBy String modifier;
	}

	static class WithZonedDateTime {

		@CreatedDate ZonedDateTime created;
	}

	static class WithEmbedded {
		Embedded embedded;
		Collection<Embedded> embeddeds;
		Map<String, Embedded> embeddedMap;
	}
}
