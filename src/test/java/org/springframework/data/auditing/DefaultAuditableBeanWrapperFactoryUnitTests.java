/*
 * Copyright 2012-2021 the original author or authors.
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper;

/**
 * Unit tests for {@link DefaultAuditableBeanWrapperFactory}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @since 1.5
 */
class DefaultAuditableBeanWrapperFactoryUnitTests {

	DefaultAuditableBeanWrapperFactory factory = new DefaultAuditableBeanWrapperFactory();

	@Test
	void rejectsNullSource() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.getBeanWrapperFor(null));
	}

	@Test
	void returnsAuditableInterfaceBeanWrapperForAuditable() {

		assertThat(factory.getBeanWrapperFor(new AuditedUser()))
				.hasValueSatisfying(it -> assertThat(it).isInstanceOf(AuditableInterfaceBeanWrapper.class));
	}

	@Test
	void returnsReflectionAuditingBeanWrapperForNonAuditableButAnnotated() {

		assertThat(factory.getBeanWrapperFor(new AnnotatedUser()))
				.hasValueSatisfying(it -> assertThat(it).isInstanceOf(ReflectionAuditingBeanWrapper.class));
	}

	@Test
	void returnsEmptyForNonAuditableType() {
		assertThat(factory.getBeanWrapperFor(new Object())).isNotPresent();
	}

	@Test // DATACMNS-643
	void setsJsr310AndThreeTenBpTypes() {

		Jsr310ThreeTenBpAuditedUser user = new Jsr310ThreeTenBpAuditedUser();
		Instant instant = Instant.now();

		Optional<AuditableBeanWrapper<Jsr310ThreeTenBpAuditedUser>> wrapper = factory.getBeanWrapperFor(user);

		assertThat(wrapper).hasValueSatisfying(it -> {

			it.setCreatedDate(instant);
			it.setLastModifiedDate(instant);

			assertThat(user.createdDate).isNotNull();
			assertThat(user.lastModifiedDate).isNotNull();
		});
	}

	@Test // DATACMNS-867
	void errorsWhenUnableToConvertDateViaIntermediateJavaUtilDateConversion() {

		Jsr310ThreeTenBpAuditedUser user = new Jsr310ThreeTenBpAuditedUser();
		ZonedDateTime zonedDateTime = ZonedDateTime.now();

		Optional<AuditableBeanWrapper<Jsr310ThreeTenBpAuditedUser>> wrapper = factory.getBeanWrapperFor(user);

		assertThat(wrapper).isNotEmpty();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> wrapper.ifPresent(it -> it.setLastModifiedDate(zonedDateTime)));
	}

	@Test // DATACMNS-1259
	void lastModifiedDateAsLongIsAvailableViaWrapper() {

		LongBasedAuditable source = new LongBasedAuditable();
		source.dateModified = 42000L;

		Optional<Long> result = factory.getBeanWrapperFor(source) //
				.flatMap(AuditableBeanWrapper::getLastModifiedDate) //
				.map(ta -> ta.getLong(ChronoField.INSTANT_SECONDS));

		assertThat(result).hasValue(42L);
	}

	@Test // DATACMNS-1259
	void canSetLastModifiedDateAsInstantViaWrapperOnLongField() {

		LongBasedAuditable source = new LongBasedAuditable();

		Optional<AuditableBeanWrapper<LongBasedAuditable>> beanWrapper = factory.getBeanWrapperFor(source);
		assertThat(beanWrapper).isPresent();

		beanWrapper.get().setLastModifiedDate(Instant.ofEpochMilli(42L));

		assertThat(source.dateModified).isEqualTo(42L);
	}

	@Test // DATACMNS-1259
	void canSetLastModifiedDateAsLocalDateTimeViaWrapperOnLongField() {

		LongBasedAuditable source = new LongBasedAuditable();

		Optional<Long> result = factory.getBeanWrapperFor(source).map(it -> {
			it.setLastModifiedDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(42L), ZoneOffset.systemDefault()));
			return it.getBean().dateModified;
		});

		assertThat(result).hasValue(42L);
	}

	@Test // DATACMNS-1259
	void lastModifiedAsLocalDateTimeDateIsAvailableViaWrapperAsLocalDateTime() {

		LocalDateTime now = LocalDateTime.now();

		AuditedUser source = new AuditedUser();
		source.setLastModifiedDate(now);

		Optional<TemporalAccessor> result = factory.getBeanWrapperFor(source) //
				.flatMap(AuditableBeanWrapper::getLastModifiedDate);

		assertThat(result).hasValue(now);
	}

	public static class LongBasedAuditable {

		@CreatedDate public Long dateCreated;

		@LastModifiedDate public Long dateModified;
	}
}
