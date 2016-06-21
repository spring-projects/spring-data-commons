/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Persistable;

/**
 * Unit tests for {@link PersistableEntityMetadata}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistableEntityInformationUnitTests {

	@SuppressWarnings({ "rawtypes",
			"unchecked" }) static final PersistableEntityInformation metadata = new PersistableEntityInformation(
					Persistable.class);

	@Mock Persistable<Long> persistable;

	@Test
	@SuppressWarnings("unchecked")
	public void usesPersistablesGetId() throws Exception {

		when(persistable.getId()).thenReturn(2L, 1L, 3L);

		assertThat(metadata.getId(persistable)).hasValue(2L);
		assertThat(metadata.getId(persistable)).hasValue(1L);
		assertThat(metadata.getId(persistable)).hasValue(3L);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void usesPersistablesIsNew() throws Exception {

		when(persistable.isNew()).thenReturn(true, false);

		assertThat(metadata.isNew(persistable)).isTrue();
		assertThat(metadata.isNew(persistable)).isFalse();
	}

	@Test
	public void returnsGivenClassAsEntityType() throws Exception {

		PersistableEntityInformation<PersistableEntity, Long> info = new PersistableEntityInformation<PersistableEntity, Long>(
				PersistableEntity.class);

		assertThat(info.getJavaType()).isEqualTo(PersistableEntity.class);
	}

	@SuppressWarnings("serial")
	static class PersistableEntity implements Persistable<Long> {

		public Long getId() {

			return null;
		}

		public boolean isNew() {

			return false;
		}
	}
}
