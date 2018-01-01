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

import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@link NamespaceHandler} to register {@link BeanDefinitionParser}s for repository initializers.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.4
 */
public class RepositoryNameSpaceHandler extends NamespaceHandlerSupport {

	private static final BeanDefinitionParser PARSER = new ResourceReaderRepositoryPopulatorBeanDefinitionParser();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	public void init() {

		registerBeanDefinitionParser("unmarshaller-populator", PARSER);
		registerBeanDefinitionParser("jackson-populator", PARSER);
		registerBeanDefinitionParser("jackson2-populator", PARSER);
	}
}
