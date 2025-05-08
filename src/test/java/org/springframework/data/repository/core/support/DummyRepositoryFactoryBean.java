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
package org.springframework.data.repository.core.support;

import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.repository.Repository;

/**
 * @author Oliver Gierke
 */
public class DummyRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends RepositoryFactoryBeanSupport<T, S, ID> {

	private final T repository;
	private RepositoryFragmentsContributor repositoryFragmentsContributor = RepositoryFragmentsContributor.empty();

	public DummyRepositoryFactoryBean(Class<? extends T> repositoryInterface) {

		super(repositoryInterface);

		this.repository = mock(repositoryInterface);
		setMappingContext(new SampleMappingContext());
	}

	public T getRepository() {
		return repository;
	}

	@Override
	public RepositoryFragmentsContributor getRepositoryFragmentsContributor() {
		return repositoryFragmentsContributor;
	}

	public void setRepositoryFragmentsContributor(RepositoryFragmentsContributor repositoryFragmentsContributor) {
		this.repositoryFragmentsContributor = repositoryFragmentsContributor;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		return new DummyRepositoryFactory(repository);
	}
}
