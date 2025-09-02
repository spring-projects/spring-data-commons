/*
 * Copyright 2025 the original author or authors.
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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Typed variant of {@link RepositoryFactoryCustomizer} that conditionally customizes a specific type of
 * {@link RepositoryFactorySupport} applying casting of the repository factory to the specificed type parameter for
 * easier customization.
 * <p>
 * Can be registered as bean for automatic picking up by repository factories.
 *
 * @param <R> type of the repository factory.
 * @author Mark Paluch
 * @since 4.0
 */
@FunctionalInterface
public interface TypedRepositoryFactoryCustomizer<R extends RepositoryFactorySupport>
		extends ConditionalRepositoryFactoryCustomizer {

	@Override
	default boolean canCustomize(RepositoryFactorySupport repositoryFactory, Class<?> repositoryInterface) {

		ResolvableType resolvableType = ResolvableType.forType(getClass()).as(TypedRepositoryFactoryCustomizer.class);
		Assert.isInstanceOf(ParameterizedType.class, resolvableType.getType(), "Type must be a parameterized type");
		ParameterizedType parameterizedType = (ParameterizedType) resolvableType.getType();
		Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
		Assert.isTrue(actualTypeArguments.length == 1, "Number of type arguments must be 1");

		return ResolvableType.forType(actualTypeArguments[0]).isInstance(repositoryFactory);
	}

	/**
	 * Callback to customize a {@link RepositoryFactorySupport} instance.
	 *
	 * @param repositoryFactory repository factory to customize.
	 */
	void customize(R repositoryFactory);

}
