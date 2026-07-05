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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragmentsContributor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Reader used to extract {@link RepositoryInformation} from {@link RepositoryConfiguration}.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @author Mark Paluch
 * @since 3.0
 */
class RepositoryBeanDefinitionReader {

	private final RootBeanDefinition beanDefinition;
	private final ConfigurableListableBeanFactory beanFactory;
	private final ClassLoader beanClassLoader;
	private final @Nullable RepositoryConfiguration<?> configuration;
	private final @Nullable RepositoryConfigurationExtensionSupport extension;

	public RepositoryBeanDefinitionReader(RegisteredBean bean) {

		this.beanDefinition = bean.getMergedBeanDefinition();
		this.beanFactory = bean.getBeanFactory();
		this.beanClassLoader = bean.getBeanClass().getClassLoader();
		this.configuration = (RepositoryConfiguration<?>) beanDefinition
				.getAttribute(RepositoryConfiguration.class.getName());
		this.extension = (RepositoryConfigurationExtensionSupport) beanDefinition
				.getAttribute(RepositoryConfigurationExtension.class.getName());
	}

	public @Nullable RepositoryConfiguration<?> getConfiguration() {
		return this.configuration;
	}

	public @Nullable RepositoryConfigurationExtensionSupport getConfigurationExtension() {
		return this.extension;
	}

	/**
	 * @return the {@link RepositoryInformation} derived from the repository bean.
	 */
	public RepositoryInformation getRepositoryInformation() {

		Assert.notNull(configuration, "Configuration must not be null");

		RepositoryMetadata metadata = AbstractRepositoryMetadata
				.getMetadata(forName(configuration.getRepositoryInterface()));
		Class<?> repositoryBaseClass = getRepositoryBaseClass();

		List<RepositoryFragment<?>> fragments = new ArrayList<>();
		fragments.addAll(readRepositoryFragments());
		fragments.addAll(readContributedRepositoryFragments(metadata));

		RepositoryFragment<?> customImplementation = getCustomImplementation();
		if (customImplementation != null) {
			fragments.add(0, customImplementation);
		}

		return new AotRepositoryInformation(metadata, repositoryBaseClass, fragments);
	}

	private @Nullable RepositoryFragment<?> getCustomImplementation() {

		PropertyValues mpv = beanDefinition.getPropertyValues();
		PropertyValue customImplementation = mpv.getPropertyValue("customImplementation");

		if (customImplementation != null) {

			if (customImplementation.getValue() instanceof RuntimeBeanReference rbr) {
				BeanDefinition customImplementationBean = beanFactory.getBeanDefinition(rbr.getBeanName());
				Class<?> beanType = getClass(customImplementationBean);
				return RepositoryFragment.structural(beanType);
			} else if (customImplementation.getValue() instanceof BeanDefinition bd) {
				Class<?> beanType = getClass(bd);
				return RepositoryFragment.structural(beanType);
			}
		}

		return null;
	}

	@SuppressWarnings("NullAway")
	private Class<?> getRepositoryBaseClass() {

		Object repoBaseClassName = beanDefinition.getPropertyValues().get("repositoryBaseClass");

		if (repoBaseClassName == null && extension != null) {
			repoBaseClassName = extension.getRepositoryBaseClassName();
		}

		if (repoBaseClassName != null) {
			return forName(repoBaseClassName.toString());
		}

		return Dummy.class;
	}

	@SuppressWarnings("NullAway")
	private List<RepositoryFragment<?>> readRepositoryFragments() {

		RuntimeBeanReference beanReference = (RuntimeBeanReference) beanDefinition.getPropertyValues()
				.get("repositoryFragments");
		BeanDefinition fragments = beanFactory.getBeanDefinition(beanReference.getBeanName());

		ValueHolder fragmentBeanNameList = fragments.getConstructorArgumentValues().getArgumentValue(0, List.class);
		List<String> fragmentBeanNames = (List<String>) fragmentBeanNameList.getValue();

		List<RepositoryFragment<?>> fragmentList = new ArrayList<>();

		for (String beanName : fragmentBeanNames) {

			BeanDefinition fragmentBeanDefinition = beanFactory.getBeanDefinition(beanName);
			ConstructorArgumentValues cv = fragmentBeanDefinition.getConstructorArgumentValues();
			ValueHolder interfaceClassVh = cv.getArgumentValue(0, String.class);
			ValueHolder implementationVh = cv.getArgumentValue(1, null, null, null);

			Object fragmentClassName = interfaceClassVh.getValue();
			Class<?> interfaceClass = forName(fragmentClassName.toString());

			if (implementationVh != null && implementationVh.getValue() instanceof RuntimeBeanReference rbf) {
				BeanDefinition implBeanDef = beanFactory.getBeanDefinition(rbf.getBeanName());
				Class<?> implClass = getClass(implBeanDef);
				fragmentList.add(RepositoryFragment.structural(interfaceClass, implClass));
			} else {
				fragmentList.add(RepositoryFragment.structural(interfaceClass));
			}
		}

		return fragmentList;
	}

	private List<RepositoryFragment<?>> readContributedRepositoryFragments(RepositoryMetadata metadata) {

		RepositoryFragmentsContributor contributor = getFragmentsContributor(metadata.getRepositoryInterface());
		return contributor.describe(metadata).stream().toList();
	}

	private RepositoryFragmentsContributor getFragmentsContributor(Class<?> repositoryInterface) {

		Object repositoryFragmentsContributor = beanDefinition.getPropertyValues().get("repositoryFragmentsContributor");

		if (repositoryFragmentsContributor instanceof BeanDefinition bd) {
			return (RepositoryFragmentsContributor) BeanUtils.instantiateClass(getClass(bd));
		}

		Assert.state(beanDefinition.getBeanClassName() != null, "No Repository BeanFactory set");

		Class<?> repositoryFactoryBean = forName(beanDefinition.getBeanClassName());
		Constructor<?> constructor = org.springframework.data.util.ClassUtils
				.getDeclaredConstructorIfAvailable(repositoryFactoryBean, Class.class);

		if (constructor == null) {
			throw new IllegalStateException("No constructor accepting Class in " + repositoryFactoryBean.getName());
		}
		RepositoryFactoryBeanSupport<?, ?, ?> factoryBean = (RepositoryFactoryBeanSupport<?, ?, ?>) BeanUtils
				.instantiateClass(constructor, repositoryInterface);

		return factoryBean.getRepositoryFragmentsContributor();
	}

	private Class<?> getClass(BeanDefinition definition) {

		String beanClassName = definition.getBeanClassName();

		if (ObjectUtils.isEmpty(beanClassName)) {
			throw new IllegalStateException("No bean class name specified for %s".formatted(definition));
		}

		return forName(beanClassName);
	}

	static abstract class Dummy implements CrudRepository<Object, Object>, PagingAndSortingRepository<Object, Object> {}

	private Class<?> forName(String name) {
		try {
			return ClassUtils.forName(name, beanClassLoader);
		} catch (ClassNotFoundException cause) {
			throw new TypeNotPresentException(name, cause);
		}
	}
}
