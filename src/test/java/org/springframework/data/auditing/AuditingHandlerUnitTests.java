/*
 * Copyright 2008-2017 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mapping.context.PersistentEntities;

/**
 * Unit test for {@code AuditingHandler}.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
@SuppressWarnings("unchecked")
public class AuditingHandlerUnitTests {

	AuditingHandler handler;
	AuditorAware<AuditedUser> auditorAware;

	AuditedUser user;

	@Before
	public void setUp() {

		handler = getHandler();
		user = new AuditedUser();

		auditorAware = mock(AuditorAware.class);
		when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(user));
	}

	protected AuditingHandler getHandler() {
		return new AuditingHandler(new PersistentEntities(Collections.emptySet()));
	}

	/**
	 * Checks that the advice does not set auditor on the target entity if no {@code AuditorAware} was configured.
	 */
	@Test
	public void doesNotSetAuditorIfNotConfigured() {

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
	public void setsAuditorIfConfigured() {

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
	public void honoursModifiedOnCreationFlag() {

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
	public void onlySetsModificationDataOnNotNewEntities() {

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
	public void doesNotSetTimeIfConfigured() {

		handler.setDateTimeForNow(false);
		handler.setAuditorAware(auditorAware);
		handler.markCreated(user);

		assertThat(user.getCreatedBy()).isPresent();
		assertThat(user.getCreatedDate()).isNotPresent();

		assertThat(user.getLastModifiedBy()).isPresent();
		assertThat(user.getLastModifiedDate()).isNotPresent();
	}

	@Test // DATAJPA-9
	public void usesDateTimeProviderIfConfigured() {

		DateTimeProvider provider = mock(DateTimeProvider.class);
		doReturn(Optional.empty()).when(provider).getNow();

		handler.setDateTimeProvider(provider);
		handler.markCreated(user);

		verify(provider, times(1)).getNow();
	}
}
