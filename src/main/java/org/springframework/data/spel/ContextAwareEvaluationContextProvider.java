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
package org.springframework.data.spel;

import reactor.util.context.Context;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
public class ContextAwareEvaluationContextProvider extends ExtensionAwareEvaluationContextProvider
		implements SubscriberContextAware<ContextAwareEvaluationContextProvider> {

	/**
	 * Creates a new {@link ContextAwareEvaluationContextProvider}. Extensions are being looked up lazily from the
	 * {@link org.springframework.beans.factory.BeanFactory} configured.
	 */
	public ContextAwareEvaluationContextProvider() {}

	/**
	 * Creates a new {@link ContextAwareEvaluationContextProvider} with a {@link Supplier} of
	 * {@link EvaluationContextExtension extensions}.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ContextAwareEvaluationContextProvider(
			Supplier<? extends Collection<? extends EvaluationContextExtension>> extensions) {
		super(extensions);
	}

	/**
	 * Creates a new {@link ContextAwareEvaluationContextProvider} for the given {@link EvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ContextAwareEvaluationContextProvider(Collection<? extends EvaluationContextExtension> extensions) {
		super(extensions);
	}

	private ContextAwareEvaluationContextProvider(ExtensionAwareEvaluationContextProvider parent) {
		super(parent);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.support.SubscriberContextAware#withSubscriberContext(reactor.util.context.Context)
	 */
	@Override
	public ContextAwareEvaluationContextProvider withSubscriberContext(Context context) {

		Lazy<List<EvaluationContextExtension>> extensions = Lazy.of(() -> {

			return getExtensions().stream().map(it -> {

				if (it instanceof SubscriberContextAwareExtension) {
					return ((SubscriberContextAwareExtension) it).withSubscriberContext(context);
				}

				return it;
			}).collect(Collectors.toList());
		});

		return new ContextAwareEvaluationContextProvider(this) {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.spel.ExtensionAwareEvaluationContextProvider#getExtensions()
			 */
			@Override
			protected List<? extends EvaluationContextExtension> getExtensions() {
				return extensions.get();
			}
		};
	}
}
