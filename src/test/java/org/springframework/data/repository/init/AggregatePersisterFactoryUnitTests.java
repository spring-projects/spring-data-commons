/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.repository.init;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.init.ResourceReaderRepositoryPopulator.AggregatePersisterFactory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.support.Repositories;

/**
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
class AggregatePersisterFactoryUnitTests {

	@Mock Repositories repositories;
	@Mock RepositoryInformation repoInfo;
	AggregatePersisterFactory factory;

	@BeforeEach
	void beforeEach() {
		factory = new AggregatePersisterFactory(repositories);
	}

	@Test // GH-2558
	void errorsOnNoRepoFoundForType() {

		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> factory.getPersisterFor(Object.class))
				.withMessageContaining("No repository found");
	}

	@Test // GH-2558
	void usesCrudRepoPersisterForNonReactiveCrudRepo() {

		CrudRepository<?, ?> crudRepository = mock(CrudRepository.class);

		when(repositories.getRequiredRepositoryInformation(any())).thenReturn(repoInfo);
		when(repoInfo.isReactiveRepository()).thenReturn(false);
		when(repositories.getRepositoryFor(Mockito.any())).thenReturn(Optional.of(crudRepository));

		assertThat(factory.getPersisterFor(Object.class))
				.satisfies(it -> it.getClass().getName().contains("CrudRepositoryPersister"));
	}

	@Test // GH-2558
	void usesReactiveCrudRepoPersisterForReactiveCrudRepo() {

		ReactiveCrudRepository<?, ?> crudRepository = mock(ReactiveCrudRepository.class);

		when(repositories.getRequiredRepositoryInformation(any())).thenReturn(repoInfo);
		when(repoInfo.isReactiveRepository()).thenReturn(true);
		when(repositories.getRepositoryFor(Mockito.any())).thenReturn(Optional.of(crudRepository));

		assertThat(factory.getPersisterFor(Object.class))
				.satisfies(it -> it.getClass().getName().contains("ReactiveCrudRepositoryPersister"));
	}

	@Test // GH-2558
	void usesReflectiveRepoPersisterForNonReactiveNonCrudRepo() {

		Repository<?, ?> repository = mock(Repository.class);

		when(repositories.getRequiredRepositoryInformation(any())).thenReturn(repoInfo);
		when(repoInfo.isReactiveRepository()).thenReturn(false);
		when(repositories.getRepositoryFor(Mockito.any())).thenReturn(Optional.of(repository));

		assertThat(factory.getPersisterFor(Object.class))
				.satisfies(it -> it.getClass().getName().contains("ReflectivePersister"));
	}

	@Test // GH-2558
	void usesReactiveReflectiveRepoPersisterForReactiveNonCrudRepo() {

		Repository<?, ?> repository = mock(Repository.class);

		when(repositories.getRequiredRepositoryInformation(any())).thenReturn(repoInfo);
		when(repoInfo.isReactiveRepository()).thenReturn(true);
		when(repositories.getRepositoryFor(Mockito.any())).thenReturn(Optional.of(repository));

		assertThat(factory.getPersisterFor(Object.class))
				.satisfies(it -> it.getClass().getName().contains("ReflectiveReactivePersister"));
	}
}
