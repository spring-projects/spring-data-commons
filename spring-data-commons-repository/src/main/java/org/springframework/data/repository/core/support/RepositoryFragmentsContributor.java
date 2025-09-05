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

import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

/**
 * Strategy interface support allowing to contribute a {@link RepositoryFragments} based on {@link RepositoryMetadata}.
 * <p>
 * Fragments contributors enhance repository functionality based on a repository declaration and activate additional
 * fragments if a repository defines them, such as extending a built-in fragment interface (e.g.
 * {@code QuerydslPredicateExecutor}, {@code QueryByExampleExecutor}).
 * <p>
 * This interface is a base-interface serving as a contract for repository fragment introspection. The actual
 * implementation and methods to contribute fragments to be used within the repository instance are store-specific and
 * require typically access to infrastructure such as a database connection hence those methods must be defined within
 * the particular store module.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface RepositoryFragmentsContributor {

	/**
	 * Empty {@code RepositoryFragmentsContributor} that does not contribute any fragments.
	 *
	 * @return empty {@code RepositoryFragmentsContributor} that does not contribute any fragments.
	 */
	public static RepositoryFragmentsContributor empty() {
		return metadata -> RepositoryFragments.empty();
	}

	/**
	 * Describe fragments that are contributed by {@link RepositoryMetadata}. Fragment description reports typically
	 * structural fragments that are not suitable for invocation but can be used to introspect the repository structure.
	 *
	 * @param metadata the repository metadata describing the repository interface.
	 * @return fragments to be (structurally) contributed to the repository.
	 */
	RepositoryFragments describe(RepositoryMetadata metadata);

}
