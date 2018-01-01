/*
 * Copyright 2016-2018 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;

/**
 * SPI to create {@link MethodInterceptor} instances based on the given source object and the target type to produce. To
 * be registered with a {@link ProxyProjectionFactory} to customize the way method executions on projection proxies are
 * handled.
 *
 * @author Oliver Gierke
 * @see ProxyProjectionFactory
 * @soundtrack Henrik Freischlader Trio - Nobody Else To Blame (Openness)
 * @since 1.13
 */
public interface MethodInterceptorFactory {

	/**
	 * Returns the {@link MethodInterceptor} to be used for the given source object and target type.
	 *
	 * @param source will never be {@literal null}.
	 * @param targetType will never be {@literal null}.
	 * @return
	 */
	MethodInterceptor createMethodInterceptor(Object source, Class<?> targetType);

	/**
	 * Returns whether the current factory is supposed to be used to create a {@link MethodInterceptor} for proxy of the
	 * given target type.
	 *
	 * @param source will never be {@literal null}.
	 * @param targetType will never be {@literal null}.
	 * @return
	 */
	boolean supports(Object source, Class<?> targetType);
}
