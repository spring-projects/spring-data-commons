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
package org.springframework.data.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Utility methods for {@link BeanDefinitionParser} implementations.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public abstract class ParsingUtils {

	private ParsingUtils() {

	}

	/**
	 * Configures a property value for the given property name reading the attribute of the given name from the given
	 * {@link Element} if the attribute is configured.
	 *
	 * @param builder must not be {@literal null}.
	 * @param element must not be {@literal null}.
	 * @param attrName must not be {@literal null} or empty.
	 * @param propertyName must not be {@literal null} or empty.
	 */
	public static void setPropertyValue(BeanDefinitionBuilder builder, Element element, String attrName,
			String propertyName) {

		Assert.notNull(builder, "BeanDefinitionBuilder must not be null!");
		Assert.notNull(element, "Element must not be null!");
		Assert.hasText(attrName, "Attribute name must not be null!");
		Assert.hasText(propertyName, "Property name must not be null!");

		String attr = element.getAttribute(attrName);

		if (StringUtils.hasText(attr)) {
			builder.addPropertyValue(propertyName, attr);
		}
	}

	/**
	 * Sets the property with the given attribute name on the given {@link BeanDefinitionBuilder} to the value of the
	 * attribute with the given name if the attribute is configured.
	 *
	 * @param builder must not be {@literal null}.
	 * @param element must not be {@literal null}.
	 * @param attribute must not be {@literal null} or empty.
	 */
	public static void setPropertyValue(BeanDefinitionBuilder builder, Element element, String attribute) {
		setPropertyValue(builder, element, attribute, attribute);
	}

	/**
	 * Configures a bean property reference with the value of the attribute of the given name if it is configured.
	 *
	 * @param builder must not be {@literal null}.
	 * @param element must not be {@literal null}.
	 * @param attribute must not be {@literal null} or empty.
	 * @param property must not be {@literal null}or empty.
	 */
	public static void setPropertyReference(BeanDefinitionBuilder builder, Element element, String attribute,
			String property) {

		Assert.notNull(builder, "BeanDefinitionBuilder must not be null!");
		Assert.notNull(element, "Element must not be null!");
		Assert.hasText(attribute, "Attribute name must not be null!");
		Assert.hasText(property, "Property name must not be null!");

		String value = element.getAttribute(attribute);

		if (StringUtils.hasText(value)) {
			builder.addPropertyReference(property, value);
		}
	}

	/**
	 * Returns the {@link BeanDefinition} built by the given {@link BeanDefinitionBuilder} enriched with source
	 * information derived from the given {@link Element}.
	 *
	 * @param builder must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @param element must not be {@literal null}.
	 * @return
	 */
	public static AbstractBeanDefinition getSourceBeanDefinition(BeanDefinitionBuilder builder, ParserContext context,
			Element element) {

		Assert.notNull(element, "Element must not be null!");
		Assert.notNull(context, "ParserContext must not be null!");

		return getSourceBeanDefinition(builder, context.extractSource(element));
	}

	/**
	 * Returns the {@link AbstractBeanDefinition} built by the given builder with the given extracted source applied.
	 *
	 * @param builder must not be {@literal null}.
	 * @param source
	 * @return
	 */
	public static AbstractBeanDefinition getSourceBeanDefinition(BeanDefinitionBuilder builder, @Nullable Object source) {

		Assert.notNull(builder, "Builder must not be null!");

		AbstractBeanDefinition definition = builder.getRawBeanDefinition();
		definition.setSource(source);
		return definition;
	}

	/**
	 * Returns a {@link BeanDefinition} for an {@link ObjectFactoryCreatingFactoryBean} pointing to the bean with the
	 * given name.
	 *
	 * @param targetBeanName must not be {@literal null} or empty.
	 * @param source
	 * @return
	 */
	public static AbstractBeanDefinition getObjectFactoryBeanDefinition(String targetBeanName, @Nullable Object source) {

		Assert.hasText(targetBeanName, "Target bean name must not be null or empty!");

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ObjectFactoryCreatingFactoryBean.class);
		builder.addPropertyValue("targetBeanName", targetBeanName);
		builder.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);

		return getSourceBeanDefinition(builder, source);
	}
}
