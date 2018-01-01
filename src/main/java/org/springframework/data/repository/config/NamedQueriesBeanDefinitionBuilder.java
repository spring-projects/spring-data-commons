/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Builder to create a {@link BeanDefinition} for a {@link NamedQueries} instance.
 *
 * @author Oliver Gierke
 */
public class NamedQueriesBeanDefinitionBuilder {

	private final String defaultLocation;
	private String locations;

	/**
	 * Creates a new {@link NamedQueriesBeanDefinitionBuilder} using the given default location.
	 *
	 * @param defaultLocation must not be {@literal null} or empty.
	 */
	@SuppressWarnings("null")
	public NamedQueriesBeanDefinitionBuilder(String defaultLocation) {

		Assert.hasText(defaultLocation, "DefaultLocation must not be null nor empty!");
		this.defaultLocation = defaultLocation;
	}

	/**
	 * Sets the (comma-separated) locations to load the properties files from to back the {@link NamedQueries} instance.
	 *
	 * @param locations must not be {@literal null} or empty.
	 */
	public void setLocations(String locations) {

		Assert.hasText(locations, "Locations must not be null nor empty!");

		this.locations = locations;
	}

	/**
	 * Builds a new {@link BeanDefinition} from the given source.
	 *
	 * @param source
	 * @return
	 */
	public BeanDefinition build(@Nullable Object source) {

		BeanDefinitionBuilder properties = BeanDefinitionBuilder.rootBeanDefinition(PropertiesFactoryBean.class);

		String locationsToUse = StringUtils.hasText(locations) ? locations : defaultLocation;
		properties.addPropertyValue("locations", locationsToUse);

		if (!StringUtils.hasText(locations)) {
			properties.addPropertyValue("ignoreResourceNotFound", true);
		}

		AbstractBeanDefinition propertiesDefinition = properties.getBeanDefinition();
		propertiesDefinition.setSource(source);

		BeanDefinitionBuilder namedQueries = BeanDefinitionBuilder.rootBeanDefinition(PropertiesBasedNamedQueries.class);
		namedQueries.addConstructorArgValue(propertiesDefinition);

		AbstractBeanDefinition namedQueriesDefinition = namedQueries.getBeanDefinition();
		namedQueriesDefinition.setSource(source);

		return namedQueriesDefinition;
	}
}
