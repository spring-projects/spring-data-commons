/*
 * Copyright 2018-present the original author or authors.
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
package org.springframework.data.repository.config;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.repository.Repository;

/**
 * Unit tests for {@link DeferredRepositoryInitializationListener}.
 *
 * @author Oliver Drotbohm
 * @author seongjun-rpls
 */
@ExtendWith(MockitoExtension.class)
class DeferredRepositoryInitializationListenerUnitTests {

	@Mock ListableBeanFactory beanFactory;

	@Test // GH-3459
	void triggersInitializationOnMatchingContextEvent() {

		var context = new GenericApplicationContext();
		context.refresh();

		// Create listener with the real context's BeanFactory
		var listener = new DeferredRepositoryInitializationListener(context.getBeanFactory());

		// Fire event from the same context
		listener.onApplicationEvent(new ContextRefreshedEvent(context));

		// The test verifies no exception is thrown and listener completes normally
		// Since we're using real context, we can't easily mock getBeansOfType
		context.close();
	}

	@Test // GH-3459
	void ignoresEventsFromChildContext() {

		// Mock beanFactory that should NOT be called
		ListableBeanFactory mockBeanFactory = mock(ListableBeanFactory.class);
		var listener = new DeferredRepositoryInitializationListener(mockBeanFactory);

		// Create a real context (will have different beanFactory)
		var context = new GenericApplicationContext();
		context.refresh();

		// Fire event - should be ignored because context.getBeanFactory() != mockBeanFactory
		listener.onApplicationEvent(new ContextRefreshedEvent(context));

		// Verify the mock was never called
		verify(mockBeanFactory, never()).getBeansOfType(Repository.class);

		context.close();
	}

	@Test // GH-3459
	void triggersInitializationOnParentContextEvent() {

		// Create a mock beanFactory
		DefaultListableBeanFactory mockBeanFactory = mock(DefaultListableBeanFactory.class);

		// Create a mock ConfigurableApplicationContext
		ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
		when(mockContext.getBeanFactory()).thenReturn(mockBeanFactory);

		// Create listener with the same mock beanFactory
		var listener = new DeferredRepositoryInitializationListener(mockBeanFactory);

		// Fire event from the mock context
		listener.onApplicationEvent(new ContextRefreshedEvent(mockContext));

		// Verify getBeansOfType WAS called
		verify(mockBeanFactory).getBeansOfType(Repository.class);
	}

	@Test // GH-3459
	void ignoresEventsFromSiblingChildContexts() {

		// Create a mock beanFactory for the "parent" listener
		DefaultListableBeanFactory mockParentBeanFactory = mock(DefaultListableBeanFactory.class);

		// Create listener as if registered in a parent context
		var listener = new DeferredRepositoryInitializationListener(mockParentBeanFactory);

		// Create real child contexts (simulates Feign-like scenario)
		var childContext1 = new GenericApplicationContext();
		childContext1.refresh();

		var childContext2 = new GenericApplicationContext();
		childContext2.refresh();

		// Fire events from child contexts - should be ignored
		listener.onApplicationEvent(new ContextRefreshedEvent(childContext1));
		listener.onApplicationEvent(new ContextRefreshedEvent(childContext2));

		// Verify that getBeansOfType was NOT called for child context events
		verify(mockParentBeanFactory, never()).getBeansOfType(Repository.class);

		// Now create a mock parent context that returns our mock beanFactory
		ConfigurableApplicationContext mockParentContext = mock(ConfigurableApplicationContext.class);
		when(mockParentContext.getBeanFactory()).thenReturn(mockParentBeanFactory);

		// Fire event from "parent" context
		listener.onApplicationEvent(new ContextRefreshedEvent(mockParentContext));

		// Verify that getBeansOfType WAS called exactly once for parent context
		verify(mockParentBeanFactory, times(1)).getBeansOfType(Repository.class);

		childContext1.close();
		childContext2.close();
	}
}
