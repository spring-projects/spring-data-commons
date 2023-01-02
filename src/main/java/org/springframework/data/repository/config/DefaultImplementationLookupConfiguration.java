/*
 * Copyright 2018-2023 the original author or authors.
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

import java.io.IOException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link ImplementationLookupConfiguration}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Kyrylo Merzlikin
 * @since 2.1
 */
class DefaultImplementationLookupConfiguration implements ImplementationLookupConfiguration {

	private final ImplementationDetectionConfiguration config;
	private final String interfaceName;
	private final String beanName;

	DefaultImplementationLookupConfiguration(ImplementationDetectionConfiguration config, String interfaceName,
			String beanName) {

		Assert.notNull(config, "ImplementationDetectionConfiguration must not be null");
		Assert.hasText(interfaceName, "Interface name must not be null or empty");
		Assert.hasText(beanName, "Bean name must not be null or empty");

		this.config = config;
		this.interfaceName = interfaceName;
		this.beanName = beanName;
	}

	@Override
	public String getImplementationBeanName() {
		return beanName;
	}

	@Override
	public String getImplementationPostfix() {
		return config.getImplementationPostfix();
	}

	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return config.getExcludeFilters().and(new AnnotationTypeFilter(NoRepositoryBean.class));
	}

	@Override
	public MetadataReaderFactory getMetadataReaderFactory() {
		return config.getMetadataReaderFactory();
	}

	@Override
	public Streamable<String> getBasePackages() {
		return Streamable.of(ClassUtils.getPackageName(interfaceName));
	}

	@Override
	public String getImplementationClassName() {
		return getLocalName(interfaceName).concat(getImplementationPostfix());
	}

	@Override
	public boolean hasMatchingBeanName(BeanDefinition definition) {

		Assert.notNull(definition, "BeanDefinition must not be null");

		return beanName != null && beanName.equals(config.generateBeanName(definition));
	}

	@Override
	public boolean matches(BeanDefinition definition) {

		Assert.notNull(definition, "BeanDefinition must not be null");

		String beanClassName = definition.getBeanClassName();

		if (beanClassName == null || isExcluded(beanClassName, getExcludeFilters())) {
			return false;
		}

		String beanPackage = ClassUtils.getPackageName(beanClassName);
		String localName = getLocalName(beanClassName);

		return localName.equals(getImplementationClassName()) //
				&& getBasePackages().stream().anyMatch(beanPackage::startsWith);
	}

	private String getLocalName(String className) {

		String shortName = ClassUtils.getShortName(className);
		return shortName.substring(shortName.lastIndexOf('.') + 1);
	}

	private boolean isExcluded(String beanClassName, Streamable<TypeFilter> filters) {

		try {

			MetadataReader reader = getMetadataReaderFactory().getMetadataReader(beanClassName);
			return filters.stream().anyMatch(it -> matches(it, reader));
		} catch (IOException o_O) {
			return true;
		}
	}

	private boolean matches(TypeFilter filter, MetadataReader reader) {

		try {
			return filter.match(reader, getMetadataReaderFactory());
		} catch (IOException e) {
			return false;
		}
	}
}
