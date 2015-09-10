/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.repository.augment;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;

/**
 * Default implementation of {@link MethodMetadata}.
 * 
 * @author Oliver Gierke
 * @since 1.12
 */
enum DefaultMethodMetadata implements MethodMetadata {

	INSTANCE;

	private MethodInvocation getMethodInvocation() {
		return ExposeInvocationInterceptor.currentInvocation();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.MethodMetadata#getInvocationTargetType()
	 */
	public List<Class<?>> getInvocationTargetType() {

		MethodInvocation invocation = getMethodInvocation();

		if (invocation instanceof ReflectiveMethodInvocation) {
			Advised proxy = (Advised) ((ReflectiveMethodInvocation) invocation).getProxy();
			return Arrays.asList(proxy.getProxiedInterfaces());
		}

		return Collections.<Class<?>> singletonList(getMethodInvocation().getThis().getClass());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.MethodMetadata#getInvocationArguments()
	 */
	public Object[] getInvocationArguments() {
		return getMethodInvocation().getArguments();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.MethodMetadata#getMethod()
	 */
	public Method getMethod() {
		return getMethodInvocation().getMethod();
	}
}
