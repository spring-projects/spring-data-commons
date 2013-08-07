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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.AuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper;

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

	DateTime time = new DateTime();

	@Before
	public void setUp() {

		this.user = new AnnotatedUser();
		this.wrapper = new ReflectionAuditingBeanWrapper(user);
	}

	@Test
	public void setsDateTimeFieldCorrectly() {

		wrapper.setCreatedDate(time);
		assertThat(user.createdDate, is(time));
	}

	@Test
	public void setsDateFieldCorrectly() {

		wrapper.setLastModifiedDate(time);
		assertThat(user.lastModifiedDate, is(time.toDate()));
	}

	@Test
	public void setsLongFieldCorrectly() {

		class Sample {

			@CreatedDate Long createdDate;

			@LastModifiedDate long modifiedDate;
		}

		Sample sample = new Sample();
		AuditableBeanWrapper wrapper = new ReflectionAuditingBeanWrapper(sample);

		wrapper.setCreatedDate(time);
		assertThat(sample.createdDate, is(time.getMillis()));

		wrapper.setLastModifiedDate(time);
		assertThat(sample.modifiedDate, is(time.getMillis()));
	}

	@Test
	public void setsAuditorFieldsCorrectly() {

		Object object = new Object();

		wrapper.setCreatedBy(object);
		assertThat(user.createdBy, is(object));

		wrapper.setLastModifiedBy(object);
		assertThat(user.lastModifiedBy, is(object));
	}
}
