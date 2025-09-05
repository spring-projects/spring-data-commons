/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.repository.init;

import java.io.Serial;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.Assert;

/**
 * {@link ApplicationEvent} being thrown after a {@link RepositoryPopulator} has finished populating the
 * {@link Repositories} available in the {@link ApplicationContext}.
 *
 * @author Oliver Gierke
 */
public class RepositoriesPopulatedEvent extends ApplicationEvent {

	private static final @Serial long serialVersionUID = 7449982118828889097L;

	private final Repositories repositories;

	/**
	 * Creates a new {@link RepositoriesPopulatedEvent} using the given {@link RepositoryPopulator} and
	 * {@link Repositories}.
	 *
	 * @param populator the {@link RepositoryPopulator} that threw the event, must not be {@literal null}.
	 * @param repositories the {@link Repositories} that were populated, must not be {@literal null}.
	 */
	public RepositoriesPopulatedEvent(RepositoryPopulator populator, Repositories repositories) {

		super(populator);

		Assert.notNull(populator, "Populator must not be null");
		Assert.notNull(repositories, "Repositories must not be null");

		this.repositories = repositories;
	}

	@Override
	public RepositoryPopulator getSource() {
		return (RepositoryPopulator) super.getSource();
	}

	/**
	 * Returns the {@link Repositories} that were populated.
	 *
	 * @return the repositories will never be {@literal null}.
	 */
	public Repositories getRepositories() {
		return repositories;
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}

		RepositoriesPopulatedEvent that = (RepositoriesPopulatedEvent) obj;
		return this.source.equals(that.source) && this.repositories.equals(that.repositories);
	}

	@Override
	public int hashCode() {

		int result = 17;
		result += 31 * source.hashCode();
		result += 31 * repositories.hashCode();

		return result;
	}
}
