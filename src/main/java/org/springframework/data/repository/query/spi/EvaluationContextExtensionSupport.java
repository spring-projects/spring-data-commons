/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.query.spi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * A base class for {@link EvaluationContextExtension}s.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @see 1.9
 */
public abstract class EvaluationContextExtensionSupport implements EvaluationContextExtension {

	private final Map<String, Object> declaredProperties;
	private final Map<String, Method> declaredFunctions;

	/**
	 * Creates a new {@link EvaluationContextExtensionSupport}.
	 */
	public EvaluationContextExtensionSupport() {

		this.declaredProperties = discoverDeclaredProperties();
		this.declaredFunctions = discoverDeclaredFunctions();
	}

	private Map<String, Object> discoverDeclaredProperties() {

		final Map<String, Object> map = new HashMap<String, Object>();

		ReflectionUtils.doWithFields(getClass(), new FieldCallback() {

			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

				if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
					map.put(field.getName(), field.get(null));
				}
			}
		});

		return map.isEmpty() ? Collections.<String, Object> emptyMap() : Collections.unmodifiableMap(map);
	}

	private Map<String, Method> discoverDeclaredFunctions() {

		final Map<String, Method> map = new HashMap<String, Method>();

		ReflectionUtils.doWithMethods(getClass(), new MethodCallback() {

			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

				if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
					map.put(method.getName(), method);
				}
			}
		});

		return map.isEmpty() ? Collections.<String, Method> emptyMap() : Collections.unmodifiableMap(map);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.EvaluationContextExtension#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		return this.declaredProperties;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.EvaluationContextExtension#getFunctions()
	 */
	@Override
	public Map<String, Method> getFunctions() {
		return this.declaredFunctions;
	}
}
