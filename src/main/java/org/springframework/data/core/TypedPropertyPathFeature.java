/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.core;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.aot.AotProcessingException;
import org.springframework.core.KotlinDetector;
import org.springframework.data.core.MemberDescriptor.KPropertyReferenceDescriptor;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.ClassUtils;

/**
 * GraalVM {@link Feature} that registers serializable {@link TypedPropertyPath} and {@link PropertyReference} lambdas.
 * This allows to use typed property paths and property references in native images without the need to pre-compute them
 * at build time.
 * <p>
 * This feature also registers Java Bean Properties (methods and fields) referenced by the property path or reference
 * for reflection, so that they are available at runtime and therefore the underlying domain model does not require
 * additional reachability configuration.
 * <p>
 * Kotlin property references ({@code Person::name}) reach the property path API through SAM-converted lambdas that
 * capture the property reference. These lambdas cannot be instantiated at build time, hence the referenced properties
 * are registered through the reachable {@link kotlin.jvm.internal.PropertyReference} classes instead.
 *
 * @author Mark Paluch
 * @since 4.1
 */
class TypedPropertyPathFeature implements Feature {

	private static final Log logger = LogFactory.getLog(TypedPropertyPathFeature.class);

	private final SerializableLambdaReader reader = new SerializableLambdaReader();

	@Override
	public void beforeAnalysis(BeforeAnalysisAccess access) {

		BiConsumer<DuringAnalysisAccess, Class<?>> serializableLambdaHandler = (ignore, cls) -> {

			if (ClassUtils.isLambdaClass(cls)) {

				try {
					registerLambdaSerialization(cls);
					registerDomainModel(cls);
				} catch (Exception e) {

					Class<?> type = cls.getEnclosingClass() != null ? cls.getEnclosingClass() : cls;
					throw new AotProcessingException("Unable to process TypedPropertyPath in [%s]. Please consider switching to String based dot notation.".formatted(type), e);
				}
			}
		};

		access.registerSubtypeReachabilityHandler(serializableLambdaHandler, TypedPropertyPath.class);
		access.registerSubtypeReachabilityHandler(serializableLambdaHandler, PropertyReference.class);

		if (KotlinDetector.isKotlinReflectPresent()) {
			KotlinDelegate.registerPropertyReferences(access);
		}
	}

	private void registerLambdaSerialization(Class<?> lambdaClass) {

		RuntimeSerialization.register(lambdaClass);
		RuntimeReflection.registerMethodLookup(lambdaClass, "writeReplace");
	}

	private void registerDomainModel(Class<?> cls) throws ReflectiveOperationException {

		// make sure to avoid capturing lambdas
		Constructor<?> constructor = ReflectionUtils.findConstructor(cls);

		if (constructor == null) {
			return;
		}

		constructor.setAccessible(true);
		Object lambdaInstance = constructor.newInstance();

		MemberDescriptor memberDescriptor = reader.read(lambdaInstance);
		registerDomainModel(memberDescriptor);
	}

	private static void registerDomainModel(MemberDescriptor descriptor) {

		PropertyDescriptor property = null;

		if (descriptor.getMember() instanceof Field f) {
			RuntimeReflection.register(f);
			property = BeanUtils.getPropertyDescriptor(descriptor.getOwner(), f.getName());
		}

		if (descriptor.getMember() instanceof Method m) {
			RuntimeReflection.register(m);
			property = BeanUtils.findPropertyForMethod(m);
		}

		if (property != null) {
			Method readMethod = property.getReadMethod();
			Method writeMethod = property.getWriteMethod();

			if (readMethod != null) {
				RuntimeReflection.register(readMethod);
			}
			if (writeMethod != null) {
				RuntimeReflection.register(writeMethod);
			}

			RuntimeReflection.registerFieldLookup(descriptor.getOwner(), property.getName());
		}
	}

	/**
	 * Delegate to register domain model members referenced through Kotlin property references. Kotlin compiles an unbound
	 * property reference such as {@code Person::name} into a singleton subclass of
	 * {@link kotlin.jvm.internal.PropertyReference} carrying the owner type and property name.
	 */
	static class KotlinDelegate {

		static void registerPropertyReferences(BeforeAnalysisAccess access) {

			access.registerSubtypeReachabilityHandler((ignore, cls) -> {

				if (Modifier.isAbstract(cls.getModifiers())) {
					return;
				}

				try {

					MemberDescriptor descriptor = getPropertyReference(cls);

					if (descriptor != null) {
						registerDomainModel(descriptor);
					}
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping registration of Kotlin property reference [%s]".formatted(cls.getName()), e);
					}
				}
			}, kotlin.jvm.internal.PropertyReference.class);
		}

		/**
		 * Describe the property referenced by the given unbound property reference class.
		 *
		 * @param cls the property reference class to inspect.
		 * @return the descriptor or {@code null} if {@code cls} is not an unbound reference to a class property.
		 */
		static @Nullable MemberDescriptor getPropertyReference(Class<?> cls) throws ReflectiveOperationException {

			// unbound references have a no-arg constructor, bound references capture their receiver.
			Constructor<?> constructor = ReflectionUtils.findConstructor(cls);

			if (constructor == null) {
				return null;
			}

			constructor.setAccessible(true);
			Object reference = constructor.newInstance();

			if (reference instanceof kotlin.jvm.internal.PropertyReference propRef
					&& propRef.getOwner() instanceof KClass<?> owner && reference instanceof KProperty1<?, ?> property) {
				return KPropertyReferenceDescriptor.create(JvmClassMappingKt.getJavaClass(owner), property);
			}

			return null;
		}

	}

}
