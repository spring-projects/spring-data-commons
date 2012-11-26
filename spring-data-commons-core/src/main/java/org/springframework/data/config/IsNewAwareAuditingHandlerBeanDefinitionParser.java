/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * {@link AuditingHandlerBeanDefinitionParser} that will register am {@link IsNewAwareAuditingHandler}. Needs to get the
 * bean id of the
 * 
 * @author Oliver Gierke
 */
public class IsNewAwareAuditingHandlerBeanDefinitionParser extends AuditingHandlerBeanDefinitionParser {

	private final String isNewStrategyFactoryBeanId;

	/**
	 * Creates a new {@link IsNewAwareAuditingHandlerBeanDefinitionParser}.
	 * 
	 * @param isNewStrategyFactoryBeanId must not be {@literal null} or empty.
	 */
	public IsNewAwareAuditingHandlerBeanDefinitionParser(String isNewStrategyFactoryBeanId) {

		Assert.hasText(isNewStrategyFactoryBeanId);
		this.isNewStrategyFactoryBeanId = isNewStrategyFactoryBeanId;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.config.AuditingHandlerBeanDefinitionParser#getBeanClass(org.w3c.dom.Element)
	 */
	@Override
	protected Class<?> getBeanClass(Element element) {
		return IsNewAwareAuditingHandler.class;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.config.AuditingHandlerBeanDefinitionParser#doParse(org.w3c.dom.Element, org.springframework.beans.factory.support.BeanDefinitionBuilder)
	 */
	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {

		builder.addConstructorArgReference(isNewStrategyFactoryBeanId);

		super.doParse(element, builder);
	}
}
