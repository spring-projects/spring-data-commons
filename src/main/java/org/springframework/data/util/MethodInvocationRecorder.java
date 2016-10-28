package org.springframework.data.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MethodInvocationRecorder {

	private static ObjenesisStd OBJENESIS = new ObjenesisStd();
	public static PropertyNameDetectionStrategy DEFAULT = DefaultPropertyNameDetectionStrategy.INSTANCE;

	private RecordingMethodInterceptor interceptor;

	/**
	 * Creates a new {@link MethodInvocationRecorder}. For ad-hoc instantation prefer the static
	 * {@link #forProxyOf(Class)}.
	 */
	private MethodInvocationRecorder() {
		this(null);
	}

	/**
	 * Creates a new {@link Recorded} for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static <T> Recorded<T> forProxyOf(Class<T> type) {

		Assert.notNull(type, "Type must not be null!");

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

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallbackType(RecordingMethodInterceptor.class);

		Factory factory = (Factory) OBJENESIS.newInstance(enhancer.createClass());
		factory.setCallbacks(new Callback[] { interceptor });

		return new Recorded<T>((T) factory, new MethodInvocationRecorder(interceptor));
	}

	private String getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
		return interceptor.getPropertyPath(strategies);
	}

	private class RecordingMethodInterceptor implements MethodInterceptor {

		private InvocationInformation information = InvocationInformation.NOT_INVOKED;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.cglib.proxy.MethodInterceptor#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], org.springframework.cglib.proxy.MethodProxy)
		 */
		public Object intercept(Object o, Method method, Object[] os, MethodProxy mp) throws Throwable {

			if (ReflectionUtils.isObjectMethod(method)) {
				return method.invoke(this, os);
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

		private String getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {

			if (!information.wasInvoked()) {
				return "";
			}

			return getInvocationInformation().getPropertyPath(strategies);
		}

		private InvocationInformation registerInvocation(Method method, Class<?> proxyType) {

			Recorded<?> create = Modifier.isFinal(proxyType.getModifiers()) ? new Unrecorded() : create(proxyType);
			InvocationInformation information = new InvocationInformation(create, method);

			this.information = information;

			return information;
		}

		private InvocationInformation getInvocationInformation() {

			InvocationInformation result = this.information;

			if (InvocationInformation.NOT_INVOKED.equals(result)) {
				throw new IllegalStateException();
			}

			return result;
		}
	}

	@Value
	static class InvocationInformation {

		static final InvocationInformation NOT_INVOKED = new InvocationInformation(new Unrecorded(), null);

		@NonNull Recorded<?> recorded;
		@Nullable Method invokedMethod;

		Object getCurrentInstance() {
			return recorded.currentInstance;
		}

		boolean wasInvoked() {
			return !Unrecorded.class.isInstance(recorded);
		}

		String getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {

			Method invokedMethod = this.invokedMethod;

			if (invokedMethod == null) {
				return "";
			}

			String propertyName = getPropertyName(invokedMethod, strategies);
			String next = recorded.getPropertyPath(strategies);

			return StringUtils.hasText(next) ? propertyName.concat(".").concat(next) : propertyName;
		}

		private static String getPropertyName(Method invokedMethod, List<PropertyNameDetectionStrategy> strategies) {

			return strategies.stream() //
					.map(it -> it.getPropertyName(invokedMethod)) //
					.findFirst() //
					.orElseThrow(() -> new IllegalArgumentException(
							String.format("No property name found for method %s!", invokedMethod)));
		}
	}

	public interface PropertyNameDetectionStrategy {

		@Nullable
		String getPropertyName(Method method);
	}

	private static enum DefaultPropertyNameDetectionStrategy implements PropertyNameDetectionStrategy {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.hateoas.core.Recorder.PropertyNameDetectionStrategy#getPropertyName(java.lang.reflect.Method)
		 */
		@Nonnull
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

	@ToString
	@RequiredArgsConstructor
	public static class Recorded<T> {

		private final T currentInstance;
		private final MethodInvocationRecorder recorder;

		public String getPropertyPath() {
			return getPropertyPath(MethodInvocationRecorder.DEFAULT);
		}

		public String getPropertyPath(PropertyNameDetectionStrategy strategy) {
			return recorder.getPropertyPath(Arrays.asList(strategy));
		}

		public String getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
			return recorder.getPropertyPath(strategies);
		}

		public boolean isRecorded() {
			return currentInstance != null;
		}

		/**
		 * Applies the given Converter to the recorded value and remembers the property accessed.
		 * 
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public <S> Recorded<S> record(Function<T, S> converter) {
			return new Recorded<S>(converter.apply(currentInstance), recorder);
		}

		public <S> Recorded<S> record(ToCollectionConverter<T, S> converter) {
			return new Recorded<S>(converter.apply(currentInstance).iterator().next(), recorder);
		}

		public <S> Recorded<S> record(ToMapConverter<T, S> converter) {
			return new Recorded<S>(converter.apply(currentInstance).values().iterator().next(), recorder);
		}

		public interface ToCollectionConverter<T, S> extends Function<T, Collection<S>> {}

		public interface ToMapConverter<T, S> extends Function<T, Map<?, S>> {}
	}

	static class Unrecorded extends Recorded<Object> {

		@SuppressWarnings("null")
		private Unrecorded() {
			super(null, null);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.util.MethodInvocationRecorder.Recorded#getPropertyPath(java.util.List)
		 */
		@Override
		public String getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
			return "";
		}
	}
}
