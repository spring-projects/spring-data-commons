/*
 * Copyright 2026-present the original author or authors.
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
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.repository.Repository;

/**
 * Unit tests for {@link DeferredRepositoryInitializationListener}.
 *
 * @author Seongjun Ha
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class DeferredRepositoryInitializationListenerUnitTests {

	@Test // GH-3459
	void triggersInitializationCorrectly() {

		var beanFactory = mock(DefaultListableBeanFactory.class);
		var context = mock(ApplicationContext.class);
		var listener = new DeferredRepositoryInitializationListener(beanFactory);

		listener.onApplicationEvent(new ContextRefreshedEvent(context));

		verify(beanFactory).getBeansOfType(Repository.class);
	}

	@Test // GH-3459
	void triggersInitializationForConfigurableApplicationContext() {

		var beanFactory = mock(DefaultListableBeanFactory.class);
		var context = mock(ConfigurableApplicationContext.class);
		var listener = new DeferredRepositoryInitializationListener(beanFactory);
		when(context.getBeanFactory()).thenReturn(beanFactory);

		listener.onApplicationEvent(new ContextRefreshedEvent(context));

		verify(beanFactory).getBeansOfType(Repository.class);
	}

	@Test // GH-3459
	void ignoresEventsFromChildContext() {

		var beanFactory = mock(ListableBeanFactory.class);
		var listener = new DeferredRepositoryInitializationListener(beanFactory);

		listener.onApplicationEvent(new ContextRefreshedEvent(new GenericApplicationContext()));

		verify(beanFactory, never()).getBeansOfType(Repository.class);
	}


}
