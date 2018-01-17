/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.util;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KCallable;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KMutableProperty;
import kotlin.reflect.KProperty;
import kotlin.reflect.KType;
import kotlin.reflect.jvm.ReflectJvmMapping;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * Spring Data specific reflection utility methods and classes.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.5
 */
@UtilityClass
public class ReflectionUtils {

	private static final boolean KOTLIN_IS_PRESENT = ClassUtils.isPresent("kotlin.Unit",
			BeanUtils.class.getClassLoader());
	private static final int KOTLIN_KIND_CLASS = 1;

	/**
	 * Creates an instance of the class with the given fully qualified name or returns the given default instance if the
	 * class cannot be loaded or instantiated.
	 *
	 * @param classname the fully qualified class name to create an instance for.
	 * @param defaultInstance the instance to fall back to in case the given class cannot be loaded or instantiated.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createInstanceIfPresent(String classname, T defaultInstance) {

		try {
			Class<?> type = ClassUtils.forName(classname, ClassUtils.getDefaultClassLoader());
			return (T) BeanUtils.instantiateClass(type);
		} catch (Exception e) {
			return defaultInstance;
		}
	}

	/**
	 * A {@link FieldFilter} that has a description.
	 *
	 * @author Oliver Gierke
	 */
	public interface DescribedFieldFilter extends FieldFilter {

		/**
		 * Returns the description of the field filter. Used in exceptions being thrown in case uniqueness shall be enforced
		 * on the field filter.
		 *
		 * @return
		 */
		String getDescription();
	}

	/**
	 * A {@link FieldFilter} for a given annotation.
	 *
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	public static class AnnotationFieldFilter implements DescribedFieldFilter {

		private final @NonNull Class<? extends Annotation> annotationType;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.util.ReflectionUtils.FieldFilter#matches(java.lang.reflect.Field)
		 */
		public boolean matches(Field field) {
			return AnnotationUtils.getAnnotation(field, annotationType) != null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.ReflectionUtils.DescribedFieldFilter#getDescription()
		 */
		public String getDescription() {
			return String.format("Annotation filter for %s", annotationType.getName());
		}
	}

	/**
	 * Finds the first field on the given class matching the given {@link FieldFilter}.
	 *
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @return the field matching the filter or {@literal null} in case no field could be found.
	 */
	@Nullable
	public static Field findField(Class<?> type, FieldFilter filter) {

		return findField(type, new DescribedFieldFilter() {

			public boolean matches(Field field) {
				return filter.matches(field);
			}

			public String getDescription() {
				return String.format("FieldFilter %s", filter.toString());
			}
		}, false);
	}

	/**
	 * Finds the field matching the given {@link DescribedFieldFilter}. Will make sure there's only one field matching the
	 * filter.
	 *
	 * @see #findField(Class, DescribedFieldFilter, boolean)
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @return the field matching the given {@link DescribedFieldFilter} or {@literal null} if none found.
	 * @throws IllegalStateException in case more than one matching field is found
	 */
	@Nullable
	public static Field findField(Class<?> type, DescribedFieldFilter filter) {
		return findField(type, filter, true);
	}

	/**
	 * Finds the field matching the given {@link DescribedFieldFilter}. Will make sure there's only one field matching the
	 * filter in case {@code enforceUniqueness} is {@literal true}.
	 *
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @param enforceUniqueness whether to enforce uniqueness of the field
	 * @return the field matching the given {@link DescribedFieldFilter} or {@literal null} if none found.
	 * @throws IllegalStateException if enforceUniqueness is true and more than one matching field is found
	 */
	@Nullable
	public static Field findField(Class<?> type, DescribedFieldFilter filter, boolean enforceUniqueness) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(filter, "Filter must not be null!");

		Class<?> targetClass = type;
		Field foundField = null;

		while (targetClass != Object.class) {

			for (Field field : targetClass.getDeclaredFields()) {

				if (!filter.matches(field)) {
					continue;
				}

				if (!enforceUniqueness) {
					return field;
				}

				if (foundField != null && enforceUniqueness) {
					throw new IllegalStateException(filter.getDescription());
				}

				foundField = field;
			}

			targetClass = targetClass.getSuperclass();
		}

