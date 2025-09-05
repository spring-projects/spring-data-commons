/*
 * Copyright 2017-2025 the original author or authors.
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

import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.util.Assert;

/**
 * Factory bean for creation of {@link RepositoryFragments}. This {@link FactoryBean} uses named
 * {@link #RepositoryFragmentsFactoryBean(List) bean references} to look up {@link RepositoryFragment} beans and
 * construct {@link RepositoryFragments}.
 *
 * @author Mark Paluch
 * @since 2.0
 */
public class RepositoryFragmentsFactoryBean<T>
		implements FactoryBean<RepositoryFragments>, BeanFactoryAware, InitializingBean {

	private final List<String> fragmentBeanNames;

	private @Nullable BeanFactory beanFactory;
	private RepositoryFragments repositoryFragments = RepositoryFragments.empty();

	/**
	 * Creates a new {@link RepositoryFragmentsFactoryBean} given {@code fragmentBeanNames}.
	 *
	 * @param fragmentBeanNames must not be {@literal null}.
	 */
	@SuppressWarnings("null")
	public RepositoryFragmentsFactoryBean(List<String> fragmentBeanNames) {

		Assert.notNull(fragmentBeanNames, "Fragment bean names must not be null");
		this.fragmentBeanNames = fragmentBeanNames;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void afterPropertiesSet() {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		List<RepositoryFragment<?>> fragments = (List) fragmentBeanNames.stream() //
				.map(it -> beanFactory.getBean(it, RepositoryFragment.class)) //
				.collect(Collectors.toList());

		this.repositoryFragments = RepositoryFragments.from(fragments);
	}

	@Override
	public @NonNull RepositoryFragments getObject() {
		return this.repositoryFragments;
	}

	@Override
	public @NonNull Class<?> getObjectType() {
		return RepositoryComposition.class;
	}

}
