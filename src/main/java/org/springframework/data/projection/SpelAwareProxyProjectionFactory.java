/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.projection;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.util.AnnotationDetectionMethodCallback;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link ProxyProjectionFactory} that adds support to use {@link Value}-annotated methods on a projection interface
 * to evaluate the contained SpEL expression to define the outcome of the method call.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @since 1.10
 */
public class SpelAwareProxyProjectionFactory extends ProxyProjectionFactory implements BeanFactoryAware {

	private final Map<Class<?>, Boolean> typeCache = new HashMap<Class<?>, Boolean>();
	private final SpelExpressionParser parser = new SpelExpressionParser();

	private BeanFactory beanFactory;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
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

		if (!typeCache.containsKey(projectionType)) {

			AnnotationDetectionMethodCallback<Value> callback = new AnnotationDetectionMethodCallback<>(Value.class);
			ReflectionUtils.doWithMethods(projectionType, callback);

			typeCache.put(projectionType, callback.hasFoundAnnotation());
		}

		return typeCache.get(projectionType)
				? new SpelEvaluatingMethodInterceptor(interceptor, source, beanFactory, parser, projectionType) : interceptor;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.ProxyProjectionFactory#getProjectionInformation(java.lang.Class)
	 */
	@Override
	public ProjectionInformation getProjectionInformation(Class<?> projectionType) {

		return new DefaultProjectionInformation(projectionType) {

			/* 
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultProjectionInformation#isInputProperty(java.beans.PropertyDescriptor)
			 */
			@Override
			protected boolean isInputProperty(PropertyDescriptor descriptor) {

				Method readMethod = descriptor.getReadMethod();

				if (readMethod == null) {
					return false;
				}

				return AnnotationUtils.findAnnotation(readMethod, Value.class) == null;
			}
		};
	}
}
