/*
 * Copyright 2020-2021 the original author or authors.
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

/**
 * Factory to expose the new package-protected {@link EntityInstantiator} implementations for the legacy types. To be
 * removed in 2.4.
 *
 * @author Oliver Drotbohm
 * @since 2.3
 * @deprecated since 2.3 as it's only a bridge from the legacy types towards the new, package-protected implementation.
 */
@Deprecated
public class InternalEntityInstantiatorFactory {

	public static EntityInstantiator getClassGeneratingEntityInstantiator() {
		return new ClassGeneratingEntityInstantiator();
	}

	public static EntityInstantiator getKotlinClassGeneratingEntityInstantiator() {
		return new KotlinClassGeneratingEntityInstantiator();
	}

	public static EntityInstantiator getReflectionEntityInstantiator() {
		return ReflectionEntityInstantiator.INSTANCE;
	}
}
