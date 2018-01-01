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
package org.springframework.data.config;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Helper to create {@link BeanComponentDefinition} more easily.
 *
 * @author Oliver Gierke
 */
public class BeanComponentDefinitionBuilder {

	private final Element defaultSource;
	private final ParserContext context;

	/**
	 * Creates a new {@link BeanComponentDefinitionBuilder} using the given {@link Element} as default source and the
	 * given {@link ParserContext}.
	 *
	 * @param defaultSource must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 */
	public BeanComponentDefinitionBuilder(Element defaultSource, ParserContext context) {

		Assert.notNull(defaultSource, "DefaultSource must not be null!");
		Assert.notNull(context, "Context must not be null!");

		this.defaultSource = defaultSource;
		this.context = context;
	}

	/**
	 * Creates a {@link BeanComponentDefinition} from the given {@link BeanDefinitionBuilder}. Will generate a bean name.
	 *
	 * @param builder must not be {@literal null}.
	 * @return
	 */
	public BeanComponentDefinition getComponent(BeanDefinitionBuilder builder) {

		Assert.notNull(builder, "Builder must not be null!");

		AbstractBeanDefinition definition = builder.getRawBeanDefinition();
		String name = BeanDefinitionReaderUtils.generateBeanName(definition, context.getRegistry(), context.isNested());

		return getComponent(builder, name);
	}

	/**
	 * Creates a {@link BeanComponentDefinition} from the given {@link BeanDefinitionBuilder} and inspects the backing
	 * {@link Element}s id attribute for a name. It will use this one if found or the given fallback if not.
	 *
	 * @param builder must not be {@literal null}.
	 * @param fallback must not be {@literal null} or empty.
	 * @return
	 */
	public BeanComponentDefinition getComponentIdButFallback(BeanDefinitionBuilder builder, String fallback) {

		Assert.hasText(fallback, "Fallback component id must not be null or empty!");

		String id = defaultSource.getAttribute("id");
		return getComponent(builder, StringUtils.hasText(id) ? id : fallback);
	}

	/**
	 * Creates a {@link BeanComponentDefinition} from the given {@link BeanDefinitionBuilder} using the given name.
	 *
	 * @param builder must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	public BeanComponentDefinition getComponent(BeanDefinitionBuilder builder, String name) {
		return getComponent(builder, name, defaultSource);
	}

	/**
	 * Creates a new {@link BeanComponentDefinition} from the given {@link BeanDefinitionBuilder} using the given name and
	 * raw source object.
	 *
	 * @param builder must not be {@literal null}.
	 * @param name must not be {@literal null}.
	 * @param rawSource
	 * @return
	 */
	public BeanComponentDefinition getComponent(BeanDefinitionBuilder builder, String name, Object rawSource) {

		Assert.notNull(builder, "Builder must not be null!");
		Assert.hasText(name, "Name of bean must not be null or empty!");

		AbstractBeanDefinition definition = builder.getRawBeanDefinition();
		definition.setSource(context.extractSource(rawSource));

		return new BeanComponentDefinition(definition, name);
	}
}
