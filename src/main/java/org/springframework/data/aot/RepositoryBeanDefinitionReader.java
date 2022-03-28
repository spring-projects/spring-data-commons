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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.data.repository.config.RepositoryFragmentConfiguration;
import org.springframework.data.repository.config.RepositoryMetadata;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.Lazy;
import org.springframework.util.ClassUtils;

/**
 * Reader that allows to extract {@link RepositoryInformation} from metadata.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
class RepositoryBeanDefinitionReader {

	static RepositoryInformation readRepositoryInformation(RepositoryMetadata metadata,
			ConfigurableListableBeanFactory beanFactory) {

		return new AotRepositoryInformation(metadataSupplier(metadata, beanFactory),
				repositoryBaseClass(metadata, beanFactory), fragments(metadata, beanFactory));
	}

	private static Supplier<Collection<RepositoryFragment<?>>> fragments(RepositoryMetadata metadata,
			ConfigurableListableBeanFactory beanFactory) {
		return Lazy
				.of(() -> (Collection<RepositoryFragment<?>>) metadata.getFragmentConfiguration().stream().flatMap(it -> {
					RepositoryFragmentConfiguration fragmentConfiguration = (RepositoryFragmentConfiguration) it;

					List<RepositoryFragment> fragments = new ArrayList<>(2);
					if (fragmentConfiguration.getClassName() != null) {
						fragments.add(RepositoryFragment.implemented(forName(fragmentConfiguration.getClassName(), beanFactory)));
					}
					if (fragmentConfiguration.getInterfaceName() != null) {
						fragments
								.add(RepositoryFragment.structural(forName(fragmentConfiguration.getInterfaceName(), beanFactory)));
					}

					return fragments.stream();
				}).collect(Collectors.toList()));
	}

	private static Supplier<Class<?>> repositoryBaseClass(RepositoryMetadata metadata,
			ConfigurableListableBeanFactory beanFactory) {
		return Lazy.of(() -> (Class<?>) metadata.getRepositoryBaseClassName().map(it -> forName(it.toString(), beanFactory))
				.orElseGet(() -> {
					// TODO: retrieve the default without loading the actual RepositoryBeanFactory
					return Object.class;
				}));
	}

	static Supplier<org.springframework.data.repository.core.RepositoryMetadata> metadataSupplier(
			RepositoryMetadata metadata, ConfigurableListableBeanFactory beanFactory) {
		return Lazy.of(() -> new DefaultRepositoryMetadata(forName(metadata.getRepositoryInterface(), beanFactory)));
	}

	static Class<?> forName(String name, ConfigurableListableBeanFactory beanFactory) {
		try {
			return ClassUtils.forName(name, beanFactory.getBeanClassLoader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
