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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper;
import org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter;

/**
 * Unit tests for {@link ReflectionAuditingBeanWrapper}.
 *
 * @author Oliver Gierke
 * @author Pavel Horal
 * @since 1.5
 */
class ReflectionAuditingBeanWrapperUnitTests {

	ConversionService conversionService;
	AnnotatedUser user;
	AuditableBeanWrapper<?> wrapper;

	LocalDateTime time = LocalDateTime.now();

	@BeforeEach
	void setUp() {
		this.conversionService = new DefaultAuditableBeanWrapperFactory().getConversionService();
		this.user = new AnnotatedUser();
		this.wrapper = new ReflectionAuditingBeanWrapper<>(conversionService, user);
	}

	@Test
	void setsDateTimeFieldCorrectly() {

		wrapper.setCreatedDate(time);
		assertThat(user.createdDate).isEqualTo(new DateTime(LocalDateTimeToDateConverter.INSTANCE.convert(time)));
	}

	@Test
	void setsDateFieldCorrectly() {

		wrapper.setLastModifiedDate(time);
		assertThat(user.lastModifiedDate).isEqualTo(LocalDateTimeToDateConverter.INSTANCE.convert(time));
	}

	@Test
	void setsLongFieldCorrectly() {

		class Sample {

			@CreatedDate Long createdDate;

			@LastModifiedDate long modifiedDate;
		}

		Sample sample = new Sample();
		AuditableBeanWrapper<Sample> wrapper = new ReflectionAuditingBeanWrapper<>(conversionService, sample);

		wrapper.setCreatedDate(time);
		assertThat(sample.createdDate).isEqualTo(time.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());

		wrapper.setLastModifiedDate(time);
		assertThat(sample.modifiedDate).isEqualTo(time.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
	}

	@Test
	void setsAuditorFieldsCorrectly() {

		Object object = new Object();

		wrapper.setCreatedBy(object);
		assertThat(user.createdBy).isEqualTo(object);

		wrapper.setLastModifiedBy(object);
		assertThat(user.lastModifiedBy).isEqualTo(object);
	}
}
