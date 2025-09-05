/*
 * Copyright 2016-2025 the original author or authors.
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
package org.springframework.data.util;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * API to record method invocations via method references on a proxy.
 *
 * @author Oliver Gierke
 * @author Johannes Englmeier
 * @since 2.2
 * @soundtrack The Intersphere - Don't Think Twice (The Grand Delusion)
 */
public class MethodInvocationRecorder {

	public static PropertyNameDetectionStrategy DEFAULT = DefaultPropertyNameDetectionStrategy.INSTANCE;

	private final @Nullable RecordingMethodInterceptor interceptor;

	/**
	 * Creates a new {@link MethodInvocationRecorder}. For ad-hoc instantation prefer the static
	 * {@link #forProxyOf(Class)}.
	 */
	private MethodInvocationRecorder() {
		this(null);
	}

	private MethodInvocationRecorder(@Nullable RecordingMethodInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	/**
	 * Creates a new {@link Recorded} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static <T> Recorded<T> forProxyOf(Class<T> type) {

		Assert.notNull(type, "Type must not be null");
		Assert.isTrue(!Modifier.isFinal(type.getModifiers()), "Type to record invocations on must not be final");

		return new MethodInvocationRecorder().create(type);
	}

	/**
	 * Creates a new {@link Recorded} for the given type based on the current {@link MethodInvocationRecorder} setup.
	 *
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> Recorded<T> create(Class<T> type) {

		RecordingMethodInterceptor interceptor = new RecordingMethodInterceptor();

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.addAdvice(interceptor);

		if (!type.isInterface()) {
			proxyFactory.setTargetClass(type);
			proxyFactory.setProxyTargetClass(true);
		} else {
			proxyFactory.addInterface(type);
		}

		T proxy = (T) proxyFactory.getProxy(type.getClassLoader());

		return new Recorded<>(proxy, new MethodInvocationRecorder(interceptor));
	}

	private @Nullable String getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {

		if (interceptor != null) {
			return interceptor.getPropertyPath(strategies);
		}

		return null;
	}

	private class RecordingMethodInterceptor implements org.aopalliance.intercept.MethodInterceptor {

		private InvocationInformation information = InvocationInformation.NOT_INVOKED;

		@Override
		@SuppressWarnings("null")
		public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			@Nullable
			Object[] arguments = invocation.getArguments();

			if (ReflectionUtils.isObjectMethod(method)) {
				return method.invoke(this, arguments);
			}

			ResolvableType type = ResolvableType.forMethodReturnType(method);
			Class<?> rawType = type.resolve(Object.class);

			if (Collection.class.isAssignableFrom(rawType)) {

				Class<?> clazz = type.getGeneric(0).resolve(Object.class);

				InvocationInformation information = registerInvocation(method, clazz);

				Collection<Object> collection = CollectionFactory.createCollection(rawType, 1);

				if (information.getCurrentInstance() != null) {
					collection.add(information.getCurrentInstance());
				}

				return collection;
			}

			if (Map.class.isAssignableFrom(rawType)) {

				Class<?> clazz = type.getGeneric(1).resolve(Object.class);
				InvocationInformation information = registerInvocation(method, clazz);

				Map<Object, Object> map = CollectionFactory.createMap(rawType, 1);

				if (information.getCurrentInstance() != null) {
					map.put("_key_", information.getCurrentInstance());
				}

				return map;
			}

			return registerInvocation(method, rawType).getCurrentInstance();
		}

		private @Nullable String getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
			return this.information.getPropertyPath(strategies);
		}

		private InvocationInformation registerInvocation(Method method, Class<?> proxyType) {

			Recorded<?> create = Modifier.isFinal(proxyType.getModifiers()) ? new Unrecorded(proxyType) : create(proxyType);
			InvocationInformation information = new InvocationInformation(create, method);

			return this.information = information;
		}
	}

	private static final class InvocationInformation {

		private static final InvocationInformation NOT_INVOKED = new InvocationInformation(new Unrecorded(null), null);

		private final Recorded<?> recorded;
		private final @Nullable Method invokedMethod;

		public InvocationInformation(Recorded<?> recorded, @Nullable Method invokedMethod) {

			Assert.notNull(recorded, "Recorded must not be null");

			this.recorded = recorded;
			this.invokedMethod = invokedMethod;
		}

		@Nullable
		Object getCurrentInstance() {
			return recorded.currentInstance;
		}

		@Nullable
		String getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {

			Method invokedMethod = this.invokedMethod;

			if (invokedMethod == null) {
				return null;
			}

			String propertyName = getPropertyName(invokedMethod, strategies);
			Optional<String> next = recorded.getPropertyPath(strategies);

			return Optionals.firstNonEmpty(() -> next.map(it -> propertyName.concat(".").concat(it)), //
					() -> Optional.of(propertyName)).orElse(null);
		}

		private static String getPropertyName(Method invokedMethod, List<PropertyNameDetectionStrategy> strategies) {

			return strategies.stream() //
					.flatMap(it -> Stream.ofNullable(it.getPropertyName(invokedMethod))) //
					.findFirst() //
					.orElseThrow(
							() -> new IllegalArgumentException(String.format("No property name found for method %s", invokedMethod)));
		}

		public Recorded<?> getRecorded() {
			return this.recorded;
		}

		public @Nullable Method getInvokedMethod() {
			return this.invokedMethod;
		}

		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof InvocationInformation that)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(recorded, that.recorded)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(invokedMethod, that.invokedMethod);
		}

		@Override
		public int hashCode() {

			int result = ObjectUtils.nullSafeHashCode(recorded);

			result = (31 * result) + ObjectUtils.nullSafeHashCode(invokedMethod);

			return result;
		}

		@Override
		public String toString() {
			return "MethodInvocationRecorder.InvocationInformation(recorded=" + this.getRecorded() + ", invokedMethod="
					+ this.getInvokedMethod() + ")";
		}
	}

	public interface PropertyNameDetectionStrategy {

		@Nullable
		String getPropertyName(Method method);
	}

	private enum DefaultPropertyNameDetectionStrategy implements PropertyNameDetectionStrategy {

		INSTANCE;

		@NonNull
		@Override
		public String getPropertyName(Method method) {
			return getPropertyName(method.getReturnType(), method.getName());
		}

		private static String getPropertyName(Class<?> type, String methodName) {

			String pattern = getPatternFor(type);
			String replaced = methodName.replaceFirst(pattern, "");

			return StringUtils.uncapitalize(replaced);
		}

		private static String getPatternFor(Class<?> type) {
			return type.equals(boolean.class) ? "^(is)" : "^(get|set)";
		}
	}

	public static class Recorded<T> {

		private final @Nullable T currentInstance;
		private final @Nullable MethodInvocationRecorder recorder;

		Recorded(@Nullable T currentInstance, @Nullable MethodInvocationRecorder recorder) {

			this.currentInstance = currentInstance;
			this.recorder = recorder;
		}

		public Optional<String> getPropertyPath() {
			return getPropertyPath(MethodInvocationRecorder.DEFAULT);
		}

		public Optional<String> getPropertyPath(PropertyNameDetectionStrategy strategy) {

			MethodInvocationRecorder recorder = this.recorder;

			return Optional.ofNullable(recorder == null ? null : recorder.getPropertyPath(List.of(strategy)));
		}

		public Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {

			MethodInvocationRecorder recorder = this.recorder;

			return Optional.ofNullable(recorder == null ? null : recorder.getPropertyPath(strategies));
		}

		/**
		 * Applies the given Converter to the recorded value and remembers the property accessed.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public <S> Recorded<S> record(Function<? super T, S> converter) {

			Assert.notNull(converter, "Function must not be null");

			return new Recorded<>(converter.apply(currentInstance), recorder);
		}

		/**
		 * Record the method invocation traversing to a collection property.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public <S> Recorded<S> record(ToCollectionConverter<T, S> converter) {

			Assert.notNull(converter, "Converter must not be null");

			return new Recorded<>(converter.apply(currentInstance).iterator().next(), recorder);
		}

		/**
		 * Record the method invocation traversing to a map property.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public <S> Recorded<S> record(ToMapConverter<T, S> converter) {

			Assert.notNull(converter, "Converter must not be null");

			return new Recorded<>(converter.apply(currentInstance).values().iterator().next(), recorder);
		}

		@Override
		public String toString() {

			return "MethodInvocationRecorder.Recorded(currentInstance=" + this.currentInstance + ", recorder=" + this.recorder
					+ ")";
		}

		public interface ToCollectionConverter<T, S> extends Function<T, Collection<S>> {}

		public interface ToMapConverter<T, S> extends Function<T, Map<?, S>> {}
	}

	static class Unrecorded extends Recorded<Object> {

		private Unrecorded(@Nullable Class<?> type) {
			super(type == null ? null : type.isPrimitive() ? getDefaultValue(type) : null, null);
		}

		@Override
		public Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
			return Optional.empty();
		}

		private static Object getDefaultValue(Class<?> clazz) {
			return Array.get(Array.newInstance(clazz, 1), 0);
		}
	}
}
