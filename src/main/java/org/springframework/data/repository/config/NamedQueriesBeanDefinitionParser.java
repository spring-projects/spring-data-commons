/*
 * Copyright 2011-2018 the original author or authors.
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

import java.util.Properties;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} to create {@link BeanDefinition}s of {@link NamedQueries} instances looking up a
 * {@link Properties} file fom the given location.
 *
 * @author Oliver Gierke
 */
public class NamedQueriesBeanDefinitionParser implements BeanDefinitionParser {

	private static final String ATTRIBUTE = "named-queries-location";
	private final String defaultLocation;

	/**
	 * Creates a new {@link NamedQueriesBeanDefinitionParser} using the given default location.
	 *
	 * @param defaultLocation must be non-empty
	 */
	public NamedQueriesBeanDefinitionParser(String defaultLocation) {
		Assert.hasText(defaultLocation, "DefaultLocation must not be null nor empty!");
		this.defaultLocation = defaultLocation;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
	 */
	@Nonnull
	public BeanDefinition parse(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder properties = BeanDefinitionBuilder.rootBeanDefinition(PropertiesFactoryBean.class);
		properties.addPropertyValue("locations", getDefaultedLocation(element));

		if (isDefaultLocation(element)) {
			properties.addPropertyValue("ignoreResourceNotFound", true);
		}

		AbstractBeanDefinition propertiesDefinition = properties.getBeanDefinition();
		propertiesDefinition.setSource(parserContext.extractSource(element));

		BeanDefinitionBuilder namedQueries = BeanDefinitionBuilder.rootBeanDefinition(PropertiesBasedNamedQueries.class);
		namedQueries.addConstructorArgValue(propertiesDefinition);

		AbstractBeanDefinition namedQueriesDefinition = namedQueries.getBeanDefinition();
		namedQueriesDefinition.setSource(parserContext.extractSource(element));

		return namedQueriesDefinition;
	}

	/**
	 * Returns whether we should use the default location.
	 *
	 * @param element
	 * @return
	 */
	private boolean isDefaultLocation(Element element) {
		return !StringUtils.hasText(element.getAttribute(ATTRIBUTE));
	}

	/**
	 * Returns the location to look for {@link Properties} if configured or the default one if not.
	 *
	 * @param element
	 * @return
	 */
	private String getDefaultedLocation(Element element) {

		String locations = element.getAttribute(ATTRIBUTE);
		return StringUtils.hasText(locations) ? locations : defaultLocation;
	}
}
