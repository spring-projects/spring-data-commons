/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link ImplementationLookupConfiguration}.
 * 
 * @author Oliver Gierke
 * @since 2.1
 */
class DefaultImplementationLookupConfiguration implements ImplementationLookupConfiguration {

	private final ImplementationDetectionConfiguration config;
	private final String interfaceName;
	private final String beanName;

	/**
	 * Creates a new {@link DefaultImplementationLookupConfiguration} for the given
	 * {@link ImplementationDetectionConfiguration} and interface name.
	 * 
	 * @param config must not be {@literal null}.
	 * @param implementationClassName must not be {@literal null} or empty.
	 */
	DefaultImplementationLookupConfiguration(ImplementationDetectionConfiguration config, String interfaceName) {

		Assert.notNull(config, "ImplementationDetectionConfiguration must not be null!");
		Assert.hasText(interfaceName, "Interface name must not be null or empty!");

		this.config = config;
		this.interfaceName = interfaceName;
		this.beanName = StringUtils
				.uncapitalize(ClassUtils.getShortName(interfaceName).concat(config.getImplementationPostfix()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.ImplementationLookupConfiguration#getImplementationBeanName()
	 */
	@Override
	public String getImplementationBeanName() {
		return beanName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.ImplementationDetectionConfiguration#getImplementationPostfix()
	 */
	@Override
	public String getImplementationPostfix() {
		return config.getImplementationPostfix();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.ImplementationDetectionConfiguration#getExcludeFilters()
	 */
	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return config.getExcludeFilters().and(Streamable.of(new AnnotationTypeFilter(NoRepositoryBean.class)));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.ImplementationDetectionConfiguration#getMetadataReaderFactory()
	 */
	@Override
	public MetadataReaderFactory getMetadataReaderFactory() {
		return config.getMetadataReaderFactory();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.ImplementationDetectionConfiguration#getBasePackages()
	 */
	@Override
	public Streamable<String> getBasePackages() {
		return Streamable.of(ClassUtils.getPackageName(interfaceName));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.ImplementationLookupConfiguration#getImplementationClassName()
	 */
	@Override
	public String getImplementationClassName() {
		return ClassUtils.getShortName(interfaceName).concat(getImplementationPostfix());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.ImplementationLookupConfiguration#hasMatchingBeanName(org.springframework.beans.factory.config.BeanDefinition)
	 */
	@Override
	public boolean hasMatchingBeanName(BeanDefinition definition) {

		Assert.notNull(definition, "BeanDefinition must not be null!");

		return beanName != null && beanName.equals(config.generateBeanName(definition));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.ImplementationLookupConfiguration#matches(org.springframework.beans.factory.config.BeanDefinition, org.springframework.core.type.classreading.MetadataReaderFactory)
	 */
	@Override
	public boolean matches(BeanDefinition definition) {

		Assert.notNull(definition, "BeanDefinition must not be null!");

		String beanClassName = definition.getBeanClassName();

		if (beanClassName == null || isExcluded(beanClassName, getExcludeFilters())) {
			return false;
		}

		String beanPackage = ClassUtils.getPackageName(beanClassName);
		String shortName = ClassUtils.getShortName(beanClassName);
		String localName = shortName.substring(shortName.lastIndexOf('.') + 1);

		return localName.equals(getImplementationClassName()) //
				&& getBasePackages().stream().anyMatch(it -> beanPackage.startsWith(it));
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
