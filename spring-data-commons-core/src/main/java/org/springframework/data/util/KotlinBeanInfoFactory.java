/*
 * Copyright 2023-2025 the original author or authors.
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

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KCallable;
import kotlin.reflect.KClass;
import kotlin.reflect.KMutableProperty;
import kotlin.reflect.KProperty;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanInfoFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.core.Ordered;
import org.springframework.lang.Contract;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BeanInfoFactory} specific to Kotlin types using Kotlin reflection to determine bean properties.
 *
 * @author Mark Paluch
 * @since 3.2
 * @see JvmClassMappingKt
 * @see ReflectJvmMapping
 */
public class KotlinBeanInfoFactory implements BeanInfoFactory, Ordered {

	@Override
	public @Nullable BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {

		if (beanClass.isInterface() || beanClass.isEnum()) {
			return null; // back-off to leave interface-based properties to the default mechanism.
		}

		if (!KotlinDetector.isKotlinReflectPresent() || !KotlinDetector.isKotlinType(beanClass)) {
			return null;
		}

		KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(beanClass);
		Collection<KCallable<?>> members = kotlinClass.getMembers();
		Map<String, PropertyDescriptor> descriptors = new LinkedHashMap<>(members.size(), 1.f);

		collectKotlinProperties(beanClass, members, descriptors);
		collectBasicJavaProperties(beanClass, descriptors);

		PropertyDescriptor[] propertyDescriptors = descriptors.values().toArray(new PropertyDescriptor[0]);

		return new SimpleBeanInfo() {
			@Override
			public BeanDescriptor getBeanDescriptor() {
				return new BeanDescriptor(beanClass);
			}

			@Override
			public PropertyDescriptor[] getPropertyDescriptors() {
				return propertyDescriptors;
			}
		};
	}

	private static void collectKotlinProperties(Class<?> beanClass, Collection<KCallable<?>> members,
			Map<String, PropertyDescriptor> descriptors) throws IntrospectionException {

		for (KCallable<?> member : members) {

			if (member instanceof KProperty<?> property) {

				Method setter = property instanceof KMutableProperty<?> kmp ? ReflectJvmMapping.getJavaSetter(kmp) : null;
				Type javaType = ReflectJvmMapping.getJavaType(property.getReturnType());
				Method getter = findGetter(beanClass, property, javaType);

				if (getter != null && (Modifier.isStatic(getter.getModifiers()) || getter.getParameterCount() != 0)) {
					continue;
				}

				if (getter != null && setter != null && setter.getParameterCount() == 1) {
					if (!getter.getReturnType().equals(setter.getParameters()[0].getType())) {
						// filter asymmetric getters/setters from being considered a Java Beans property
						continue;
					}
				}

				descriptors.put(property.getName(), new PropertyDescriptor(property.getName(), getter, setter));
			}
		}
	}

	private static @Nullable Method findGetter(Class<?> beanClass, KProperty<?> property, Type javaType) {

		Method getter = ReflectJvmMapping.getJavaGetter(property);

		if (getter == null && javaType == Boolean.TYPE) {
			getter = ReflectionUtils.findMethod(beanClass, "is" + StringUtils.capitalize(property.getName()));
		}

		if (getter == null) {
			getter = ReflectionUtils.findMethod(beanClass, "get" + StringUtils.capitalize(property.getName()));
		}

		return getter != null ? ClassUtils.getMostSpecificMethod(getter, beanClass) : null;
	}

	private static void collectBasicJavaProperties(Class<?> beanClass, Map<String, PropertyDescriptor> descriptors)
			throws IntrospectionException {

		Class<?> javaClass = beanClass;
		do {

			javaClass = javaClass.getSuperclass();
		} while (KotlinDetector.isKotlinType(javaClass));

		if (javaClass != Object.class) {

			PropertyDescriptor[] javaPropertyDescriptors = BeanUtils.getPropertyDescriptors(javaClass);

			for (PropertyDescriptor descriptor : javaPropertyDescriptors) {

				Method getter = specialize(beanClass, descriptor.getReadMethod());
				Method setter = specialize(beanClass, descriptor.getWriteMethod());

				if (!ObjectUtils.nullSafeEquals(descriptor.getReadMethod(), getter)
						|| !ObjectUtils.nullSafeEquals(descriptor.getWriteMethod(), setter)) {
					descriptor = new BasicPropertyDescriptor(descriptor.getName(), getter, setter);
				}
				descriptors.put(descriptor.getName(), descriptor);
			}
		}
	}

	@Contract("_, null -> null; _, !null -> !null")
	private static @Nullable Method specialize(Class<?> beanClass, @Nullable Method method) {

		if (method == null) {
			return method;
		}

		return ClassUtils.getMostSpecificMethod(method, beanClass);
	}

	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE - 10; // leave some space for customizations.
	}

	/**
	 * PropertyDescriptor for {@link KotlinBeanInfoFactory}, not performing any early type determination for
	 * {@link #setReadMethod}/{@link #setWriteMethod}.
	 *
	 * @since 3.3.5
	 */
	private static class BasicPropertyDescriptor extends PropertyDescriptor {

		private @Nullable Method readMethod;

		private @Nullable Method writeMethod;

		public BasicPropertyDescriptor(String propertyName, @Nullable Method readMethod, @Nullable Method writeMethod)
				throws IntrospectionException {

			super(propertyName, readMethod, writeMethod);
		}

		@Override
		public void setReadMethod(@Nullable Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		public @Nullable Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setWriteMethod(@Nullable Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		@Override
		@Nullable
		public Method getWriteMethod() {
			return this.writeMethod;
		}

	}

}
