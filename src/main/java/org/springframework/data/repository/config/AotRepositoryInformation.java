/*
 * Copyright 2022. the original author or authors.
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
package org.springframework.data.repository.config;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformationSupport;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.Lazy;

/**
 * {@link RepositoryInformation} based on {@link RepositoryMetadata} collected at build time.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
class AotRepositoryInformation extends RepositoryInformationSupport implements RepositoryInformation {

	private final @Nullable String moduleName;
	private final Supplier<Collection<RepositoryFragment<?>>> fragments;

	private final Lazy<RepositoryComposition> repositoryComposition;
	private final Lazy<RepositoryComposition> baseComposition;

	AotRepositoryInformation(@Nullable String moduleName, Supplier<RepositoryMetadata> repositoryMetadata,
			Supplier<Class<?>> repositoryBaseClass, Supplier<Collection<RepositoryFragment<?>>> fragments) {

		super(repositoryMetadata, repositoryBaseClass);

		this.moduleName = moduleName;
		this.fragments = fragments;

		this.repositoryComposition = Lazy
				.of(() -> RepositoryComposition.fromMetadata(getMetadata()).append(RepositoryFragments.from(getFragments())));

		this.baseComposition = Lazy.of(() -> {

			RepositoryComposition targetRepoComposition = repositoryComposition.get();

			return RepositoryComposition.of(RepositoryFragment.structural(getRepositoryBaseClass())) //
					.withArgumentConverter(targetRepoComposition.getArgumentConverter()) //
					.withMethodLookup(targetRepoComposition.getMethodLookup());
		});
	}

	/**
	 * @return configured repository fragments.
	 * @since 3.0
	 */
	@Override
	public Set<RepositoryFragment<?>> getFragments() {
		return new LinkedHashSet<>(fragments.get());
	}

	@Override
	public boolean isCustomMethod(Method method) {
		return repositoryComposition.get().findMethod(method).isPresent();
	}

	@Override
	public boolean isBaseClassMethod(Method method) {
		return baseComposition.get().findMethod(method).isPresent();
	}

	@Override
	public Method getTargetClassMethod(Method method) {
		return baseComposition.get().findMethod(method).orElse(method);
	}

	@Override
	public RepositoryComposition getRepositoryComposition() {
		return repositoryComposition.get();
	}

	@Override
	public @Nullable String moduleName() {
		return moduleName;
	}
}
