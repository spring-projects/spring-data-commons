/*
 * Copyright 2016-2021 the original author or authors.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * API to record method invocations via method references on a proxy.
 *
 * @author Oliver Gierke
 * @since 2.2
 * @soundtrack The Intersphere - Don't Think Twice (The Grand Delusion)
 */
public class MethodInvocationRecorder {

	public static PropertyNameDetectionStrategy DEFAULT = DefaultPropertyNameDetectionStrategy.INSTANCE;

	private Optional<RecordingMethodInterceptor> interceptor;

	/**
	 * Creates a new {@link MethodInvocationRecorder}. For ad-hoc instantation prefer the static
	 * {@link #forProxyOf(Class)}.
	 */
	private MethodInvocationRecorder() {
		this(Optional.empty());
	}

	private MethodInvocationRecorder(Optional<RecordingMethodInterceptor> interceptor) {
		this.interceptor = interceptor;
	}

	/**
	 * Creates a new {@link Recorded} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static <T> Recorded<T> forProxyOf(Class<T> type) {

		Assert.notNull(type, "Type must not be null!");
		Assert.isTrue(!Modifier.isFinal(type.getModifiers()), "Type to record invocations on must not be final!");

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

		return new Recorded<T>(proxy, new MethodInvocationRecorder(Optional.ofNullable(interceptor)));
	}

	private Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
		return interceptor.flatMap(it -> it.getPropertyPath(strategies));
	}

	private class RecordingMethodInterceptor implements org.aopalliance.intercept.MethodInterceptor {

		private InvocationInformation information = InvocationInformation.NOT_INVOKED;

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		@SuppressWarnings("null")
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
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
				collection.add(information.getCurrentInstance());

				return collection;
			}

			if (Map.class.isAssignableFrom(rawType)) {

				Class<?> clazz = type.getGeneric(1).resolve(Object.class);
				InvocationInformation information = registerInvocation(method, clazz);

				Map<Object, Object> map = CollectionFactory.createMap(rawType, 1);
				map.put("_key_", information.getCurrentInstance());

				return map;
			}

			return registerInvocation(method, rawType).getCurrentInstance();
		}

		private Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
			return this.information.getPropertyPath(strategies);
		}

		private InvocationInformation registerInvocation(Method method, Class<?> proxyType) {

			Recorded<?> create = Modifier.isFinal(proxyType.getModifiers()) ? new Unrecorded() : create(proxyType);
			InvocationInformation information = new InvocationInformation(create, method);

			return this.information = information;
		}
	}

	private static final class InvocationInformation {

		private static final InvocationInformation NOT_INVOKED = new InvocationInformation(new Unrecorded(), null);

		private final Recorded<?> recorded;
		private final @Nullable Method invokedMethod;

		public InvocationInformation(Recorded<?> recorded, @Nullable Method invokedMethod) {

			Assert.notNull(recorded, "Recorded must not be null!");

			this.recorded = recorded;
			this.invokedMethod = invokedMethod;
		}

		@Nullable
		Object getCurrentInstance() {
			return recorded.currentInstance;
		}

		Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {

			Method invokedMethod = this.invokedMethod;

			if (invokedMethod == null) {
				return Optional.empty();
			}

			String propertyName = getPropertyName(invokedMethod, strategies);
			Optional<String> next = recorded.getPropertyPath(strategies);

			return Optionals.firstNonEmpty(() -> next.map(it -> propertyName.concat(".").concat(it)), //
					() -> Optional.of(propertyName));
		}

		private static String getPropertyName(Method invokedMethod, List<PropertyNameDetectionStrategy> strategies) {

			return strategies.stream() //
					.map(it -> it.getPropertyName(invokedMethod)) //
					.findFirst() //
					.orElseThrow(() -> new IllegalArgumentException(
							String.format("No property name found for method %s!", invokedMethod)));
		}

		public Recorded<?> getRecorded() {
			return this.recorded;
		}

		@Nullable
		public Method getInvokedMethod() {
			return this.invokedMethod;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof InvocationInformation)) {
				return false;
			}

			InvocationInformation that = (InvocationInformation) o;

			if (!ObjectUtils.nullSafeEquals(recorded, that.recorded)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(invokedMethod, that.invokedMethod);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {

			int result = ObjectUtils.nullSafeHashCode(recorded);

			result = 31 * result + ObjectUtils.nullSafeHashCode(invokedMethod);

			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
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

		/*
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.Recorder.PropertyNameDetectionStrategy#getPropertyName(java.lang.reflect.Method)
		 */
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

			return recorder == null ? Optional.empty() : recorder.getPropertyPath(Arrays.asList(strategy));
		}

		public Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {

			MethodInvocationRecorder recorder = this.recorder;

			return recorder == null ? Optional.empty() : recorder.getPropertyPath(strategies);
		}

		/**
		 * Applies the given Converter to the recorded value and remembers the property accessed.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public <S> Recorded<S> record(Function<? super T, S> converter) {

			Assert.notNull(converter, "Function must not be null!");

			return new Recorded<S>(converter.apply(currentInstance), recorder);
		}

		/**
		 * Record the method invocation traversing to a collection property.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public <S> Recorded<S> record(ToCollectionConverter<T, S> converter) {

			Assert.notNull(converter, "Converter must not be null!");

			return new Recorded<S>(converter.apply(currentInstance).iterator().next(), recorder);
		}

		/**
		 * Record the method invocation traversing to a map property.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public <S> Recorded<S> record(ToMapConverter<T, S> converter) {

			Assert.notNull(converter, "Converter must not be null!");

			return new Recorded<S>(converter.apply(currentInstance).values().iterator().next(), recorder);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {

			return "MethodInvocationRecorder.Recorded(currentInstance=" + this.currentInstance + ", recorder=" + this.recorder
					+ ")";
		}

		public interface ToCollectionConverter<T, S> extends Function<T, Collection<S>> {}

		public interface ToMapConverter<T, S> extends Function<T, Map<?, S>> {}
	}

	static class Unrecorded extends Recorded<Object> {

		private Unrecorded() {
			super(null, null);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.MethodInvocationRecorder.Recorded#getPropertyPath(java.util.List)
		 */
		@Override
		public Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
			return Optional.empty();
		}
	}
}
