/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.aot;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformationSupport;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.lang.Nullable;

/**
 * {@link RepositoryInformation} based on {@link RepositoryMetadata} collected at build time.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
class AotRepositoryInformation extends RepositoryInformationSupport implements RepositoryInformation {

	private final Supplier<Collection<RepositoryFragment<?>>> fragments;

	AotRepositoryInformation(Supplier<RepositoryMetadata> repositoryMetadata, Supplier<Class<?>> repositoryBaseClass,
			Supplier<Collection<RepositoryFragment<?>>> fragments) {

		super(repositoryMetadata, repositoryBaseClass);
		this.fragments = fragments;
	}

	@Override
	public boolean isCustomMethod(Method method) {

		// TODO:
		return false;
	}

	@Override
	public boolean isBaseClassMethod(Method method) {
		// TODO
		return false;
	}

	@Override
	public Method getTargetClassMethod(Method method) {

		// TODO
		return method;
	}

	/**
	 * @return
	 * @since 3.0
	 */
	@Nullable
	public Set<RepositoryFragment<?>> getFragments() {
		return new LinkedHashSet<>(fragments.get());
	}

}
