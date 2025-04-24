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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragment.ImplementedRepositoryFragment;
import org.springframework.util.ClassUtils;

/**
 * Reader used to extract {@link RepositoryInformation} from {@link RepositoryConfiguration}.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @since 3.0
 */
class RepositoryBeanDefinitionReader {

	/**
	 * @return
	 */
	static RepositoryInformation repositoryInformation(RepositoryConfiguration<?> repoConfig, RegisteredBean repoBean) {
		return repositoryInformation(repoConfig, repoBean.getMergedBeanDefinition(), repoBean.getBeanFactory());
	}

	/**
	 * @param source the RepositoryFactoryBeanSupport bean definition.
	 * @param beanFactory
	 * @return
	 */
	@SuppressWarnings("NullAway")
	static RepositoryInformation repositoryInformation(RepositoryConfiguration<?> repoConfig, BeanDefinition source,
			ConfigurableListableBeanFactory beanFactory) {

		RepositoryMetadata metadata = AbstractRepositoryMetadata
				.getMetadata(forName(repoConfig.getRepositoryInterface(), beanFactory));
		Class<?> repositoryBaseClass = readRepositoryBaseClass(source, beanFactory);
		List<RepositoryFragment<?>> fragmentList = readRepositoryFragments(source, beanFactory);
		if (source.getPropertyValues().contains("customImplementation")) {

			Object o = source.getPropertyValues().get("customImplementation");
			if (o instanceof RuntimeBeanReference rbr) {
				BeanDefinition customImplBeanDefintion = beanFactory.getBeanDefinition(rbr.getBeanName());
				Class<?> beanType = forName(customImplBeanDefintion.getBeanClassName(), beanFactory);
				ResolvableType[] interfaces = ResolvableType.forClass(beanType).getInterfaces();
				if (interfaces.length == 1) {
					fragmentList.add(new ImplementedRepositoryFragment(interfaces[0].toClass(), beanType));
				} else {
					boolean found = false;
					for (ResolvableType i : interfaces) {
						if (beanType.getSimpleName().contains(i.resolve().getSimpleName())) {
							fragmentList.add(new ImplementedRepositoryFragment(interfaces[0].toClass(), beanType));
							found = true;
							break;
						}
					}
					if (!found) {
						fragmentList.add(RepositoryFragment.implemented(beanType));
					}
				}
			}
		}

		String moduleName = (String) source.getPropertyValues().get("moduleName");
		AotRepositoryInformation repositoryInformation = new AotRepositoryInformation(moduleName, () -> metadata,
				() -> repositoryBaseClass, () -> fragmentList);
		return repositoryInformation;
	}

	@SuppressWarnings("NullAway")
	private static Class<?> readRepositoryBaseClass(BeanDefinition source, ConfigurableListableBeanFactory beanFactory) {

		Object repoBaseClassName = source.getPropertyValues().get("repositoryBaseClass");
		if (repoBaseClassName != null) {
			return forName(repoBaseClassName.toString(), beanFactory);
		}
		if (source.getPropertyValues().contains("moduleBaseClass")) {
			return forName((String) source.getPropertyValues().get("moduleBaseClass"), beanFactory);
		}
		return Dummy.class;
	}

	@SuppressWarnings("NullAway")
	private static List<RepositoryFragment<?>> readRepositoryFragments(BeanDefinition source,
			ConfigurableListableBeanFactory beanFactory) {

		RuntimeBeanReference beanReference = (RuntimeBeanReference) source.getPropertyValues().get("repositoryFragments");
		BeanDefinition fragments = beanFactory.getBeanDefinition(beanReference.getBeanName());

		ValueHolder fragmentBeanNameList = fragments.getConstructorArgumentValues().getArgumentValue(0, List.class);
		List<String> fragmentBeanNames = (List<String>) fragmentBeanNameList.getValue();

		List<RepositoryFragment<?>> fragmentList = new ArrayList<>();
		for (String beanName : fragmentBeanNames) {

			BeanDefinition fragmentBeanDefinition = beanFactory.getBeanDefinition(beanName);
			ValueHolder argumentValue = fragmentBeanDefinition.getConstructorArgumentValues().getArgumentValue(0,
					String.class);
			ValueHolder argumentValue1 = fragmentBeanDefinition.getConstructorArgumentValues().getArgumentValue(1, null, null,
					null);
			Object fragmentClassName = argumentValue.getValue();

			try {
				Class<?> type = ClassUtils.forName(fragmentClassName.toString(), beanFactory.getBeanClassLoader());

				if (argumentValue1 != null && argumentValue1.getValue() instanceof RuntimeBeanReference rbf) {
					BeanDefinition implBeanDef = beanFactory.getBeanDefinition(rbf.getBeanName());
					Class implClass = ClassUtils.forName(implBeanDef.getBeanClassName(), beanFactory.getBeanClassLoader());
					fragmentList.add(new RepositoryFragment.ImplementedRepositoryFragment(type, implClass));
				} else {
					fragmentList.add(RepositoryFragment.structural(type));
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return fragmentList;
	}

	static abstract class Dummy implements CrudRepository<Object, Object>, PagingAndSortingRepository<Object, Object> {}

	static Class<?> forName(String name, ConfigurableListableBeanFactory beanFactory) {
		try {
			return ClassUtils.forName(name, beanFactory.getBeanClassLoader());
		} catch (ClassNotFoundException cause) {
			throw new TypeNotPresentException(name, cause);
		}
	}
}