		return foundField;
	}

	/**
	 * Finds the field of the given name on the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @return
	 * @throws IllegalArgumentException in case the field can't be found.
	 */
	public static Field findRequiredField(Class<?> type, String name) {

		Field result = org.springframework.util.ReflectionUtils.findField(type, name);

		if (result == null) {
			throw new IllegalArgumentException(String.format("Unable to find field %s on %s!", name, type));
		}

		return result;
	}

	/**
	 * Sets the given field on the given object to the given value. Will make sure the given field is accessible.
	 *
	 * @param field must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param value
	 */
	public static void setField(Field field, Object target, @Nullable Object value) {

		org.springframework.util.ReflectionUtils.makeAccessible(field);
		org.springframework.util.ReflectionUtils.setField(field, target, value);
	}

	/**
	 * Finds a constructor on the given type that matches the given constructor arguments.
	 *
	 * @param type must not be {@literal null}.
	 * @param constructorArguments must not be {@literal null}.
	 * @return a {@link Constructor} that is compatible with the given arguments.
	 */
	public static Optional<Constructor<?>> findConstructor(Class<?> type, Object... constructorArguments) {

		Assert.notNull(type, "Target type must not be null!");
		Assert.notNull(constructorArguments, "Constructor arguments must not be null!");

		return Arrays.stream(type.getDeclaredConstructors())//
				.filter(constructor -> argumentsMatch(constructor.getParameterTypes(), constructorArguments))//
				.findFirst();
	}

	/**
	 * Returns the method with the given name of the given class and parameter types.
	 *
	 * @param type must not be {@literal null}.
	 * @param name must not be {@literal null}.
	 * @param parameterTypes must not be {@literal null}.
	 * @return
	 * @throws IllegalArgumentException in case the method cannot be resolved.
	 */
	public static Method findRequiredMethod(Class<?> type, String name, Class<?>... parameterTypes) {

		Method result = org.springframework.util.ReflectionUtils.findMethod(type, name, parameterTypes);

		if (result == null) {

			String parameterTypeNames = Arrays.stream(parameterTypes) //
					.map(Object::toString) //
					.collect(Collectors.joining(", "));

			throw new IllegalArgumentException(
					String.format("Unable to find method %s(%s)on %s!", name, parameterTypeNames, type));
		}

		return result;
	}

	/**
	 * Returns a {@link Stream} of the return and parameters types of the given {@link Method}.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public static Stream<Class<?>> returnTypeAndParameters(Method method) {

		Assert.notNull(method, "Method must not be null!");

		Stream<Class<?>> returnType = Stream.of(method.getReturnType());
		Stream<Class<?>> parameterTypes = Arrays.stream(method.getParameterTypes());

		return Stream.concat(returnType, parameterTypes);
	}

	/**
	 * Returns the {@link Method} with the given name and parameters declared on the given type, if available.
	 *
	 * @param type must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @param parameterTypes must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public static Optional<Method> getMethod(Class<?> type, String name, ResolvableType... parameterTypes) {

		Assert.notNull(type, "Type must not be null!");
		Assert.hasText(name, "Name must not be null or empty!");
		Assert.notNull(parameterTypes, "Parameter types must not be null!");

		List<Class<?>> collect = Arrays.stream(parameterTypes)//
				.map(ResolvableType::getRawClass)//
				.collect(Collectors.toList());

		Method method = org.springframework.util.ReflectionUtils.findMethod(type, name,
				collect.toArray(new Class<?>[collect.size()]));

		return Optional.ofNullable(method)//
				.filter(it -> IntStream.range(0, it.getParameterCount())//
						.allMatch(index -> ResolvableType.forMethodParameter(it, index).equals(parameterTypes[index])));
	}

	private static boolean argumentsMatch(Class<?>[] parameterTypes, Object[] arguments) {

		if (parameterTypes.length != arguments.length) {
			return false;
		}

		int index = 0;

		for (Class<?> argumentType : parameterTypes) {

			Object argument = arguments[index];

			// Reject nulls for primitives
			if (argumentType.isPrimitive() && argument == null) {
				return false;
			}

			// Type check if argument is not null
			if (argument != null && !ClassUtils.isAssignableValue(argumentType, argument)) {
				return false;
			}

			index++;
		}

		return true;
	}

	/**
	 * Return {@literal true} if the specified class is a Kotlin one.
	 *
	 * @return {@literal true} if {@code type} is a Kotlin class.
	 * @since 2.0
	 */
	public static boolean isKotlinClass(Class<?> type) {

		return KOTLIN_IS_PRESENT && Arrays.stream(type.getDeclaredAnnotations()) //
				.map(Annotation::annotationType) //
				.anyMatch(annotation -> annotation.getName().equals("kotlin.Metadata"));
	}

	/**
	 * Return {@literal true} if the specified class is a supported Kotlin class. Currently supported are only regular
	 * Kotlin classes. Other class types (synthetic, SAM, lambdas) are not supported via reflection.
	 *
	 * @return {@literal true} if {@code type} is a supported Kotlin class.
	 * @since 2.0
	 */
	public static boolean isSupportedKotlinClass(Class<?> type) {

		if (!isKotlinClass(type)) {
			return false;
		}

		return Arrays.stream(type.getDeclaredAnnotations()) //
				.filter(annotation -> annotation.annotationType().getName().equals("kotlin.Metadata")) //
				.map(annotation -> AnnotationUtils.getValue(annotation, "k")) //
				.anyMatch(it -> Integer.valueOf(KOTLIN_KIND_CLASS).equals(it));
	}

	/**
	 * Returns {@literal} whether the given {@link MethodParameter} is nullable. Nullable parameters are reference types
	 * and ones that are defined in Kotlin as such.
	 *
	 * @return {@literal true} if {@link MethodParameter} is nullable.
	 * @since 2.0
	 */
	public static boolean isNullable(MethodParameter parameter) {

		if (Void.class.equals(parameter.getParameterType()) || Void.TYPE.equals(parameter.getParameterType())) {
			return true;
		}

		if (isSupportedKotlinClass(parameter.getDeclaringClass())) {
			return KotlinReflectionUtils.isNullable(parameter);
		}

		return !parameter.getParameterType().isPrimitive();
	}

	/**
	 * Reflection utility methods specific to Kotlin reflection.
	 */
	static class KotlinReflectionUtils {

		/**
		 * Returns {@literal} whether the given {@link MethodParameter} is nullable. Its declaring method can reference a
		 * Kotlin function, property or interface property.
		 *
		 * @return {@literal true} if {@link MethodParameter} is nullable.
		 * @since 2.0.1
		 */
		static boolean isNullable(MethodParameter parameter) {

			Method method = parameter.getMethod();

			if (method == null) {
				throw new IllegalStateException(String.format("Cannot obtain method from parameter %s!", parameter));
			}

			KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(method);

			if (kotlinFunction == null) {

				// Fallback to own lookup because there's no public Kotlin API for that kind of lookup until
				// https://youtrack.jetbrains.com/issue/KT-20768 gets resolved.
				kotlinFunction = findKFunction(method)//
						.orElseThrow(() -> new IllegalArgumentException(
								String.format("Cannot resolve %s to a Kotlin function!", parameter)));
			}

			KType type = parameter.getParameterIndex() == -1 //
					? kotlinFunction.getReturnType() //
					: kotlinFunction.getParameters().get(parameter.getParameterIndex() + 1).getType();

			return type.isMarkedNullable();
		}

		/**
		 * Lookup a {@link Method} to a {@link KFunction}.
		 *
		 * @param method the JVM {@link Method} to look up.
		 * @return {@link Optional} wrapping a possibly existing {@link KFunction}.
		 */
		private static Optional<? extends KFunction<?>> findKFunction(Method method) {

			KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(method.getDeclaringClass());

			return kotlinClass.getMembers() //
					.stream() //
					.flatMap(KotlinReflectionUtils::toKFunctionStream) //
					.filter(it -> isSame(it, method)) //
					.findFirst();
		}

		private static Stream<? extends KFunction<?>> toKFunctionStream(KCallable<?> it) {

			if (it instanceof KMutableProperty<?>) {

				KMutableProperty<?> property = (KMutableProperty<?>) it;
				return Stream.of(property.getGetter(), property.getSetter());
			}

			if (it instanceof KProperty<?>) {

				KProperty<?> property = (KProperty<?>) it;
				return Stream.of(property.getGetter());
			}

			if (it instanceof KFunction<?>) {
				return Stream.of((KFunction<?>) it);
			}

			return Stream.empty();
		}

		private static boolean isSame(KFunction<?> function, Method method) {

			Method javaMethod = ReflectJvmMapping.getJavaMethod(function);
			return javaMethod != null && javaMethod.equals(method);
		}
	}
}
