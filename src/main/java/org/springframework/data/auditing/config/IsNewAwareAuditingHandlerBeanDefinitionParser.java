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
package org.springframework.data.auditing.config;

import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.w3c.dom.Element;

/**
 * {@link AuditingHandlerBeanDefinitionParser} that will register am {@link IsNewAwareAuditingHandler}. Needs to get the
 * bean id of the {@link MappingContext} it shall refer to.
 *
 * @author Oliver Gierke
 * @since 1.5
 */
public class IsNewAwareAuditingHandlerBeanDefinitionParser extends AuditingHandlerBeanDefinitionParser {

	/**
	 * Creates a new {@link IsNewAwareAuditingHandlerBeanDefinitionParser}.
	 *
	 * @param mappingContextBeanName must not be {@literal null} or empty.
	 */
	public IsNewAwareAuditingHandlerBeanDefinitionParser(String mappingContextBeanName) {
		super(mappingContextBeanName);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.config.AuditingHandlerBeanDefinitionParser#getBeanClass(org.w3c.dom.Element)
	 */
	@Override
	protected Class<?> getBeanClass(Element element) {
		return IsNewAwareAuditingHandler.class;
	}
}
