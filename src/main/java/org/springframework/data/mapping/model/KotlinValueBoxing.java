/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.mapping.model;

import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;

import java.util.function.Function;

import org.springframework.data.util.KotlinReflectionUtils;

/**
 * Utility to box values into inline class instances.
 *
 * @author Mark Paluch
 * @since 3.2
 */
class KotlinValueBoxing {

	/**
	 * @param kParameter the kotlin parameter to wrap.
	 * @return a wrapper function that potentially wraps (value-boxing) values into value classes if these are nullable or
	 *         used in a lower part of the type hierarchy.
	 */
	static Function<Object, Object> getWrapper(KParameter kParameter) {
		return getWrapper(kParameter, true);
	}

	/**
	 * @param kParameter the kotlin parameter to wrap.
	 * @param domainTypeUsage optional in the domain type require value type casting. Inner/nested ones don't. This is
	 *          because calling the synthetic constructor with an optional parameter requires an inline class while
	 *          optional parameters via reflection are handled within Kotlin-Reflection.
	 * @return a wrapper function that potentially wraps (value-boxing) values into value classes if these are nullable or
	 *         used in a lower part of the type hierarchy.
	 */
	private static Function<Object, Object> getWrapper(KParameter kParameter, boolean domainTypeUsage) {

		if (KotlinReflectionUtils.isValueClass(kParameter.getType()) && (!domainTypeUsage || kParameter.isOptional())) {

			KClass<?> kClass = (KClass<?>) kParameter.getType().getClassifier();
			KFunction<?> ctor = kClass.getConstructors().iterator().next();

			// using reflection to construct a value class wrapper. Everything
			// else would require too many levels of indirections.

			KParameter nested = ctor.getParameters().get(0);
			Function<Object, Object> wrapper = getWrapper(nested, false);

			return o -> kClass.isInstance(o) ? o : ctor.call(wrapper.apply(o));
		}

		return Function.identity();
	}
}
