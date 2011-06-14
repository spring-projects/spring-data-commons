/*
 * Copyright 2008-2011 the original author or authors.
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
package org.springframework.data.repository.query;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Unit tests for {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryMethodUnitTest {

	@Mock
	RepositoryMetadata metadata;

	@Test(expected = IllegalStateException.class)
	public void rejectsPagingMethodWithInvalidaReturnType() throws SecurityException, NoSuchMethodException {

		Method method = SampleRepository.class.getMethod("pagingMethodWithInvalidReturnType", Pageable.class);
		new QueryMethod(method, metadata);
	}

	interface SampleRepository {
		String pagingMethodWithInvalidReturnType(Pageable pageable);
	}
}
