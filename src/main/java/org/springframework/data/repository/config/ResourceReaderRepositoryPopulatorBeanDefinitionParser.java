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

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.data.repository.init.Jackson2RepositoryPopulatorFactoryBean;
import org.springframework.data.repository.init.UnmarshallerRepositoryPopulatorFactoryBean;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} to parse repository initializers.
 *
 * @author Oliver Gierke
 */
public class ResourceReaderRepositoryPopulatorBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser#getBeanClassName(org.w3c.dom.Element)
	 */
	@Nonnull
	@Override
	protected String getBeanClassName(Element element) {

		String name = element.getLocalName();

		if ("unmarshaller-populator".equals(name)) {
			return UnmarshallerRepositoryPopulatorFactoryBean.class.getName();
		} else if ("jackson2-populator".equals(name)) {
			return Jackson2RepositoryPopulatorFactoryBean.class.getName();
		}

		throw new IllegalStateException("Unsupported populator type " + name + "!");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser#doParse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext, org.springframework.beans.factory.support.BeanDefinitionBuilder)
	 */
	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {

		String localName = element.getLocalName();

		builder.addPropertyValue("resources", element.getAttribute("locations"));

		if ("unmarshaller-populator".equals(localName)) {
			parseXmlPopulator(element, builder);
		} else if (Arrays.asList("jackson-populator", "jackson2-populator").contains(localName)) {
			parseJsonPopulator(element, builder);
		}
	}

	/**
	 * Populates the {@link BeanDefinitionBuilder} for a Jackson reader.
	 *
	 * @param element
	 * @param builder
	 */
	private static void parseJsonPopulator(Element element, BeanDefinitionBuilder builder) {

		String objectMapperRef = element.getAttribute("object-mapper-ref");

		if (StringUtils.hasText(objectMapperRef)) {
			builder.addPropertyReference("mapper", objectMapperRef);
		}
	}

	/**
	 * Populate the {@link BeanDefinitionBuilder} for XML reader.
	 *
	 * @param element
	 * @param builder
	 */
	private static void parseXmlPopulator(Element element, BeanDefinitionBuilder builder) {

		String unmarshallerRefName = element.getAttribute("unmarshaller-ref");

		if (StringUtils.hasText(unmarshallerRefName)) {
			builder.addPropertyReference("unmarshaller", unmarshallerRefName);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#shouldGenerateIdAsFallback()
	 */
	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}
}
