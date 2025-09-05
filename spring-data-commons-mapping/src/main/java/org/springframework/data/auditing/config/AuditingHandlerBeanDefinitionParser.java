/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.auditing.config;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} that parses an {@link AuditingHandler} {@link BeanDefinition}
 *
 * @author Oliver Gierke
 * @since 1.5
 */
public class AuditingHandlerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String AUDITOR_AWARE_REF = "auditor-aware-ref";

	private final String mappingContextBeanName;
	private @Nullable String resolvedBeanName;

	/**
	 * Creates a new {@link AuditingHandlerBeanDefinitionParser} to point to a {@link MappingContext} with the given bean
	 * name.
	 *
	 * @param mappingContextBeanName must not be {@literal null} or empty.
	 */
	@SuppressWarnings("null")
	public AuditingHandlerBeanDefinitionParser(String mappingContextBeanName) {

		Assert.hasText(mappingContextBeanName, "MappingContext bean name must not be null");
		this.mappingContextBeanName = mappingContextBeanName;
	}

	/**
	 * Returns the name of the bean definition the {@link AuditingHandler} was registered under.
	 *
	 * @return the resolvedBeanName
	 */
	public @Nullable String getResolvedBeanName() {
		return resolvedBeanName;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return AuditingHandler.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {

		// TODO SPLIT: PersistentEntitiesFactoryBean
		final BeanDefinitionBuilder persistentEntities = rootBeanDefinition(Object.class);
		persistentEntities.addConstructorArgReference(mappingContextBeanName);
		builder.addConstructorArgValue(persistentEntities.getBeanDefinition());

		String auditorAwareRef = element.getAttribute(AUDITOR_AWARE_REF);

		if (StringUtils.hasText(auditorAwareRef)) {
			builder.addPropertyValue("auditorAware", createLazyInitTargetSourceBeanDefinition(auditorAwareRef));
		}

		ParsingUtils.setPropertyValue(builder, element, "set-dates", "dateTimeForNow");
		ParsingUtils.setPropertyReference(builder, element, "date-time-provider-ref", "dateTimeProvider");
		ParsingUtils.setPropertyValue(builder, element, "modify-on-creation", "modifyOnCreation");
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {

		this.resolvedBeanName = super.resolveId(element, definition, parserContext);
		return resolvedBeanName;
	}

	private BeanDefinition createLazyInitTargetSourceBeanDefinition(String auditorAwareRef) {

		BeanDefinitionBuilder targetSourceBuilder = rootBeanDefinition(LazyInitTargetSource.class);
		targetSourceBuilder.addPropertyValue("targetBeanName", auditorAwareRef);

		BeanDefinitionBuilder builder = rootBeanDefinition(ProxyFactoryBean.class);
		builder.addPropertyValue("targetSource", targetSourceBuilder.getBeanDefinition());

		return builder.getBeanDefinition();
	}
}
