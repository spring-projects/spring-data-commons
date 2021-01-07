/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.projection;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link MethodInterceptor} to invoke a SpEL expression to compute the method result. Will forward the resolution to a
 * delegate {@link MethodInterceptor} if no {@link Value} annotation is found.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @see 1.10
 */
class SpelEvaluatingMethodInterceptor implements MethodInterceptor {

	private static final ParserContext PARSER_CONTEXT = new TemplateParserContext();

	private final EvaluationContext evaluationContext;
	private final MethodInterceptor delegate;
	private final Map<Integer, Expression> expressions;
	private final Object target;

	/**
	 * Creates a new {@link SpelEvaluatingMethodInterceptor} delegating to the given {@link MethodInterceptor} as fallback
	 * and exposing the given target object via {@code target} to the SpEl expressions. If a {@link BeanFactory} is given,
	 * bean references in SpEl expressions can be resolved as well.
	 *
	 * @param delegate must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param beanFactory can be {@literal null}.
	 * @param parser must not be {@literal null}.
	 * @param targetInterface must not be {@literal null}.
	 */
	public SpelEvaluatingMethodInterceptor(MethodInterceptor delegate, Object target, @Nullable BeanFactory beanFactory,
			SpelExpressionParser parser, Class<?> targetInterface) {

		Assert.notNull(delegate, "Delegate MethodInterceptor must not be null!");
		Assert.notNull(target, "Target object must not be null!");
		Assert.notNull(parser, "SpelExpressionParser must not be null!");
		Assert.notNull(targetInterface, "Target interface must not be null!");

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

		if (target instanceof Map) {
			evaluationContext.addPropertyAccessor(new MapAccessor());
		}

		if (beanFactory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}

		this.expressions = potentiallyCreateExpressionsForMethodsOnTargetInterface(parser, targetInterface);

		this.evaluationContext = evaluationContext;
		this.delegate = delegate;
		this.target = target;
	}

	/**
	 * Eagerly parses {@link Expression} defined on {@link Value} annotations. Returns a map with
	 * {@code method.hashCode()} as key and the parsed {@link Expression} or an {@link Collections#emptyMap()} if no
	 * {@code Expressions} were found.
	 *
	 * @param parser must not be {@literal null}.
	 * @param targetInterface must not be {@literal null}.
	 * @return
	 */
	private static Map<Integer, Expression> potentiallyCreateExpressionsForMethodsOnTargetInterface(
			SpelExpressionParser parser, Class<?> targetInterface) {

		Map<Integer, Expression> expressions = new HashMap<>();

		for (Method method : targetInterface.getMethods()) {

			if (!method.isAnnotationPresent(Value.class)) {
				continue;
			}

			Value value = method.getAnnotation(Value.class);

			if (!StringUtils.hasText(value.value())) {
				throw new IllegalStateException(String.format("@Value annotation on %s contains empty expression!", method));
			}

			expressions.put(method.hashCode(), parser.parseExpression(value.value(), PARSER_CONTEXT));
		}

		return Collections.unmodifiableMap(expressions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Nullable
	@Override
	public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

		Expression expression = expressions.get(invocation.getMethod().hashCode());

		if (expression == null) {
			return delegate.invoke(invocation);
		}

		return expression.getValue(evaluationContext, TargetWrapper.of(target, invocation.getArguments()));
	}

	/**
	 * Wrapper class to expose an object to the SpEL expression as {@code target}.
	 *
	 * @author Oliver Gierke
	 */
	static final class TargetWrapper {

		private final Object target;
		private final Object[] args;

		private TargetWrapper(Object target, Object[] args) {
			this.target = target;
			this.args = args;
		}

		public static TargetWrapper of(Object target, Object[] args) {
			return new TargetWrapper(target, args);
		}

		public Object getTarget() {
			return this.target;
		}

		public Object[] getArgs() {
			return this.args;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof TargetWrapper)) {
				return false;
			}

			TargetWrapper that = (TargetWrapper) o;

			if (!ObjectUtils.nullSafeEquals(target, that.target)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(args, that.args);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(target);
			result = 31 * result + ObjectUtils.nullSafeHashCode(args);
			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "SpelEvaluatingMethodInterceptor.TargetWrapper(target=" + this.getTarget() + ", args="
					+ java.util.Arrays.deepToString(this.getArgs()) + ")";
		}
	}
}
