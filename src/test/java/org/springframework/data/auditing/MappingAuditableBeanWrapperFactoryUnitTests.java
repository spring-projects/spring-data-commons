/*
 * Copyright 2014 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.auditing.AuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper;
import org.springframework.data.domain.Auditable;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit tests for {@link MappingAuditableBeanWrapperFactory}.
 * 
 * @author Oliver Gierke
 * @since 1.8
 */
public class MappingAuditableBeanWrapperFactoryUnitTests {

	AuditableBeanWrapperFactory factory;

	@Before
	public void setUp() {
		factory = new MappingAuditableBeanWrapperFactory(new SampleMappingContext());
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

	static class Sample {

		@CreatedBy private Object createdBy;
		private Object lastModifiedBy;

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
