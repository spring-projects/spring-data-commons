/*
 * Copyright 2018 the original author or authors.
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

import reactor.util.context.Context;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.spel.ContextAwareEvaluationContextProvider;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.SubscriberContextAwareExtension;
import org.springframework.data.support.SubscriberContextAware;
import org.springframework.data.util.Lazy;

/**
 * {@link ExtensionAwareEvaluationContextProvider} implementation that is notified with a Reactor subscriber
 * {@link Context} upon execution. It propagates the context to context-aware {@link SubscriberContextAwareExtension
 * extensions}.
 *
 * @author Mark Paluch
 * @since 2.1
 */
public class ContextAwareQueryMethodEvaluationContextProvider extends ExtensionAwareQueryMethodEvaluationContextProvider
		implements SubscriberContextAware<ContextAwareQueryMethodEvaluationContextProvider> {

	/**
	 * Creates a new {@link ContextAwareQueryMethodEvaluationContextProvider}.
	 *
	 * @param beanFactory the {@link ListableBeanFactory} to lookup the {@link EvaluationContextExtension}s from, must not
	 *          be {@literal null}.
	 */
	public ContextAwareQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * Creates a new {@link ContextAwareQueryMethodEvaluationContextProvider} for the given
	 * {@link EvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ContextAwareQueryMethodEvaluationContextProvider(List<? extends EvaluationContextExtension> extensions) {
		super(extensions);
	}

	private ContextAwareQueryMethodEvaluationContextProvider(
			Lazy<? extends ExtensionAwareEvaluationContextProvider> delegate) {
		super(delegate);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider#createEvaluationContextProvider(java.util.function.Supplier)
	 */
	@Override
	ExtensionAwareEvaluationContextProvider createEvaluationContextProvider(
			Supplier<List<? extends EvaluationContextExtension>> extensions) {
		return new ContextAwareEvaluationContextProvider(extensions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.support.SubscriberContextAware#withSubscriberContext(reactor.util.context.Context)
	 */
	@Override
	public ContextAwareQueryMethodEvaluationContextProvider withSubscriberContext(Context context) {

		ContextAwareEvaluationContextProvider provider = (ContextAwareEvaluationContextProvider) getExtensionProvider();

		Lazy<ContextAwareEvaluationContextProvider> contextualized = Lazy.of(() -> provider.withSubscriberContext(context));

		return new ContextAwareQueryMethodEvaluationContextProvider(contextualized);
	}
}
