/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.repository.query;

import java.util.Collections;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.repository.util.ReactiveWrappers.ReactiveLibrary;

/**
 * Factory for {@link ExtensionAwareQueryMethodEvaluationContextProvider}. Considers availability of Project Reactor and
 * returns {@link ReactiveQueryMethodEvaluationContextProvider} if Project Reactor is on the class path.
 *
 * @author Mark Paluch
 * @since 2.1
 * @see ExtensionAwareQueryMethodEvaluationContextProvider
 * @see ReactiveQueryMethodEvaluationContextProvider
 */
public class ExtensionAwareQueryMethodEvaluationContextProviderFactory {

	private final static boolean REACTOR_PRESENT = ReactiveWrappers.isAvailable(ReactiveLibrary.PROJECT_REACTOR);

	/**
	 * Create a default {@link ExtensionAwareQueryMethodEvaluationContextProvider}.
	 *
	 * @return a default {@link ExtensionAwareQueryMethodEvaluationContextProvider}.
	 */
	public static ExtensionAwareQueryMethodEvaluationContextProvider create() {
		return REACTOR_PRESENT ? createContextAwareEvaluationContextProvider() : createEvaluationContextProvider();
	}

	/**
	 * Create a default {@link ExtensionAwareQueryMethodEvaluationContextProvider} given {@link ListableBeanFactory}.
	 *
	 * @return a default {@link ExtensionAwareQueryMethodEvaluationContextProvider} given {@link ListableBeanFactory}.
	 */
	public static ExtensionAwareQueryMethodEvaluationContextProvider create(ListableBeanFactory beanFactory) {
		return REACTOR_PRESENT ? createContextAwareEvaluationContextProvider(beanFactory)
				: createEvaluationContextProvider(beanFactory);
	}

	private static ExtensionAwareQueryMethodEvaluationContextProvider createEvaluationContextProvider() {
		return new ExtensionAwareQueryMethodEvaluationContextProvider(Collections.emptyList());
	}

	private static ExtensionAwareQueryMethodEvaluationContextProvider createContextAwareEvaluationContextProvider() {
		return new ReactiveQueryMethodEvaluationContextProvider(Collections.emptyList());
	}

	private static ExtensionAwareQueryMethodEvaluationContextProvider createEvaluationContextProvider(
			ListableBeanFactory beanFactory) {
		return new ExtensionAwareQueryMethodEvaluationContextProvider(beanFactory);
	}

	private static ExtensionAwareQueryMethodEvaluationContextProvider createContextAwareEvaluationContextProvider(
			ListableBeanFactory beanFactory) {
		return new ReactiveQueryMethodEvaluationContextProvider(beanFactory);
	}
}
