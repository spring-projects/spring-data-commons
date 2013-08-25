/*
 * Copyright 2008-2013 the original author or authors.
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

import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * Base class to implement repository namespaces. These will typically consist of a main XML element potentially having
 * child elements. The parser will wrap the XML element into a {@link GlobalRepositoryConfigInformation} object and
 * allow either manual configuration or automatic detection of repository interfaces.
 * 
 * @author Oliver Gierke
 */
public class RepositoryBeanDefinitionParser implements BeanDefinitionParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryBeanDefinitionParser.class);

	private final RepositoryConfigurationExtension extension;

	/**
	 * Creates a new {@link RepositoryBeanDefinitionParser} using the given {@link RepositoryConfigurationExtension}.
	 * 
	 * @param extension must not be {@literal null}.
	 */
	public RepositoryBeanDefinitionParser(RepositoryConfigurationExtension extension) {

		Assert.notNull(extension);
		this.extension = extension;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
	 */
	public BeanDefinition parse(Element element, ParserContext parser) {

		try {

			Environment environment = parser.getDelegate().getEnvironment();
			XmlRepositoryConfigurationSource configSource = new XmlRepositoryConfigurationSource(element, parser, environment);

			for (RepositoryConfiguration<XmlRepositoryConfigurationSource> config : extension.getRepositoryConfigurations(
					configSource, parser.getReaderContext().getResourceLoader())) {
				registerGenericRepositoryFactoryBean(config, parser);
			}

			extension.registerBeansForRoot(parser.getRegistry(), configSource);

		} catch (RuntimeException e) {
			handleError(e, element, parser.getReaderContext());
		}

		return null;
	}

	private void handleError(Exception e, Element source, ReaderContext reader) {
		reader.error(e.getMessage(), reader.extractSource(source), e);
	}

	/**
	 * Registers a generic repository factory bean for a bean with the given name and the provided configuration context.
	 * 
	 * @param parser
	 * @param name
	 * @param context
	 */
	private void registerGenericRepositoryFactoryBean(
			RepositoryConfiguration<XmlRepositoryConfigurationSource> configuration, ParserContext parser) {

		RepositoryBeanDefinitionBuilder definitionBuilder = new RepositoryBeanDefinitionBuilder(configuration, extension);

		try {

			BeanDefinitionBuilder builder = definitionBuilder.build(parser.getRegistry(), parser.getReaderContext()
					.getResourceLoader());

			extension.postProcess(builder, configuration.getConfigurationSource());

			AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
			beanDefinition.setSource(configuration.getSource());

			RepositoryBeanNameGenerator generator = new RepositoryBeanNameGenerator();
			generator.setBeanClassLoader(parser.getReaderContext().getBeanClassLoader());

			String beanName = generator.generateBeanName(beanDefinition, parser.getRegistry());

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Registering repository: " + beanName + " - Interface: " + configuration.getRepositoryInterface()
						+ " - Factory: " + extension.getRepositoryFactoryClassName());
			}

			BeanComponentDefinition definition = new BeanComponentDefinition(beanDefinition, beanName);
			parser.registerBeanComponent(definition);
		} catch (RuntimeException e) {
			handleError(e, configuration.getConfigurationSource().getElement(), parser.getReaderContext());
		}
	}

	/**
	 * Returns whether the given {@link BeanDefinitionRegistry} already contains a bean of the given type assuming the
	 * bean name has been autogenerated.
	 * 
	 * @param type
	 * @param registry
	 * @return
	 */
	protected static boolean hasBean(Class<?> type, BeanDefinitionRegistry registry) {

		String name = String.format("%s%s0", type.getName(), GENERATED_BEAN_NAME_SEPARATOR);
		return registry.containsBeanDefinition(name);
	}
}
