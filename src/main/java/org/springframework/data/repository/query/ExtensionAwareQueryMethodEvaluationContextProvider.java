/*
 * Copyright 2014-2018 the original author or authors.
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

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Streamable;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link QueryMethodEvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link EvaluationContextExtension} instances.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @since 1.9
 */
public class ExtensionAwareQueryMethodEvaluationContextProvider implements QueryMethodEvaluationContextProvider {

	private final Lazy<org.springframework.data.spel.ExtensionAwareEvaluationContextProvider> delegate;

	/**
	 * Creates a new {@link ExtensionAwareQueryMethodEvaluationContextProvider}.
	 * 
	 * @param beanFactory the {@link ListableBeanFactory} to lookup the {@link EvaluationContextExtension}s from, must not
	 *          be {@literal null}.
	 */
	@SuppressWarnings("deprecation")
	public ExtensionAwareQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {

		Assert.notNull(beanFactory, "ListableBeanFactory must not be null!");

		this.delegate = Lazy.of(() -> {

			org.springframework.data.spel.ExtensionAwareEvaluationContextProvider delegate = new org.springframework.data.spel.ExtensionAwareEvaluationContextProvider(
					() -> getExtensionsFrom(beanFactory));
			delegate.setBeanFactory(beanFactory);

			return delegate;
		});
	}

	/**
	 * Creates a new {@link ExtensionAwareQueryMethodEvaluationContextProvider} using the given
	 * {@link EvaluationContextExtension}s.
	 * 
	 * @param extensions must not be {@literal null}.
	 */
	public ExtensionAwareQueryMethodEvaluationContextProvider(List<? extends EvaluationContextExtension> extensions) {

		Assert.notNull(extensions, "EvaluationContextExtensions must not be null!");

		this.delegate = Lazy.of(new org.springframework.data.spel.ExtensionAwareEvaluationContextProvider(extensions));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethodEvaluationContextProvider#getEvaluationContext(org.springframework.data.repository.query.Parameters, java.lang.Object[])
	 */
	@Override
	public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues) {

		StandardEvaluationContext evaluationContext = delegate.get().getEvaluationContext(parameterValues);

		evaluationContext.setVariables(collectVariables(parameters, parameterValues));

		return evaluationContext;
	}

	/**
	 * Exposes variables for all named parameters for the given arguments. Also exposes non-bindable parameters under the
	 * names of their types.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param arguments must not be {@literal null}.
	 * @return
	 */
	private static Map<String, Object> collectVariables(Streamable<? extends Parameter> parameters, Object[] arguments) {

		Map<String, Object> variables = new HashMap<>();

		parameters.stream()//
				.filter(Parameter::isSpecialParameter)//
				.forEach(it -> variables.put(//
						StringUtils.uncapitalize(it.getType().getSimpleName()), //
						arguments[it.getIndex()]));

		parameters.stream()//
				.filter(Parameter::isNamedParameter)//
				.forEach(it -> variables.put(//
						it.getName().orElseThrow(() -> new IllegalStateException("Should never occur!")), //
						arguments[it.getIndex()]));

		return variables;
	}

	/**
	 * Looks up all {@link EvaluationContextExtension} and
	 * {@link org.springframework.data.repository.query.spi.EvaluationContextExtension} instances from the given
	 * {@link ListableBeanFactory} and wraps the latter into proxies so that they implement the former.
	 *
	 * @param beanFactory must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private static List<EvaluationContextExtension> getExtensionsFrom(ListableBeanFactory beanFactory) {

		Stream<EvaluationContextExtension> legacyExtensions = beanFactory //
				.getBeansOfType(org.springframework.data.repository.query.spi.EvaluationContextExtension.class, true, false)
				.values().stream() //
				.map(ExtensionAwareQueryMethodEvaluationContextProvider::adaptFromLegacyApi);

		Stream<EvaluationContextExtension> extensions = beanFactory
				.getBeansOfType(EvaluationContextExtension.class, true, false).values().stream();

		return Stream.concat(extensions, legacyExtensions).collect(Collectors.toList());
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private static final EvaluationContextExtension adaptFromLegacyApi(
			org.springframework.data.repository.query.spi.EvaluationContextExtension extension) {

		DelegatingMethodInterceptor advice = new DelegatingMethodInterceptor(extension);
		advice.registerResultMapping("getFunctions",
				result -> ((Map<String, org.springframework.data.repository.query.spi.Function>) result).entrySet().stream()
						.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toNewFunction())));

		ProxyFactory factory = new ProxyFactory();
		factory.setInterfaces(EvaluationContextExtension.class);
		factory.setTargetClass(extension.getClass());
		factory.setProxyTargetClass(true);
		factory.setTarget(extension);
		factory.addAdvice(advice);

		return (EvaluationContextExtension) factory.getProxy();
	}

	/**
	 * A {@link MethodInterceptor} that forwards all invocations of methods (by name and parameter types) that are
	 * available on a given target object
	 *
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	static class DelegatingMethodInterceptor implements MethodInterceptor {

		private static final Map<Method, Method> METHOD_CACHE = new ConcurrentReferenceHashMap<Method, Method>();

		private final Object target;
		private final Map<String, java.util.function.Function<Object, Object>> directMappings = new HashMap<>();

		/**
		 * Registers a result mapping for the method with the given name. Invocation results for matching methods will be
		 * piped through the mapping.
		 * 
		 * @param methodName
		 * @param mapping
		 */
		public void registerResultMapping(String methodName, java.util.function.Function<Object, Object> mapping) {
			this.directMappings.put(methodName, mapping);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Nullable
		@Override
		public Object invoke(@Nullable MethodInvocation invocation) throws Throwable {

			if (invocation == null) {
				throw new IllegalArgumentException("Invocation must not be null!");
			}

			Method method = invocation.getMethod();
			Method targetMethod = METHOD_CACHE.computeIfAbsent(method,
					it -> Optional.ofNullable(findTargetMethod(it)).orElse(it));

			Object result = method.equals(targetMethod) ? invocation.proceed()
					: ReflectionUtils.invokeMethod(targetMethod, target, invocation.getArguments());

			if (result == null) {
				return result;
			}

			java.util.function.Function<Object, Object> mapper = directMappings.get(targetMethod.getName());

			return mapper != null ? mapper.apply(result) : result;
		}

		@Nullable
		private Method findTargetMethod(Method method) {

			try {
				return target.getClass().getMethod(method.getName(), method.getParameterTypes());
			} catch (Exception e) {
				return null;
			}
		}
	}
}
