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

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper;
import org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter;

/**
 * Unit tests for {@link ReflectionAuditingBeanWrapper}.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
public class ReflectionAuditingBeanWrapperUnitTests {

	AnnotationAuditingMetadata metadata;
	AnnotatedUser user;
	AuditableBeanWrapper wrapper;

	LocalDateTime time = LocalDateTime.now();

	@Before
	public void setUp() {

		this.user = new AnnotatedUser();
		this.wrapper = new ReflectionAuditingBeanWrapper(user);
	}

	@Test
	public void setsDateTimeFieldCorrectly() {

		wrapper.setCreatedDate(Optional.of(time));
		assertThat(user.createdDate).isEqualTo(new DateTime(LocalDateTimeToDateConverter.INSTANCE.convert(time)));
	}

	@Test
	public void setsDateFieldCorrectly() {

		wrapper.setLastModifiedDate(Optional.of(time));
		assertThat(user.lastModifiedDate).isEqualTo(LocalDateTimeToDateConverter.INSTANCE.convert(time));
	}

	@Test
	public void setsLongFieldCorrectly() {

		class Sample {

			@CreatedDate Long createdDate;

			@LastModifiedDate long modifiedDate;
		}

		Sample sample = new Sample();
		AuditableBeanWrapper wrapper = new ReflectionAuditingBeanWrapper(sample);

		wrapper.setCreatedDate(Optional.of(time));
		assertThat(sample.createdDate).isEqualTo(time.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());

		wrapper.setLastModifiedDate(Optional.of(time));
		assertThat(sample.modifiedDate).isEqualTo(time.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
	}

	@Test
	public void setsAuditorFieldsCorrectly() {

		Object object = new Object();

		wrapper.setCreatedBy(Optional.of(object));
		assertThat(user.createdBy).isEqualTo(object);

		wrapper.setLastModifiedBy(Optional.of(object));
		assertThat(user.lastModifiedBy).isEqualTo(object);
	}
}
