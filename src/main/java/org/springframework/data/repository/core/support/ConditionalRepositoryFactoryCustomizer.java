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

/**
 * Allows a {@link RepositoryFactoryCustomizer} or {@link TypedRepositoryFactoryCustomizer} to conditionally be applied
 * based on attributes of the {@link RepositoryFactorySupport} and {@code repositoryInterface}.
 * <p>
 * Used to selectively apply customization based on a repository interface or a specific persistence technology.
 * <p>
 * Useful for extension authors that want to contribute functionality based on the repository interface or the
 * underlying Spring Data module.
 *
 * @author Mark Paluch
 * @since 4.0
 * @see RepositoryFactoryCustomizer
 * @see TypedRepositoryFactoryCustomizer
 */
public interface ConditionalRepositoryFactoryCustomizer {

	/**
	 * Factory method to create a conditional {@link RepositoryFactoryCustomizer} to be applied to the repository factory
	 * that implements the given {@code repositoryInterface}.
	 *
	 * @param repositoryInterface the repository interface for which the customizer should be applied.
	 * @param customizer the {@code RepositoryFactoryCustomizer} to apply.
	 * @return a new {@link TypedRepositoryFactoryCustomizer} instance that customizes the repository factory used to
	 *         implement the given {@code repositoryInterface}.
	 */
	static RepositoryFactoryCustomizer forRepository(Class<?> repositoryInterface,
			RepositoryFactoryCustomizer customizer) {

		interface ActualConditionalRepositoryFactoryCustomizer
				extends ConditionalRepositoryFactoryCustomizer, RepositoryFactoryCustomizer {}

		return new ActualConditionalRepositoryFactoryCustomizer() {

			@Override
			public boolean canCustomize(RepositoryFactorySupport repositoryFactory, Class<?> actualRepositoryInterface) {
				return actualRepositoryInterface.equals(repositoryInterface);
			}

			@Override
			public void customize(RepositoryFactorySupport repositoryFactory) {
				customizer.customize(repositoryFactory);
			}
		};
	}

	/**
	 * Should the customization of {@link RepositoryFactorySupport} and {@code repositoryInterface} currently under
	 * consideration be applied?
	 *
	 * @param repositoryFactory the repository factory that should be customized.
	 * @param repositoryInterface the repository interface for which this customization should be applied.
	 * @return {@literal true} if customization should be performed; {@literal false} otherwise.
	 */
	boolean canCustomize(RepositoryFactorySupport repositoryFactory, Class<?> repositoryInterface);

}
