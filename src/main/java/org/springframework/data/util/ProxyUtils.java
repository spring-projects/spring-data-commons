/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.util;

import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Proxy type detection utilities, extensible via {@link ProxyDetector} registered via Spring factories.
 *
 * @author Oliver Gierke
 * @soundtrack Victor Wooten - Cruising Altitude (Trypnotix)
 */
public abstract class ProxyUtils {

	private static Map<Class<?>, Class<?>> USER_TYPES = new ConcurrentReferenceHashMap<>();

	private static final List<ProxyDetector> DETECTORS = SpringFactoriesLoader.loadFactories(ProxyDetector.class,
			ProxyUtils.class.getClassLoader());

	static {
		DETECTORS.add(ClassUtils::getUserClass);
	}

	private ProxyUtils() {}

	/**
	 * Returns the user class for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static Class<?> getUserClass(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return USER_TYPES.computeIfAbsent(type, it -> {

			Class<?> result = it;

			for (ProxyDetector proxyDetector : DETECTORS) {
				result = proxyDetector.getUserType(result);
			}

			return result;
		});
	}

	/**
	 * Returns the user class for the given source object.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	public static Class<?> getUserClass(Object source) {

		Assert.notNull(source, "Source object must not be null!");

		return getUserClass(AopUtils.getTargetClass(source));
	}

	/**
	 * SPI to extend Spring's default proxy detection capabilities.
	 *
	 * @author Oliver Gierke
	 */
	public static interface ProxyDetector {

		/**
		 * Returns the user class for the given type.
		 *
		 * @param type will never be {@literal null}.
		 * @return
		 */
		Class<?> getUserType(Class<?> type);
	}
}
