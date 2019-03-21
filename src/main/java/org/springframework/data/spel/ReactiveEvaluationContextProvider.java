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
 * @see Context
 */
public class ReactiveEvaluationContextProvider extends ExtensionAwareEvaluationContextProvider
		implements SubscriberContextAware<ExtensionAwareEvaluationContextProvider> {

	/**
	 * Creates a new {@link ReactiveEvaluationContextProvider}. Extensions are being looked up lazily from the
	 * {@link org.springframework.beans.factory.BeanFactory} configured.
	 */
	public ReactiveEvaluationContextProvider() {}

	/**
	 * Creates a new {@link ReactiveEvaluationContextProvider} with a {@link Supplier} of
	 * {@link EvaluationContextExtension extensions}.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ReactiveEvaluationContextProvider(
			Supplier<? extends Collection<? extends EvaluationContextExtension>> extensions) {
		super(extensions);
	}

	/**
	 * Creates a new {@link ReactiveEvaluationContextProvider} for the given {@link EvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ReactiveEvaluationContextProvider(Collection<? extends EvaluationContextExtension> extensions) {
		super(extensions);
	}

	private ReactiveEvaluationContextProvider(ExtensionAwareEvaluationContextProvider parent) {
		super(parent);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.support.SubscriberContextAware#withSubscriberContext(reactor.util.context.Context)
	 */
	@Override
	public ReactiveEvaluationContextProvider withSubscriberContext(Context context) {

		Lazy<List<EvaluationContextExtension>> extensions = Lazy.of(() -> {

			return getExtensions().stream().map(it -> {

				if (it instanceof SubscriberContextAwareExtension) {
					return ((SubscriberContextAwareExtension) it).withSubscriberContext(context);
				}

				return it;
			}).collect(Collectors.toList());
		});

		return new ReactiveEvaluationContextProvider(this) {

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
