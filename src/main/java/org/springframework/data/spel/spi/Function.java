/*
 * Copyright 2014-2024 the original author or authors.
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
package org.springframework.data.spel.spi;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.util.ParameterTypes;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object to represent a function. Can either be backed by a static {@link Method} invocation (see
 * {@link #Function(Method)}) or a method invocation on an instance (see {@link #Function(Method, Object)}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Johannes Englmeier
 * @since 1.9
 */
public class Function {

	private final Method method;
	private final @Nullable Object target;

	/**
	 * Creates a new {@link Function} to statically invoke the given {@link Method}.
	 *
	 * @param method
	 */
	public Function(Method method) {

		this(method, null);

		Assert.isTrue(Modifier.isStatic(method.getModifiers()), "Method must be static");
	}

	/**
	 * Creates a new {@link Function} for the given method on the given target instance.
	 *
	 * @param method must not be {@literal null}.
	 * @param target can be {@literal null}, if so, the method
	 */
	public Function(Method method, @Nullable Object target) {

		Assert.notNull(method, "Method must not be null");
		Assert.isTrue(target != null || Modifier.isStatic(method.getModifiers()),
				"Method must either be static or a non-static one with a target object");

		this.method = method;
		this.target = target;
	}

	/**
	 * Invokes the function with the given arguments.
	 *
	 * @param arguments must not be {@literal null}.
	 * @return
	 * @throws Exception
	 */
	public Object invoke(Object[] arguments) throws Exception {

		if (method.getParameterCount() == arguments.length) {
			return method.invoke(target, arguments);
		}

		Class<?>[] types = method.getParameterTypes();
		Class<?> tailType = types[types.length - 1];

		if (tailType.isArray()) {

			List<Object> argumentsToUse = new ArrayList<>(types.length);

			// Add all arguments up until the last one
			for (int i = 0; i < types.length - 1; i++) {
				argumentsToUse.add(arguments[i]);
			}

			// Gather all other arguments into an array of the tail type
			Object[] varargs = (Object[]) Array.newInstance(tailType.getComponentType(), arguments.length - types.length + 1);
			int count = 0;

			for (int i = types.length - 1; i < arguments.length; i++) {
				varargs[count++] = arguments[i];
			}

			argumentsToUse.add(varargs);

			return method.invoke(target, argumentsToUse.size() == 1 ? argumentsToUse.get(0) : argumentsToUse.toArray());
		}

		throw new IllegalStateException(String.format("Could not invoke method %s for arguments %s", method, arguments));
	}

	/**
	 * Returns the name of the function.
	 *
	 * @return
	 */
	public String getName() {
		return method.getName();
	}

	/**
	 * Returns the type declaring the {@link Function}.
	 *
	 * @return
	 */
	public Class<?> getDeclaringClass() {
		return method.getDeclaringClass();
	}

	/**
	 * Returns {@literal true} if the function can be called with the given {@code argumentTypes}.
	 *
	 * @param argumentTypes
	 * @return
	 */
	public boolean supports(List<TypeDescriptor> argumentTypes) {
		return ParameterTypes.of(argumentTypes).areValidFor(method);
	}

	/**
	 * Returns the number of parameters required by the underlying method.
	 *
	 * @return
	 */
	public int getParameterCount() {
		return method.getParameterCount();
	}

	/**
	 * Checks if the encapsulated method has exactly the argument types as those passed as an argument.
	 *
	 * @param argumentTypes a list of {@link TypeDescriptor}s to compare with the argument types of the method
	 * @return {@literal true} if the types are equal, {@literal false} otherwise.
	 */
	public boolean supportsExact(List<TypeDescriptor> argumentTypes) {
		return ParameterTypes.of(argumentTypes).exactlyMatchParametersOf(method);
	}

	/**
	 * Checks whether this {@code Function} has the same signature as another {@code Function}.
	 *
	 * @param other the {@code Function} to compare {@code this} with.
	 * @return {@literal true} if name and argument list are the same.
	 */
	public boolean isSignatureEqual(Function other) {

		return getName().equals(other.getName()) //
				&& method.getParameterCount() == other.method.getParameterCount()
				&& Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes());
	}
}
