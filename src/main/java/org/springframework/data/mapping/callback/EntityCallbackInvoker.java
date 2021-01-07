/*
 * Copyright 2019-2021 the original author or authors.
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
	 * @param <T>
	 * @return never {@literal null}.
	 */
	<T> Object invokeCallback(EntityCallback<T> callback, T entity,
			BiFunction<EntityCallback<T>, T, Object> callbackInvokerFunction);

	static boolean matchesClassCastMessage(String classCastMessage, Class<?> eventClass) {

		// On Java 8, the message starts with the class name: "java.lang.String cannot be cast..."
		if (classCastMessage.startsWith(eventClass.getName())) {
			return true;
		}

		// On Java 11, the message starts with "class ..." a.k.a. Class.toString()
		if (classCastMessage.startsWith(eventClass.toString())) {
			return true;
		}

		// On Java 9, the message used to contain the module name: "java.base/java.lang.String cannot be cast..."
		int moduleSeparatorIndex = classCastMessage.indexOf('/');
		if (moduleSeparatorIndex != -1 && classCastMessage.startsWith(eventClass.getName(), moduleSeparatorIndex + 1)) {
			return true;
		}

		// Assuming an unrelated class cast failure...
		return false;
	}
}
