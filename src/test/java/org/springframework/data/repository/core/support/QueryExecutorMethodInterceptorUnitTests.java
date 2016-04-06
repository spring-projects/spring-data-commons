/*
 * Copyright 2011-2015 the original author or authors.
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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

/**
 * Unit test for {@link QueryExecuterMethodInterceptor}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryExecutorMethodInterceptorUnitTests {

	@Mock RepositoryFactorySupport factory;
	@Mock RepositoryInformation information;
	@Mock QueryLookupStrategy strategy;

	@Test(expected = IllegalStateException.class)
	public void rejectsRepositoryInterfaceWithQueryMethodsIfNoQueryLookupStrategyIsDefined() throws Exception {

		when(information.getQueryMethods()).thenReturn(Arrays.asList(Object.class.getMethod("toString")));
		when(factory.getQueryLookupStrategy(any(Key.class))).thenReturn(null);

		factory.new QueryExecutorMethodInterceptor(information);
	}

	@Test
	public void skipsQueryLookupsIfQueryLookupStrategyIsNull() {

		when(information.getQueryMethods()).thenReturn(Collections.<Method>emptySet());
		when(factory.getQueryLookupStrategy(any(Key.class))).thenReturn(strategy);

		factory.new QueryExecutorMethodInterceptor(information);
		verify(strategy, times(0)).resolveQuery(any(Method.class), any(RepositoryMetadata.class),
				any(ProjectionFactory.class), any(NamedQueries.class));
	}
}
