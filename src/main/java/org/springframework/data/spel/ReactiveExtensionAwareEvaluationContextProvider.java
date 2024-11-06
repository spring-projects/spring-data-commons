/*
 * Copyright 2020-2024 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ExtensionIdAware;
import org.springframework.data.spel.spi.ReactiveEvaluationContextExtension;
import org.springframework.data.util.Predicates;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

/**
 * A reactive {@link EvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link ReactiveEvaluationContextExtension} and {@link EvaluationContextExtension} instances.
 *
 * @author Mark Paluch
 * @since 2.4
 */
public class ReactiveExtensionAwareEvaluationContextProvider implements ReactiveEvaluationContextProvider {

	private static final ResolvableType GENERIC_EXTENSION_TYPE = ResolvableType
			.forClass(EvaluationContextExtension.class);

	private final ExtensionAwareEvaluationContextProvider evaluationContextProvider;

	public ReactiveExtensionAwareEvaluationContextProvider() {
		evaluationContextProvider = new ExtensionAwareEvaluationContextProvider();
	}

	/**
	 * Create a new {@link ReactiveExtensionAwareEvaluationContextProvider} with extensions looked up lazily from the
	 * given {@link ListableBeanFactory}.
	 *
	 * @param beanFactory the {@link ListableBeanFactory} to lookup extensions from.
	 */
	public ReactiveExtensionAwareEvaluationContextProvider(ListableBeanFactory beanFactory) {
		evaluationContextProvider = new ExtensionAwareEvaluationContextProvider(beanFactory);
	}

	/**
	 * Creates a new {@link ReactiveExtensionAwareEvaluationContextProvider} for the given
	 * {@link EvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ReactiveExtensionAwareEvaluationContextProvider(Collection<? extends ExtensionIdAware> extensions) {
		evaluationContextProvider = new ExtensionAwareEvaluationContextProvider(extensions);
	}

	@Override
	public EvaluationContext getEvaluationContext(Object rootObject) {
		return evaluationContextProvider.getEvaluationContext(rootObject);
	}

	@Override
	public EvaluationContext getEvaluationContext(Object rootObject, ExpressionDependencies dependencies) {
		return evaluationContextProvider.getEvaluationContext(rootObject, dependencies);
	}

	@Override
	public Mono<StandardEvaluationContext> getEvaluationContextLater(@Nullable Object rootObject) {
		return getExtensions(Predicates.isTrue()) //
				.map(it -> evaluationContextProvider.doGetEvaluationContext(rootObject, it));
	}

	@Override
	public Mono<StandardEvaluationContext> getEvaluationContextLater(@Nullable Object rootObject,
			ExpressionDependencies dependencies) {

		return getExtensions(it -> dependencies.stream().anyMatch(it::provides)) //
				.map(it -> evaluationContextProvider.doGetEvaluationContext(rootObject, it));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Mono<List<EvaluationContextExtension>> getExtensions(
			Predicate<EvaluationContextExtensionInformation> extensionFilter) {

		Collection<? extends ExtensionIdAware> extensions = evaluationContextProvider.getExtensions();

		return Flux.fromIterable(extensions).concatMap(it -> {

			if (it instanceof EvaluationContextExtension extension) {

				EvaluationContextExtensionInformation information = evaluationContextProvider.getOrCreateInformation(extension);

				if (extensionFilter.test(information)) {
					return Mono.just(extension);
				}

				return Mono.empty();
			}

			if (it instanceof ReactiveEvaluationContextExtension extension) {

				ResolvableType actualType = getExtensionType(it);

				if (actualType.equals(ResolvableType.NONE) || actualType.isAssignableFrom(GENERIC_EXTENSION_TYPE)) {
					return extension.getExtension();
				}

				EvaluationContextExtensionInformation information = evaluationContextProvider
						.getOrCreateInformation((Class) actualType.getRawClass());

				if (extensionFilter.test(information)) {
					return extension.getExtension();
				}

				return Mono.empty();
			}

			return Mono.error(new IllegalStateException("Unsupported extension type: " + it));
		}).collectList();
	}

	private static ResolvableType getExtensionType(ExtensionIdAware extensionCandidate) {

		return ResolvableType
				.forMethodReturnType(ReflectionUtils.getRequiredMethod(extensionCandidate.getClass(), "getExtension"))
				.getGeneric(0);
	}
}
