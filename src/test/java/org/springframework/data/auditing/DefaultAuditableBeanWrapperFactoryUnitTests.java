/*
 * Copyright 2012-2017 the original author or authors.
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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.Test;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper;

/**
 * Unit tests for {@link DefaultAuditableBeanWrapperFactory}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.5
 */
public class DefaultAuditableBeanWrapperFactoryUnitTests {

	DefaultAuditableBeanWrapperFactory factory = new DefaultAuditableBeanWrapperFactory();

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullSource() {
		factory.getBeanWrapperFor(null);
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

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(user);

		assertThat(wrapper).hasValueSatisfying(it -> {

			it.setCreatedDate(instant);
			it.setLastModifiedDate(instant);

			assertThat(user.createdDate).isNotNull();
			assertThat(user.lastModifiedDate).isNotNull();
		});
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-867
	public void errorsWhenUnableToConvertDateViaIntermediateJavaUtilDateConversion() {

		Jsr310ThreeTenBpAuditedUser user = new Jsr310ThreeTenBpAuditedUser();
		ZonedDateTime zonedDateTime = ZonedDateTime.now();

		Optional<AuditableBeanWrapper> wrapper = factory.getBeanWrapperFor(user);

		assertThat(wrapper).hasValueSatisfying(it -> it.setLastModifiedDate(zonedDateTime));
	}
}
