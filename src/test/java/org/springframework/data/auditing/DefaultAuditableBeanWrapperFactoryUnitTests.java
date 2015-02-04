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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Calendar;

import org.junit.Test;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper;

/**
 * Unit tests for {@link DefaultAuditableBeanWrapperFactory}.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
public class DefaultAuditableBeanWrapperFactoryUnitTests {

	DefaultAuditableBeanWrapperFactory factory = new DefaultAuditableBeanWrapperFactory();

	@Test
	public void returnsNullForNullSource() {
		assertThat(factory.getBeanWrapperFor(null), is(nullValue()));
	}

	@Test
	public void returnsAuditableInterfaceBeanWrapperForAuditable() {

		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(new AuditedUser());
		assertThat(wrapper, is(instanceOf(AuditableInterfaceBeanWrapper.class)));
	}

	@Test
	public void returnsReflectionAuditingBeanWrapperForNonAuditableButAnnotated() {

		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(new AnnotatedUser());
		assertThat(wrapper, is(instanceOf(ReflectionAuditingBeanWrapper.class)));
	}

	@Test
	public void returnsNullForNonAuditableType() {

		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(new Object());
		assertThat(wrapper, is(nullValue()));
	}

	/**
	 * @see DATACMNS-643
	 */
	@Test
	public void setsJsr310AndThreeTenBpTypes() {

		Jsr310ThreeTenBpAuditedUser user = new Jsr310ThreeTenBpAuditedUser();
		Calendar calendar = Calendar.getInstance();

		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(user);
		wrapper.setCreatedDate(calendar);
		wrapper.setLastModifiedDate(calendar);

		assertThat(user.createdDate, is(notNullValue()));
		assertThat(user.lastModifiedDate, is(notNullValue()));
	}
}
