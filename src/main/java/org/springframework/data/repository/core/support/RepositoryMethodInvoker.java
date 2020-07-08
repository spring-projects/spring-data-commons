/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.repository.core.support;

import kotlin.coroutines.Continuation;
import kotlin.reflect.KFunction;
import kotlinx.coroutines.reactive.AwaitKt;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import org.springframework.core.KotlinDetector;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.util.KotlinReflectionUtils;
import org.springframework.lang.Nullable;

/**
 * Invoker for repository methods. Used to invoke query methods and fragment methods. This invoker considers Kotlin
 * coroutine method adaption by either forwarding the entire call or by bridging invocations over a reactive
 * implementation.
 *
 * @author Mark Paluch
 * @since 2.4
 * @see #forFragmentMethod(Method, Object, Method)
 * @see #forRepositoryQuery(Method, RepositoryQuery)
 * @see RepositoryQuery
 * @see RepositoryComposition
 */
abstract class RepositoryMethodInvoker {

	private final Method method;
	private final Class<?> returnedType;
	private final Invokable invokable;
	private final boolean suspendedDeclaredMethod;
	private final boolean returnsReactiveType;

	protected RepositoryMethodInvoker(Method method, Invokable invokable) {

		this.method = method;
		this.invokable = invokable;

		if (KotlinDetector.isKotlinReflectPresent()) {

			this.suspendedDeclaredMethod = KotlinReflectionUtils.isSuspend(method);
			this.returnedType = this.suspendedDeclaredMethod ? KotlinReflectionUtils.getReturnType(method)
					: method.getReturnType();
		} else {

			this.suspendedDeclaredMethod = false;
			this.returnedType = method.getReturnType();
		}

		this.returnsReactiveType = ReactiveWrappers.supports(returnedType);
	}

	static RepositoryQueryMethodInvoker forRepositoryQuery(Method declaredMethod, RepositoryQuery query) {
		return new RepositoryQueryMethodInvoker(declaredMethod, query);
	}

	/**
	 * Create a {@link RepositoryMethodInvoker} to call a fragment {@link Method}.
	 *
	 * @param declaredMethod the declared repository method from the repository interface.
	 * @param instance fragment instance.
	 * @param baseMethod the base method to call on fragment {@code instance}.
	 * @return {@link RepositoryMethodInvoker} to call a fragment {@link Method}.
	 */
	static RepositoryMethodInvoker forFragmentMethod(Method declaredMethod, Object instance, Method baseMethod) {
		return new RepositoryFragmentMethodInvoker(declaredMethod, instance, baseMethod);
	}

	/**
	 * Return whether the {@link Method declared method} can be adapted by calling {@link Method baseClassMethod}.
	 *
	 * @param declaredMethod the declared repository method from the repository interface.
	 * @param baseMethod the base method to call on fragment {@code instance}.
	 * @return
	 */
	public static boolean canInvoke(Method declaredMethod, Method baseClassMethod) {
		return RepositoryFragmentMethodInvoker.CoroutineAdapterInformation.create(declaredMethod, baseClassMethod)
				.canInvoke();
	}

	/**
	 * Invoke the repository method and return its value.
	 *
	 * @param listener listener to notify about the call outcome.
	 * @param args invocation arguments.
	 * @return
	 * @throws Exception
	 */
	@Nullable
	public Object invoke(RepositoryInvocationListener listener, Object[] args) throws Exception {
		return shouldAdaptReactiveToSuspended() ? doInvokeReactiveToSuspended(listener, args) : doInvoke(listener, args);
	}

	protected boolean shouldAdaptReactiveToSuspended() {
		return suspendedDeclaredMethod;
	}

	@Nullable
	private Object doInvoke(RepositoryInvocationListener listener, Object[] args) throws Exception {

		try {
			Object result = invokable.invoke(args);

			if (result != null && ReactiveWrappers.supports(result.getClass())) {
				return ReactiveWrapperConverters.doOnError(
						ReactiveWrapperConverters.doOnSuccess(result, () -> listener.afterInvocation(method, args, result, null)),
						ex -> listener.afterInvocation(method, args, result, ex));
			}

			if (result instanceof Stream) {
				return ((Stream<?>) result).onClose(() -> listener.afterInvocation(method, args, result, null));
			}

			listener.afterInvocation(method, args, result, null);

			return result;
		} catch (Exception e) {
			listener.afterInvocation(method, args, null, e);
			throw e;
		}
	}

	@Nullable
	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private Object doInvokeReactiveToSuspended(RepositoryInvocationListener listener, Object[] args) throws Exception {

		/*
		 * Kotlin suspended functions are invoked with a synthetic Continuation parameter that keeps track of the Coroutine context.
		 * We're invoking a method without Continuation as we expect the method to return any sort of reactive type,
		 * therefore we need to strip the Continuation parameter.
		 */
		Continuation<Object> continuation = (Continuation) args[args.length - 1];
		args[args.length - 1] = null;
		try {
			Object result = invokable.invoke(args);

			if (returnsReactiveType) {
				listener.afterInvocation(method, args, result, null);
				return ReactiveWrapperConverters.toWrapper(result, returnedType);
			}

			Publisher<?> publisher = result instanceof Publisher ? (Publisher<?>) result
					: ReactiveWrapperConverters.toWrapper(result, Publisher.class);

			publisher = ReactiveWrapperConverters.doOnError(
					ReactiveWrapperConverters.doOnSuccess(publisher, () -> listener.afterInvocation(method, args, result, null)),
					ex -> listener.afterInvocation(method, args, result, ex));

			return AwaitKt.awaitFirstOrNull(publisher, continuation);
		} catch (Exception e) {
			listener.afterInvocation(method, args, null, e);
			throw e;
		}
	}

