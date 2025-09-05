/*
 * Copyright 2019-2025 the original author or authors.
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
package org.springframework.data.mapping.callback;

import java.util.function.BiFunction;

/**
 * @author Christoph Strobl
 * @since 2.2
 */
interface EntityCallbackInvoker {

	/**
	 * Invoke the actual {@link EntityCallback} for the given entity via the {@link BiFunction invoker function}.
	 *
	 * @param callback must not be {@literal null}.
	 * @param entity must not be {@literal null}
	 * @param callbackInvokerFunction must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	<T> Object invokeCallback(EntityCallback<T> callback, T entity,
			BiFunction<EntityCallback<T>, T, Object> callbackInvokerFunction);

	static boolean matchesClassCastMessage(String exceptionMessage, Class<?> eventClass) {

		// On Java 8, the message starts with the class name: "java.lang.String cannot be cast..."
		if (exceptionMessage.startsWith(eventClass.getName())) {
			return true;
		}

		// On Java 11, the message starts with "class ..." a.k.a. Class.toString()
		if (exceptionMessage.startsWith(eventClass.toString())) {
			return true;
		}

		// On Java 9, the message used to contain the module name: "java.base/java.lang.String cannot be cast..."
		int moduleSeparatorIndex = exceptionMessage.indexOf('/');
		if (moduleSeparatorIndex != -1 && exceptionMessage.startsWith(eventClass.getName(), moduleSeparatorIndex + 1)) {
			return true;
		}

		// On Java 18, the message is "IllegalArgumentException: argument type mismatch"
		if (exceptionMessage.equals("argument type mismatch")) {
			return true;
		}

		// Assuming an unrelated class cast failure...
		return false;
	}
}
