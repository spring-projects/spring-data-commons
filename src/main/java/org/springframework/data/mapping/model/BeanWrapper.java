/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.mapping.model;

import kotlin.reflect.KCallable;
import kotlin.reflect.KParameter;
import kotlin.reflect.KParameter.Kind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.KotlinDetector;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * Domain service to allow accessing the values of {@link PersistentProperty}s on a given bean.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class BeanWrapper<T> implements PersistentPropertyAccessor<T> {

	private T bean;

	/**
	 * Creates a new {@link BeanWrapper} for the given bean.
	 *
	 * @param bean must not be {@literal null}.
	 */
	protected BeanWrapper(T bean) {

		Assert.notNull(bean, "Bean must not be null!");
		this.bean = bean;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#setProperty(org.springframework.data.mapping.PersistentProperty, java.util.Optional)
	 */
	@SuppressWarnings("unchecked")
	public void setProperty(PersistentProperty<?> property, @Nullable Object value) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		try {

			if (property.isImmutable()) {

				Method wither = property.getWither();

				if (wither != null) {

					ReflectionUtils.makeAccessible(wither);
					this.bean = (T) ReflectionUtils.invokeMethod(wither, bean, value);
					return;
				}

				if (KotlinDetector.isKotlinType(property.getOwner().getType())) {

					this.bean = (T) KotlinCopyUtil.setProperty(property, bean, value);
					return;
				}

				throw new UnsupportedOperationException(
						String.format("Cannot set immutable property %s.%s!", property.getOwner().getName(), property.getName()));
			}

			if (!property.usePropertyAccess()) {

				Field field = property.getRequiredField();

				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, bean, value);
				return;
			}

			Method setter = property.getRequiredSetter();

			ReflectionUtils.makeAccessible(setter);
			ReflectionUtils.invokeMethod(setter, bean, value);

		} catch (IllegalStateException e) {
			throw new MappingException("Could not set object property!", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	@Nullable
	public Object getProperty(PersistentProperty<?> property) {
		return getProperty(property, property.getType());
	}

	/**
	 * Returns the value of the given {@link PersistentProperty} potentially converted to the given type.
	 *
	 * @param <S>
	 * @param property must not be {@literal null}.
	 * @param type can be {@literal null}.
	 * @return
	 * @throws MappingException in case an exception occured when accessing the property.
	 */
	@Nullable
	public <S> Object getProperty(PersistentProperty<?> property, Class<? extends S> type) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		try {

			if (!property.usePropertyAccess()) {

				Field field = property.getRequiredField();

				ReflectionUtils.makeAccessible(field);
				return ReflectionUtils.getField(field, bean);
			}

			Method getter = property.getRequiredGetter();

			ReflectionUtils.makeAccessible(getter);
			return ReflectionUtils.invokeMethod(getter, bean);

		} catch (IllegalStateException e) {
			throw new MappingException(
					String.format("Could not read property %s of %s!", property.toString(), bean.toString()), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getBean()
	 */
	public T getBean() {
		return bean;
	}

	/**
	 * Utility class to leverage Kotlin's copy method for immutable data classes.
	 *
	 * @since 2.1
	 */
	static class KotlinCopyUtil {

		private static final Map<Class<?>, KCallable<?>> copyMethodCache = new ConcurrentReferenceHashMap<>();

		/**
		 * Set a single property by calling {@code copy(…)} on a Kotlin data class. Copying creates a new instance that
		 * holds all values of the original instance and the newly set {@link PersistentProperty} value.
		 *
		 * @see KCallable#callBy(Map)
		 */
		static <T> Object setProperty(PersistentProperty<?> property, T bean, @Nullable Object value) {

			Class<?> type = property.getOwner().getType();
			KCallable<?> copy = copyMethodCache.computeIfAbsent(type, it -> getCopyMethod(it, property));

			if (copy == null) {
				throw new UnsupportedOperationException(String.format(
						"Kotlin class %s has no .copy(…) method for property %s!", type.getName(), property.getName()));
			}

			return copy.callBy(getCallArgs(copy, property, bean, value));
		}

		private static <T> Map<KParameter, Object> getCallArgs(KCallable<?> callable, PersistentProperty<?> property,
				T bean, @Nullable Object value) {

			Map<KParameter, Object> args = new LinkedHashMap<>(2, 1);

			List<KParameter> parameters = callable.getParameters();

			for (KParameter parameter : parameters) {

				if (parameter.getKind() == Kind.INSTANCE) {
					args.put(parameter, bean);
				}

				if (parameter.getKind() == Kind.VALUE && parameter.getName() != null
						&& parameter.getName().equals(property.getName())) {
					args.put(parameter, value);
				}
			}
			return args;
		}

		@Nullable
		private static KCallable<?> getCopyMethod(Class<?> type, PersistentProperty<?> property) {
			return KotlinCopyMethod.findCopyMethod(type).filter(it -> it.supportsProperty(property))
					.map(KotlinCopyMethod::getCopyFunction).orElse(null);
		}
	}
}