	interface Invokable {

		@Nullable
		Object invoke(Object[] args) throws ReflectiveOperationException;
	}

	/**
	 * Implementation to invoke query methods.
	 */
	private static class RepositoryQueryMethodInvoker extends RepositoryMethodInvoker {
		public RepositoryQueryMethodInvoker(Method method, RepositoryQuery repositoryQuery) {
			super(method, repositoryQuery::execute);
		}
	}

	/**
	 * Implementation to invoke fragment methods.
	 */
	private static class RepositoryFragmentMethodInvoker extends RepositoryMethodInvoker {

		private final CoroutineAdapterInformation adapterInformation;

		public RepositoryFragmentMethodInvoker(Method declaredMethod, Object instance, Method baseClassMethod) {
			this(CoroutineAdapterInformation.create(declaredMethod, baseClassMethod), declaredMethod, instance,
					baseClassMethod);
		}

		public RepositoryFragmentMethodInvoker(CoroutineAdapterInformation adapterInformation, Method declaredMethod,
				Object instance, Method baseClassMethod) {
			super(declaredMethod, args -> {

				if (adapterInformation.isAdapterMethod()) {

					/*
					 * Kotlin suspended functions are invoked with a synthetic Continuation parameter that keeps track of the Coroutine context.
					 * We're invoking a method without Continuation as we expect the method to return any sort of reactive type,
					 * therefore we need to strip the Continuation parameter.
					 */
					Object[] invocationArguments = new Object[args.length - 1];
					System.arraycopy(args, 0, invocationArguments, 0, invocationArguments.length);

					return baseClassMethod.invoke(instance, invocationArguments);
				}

				return baseClassMethod.invoke(instance, args);
			});
			this.adapterInformation = adapterInformation;
		}

		@Override
		protected boolean shouldAdaptReactiveToSuspended() {
			return adapterInformation.shouldAdaptReactiveToSuspended();
		}

		/**
		 * Value object capturing whether a suspended Kotlin method (Coroutine method) can be bridged with a native or
		 * reactive fragment method.
		 */
		static class CoroutineAdapterInformation {

			private static CoroutineAdapterInformation DISABLED = new CoroutineAdapterInformation(false, false, false, 0, 0);

			private final boolean suspendedDeclaredMethod;
			private final boolean suspendedBaseClassMethod;
			private final boolean reactiveBaseClassMethod;
			private final int declaredMethodParameterCount;
			private final int baseClassMethodParameterCount;

			private CoroutineAdapterInformation(boolean suspendedDeclaredMethod, boolean suspendedBaseClassMethod,
					boolean reactiveBaseClassMethod, int declaredMethodParameterCount, int baseClassMethodParameterCount) {
				this.suspendedDeclaredMethod = suspendedDeclaredMethod;
				this.suspendedBaseClassMethod = suspendedBaseClassMethod;
				this.reactiveBaseClassMethod = reactiveBaseClassMethod;
				this.declaredMethodParameterCount = declaredMethodParameterCount;
				this.baseClassMethodParameterCount = baseClassMethodParameterCount;
			}

			/**
			 * Create {@link CoroutineAdapterInformation}.
			 *
			 * @param declaredMethod
			 * @param baseClassMethod
			 * @return
			 */
			public static CoroutineAdapterInformation create(Method declaredMethod, Method baseClassMethod) {

				if (!KotlinDetector.isKotlinReflectPresent()) {
					return DISABLED;
				}

				KFunction<?> declaredFunction = KotlinDetector.isKotlinType(declaredMethod.getDeclaringClass())
						? KotlinReflectionUtils.findKotlinFunction(declaredMethod)
						: null;
				KFunction<?> baseClassFunction = KotlinDetector.isKotlinType(baseClassMethod.getDeclaringClass())
						? KotlinReflectionUtils.findKotlinFunction(baseClassMethod)
						: null;

				boolean suspendedDeclaredMethod = declaredFunction != null && declaredFunction.isSuspend();
				boolean suspendedBaseClassMethod = baseClassFunction != null && baseClassFunction.isSuspend();
				boolean reactiveBaseClassMethod = !suspendedBaseClassMethod
						&& ReactiveWrapperConverters.supports(baseClassMethod.getReturnType());

				return new CoroutineAdapterInformation(suspendedDeclaredMethod, suspendedBaseClassMethod,
						reactiveBaseClassMethod, declaredMethod.getParameterCount(), baseClassMethod.getParameterCount());
			}

			boolean canInvoke() {

				if (suspendedDeclaredMethod == suspendedBaseClassMethod) {
					return declaredMethodParameterCount == baseClassMethodParameterCount;
				}

				if (isAdapterMethod()) {
					return declaredMethodParameterCount - 1 == baseClassMethodParameterCount;
				}

				return false;
			}

			boolean isAdapterMethod() {
				return suspendedDeclaredMethod && reactiveBaseClassMethod;
			}

			public boolean shouldAdaptReactiveToSuspended() {
				return suspendedDeclaredMethod && !suspendedBaseClassMethod && reactiveBaseClassMethod;
			}
		}
	}
}
