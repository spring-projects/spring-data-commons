/*
 * Copyright 2008-2021 the original author or authors.
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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit test for {@code AuditingHandler}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.5
 */
@SuppressWarnings("unchecked")
class AuditingHandlerUnitTests {

	AuditingHandler handler;
	AuditorAware<AuditedUser> auditorAware;

	AuditedUser user;

	@BeforeEach
	void setUp() {

		handler = getHandler();
		user = new AuditedUser();

		auditorAware = mock(AuditorAware.class);
		when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(user));
	}

	protected AuditingHandler getHandler() {
		return new AuditingHandler(PersistentEntities.of());
	}

	/**
	 * Checks that the advice does not set auditor on the target entity if no {@code AuditorAware} was configured.
	 */
	@Test
	void doesNotSetAuditorIfNotConfigured() {

		handler.markCreated(user);

		assertThat(user.getCreatedDate()).isPresent();
		assertThat(user.getLastModifiedDate()).isPresent();

		assertThat(user.getCreatedBy()).isNotPresent();
		assertThat(user.getLastModifiedBy()).isNotPresent();
	}

	/**
	 * Checks that the advice sets the auditor on the target entity if an {@code AuditorAware} was configured.
	 */
	@Test
	void setsAuditorIfConfigured() {

		handler.setAuditorAware(auditorAware);

		handler.markCreated(user);

		assertThat(user.getCreatedDate()).isPresent();
		assertThat(user.getLastModifiedDate()).isPresent();

		assertThat(user.getCreatedBy()).isPresent();
		assertThat(user.getLastModifiedBy()).isPresent();

		verify(auditorAware).getCurrentAuditor();
	}

	/**
	 * Checks that the advice does not set modification information on creation if the falg is set to {@code false}.
	 */
	@Test
	void honoursModifiedOnCreationFlag() {

		handler.setAuditorAware(auditorAware);
		handler.setModifyOnCreation(false);
		handler.markCreated(user);

		assertThat(user.getCreatedDate()).isPresent();
		assertThat(user.getCreatedBy()).isPresent();

		assertThat(user.getLastModifiedBy()).isNotPresent();
		assertThat(user.getLastModifiedDate()).isNotPresent();

		verify(auditorAware).getCurrentAuditor();
	}

	/**
	 * Tests that the advice only sets modification data if a not-new entity is handled.
	 */
	@Test
	void onlySetsModificationDataOnNotNewEntities() {

		AuditedUser audited = new AuditedUser();
		audited.id = 1L;

		handler.setAuditorAware(auditorAware);
		handler.markModified(audited);

		assertThat(audited.getCreatedBy()).isNotPresent();
		assertThat(audited.getCreatedDate()).isNotPresent();

		assertThat(audited.getLastModifiedBy()).isPresent();
		assertThat(audited.getLastModifiedDate()).isPresent();

		verify(auditorAware).getCurrentAuditor();
	}

	@Test
	void doesNotSetTimeIfConfigured() {

		handler.setDateTimeForNow(false);
		handler.setAuditorAware(auditorAware);
		handler.markCreated(user);

		assertThat(user.getCreatedBy()).isPresent();
		assertThat(user.getCreatedDate()).isNotPresent();

		assertThat(user.getLastModifiedBy()).isPresent();
		assertThat(user.getLastModifiedDate()).isNotPresent();
	}

	@Test // DATAJPA-9
	void usesDateTimeProviderIfConfigured() {

		DateTimeProvider provider = mock(DateTimeProvider.class);
		doReturn(Optional.empty()).when(provider).getNow();

		handler.setDateTimeProvider(provider);
		handler.markCreated(user);

		verify(provider, times(1)).getNow();
	}

	@Test
	void setsAuditingInfoOnEntityUsingInheritance() {

		AuditingHandler handler = new AuditingHandler(PersistentEntities.of(new SampleMappingContext()));
		handler.setModifyOnCreation(false);

		MyDocument result = handler.markCreated(new MyDocument());

		assertThat(result.created).isNotNull();
		assertThat(result.modified).isNull();

		result = handler.markModified(result);

		assertThat(result.created).isNotNull();
		assertThat(result.modified).isNotNull();
	}

	@Test // DATACMNS-1231
	void getAuditorGetsAuditorNoneWhenNoAuditorAwareNotPresent() {
		assertThat(handler.getAuditor()).isEqualTo(Auditor.none());
	}

	@Test // DATACMNS-1231
	void getAuditorGetsAuditorWhenPresent() {

		handler.setAuditorAware(auditorAware);
		assertThat(handler.getAuditor()).isEqualTo(Auditor.of(user));
	}

	@Test // DATACMNS-1231
	void getAuditorShouldReturnNoneIfAuditorAwareDoesNotHoldObject() {

		when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());

		handler.setAuditorAware(auditorAware);
		assertThat(handler.getAuditor()).isEqualTo(Auditor.none());
	}

	static abstract class AbstractModel {

		@CreatedDate Instant created;
		@CreatedBy String creator;
		@LastModifiedDate Instant modified;
		@LastModifiedBy String modifier;
	}

	static class MyModel extends AbstractModel {
		List<MyModel> models;
	}

	static class MyDocument extends MyModel {}
}
