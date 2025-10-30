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
package org.springframework.data.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.Assert;

/**
 * Interface providing access to the method, containing class and domain type as source object for parameter
 * descriptors.
 *
 * @author Mark Paluch
 * @since 3.2.1
 */
public interface ParametersSource {

	/**
	 * Create a new parameter source for the given {@link Method}.
	 *
	 * @param method the method to use.
	 * @return a new parameter source for the given {@link Method}.
	 */
	static ParametersSource of(Method method) {

		Assert.notNull(method, "Method must not be null");

		return new ParametersSource() {

			@Override
			public Class<?> getContainingClass() {
				return method.getDeclaringClass();
			}

			@Override
			public Method getMethod() {
				return method;
			}

			@Override
			public TypeInformation<?> getDomainTypeInformation() {
				return TypeInformation.OBJECT;
			}
		};
	}

	/**
	 * Create a new parameter source for the given {@link Method} in the context of {@link RepositoryMetadata}.
	 *
	 * @param method the method to use.
	 * @return a new parameter source for the given {@link Method}.
	 */
	static ParametersSource of(RepositoryMetadata metadata, Method method) {

		Assert.notNull(metadata, "RepositoryMetadata must not be null");
		Assert.notNull(method, "Method must not be null");

		return new ParametersSource() {

			@Override
			public Class<?> getContainingClass() {
				return metadata.getRepositoryInterface();
			}

			@Override
			public Method getMethod() {
				return method;
			}

			@Override
			public TypeInformation<?> getDomainTypeInformation() {
				return metadata.getDomainTypeInformation();
			}
		};
	}

	/**
	 * @return the class that contains the {@link #getMethod()}.
	 */
	Class<?> getContainingClass();

	/**
	 * @return the method for which the parameter source is defined.
	 */
	Method getMethod();

	/**
	 * @return the domain type associated with the parameter source.
	 */
	TypeInformation<?> getDomainTypeInformation();

}
