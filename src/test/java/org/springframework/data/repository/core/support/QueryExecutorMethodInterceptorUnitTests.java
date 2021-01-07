/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryLookupStrategy;

/**
 * Unit test for {@link QueryExecutorMethodInterceptor}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
@ExtendWith(MockitoExtension.class)
class QueryExecutorMethodInterceptorUnitTests {

	@Mock RepositoryInformation information;
	@Mock QueryLookupStrategy strategy;

	@Test // DATACMNS-1508
	void rejectsRepositoryInterfaceWithQueryMethodsIfNoQueryLookupStrategyIsDefined() {

		when(information.hasQueryMethods()).thenReturn(true);

		assertThatIllegalStateException()
				.isThrownBy(() -> new QueryExecutorMethodInterceptor(information, new SpelAwareProxyProjectionFactory(),
						Optional.empty(), PropertiesBasedNamedQueries.EMPTY, Collections.emptyList(), Collections.emptyList()));
	}

	@Test // DATACMNS-1508
	void skipsQueryLookupsIfQueryLookupStrategyIsNotPresent() {

		new QueryExecutorMethodInterceptor(information, new SpelAwareProxyProjectionFactory(), Optional.empty(),
				PropertiesBasedNamedQueries.EMPTY, Collections.emptyList(), Collections.emptyList());

		verify(strategy, times(0)).resolveQuery(any(), any(), any(), any());
	}
}
