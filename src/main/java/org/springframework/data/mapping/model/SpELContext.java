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
package org.springframework.data.mapping.model;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple factory to create {@link SpelExpressionParser} and {@link EvaluationContext} instances.
 *
 * @author Oliver Gierke
 */
public class SpELContext {

	private final SpelExpressionParser parser;
	private final PropertyAccessor accessor;
	private final @Nullable BeanFactory factory;

	/**
	 * Creates a new {@link SpELContext} with the given {@link PropertyAccessor}. Defaults the
	 * {@link SpelExpressionParser}.
	 *
	 * @param accessor
	 */
	public SpELContext(PropertyAccessor accessor) {
		this(accessor, null, null);
	}

	/**
	 * Creates a new {@link SpELContext} using the given {@link SpelExpressionParser} and {@link PropertyAccessor}. Will
	 * default the {@link SpelExpressionParser} in case the given value for it is {@literal null}.
	 *
	 * @param parser
	 * @param accessor
	 */
	public SpELContext(SpelExpressionParser parser, PropertyAccessor accessor) {
		this(accessor, parser, null);
	}

	/**
	 * Copy constructor to create a {@link SpELContext} using the given one's {@link PropertyAccessor} and
	 * {@link SpelExpressionParser} as well as the given {@link BeanFactory}.
	 *
	 * @param source
	 * @param factory
	 */
	public SpELContext(SpELContext source, BeanFactory factory) {
		this(source.accessor, source.parser, factory);
	}

	/**
	 * Creates a new {@link SpELContext} using the given {@link SpelExpressionParser}, {@link PropertyAccessor} and
	 * {@link BeanFactory}. Will default the {@link SpelExpressionParser} in case the given value for it is
	 * {@literal null}.
	 *
	 * @param accessor
	 * @param parser
	 * @param factory
	 */
	private SpELContext(PropertyAccessor accessor, @Nullable SpelExpressionParser parser, @Nullable BeanFactory factory) {

		Assert.notNull(accessor, "PropertyAccessor must not be null!");

		this.parser = parser == null ? new SpelExpressionParser() : parser;
		this.accessor = accessor;
		this.factory = factory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.SpELContext#getParser()
	 */
	public ExpressionParser getParser() {
		return this.parser;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.SpELContext#getEvaluationContext(java.lang.Object)
	 */
	public EvaluationContext getEvaluationContext(Object source) {

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(source);
		evaluationContext.addPropertyAccessor(accessor);

		if (factory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(factory));
		}

		return evaluationContext;
	}
}
