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
import java.util.Set;

import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryInformationSupport;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFragment;

/**
 * {@link RepositoryInformation} based on {@link RepositoryMetadata} collected at build time.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.0
 */
public class AotRepositoryInformation extends RepositoryInformationSupport implements RepositoryInformation {

	private final RepositoryComposition fragmentsComposition;
	private final RepositoryComposition baseComposition;
	private final RepositoryComposition composition;

	public AotRepositoryInformation(RepositoryMetadata repositoryMetadata, Class<?> repositoryBaseClass,
			Collection<RepositoryFragment<?>> fragments) {

		super(() -> repositoryMetadata, () -> repositoryBaseClass);

		this.fragmentsComposition = RepositoryComposition.fromMetadata(getMetadata())
				.append(RepositoryFragments.from(fragments));
		this.baseComposition = RepositoryComposition.of(RepositoryFragment.structural(getRepositoryBaseClass())) //
				.withArgumentConverter(this.fragmentsComposition.getArgumentConverter()) //
				.withMethodLookup(this.fragmentsComposition.getMethodLookup());

		this.composition = this.fragmentsComposition.append(this.baseComposition.getFragments());
	}

	/**
	 * @return configured repository fragments.
	 * @since 3.0
	 */
	@Override
	public Set<RepositoryFragment<?>> getFragments() {
		return fragmentsComposition.getFragments().toSet();
	}

	@Override
	public boolean isCustomMethod(Method method) {
		return fragmentsComposition.findMethod(method).isPresent();
	}

	@Override
	public boolean isBaseClassMethod(Method method) {
		return baseComposition.findMethod(method).isPresent();
	}

	@Override
	public Method getTargetClassMethod(Method method) {
		return baseComposition.findMethod(method).orElse(method);
	}

	@Override
	public RepositoryComposition getRepositoryComposition() {
		return composition;
	}

}
