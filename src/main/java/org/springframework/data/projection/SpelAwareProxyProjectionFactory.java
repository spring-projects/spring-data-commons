/*
 * Copyright 2015-2024 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.util.AnnotationDetectionMethodCallback;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link ProxyProjectionFactory} that adds support to use {@link Value}-annotated methods on a projection interface
 * to evaluate the contained SpEL expression to define the outcome of the method call.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 1.10
 */
public class SpelAwareProxyProjectionFactory extends ProxyProjectionFactory implements BeanFactoryAware {

	private final Map<Class<?>, Boolean> typeCache = new ConcurrentHashMap<>();
	private final ExpressionParser parser;

	private @Nullable BeanFactory beanFactory;

	/**
	 * Create a new {@link SpelAwareProxyProjectionFactory}.
	 */
	public SpelAwareProxyProjectionFactory() {
		this(new SpelExpressionParser());
	}

	/**
	 * Create a new {@link SpelAwareProxyProjectionFactory} for a given {@link ExpressionParser}.
	 *
	 * @param parser the parser to use.
	 * @since 3.3
	 */
	public SpelAwareProxyProjectionFactory(ExpressionParser parser) {

		Assert.notNull(parser, "ExpressionParser must not be null");
		this.parser = parser;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	protected ProjectionInformation createProjectionInformation(Class<?> projectionType) {
		return new SpelAwareProjectionInformation(projectionType);
	}

	/**
	 * Inspects the given target type for methods with {@link Value} annotations and caches the result. Will create a
	 * {@link SpelEvaluatingMethodInterceptor} if an annotation was found or return the delegate as is if not.
	 *
	 * @param interceptor the root {@link MethodInterceptor}.
	 * @param source The backing source object.
	 * @param projectionType the proxy target type.
	 * @return
	 */
	@Override
	protected MethodInterceptor postProcessAccessorInterceptor(MethodInterceptor interceptor, Object source,
			Class<?> projectionType) {

		return typeCache.computeIfAbsent(projectionType, SpelAwareProxyProjectionFactory::hasMethodWithValueAnnotation)
				? new SpelEvaluatingMethodInterceptor(interceptor, source, beanFactory, parser, projectionType)
				: interceptor;
	}

	/**
	 * Returns whether the given type as a method annotated with {@link Value}.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static boolean hasMethodWithValueAnnotation(Class<?> type) {

		Assert.notNull(type, "Type must not be null");

		AnnotationDetectionMethodCallback<Value> callback = new AnnotationDetectionMethodCallback<Value>(Value.class);
		ReflectionUtils.doWithMethods(type, callback);

		return callback.hasFoundAnnotation();
	}

	protected static class SpelAwareProjectionInformation extends DefaultProjectionInformation {

		protected SpelAwareProjectionInformation(Class<?> projectionType) {
			super(projectionType);
		}

		@Override
		protected boolean isInputProperty(PropertyDescriptor descriptor) {

			if (!super.isInputProperty(descriptor)) {
				return false;
			}

			Method readMethod = descriptor.getReadMethod();

			if (readMethod == null) {
				return false;
			}

			return AnnotationUtils.findAnnotation(readMethod, Value.class) == null;
		}
	}
}
