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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.aot.AotProcessingException;
import org.springframework.util.ClassUtils;

/**
 * GraalVM {@link Feature} that registers serializable {@link TypedPropertyPath} and {@link PropertyReference} lambdas.
 * This allows to use typed property paths and property references in native images without the need to pre-compute them
 * at build time.
 * <p>
 * This feature also registers Java Bean Properties (methods and fields) referenced by the property path or reference
 * for reflection, so that they are available at runtime and therefore the underlying domain model does not require
 * additional reachability configuration.
 *
 * @author Mark Paluch
 * @since 4.1
 */
class TypedPropertyPathFeature implements Feature {

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
	}

	private void registerLambdaSerialization(Class<?> lambdaClass) {

		RuntimeSerialization.register(lambdaClass);
		RuntimeReflection.registerMethodLookup(lambdaClass, "writeReplace");
	}

	private void registerDomainModel(Class<?> cls) throws ReflectiveOperationException {
		Constructor<?> declaredConstructor = cls.getDeclaredConstructor();
		declaredConstructor.setAccessible(true);
		Object lambdaInstance = declaredConstructor.newInstance();

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

}
