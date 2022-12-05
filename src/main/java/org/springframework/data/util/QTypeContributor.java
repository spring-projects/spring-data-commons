/*
 * Copyright 2022 the original author or authors.
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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 4.1
 */
public class QTypeContributor {

	private final static Log logger = LogFactory.getLog(QTypeContributor.class);
	private static Function<ClassLoader, Class<?>> entityPathType = cacheOf(QTypeContributor::getEntityPathType);

	public static void contributeEntityPath(Class<?> type, GenerationContext context, @Nullable ClassLoader classLoader) {

		Class<?> entityPathType = QTypeContributor.entityPathType.apply(classLoader);
		if (entityPathType == null) {
			return;
		}

		String queryClassName = getQueryClassName(type);
		if (ClassUtils.isPresent(queryClassName, classLoader)) {
			try {
				if (ClassUtils.isAssignable(entityPathType, ClassUtils.forName(queryClassName, classLoader))) {

					logger.debug("Registering Q type %s for %s.");
					context.getRuntimeHints().reflection().registerType(TypeReference.of(queryClassName),
							MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS);
				} else {
					logger.debug("Skipping Q type %s. Not an EntityPath.");
				}
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("%s could not be loaded".formatted(queryClassName), e);
			}
		}
	}

	@Nullable
	private static Class<?> getEntityPathType(ClassLoader classLoader) {

		if (!ClassUtils.isPresent("com.querydsl.core.types.EntityPath", classLoader)) {
			return null;
		}

		try {
			return ClassUtils.forName("com.querydsl.core.types.EntityPath", classLoader);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static Function<ClassLoader, Class<?>> cacheOf(Function<ClassLoader, Class<?>> inFunction) {
		Map<ClassLoader, Class<?>> cache = new WeakHashMap<>();
		return in -> cache.computeIfAbsent(in, inFunction::apply);
	}

	/**
	 * Returns the name of the query class for the given domain class.
	 *
	 * @param domainClass
	 * @return
	 */
	private static String getQueryClassName(Class<?> domainClass) {

		String simpleClassName = ClassUtils.getShortName(domainClass);
		String pkgName = domainClass.getPackage().getName();

		return String.format("%s.Q%s%s", pkgName, getClassBase(simpleClassName), domainClass.getSimpleName());
	}

	/**
	 * Analyzes the short class name and potentially returns the outer class.
	 *
	 * @param shortName
	 * @return
	 */
	private static String getClassBase(String shortName) {

		String[] parts = shortName.split("\\.");

		return parts.length < 2 ? "" : parts[0] + "_";
	}
}
