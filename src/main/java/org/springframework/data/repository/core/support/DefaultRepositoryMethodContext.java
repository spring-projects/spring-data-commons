/*
 * Copyright 2024 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.RepositoryMethodContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Class containing value objects providing information about the current repository method invocation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @since 3.4.0
 */
public class DefaultRepositoryMethodContext implements RepositoryMethodContext {

	/**
	 * ThreadLocal holder for repository method associated with this thread. Will contain {@code null} unless the
	 * "exposeMetadata" property on the controlling repository factory configuration has been set to "true".
	 */
	private static final ThreadLocal<RepositoryMethodContext> currentMethod = new NamedThreadLocal<>(
			"Current Repository Method");

	private final RepositoryMetadata repositoryMetadata;
	private final Method method;

	DefaultRepositoryMethodContext(RepositoryMetadata repositoryMetadata, Method method) {

		this.repositoryMetadata = repositoryMetadata;
		this.method = method;
	}

	/**
	 * Creates a new {@link RepositoryMethodContext} for the given {@link Method}.
	 *
	 * @param method must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static RepositoryMethodContext forMethod(Method method) {

		Assert.notNull(method, "Method must not be null!");

		return new DefaultRepositoryMethodContext(AbstractRepositoryMetadata.getMetadata(method.getDeclaringClass()),
				method);
	}

	/**
	 * Creates a proxy {@link RepositoryMethodContext} instance suitable for dependency injection.
	 *
	 * @return will never be {@literal null}.
	 */
	public static RepositoryMethodContext getInjectionProxy() {

		return ProxyFactory.getProxy(RepositoryMethodContext.class,
				new DynamicLookupTargetSource<>(RepositoryMethodContext.class, () -> getInstance()));
	}

	@Nullable
	static RepositoryMethodContext getInstance() {
		return currentMethod.get();
	}

	@Nullable
	static RepositoryMethodContext setMetadata(@Nullable RepositoryMethodContext metadata) {

		RepositoryMethodContext old = currentMethod.get();

		if (metadata != null) {
			currentMethod.set(metadata);
		} else {
			currentMethod.remove();
		}

		return old;
	}

	@Override
	public RepositoryMetadata getMetadata() {
		return repositoryMetadata;
	}

	@Override
	public Method getMethod() {
		return method;
	}
}
