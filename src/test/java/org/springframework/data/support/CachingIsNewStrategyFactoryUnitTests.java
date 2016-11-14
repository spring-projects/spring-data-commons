/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link CachingIsNewStrategyFactory}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class CachingIsNewStrategyFactoryUnitTests {

	static final IsNewStrategy REFERENCE = PersistableIsNewStrategy.INSTANCE;

	@Mock
	IsNewStrategyFactory delegate;

	CachingIsNewStrategyFactory factory;

	@Before
	public void setUp() {
		factory = new CachingIsNewStrategyFactory(delegate);
	}

	@Test
	public void invokesDelegateForFirstInvocation() {

		when(delegate.getIsNewStrategy(Object.class)).thenReturn(REFERENCE);

		IsNewStrategy strategy = factory.getIsNewStrategy(Object.class);

		assertThat(strategy).isEqualTo(REFERENCE);
		verify(delegate, times(1)).getIsNewStrategy(Object.class);
	}

	@Test
	public void usesCachedValueForSecondInvocation() {

		when(delegate.getIsNewStrategy(Mockito.any(Class.class))).thenReturn(REFERENCE);

		IsNewStrategy strategy = factory.getIsNewStrategy(Object.class);

		assertThat(strategy).isEqualTo(REFERENCE);
		verify(delegate, times(1)).getIsNewStrategy(Object.class);
		verify(delegate, times(0)).getIsNewStrategy(String.class);

		strategy = factory.getIsNewStrategy(Object.class);
		assertThat(strategy).isEqualTo(REFERENCE);
		verify(delegate, times(1)).getIsNewStrategy(Object.class);
		verify(delegate, times(0)).getIsNewStrategy(String.class);

		strategy = factory.getIsNewStrategy(String.class);
		assertThat(strategy).isEqualTo(REFERENCE);
		verify(delegate, times(1)).getIsNewStrategy(Object.class);
		verify(delegate, times(1)).getIsNewStrategy(String.class);
	}
}
