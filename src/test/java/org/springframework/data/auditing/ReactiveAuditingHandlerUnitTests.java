/*
 * Copyright 2020-2021 the original author or authors.
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

import lombok.Value;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit test for {@link ReactiveAuditingHandler}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
class ReactiveAuditingHandlerUnitTests {

	ReactiveAuditingHandler handler;
	ReactiveAuditorAware<AuditedUser> auditorAware;

	AuditedUser user;

	@BeforeEach
	void setUp() {

		SampleMappingContext sampleMappingContext = new SampleMappingContext();
		sampleMappingContext.getRequiredPersistentEntity(Immutable.class); // initialize to ensure we're using mapping
		// metadata instead of plain reflection

		handler = new ReactiveAuditingHandler(PersistentEntities.of(sampleMappingContext));
		user = new AuditedUser();

		auditorAware = mock(ReactiveAuditorAware.class);
		when(auditorAware.getCurrentAuditor()).thenReturn(Mono.just(user));
	}

	@Test // DATACMNS-1231
	void markCreatedShouldSetDatesIfAuditorNotSet() {

		Immutable immutable = new Immutable(null, null, null, null);

		handler.markCreated(immutable).as(StepVerifier::create).consumeNextWith(actual -> {

			assertThat(actual.getCreatedDate()).isNotNull();
			assertThat(actual.getModifiedDate()).isNotNull();

			assertThat(actual.getCreatedBy()).isNull();
			assertThat(actual.getModifiedBy()).isNull();
		}).verifyComplete();

		assertThat(immutable.getCreatedDate()).isNull();
	}

	@Test // DATACMNS-1231
	void markModifiedSetsModifiedFields() {

		AuditedUser audited = new AuditedUser();
		audited.id = 1L;

		handler.setAuditorAware(auditorAware);
		handler.markModified(audited).as(StepVerifier::create).expectNext(audited).verifyComplete();

		assertThat(audited.getCreatedBy()).isNotPresent();
		assertThat(audited.getCreatedDate()).isNotPresent();

		assertThat(audited.getLastModifiedBy()).isPresent();
		assertThat(audited.getLastModifiedDate()).isPresent();

		verify(auditorAware).getCurrentAuditor();
	}

	@Value
	static class Immutable {

		@CreatedDate Instant createdDate;
		@CreatedBy String createdBy;
		@LastModifiedDate Instant modifiedDate;
		@LastModifiedBy String modifiedBy;
	}
}
