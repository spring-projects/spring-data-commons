/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.springframework.util.ReflectionUtils.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformationSupport;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link RepositoryInformation}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Alessandro Nistico
 */
class DefaultRepositoryInformation extends RepositoryInformationSupport implements RepositoryInformation {

	private final Map<Method, Method> methodCache = new ConcurrentHashMap<>();

	private final RepositoryComposition composition;
	private final RepositoryComposition baseComposition;

	/**
	 * Creates a new {@link DefaultRepositoryMetadata} for the given repository interface and repository base class.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param repositoryBaseClass must not be {@literal null}.
	 * @param composition must not be {@literal null}.
	 */
	public DefaultRepositoryInformation(RepositoryMetadata metadata, Class<?> repositoryBaseClass,
			RepositoryComposition composition) {

		super(() -> metadata, () -> repositoryBaseClass);
		Assert.notNull(composition, "Repository composition must not be null");

		this.composition = composition;
		this.baseComposition = RepositoryComposition.of(RepositoryFragment.structural(repositoryBaseClass)) //
				.withArgumentConverter(composition.getArgumentConverter()) //
				.withMethodLookup(composition.getMethodLookup());
	}

	@Override
	public Method getTargetClassMethod(Method method) {

		if (methodCache.containsKey(method)) {
			return methodCache.get(method);
		}

		Method result = composition.findMethod(method).orElse(method);

		if (!result.equals(method)) {
			return cacheAndReturn(method, result);
		}

		return cacheAndReturn(method, baseComposition.findMethod(method).orElse(method));
	}

	private Method cacheAndReturn(Method key, Method value) {

		if (value != null) {
			makeAccessible(value);
		}

		methodCache.put(key, value);
		return value;
	}

	@Override
	public boolean isCustomMethod(Method method) {
		return composition.getMethod(method) != null;
	}

	@Override
	public boolean isBaseClassMethod(Method method) {

		Assert.notNull(method, "Method must not be null");
		return baseComposition.getMethod(method) != null;
	}

	@Override
	public Set<RepositoryFragment<?>> getFragments() {
		return composition.getFragments().toSet();
	}

}
