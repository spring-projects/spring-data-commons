/*
 * Copyright 2008-2015 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit test for {@code AuditingHandler}.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
@RunWith(MockitoJUnitRunner.class)
public class IsNewAwareAuditingHandlerUnitTests extends AuditingHandlerUnitTests {

	SampleMappingContext mappingContext;

	@Before
	public void init() {
		this.mappingContext = new SampleMappingContext();
	}

	@Override
	protected IsNewAwareAuditingHandler getHandler() {
		return new IsNewAwareAuditingHandler(mock(PersistentEntities.class));
	}

	@Test
	public void delegatesToMarkCreatedForNewEntity() {

		AuditedUser user = new AuditedUser();
		getHandler().markAudited(user);

		assertThat(user.createdDate, is(notNullValue()));
		assertThat(user.modifiedDate, is(notNullValue()));
	}

	@Test
	public void delegatesToMarkModifiedForNonNewEntity() {

		AuditedUser user = new AuditedUser();
		user.id = 1L;
		getHandler().markAudited(user);

		assertThat(user.createdDate, is(nullValue()));
		assertThat(user.modifiedDate, is(notNullValue()));
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMappingContext() {
		new IsNewAwareAuditingHandler((PersistentEntities) null);
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void setsUpHandlerWithMappingContext() {
		new IsNewAwareAuditingHandler(new PersistentEntities(Collections.<MappingContext<?, ?>> emptySet()));
	}

	/**
	 * @see DATACMNS-638
	 */
	@Test
	public void handlingNullIsANoOp() {

		IsNewAwareAuditingHandler handler = getHandler();

		handler.markAudited(null);
		handler.markCreated(null);
		handler.markModified(null);
	}

	@Test // DATACMNS-957
	public void skipsEntityWithoutIdentifier() {
		getHandler().markAudited(new EntityWithoutId());
	}

	static class Domain {

		@Id Long id;
	}

	static class EntityWithoutId {}
}
