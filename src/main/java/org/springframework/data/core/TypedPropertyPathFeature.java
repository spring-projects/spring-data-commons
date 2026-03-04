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

	/**
	 * Token indicating a class is or is not a lambda.
	 */
	private static final String LAMBDA_CLASS_MARKER = "$$Lambda";

	/**
	 * The offset from {@link #LAMBDA_CLASS_MARKER} where the end marker is found.
	 */
	private static final int LAMBDA_CLASS_END_MARKER = LAMBDA_CLASS_MARKER.length();

	private final SerializableLambdaReader reader = new SerializableLambdaReader();

	@Override
	public void beforeAnalysis(BeforeAnalysisAccess access) {

		BiConsumer<DuringAnalysisAccess, Class<?>> serializableLambdaHandler = (ignore, cls) -> {

			if (isLambdaClass(cls)) {

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

	/**
	 * Return true if the specified Class represents a raw lambda.
	 *
	 * @param cls class to inspect.
	 * @return true if the class represents a raw lambda.
	 */
	public static boolean isLambdaClass(Class<?> cls) {

		String name = cls.getName();
		int marker = name.indexOf(LAMBDA_CLASS_MARKER);
		if (marker == -1) {
			return false;
		}

		int noffset = marker + LAMBDA_CLASS_END_MARKER;
		if (noffset > name.length()) {
			return false;
		}

		char c = name.charAt(noffset);

		// '$' character will be seen in releases between Java {8,20}
		// '/' is used in Java 21
		// See bug 35177243
		return c == '$' || c == '/';
	}

}
