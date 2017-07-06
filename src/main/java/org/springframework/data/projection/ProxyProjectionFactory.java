/*
 * Copyright 2014-2105 the original author or authors.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link ProjectionFactory} to create JDK proxies to back interfaces and handle method invocations on them. By
 * default accessor methods are supported. In case the delegating lookups result in an object of different type that the
 * projection interface method's return type, another projection will be created to transparently mitigate between the
 * types.
 * 
 * @author Oliver Gierke
 * @see SpelAwareProxyProjectionFactory
 * @since 1.10
 */
class ProxyProjectionFactory implements ProjectionFactory, ResourceLoaderAware, BeanClassLoaderAware {

	private static final boolean IS_JAVA_8 = org.springframework.util.ClassUtils.isPresent("java.util.Optional",
			ProxyProjectionFactory.class.getClassLoader());

	private final ConversionService conversionService = new DefaultConversionService();
	private ClassLoader classLoader;

	/**
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 * @deprecated rather set the {@link ClassLoader} directly via {@link #setBeanClassLoader(ClassLoader)}.
	 */
	@Override
	@Deprecated
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.classLoader = resourceLoader.getClassLoader();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.projection.ProjectionFactory#createProjection(java.lang.Object, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T createProjection(Class<T> projectionType, Object source) {

		Assert.notNull(projectionType, "Projection type must not be null!");
		Assert.isTrue(projectionType.isInterface(), "Projection type must be an interface!");

		if (source == null) {
			return null;
		}

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(source);
		factory.setOpaque(true);
		factory.setInterfaces(projectionType, TargetAware.class);

		if (IS_JAVA_8) {
			factory.addAdvice(new DefaultMethodInvokingMethodInterceptor());
		}

		factory.addAdvice(new TargetAwareMethodInterceptor(source.getClass()));
		factory.addAdvice(getMethodInterceptor(source, projectionType));

		return (T) factory.getProxy(classLoader == null ? ClassUtils.getDefaultClassLoader() : classLoader);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.ProjectionFactory#createProjection(java.lang.Class)
	 */
	@Override
	public <T> T createProjection(Class<T> projectionType) {

		Assert.notNull(projectionType, "Projection type must not be null!");

		return createProjection(projectionType, new HashMap<String, Object>());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.ProjectionFactory#getProperties(java.lang.Class)
	 */
	@Override
	public List<String> getInputProperties(Class<?> projectionType) {

		Assert.notNull(projectionType, "Projection type must not be null!");

		List<String> result = new ArrayList<String>();

		for (PropertyDescriptor descriptor : getProjectionInformation(projectionType).getInputProperties()) {
			result.add(descriptor.getName());
		}

		return result;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.ProjectionFactory#getProjectionInformation(java.lang.Class)
	 */
	@Override
	public ProjectionInformation getProjectionInformation(Class<?> projectionType) {
		return new DefaultProjectionInformation(projectionType);
	}

	/**
	 * Returns the {@link MethodInterceptor} to add to the proxy.
	 * 
	 * @param source must not be {@literal null}.
	 * @param projectionType must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private MethodInterceptor getMethodInterceptor(Object source, Class<?> projectionType) {

		MethodInterceptor propertyInvocationInterceptor = source instanceof Map
				? new MapAccessingMethodInterceptor((Map<String, Object>) source)
				: new PropertyAccessingMethodInterceptor(source);

		return new ProjectingMethodInterceptor(this,
				postProcessAccessorInterceptor(propertyInvocationInterceptor, source, projectionType), conversionService);
	}

	/**
	 * Post-process the given {@link MethodInterceptor} for the given source instance and projection type. Default
	 * implementation will simply return the given interceptor.
	 * 
	 * @param interceptor will never be {@literal null}.
	 * @param source will never be {@literal null}.
	 * @param projectionType will never be {@literal null}.
	 * @return
	 */
	protected MethodInterceptor postProcessAccessorInterceptor(MethodInterceptor interceptor, Object source,
			Class<?> projectionType) {
		return interceptor;
	}

	/**
	 * Custom {@link MethodInterceptor} to expose the proxy target class even if we set
	 * {@link ProxyFactory#setOpaque(boolean)} to true to prevent properties on {@link Advised} to be rendered.
	 * 
	 * @author Oliver Gierke
	 */
	private static class TargetAwareMethodInterceptor implements MethodInterceptor {

		private static final Method GET_TARGET_CLASS_METHOD;
		private static final Method GET_TARGET_METHOD;

		private final Class<?> targetType;

		static {
			try {
				GET_TARGET_CLASS_METHOD = TargetAware.class.getMethod("getTargetClass");
				GET_TARGET_METHOD = TargetAware.class.getMethod("getTarget");
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}
		}

		/**
		 * Creates a new {@link TargetAwareMethodInterceptor} with the given target class.
		 * 
		 * @param targetType must not be {@literal null}.
		 */
		public TargetAwareMethodInterceptor(Class<?> targetType) {

			Assert.notNull(targetType, "Target type must not be null!");
			this.targetType = targetType;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			if (invocation.getMethod().equals(GET_TARGET_CLASS_METHOD)) {
				return targetType;
			} else if (invocation.getMethod().equals(GET_TARGET_METHOD)) {
				return invocation.getThis();
			}

			return invocation.proceed();
		}
	}
}
