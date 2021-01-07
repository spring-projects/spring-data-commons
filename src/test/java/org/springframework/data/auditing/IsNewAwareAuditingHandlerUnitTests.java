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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit test for {@code AuditingHandler}.
 *
 * @author Oliver Gierke
 * @since 1.5
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IsNewAwareAuditingHandlerUnitTests extends AuditingHandlerUnitTests {

	SampleMappingContext mappingContext;

	@BeforeEach
	void init() {

		this.mappingContext = new SampleMappingContext();
		this.mappingContext.getPersistentEntity(AuditedUser.class);
		this.mappingContext.afterPropertiesSet();
	}

	@Override
	protected IsNewAwareAuditingHandler getHandler() {
		return new IsNewAwareAuditingHandler(PersistentEntities.of(mappingContext));
	}

	@Test
	void delegatesToMarkCreatedForNewEntity() {

		AuditedUser user = new AuditedUser();

		getHandler().markAudited(user);

		assertThat(user.createdDate).isNotNull();
		assertThat(user.modifiedDate).isNotNull();
	}

	@Test
	void delegatesToMarkModifiedForNonNewEntity() {

		AuditedUser user = new AuditedUser();
		user.id = 1L;

		getHandler().markAudited(user);

		assertThat(user.createdDate).isNull();
		assertThat(user.modifiedDate).isNotNull();
	}

	@Test // DATACMNS-365
	void rejectsNullMappingContext() {
		assertThatIllegalArgumentException().isThrownBy(() -> new IsNewAwareAuditingHandler((PersistentEntities) null));
	}

	@Test // DATACMNS-365
	void setsUpHandlerWithMappingContext() {
		new IsNewAwareAuditingHandler(PersistentEntities.of());
	}

	@Test // DATACMNS-638
	void handlingOptionalIsANoOp() {

		IsNewAwareAuditingHandler handler = getHandler();

		handler.markAudited(Optional.empty());
		handler.markCreated(Optional.empty());
		handler.markModified(Optional.empty());
	}

	@Test // DATACMNS-957
	void skipsEntityWithoutIdentifier() {
		getHandler().markAudited(Optional.of(new EntityWithoutId()));
	}

	@Test // DATACMNS-1780
	void singleContextAllowsInFlightMetadataCreationForUnknownPersistentEntities() {

		SampleMappingContext mappingContext = spy(new SampleMappingContext());
		mappingContext.afterPropertiesSet();

		AuditedUser user = new AuditedUser();
		user.id = 1L;

		new IsNewAwareAuditingHandler(PersistentEntities.of(mappingContext)).markAudited(user);
		verify(mappingContext).getPersistentEntity(AuditedUser.class);
	}

	static class Domain {

		@Id Long id;
	}

	static class EntityWithoutId {}
}
