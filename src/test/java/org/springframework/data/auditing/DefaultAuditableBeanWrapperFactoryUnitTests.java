/*
 * Copyright 2012-2019 the original author or authors.
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

import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.junit.Test;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper;
import org.springframework.data.domain.Auditable;

/**
 * Unit tests for {@link DefaultAuditableBeanWrapperFactory}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Daniel Shuy
 * @since 1.5
 */
public class DefaultAuditableBeanWrapperFactoryUnitTests {

	DefaultAuditableBeanWrapperFactory factory = new DefaultAuditableBeanWrapperFactory();

	@Test
	public void rejectsNullSource() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.getBeanWrapperFor(null));
	}

	@Test
	public void returnsAuditableInterfaceBeanWrapperForAuditable() {

		assertThat(factory.getBeanWrapperFor(new AuditedUser()))
				.hasValueSatisfying(it -> assertThat(it).isInstanceOf(AuditableInterfaceBeanWrapper.class));
	}

	@Test
	public void returnsReflectionAuditingBeanWrapperForNonAuditableButAnnotated() {

		assertThat(factory.getBeanWrapperFor(new AnnotatedUser()))
				.hasValueSatisfying(it -> assertThat(it).isInstanceOf(ReflectionAuditingBeanWrapper.class));
	}

	@Test
	public void returnsEmptyForNonAuditableType() {
		assertThat(factory.getBeanWrapperFor(new Object())).isNotPresent();
	}

	@Test // DATACMNS-643
	public void setsJsr310AndThreeTenBpTypes() {

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
	public void errorsWhenUnableToConvertDateViaIntermediateJavaUtilDateConversion() {

		Jsr310ThreeTenBpAuditedUser user = new Jsr310ThreeTenBpAuditedUser();
		ZonedDateTime zonedDateTime = ZonedDateTime.now();

		Optional<AuditableBeanWrapper<Jsr310ThreeTenBpAuditedUser>> wrapper = factory.getBeanWrapperFor(user);

		assertThat(wrapper).isNotEmpty();

		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> wrapper.ifPresent(it -> it.setLastModifiedDate(zonedDateTime)));
	}

	@Test // DATACMNS-1259
	public void lastModifiedDateAsLongIsAvailableViaWrapper() {

		LongBasedAuditable source = new LongBasedAuditable();
		source.dateModified = 42000L;

		Optional<Long> result = factory.getBeanWrapperFor(source) //
				.flatMap(AuditableBeanWrapper::getLastModifiedDate) //
				.map(ta -> ta.getLong(ChronoField.INSTANT_SECONDS));

		assertThat(result).hasValue(42L);
	}

	@Test // DATACMNS-1259
	public void canSetLastModifiedDateAsInstantViaWrapperOnLongField() {

		LongBasedAuditable source = new LongBasedAuditable();

		Optional<AuditableBeanWrapper<LongBasedAuditable>> beanWrapper = factory.getBeanWrapperFor(source);
		assertThat(beanWrapper).isPresent();

		beanWrapper.get().setLastModifiedDate(Instant.ofEpochMilli(42L));

		assertThat(source.dateModified).isEqualTo(42L);
	}

	@Test // DATACMNS-1259
	public void canSetLastModifiedDateAsLocalDateTimeViaWrapperOnLongField() {

		LongBasedAuditable source = new LongBasedAuditable();

		Optional<Long> result = factory.getBeanWrapperFor(source).map(it -> {
			it.setLastModifiedDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(42L), ZoneOffset.systemDefault()));
			return it.getBean().dateModified;
		});

		assertThat(result).hasValue(42L);
	}

	@Test // DATACMNS-1259
	public void lastModifiedAsLocalDateTimeDateIsAvailableViaWrapperAsLocalDateTime() {

		LocalDateTime now = LocalDateTime.now();

		AuditedUser source = new AuditedUser();
		source.setLastModifiedDate(now);

		Optional<TemporalAccessor> result = factory.getBeanWrapperFor(source) //
				.flatMap(AuditableBeanWrapper::getLastModifiedDate);

		assertThat(result).hasValue(now);
	}

	@Test // DATACMNS-1641
	public void fieldWithOverwriteBehaviorDefaultInClassExtendingAuditableShouldBeOverwritten() {

		String oldUser = "old";
		Instant oldDate = Instant.now();

		ExtendingAuditableWithOverwriteBehaviorDefault source = new ExtendingAuditableWithOverwriteBehaviorDefault();
		source.setCreatedBy(oldUser);
		source.setCreatedDate(oldDate);
		source.setLastModifiedBy(oldUser);
		source.setLastModifiedDate(oldDate);

		Optional<AuditableBeanWrapper<ExtendingAuditableWithOverwriteBehaviorDefault>> wrapper = factory
				.getBeanWrapperFor(source);

		assertThat(wrapper).hasValueSatisfying(it -> {

			assertThat(it).isInstanceOf(DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper.class);

			String newUser = "new";
			Instant newDate = Instant.now();

			it.setCreatedBy(newUser);
			it.setCreatedDate(newDate);
			it.setLastModifiedBy(newUser);
			it.setLastModifiedDate(newDate);

			assertThat(source.getCreatedBy()).hasValue(newUser);
			assertThat(source.getCreatedDate()).hasValue(newDate);
			assertThat(source.getLastModifiedBy()).hasValue(newUser);
			assertThat(source.getLastModifiedDate()).hasValue(newDate);
		});
	}

	@Test // DATACMNS-1641
	public void fieldWithOverwriteBehaviorOverwriteInClassExtendingAuditableShouldBeOverwritten() {

		String oldUser = "old";
		Instant oldDate = Instant.now();

		ExtendingAuditableWithOverwriteBehaviorOverwrite source = new ExtendingAuditableWithOverwriteBehaviorOverwrite();
		source.setCreatedBy(oldUser);
		source.setCreatedDate(oldDate);
		source.setLastModifiedBy(oldUser);
		source.setLastModifiedDate(oldDate);

		Optional<AuditableBeanWrapper<ExtendingAuditableWithOverwriteBehaviorOverwrite>> wrapper = factory
				.getBeanWrapperFor(source);

		assertThat(wrapper).hasValueSatisfying(it -> {

			assertThat(it).isInstanceOf(DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper.class);

			String newUser = "new";
			Instant newDate = Instant.now();

			it.setCreatedBy(newUser);
			it.setCreatedDate(newDate);
			it.setLastModifiedBy(newUser);
			it.setLastModifiedDate(newDate);

			assertThat(source.getCreatedBy()).hasValue(newUser);
			assertThat(source.getCreatedDate()).hasValue(newDate);
			assertThat(source.getLastModifiedBy()).hasValue(newUser);
			assertThat(source.getLastModifiedDate()).hasValue(newDate);
		});
	}

	@Test // DATACMNS-1641
	public void fieldWithOverwriteBehaviorSkipInClassExtendingAuditableShouldNotBeOverwritten() {

		String oldUser = "old";
		Instant oldDate = Instant.now();

		ExtendingAuditableWithOverwriteBehaviorSkip source = new ExtendingAuditableWithOverwriteBehaviorSkip();
		source.setCreatedBy(oldUser);
		source.setCreatedDate(oldDate);
		source.setLastModifiedBy(oldUser);
		source.setLastModifiedDate(oldDate);

		Optional<AuditableBeanWrapper<ExtendingAuditableWithOverwriteBehaviorSkip>> wrapper = factory
				.getBeanWrapperFor(source);

		assertThat(wrapper).hasValueSatisfying(it -> {

			assertThat(it).isInstanceOf(DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper.class);

			String newUser = "new";
			Instant newDate = Instant.now();

			it.setCreatedBy(newUser);
			it.setCreatedDate(newDate);
			it.setLastModifiedBy(newUser);
			it.setLastModifiedDate(newDate);

			assertThat(source.getCreatedBy()).hasValue(oldUser);
			assertThat(source.getCreatedDate()).hasValue(oldDate);
			assertThat(source.getLastModifiedBy()).hasValue(newUser);
			assertThat(source.getLastModifiedDate()).hasValue(newDate);
		});
	}

	@Test // DATACMNS-1641
	public void fieldWithOverwriteBehaviorDefaultShouldBeOverwritten() {

		String oldUser = "old";
		Instant oldDate = Instant.now();

		AuditableWithOverwriteBehaviorDefault source = new AuditableWithOverwriteBehaviorDefault();
		source.createdBy = oldUser;
		source.createdDate = oldDate;

		Optional<AuditableBeanWrapper<AuditableWithOverwriteBehaviorDefault>> wrapper = factory.getBeanWrapperFor(source);

		assertThat(wrapper).hasValueSatisfying(it -> {

			assertThat(it).isInstanceOf(DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper.class);

			String newUser = "new";
			Instant newDate = Instant.now();

			it.setCreatedBy(newUser);
			it.setCreatedDate(newDate);
			it.setLastModifiedBy(newUser);
			it.setLastModifiedDate(newDate);

			assertThat(source.createdBy).isEqualTo(newUser);
			assertThat(source.createdDate).isEqualTo(newDate);
		});
	}

	@Test // DATACMNS-1641
	public void fieldWithOverwriteBehaviorOverwriteShouldBeOverwritten() {

		String oldUser = "old";
		Instant oldDate = Instant.now();

		AuditableWithOverwriteBehaviorOverwrite source = new AuditableWithOverwriteBehaviorOverwrite();
		source.createdBy = oldUser;
		source.createdDate = oldDate;

		Optional<AuditableBeanWrapper<AuditableWithOverwriteBehaviorOverwrite>> wrapper = factory.getBeanWrapperFor(source);

		assertThat(wrapper).hasValueSatisfying(it -> {

			assertThat(it).isInstanceOf(DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper.class);

			String newUser = "new";
			Instant newDate = Instant.now();

			it.setCreatedBy(newUser);
			it.setCreatedDate(newDate);
			it.setLastModifiedBy(newUser);
			it.setLastModifiedDate(newDate);

			assertThat(source.createdBy).isEqualTo(newUser);
			assertThat(source.createdDate).isEqualTo(newDate);
		});
	}

	@Test // DATACMNS-1641
	public void fieldWithOverwriteBehaviorSkipShouldNotBeOverwritten() {

		String oldUser = "old";
		Instant oldDate = Instant.now();

		AuditableWithOverwriteBehaviorSkip source = new AuditableWithOverwriteBehaviorSkip();
		source.createdBy = oldUser;
		source.createdDate = oldDate;

		Optional<AuditableBeanWrapper<AuditableWithOverwriteBehaviorSkip>> wrapper = factory.getBeanWrapperFor(source);

		assertThat(wrapper).hasValueSatisfying(it -> {

			assertThat(it).isInstanceOf(DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper.class);

			String newUser = "new";
			Instant newDate = Instant.now();

			it.setCreatedBy(newUser);
			it.setCreatedDate(newDate);
			it.setLastModifiedBy(newUser);
			it.setLastModifiedDate(newDate);

			assertThat(source.createdBy).isEqualTo(oldUser);
			assertThat(source.createdDate).isEqualTo(oldDate);
		});
	}

	public static class LongBasedAuditable {

		@CreatedDate public Long dateCreated;

		@LastModifiedDate public Long dateModified;
	}

	@Setter
	public static abstract class ExtendingAuditableBase implements Auditable<String, Long, Instant> {

		protected String createdBy;
		protected Instant createdDate;
		protected String lastModifiedBy;
		protected Instant lastModifiedDate;

		@Override
		public Long getId() {
			return null;
		}

		@Override
		public boolean isNew() {
			return false;
		}
	}

	public static class ExtendingAuditableWithOverwriteBehaviorDefault extends ExtendingAuditableBase {

		@Override
		public Optional<String> getCreatedBy() {
			return Optional.ofNullable(createdBy);
		}

		@Override
		public Optional<Instant> getCreatedDate() {
			return Optional.ofNullable(createdDate);
		}

		@Override
		public Optional<String> getLastModifiedBy() {
			return Optional.ofNullable(lastModifiedBy);
		}

		@Override
		public Optional<Instant> getLastModifiedDate() {
			return Optional.ofNullable(lastModifiedDate);
		}
	}

	public static class ExtendingAuditableWithOverwriteBehaviorOverwrite extends ExtendingAuditableBase {

		@CreatedBy(overwriteBehavior = OverwriteBehavior.OVERWRITE)
		@Override
		public Optional<String> getCreatedBy() {
			return Optional.ofNullable(createdBy);
		}

		@CreatedDate(overwriteBehavior = OverwriteBehavior.OVERWRITE)
		@Override
		public Optional<Instant> getCreatedDate() {
			return Optional.ofNullable(createdDate);
		}

		@Override
		public Optional<String> getLastModifiedBy() {
			return Optional.ofNullable(lastModifiedBy);
		}

		@Override
		public Optional<Instant> getLastModifiedDate() {
			return Optional.ofNullable(lastModifiedDate);
		}
	}

	public static class ExtendingAuditableWithOverwriteBehaviorSkip extends ExtendingAuditableBase {

		@CreatedBy(overwriteBehavior = OverwriteBehavior.SKIP)
		@Override
		public Optional<String> getCreatedBy() {
			return Optional.ofNullable(createdBy);
		}

		@CreatedDate(overwriteBehavior = OverwriteBehavior.SKIP)
		@Override
		public Optional<Instant> getCreatedDate() {
			return Optional.ofNullable(createdDate);
		}

		@Override
		public Optional<String> getLastModifiedBy() {
			return Optional.ofNullable(lastModifiedBy);
		}

		@Override
		public Optional<Instant> getLastModifiedDate() {
			return Optional.ofNullable(lastModifiedDate);
		}
	}

	public static class AuditableWithOverwriteBehaviorDefault {

		@CreatedBy public String createdBy;

		@CreatedDate public Instant createdDate;
	}

	public static class AuditableWithOverwriteBehaviorOverwrite {

		@CreatedBy(overwriteBehavior = OverwriteBehavior.OVERWRITE) //
		public String createdBy;

		@CreatedDate(overwriteBehavior = OverwriteBehavior.OVERWRITE) //
		public Instant createdDate;
	}

	public static class AuditableWithOverwriteBehaviorSkip {

		@CreatedBy(overwriteBehavior = OverwriteBehavior.SKIP) //
		public String createdBy;

		@CreatedDate(overwriteBehavior = OverwriteBehavior.SKIP) //
		public Instant createdDate;
	}
}
