/*
 * Copyright 2011-2016 the original author or authors.
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

import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.util.Streamable;

/**
 * Unit test for {@link QueryExecuterMethodInterceptor}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryExecutorMethodInterceptorUnitTests {

	@Mock RepositoryFactorySupport factory;
	@Mock RepositoryInformation information;
	@Mock QueryLookupStrategy strategy;

	@Test(expected = IllegalStateException.class)
	public void rejectsRepositoryInterfaceWithQueryMethodsIfNoQueryLookupStrategyIsDefined() throws Exception {

		when(information.hasQueryMethods()).thenReturn(true);
		when(factory.getQueryLookupStrategy(any(), any())).thenReturn(Optional.empty());

		factory.new QueryExecutorMethodInterceptor(information);
	}

	@Test
	public void skipsQueryLookupsIfQueryLookupStrategyIsNotPresent() {

		when(information.getQueryMethods()).thenReturn(Streamable.empty());
		when(factory.getQueryLookupStrategy(any(), any())).thenReturn(Optional.of(strategy));

		factory.new QueryExecutorMethodInterceptor(information);

		verify(strategy, times(0)).resolveQuery(any(), any(), any(), any());
	}
}
