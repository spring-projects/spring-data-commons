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
package org.springframework.data.aot.hint;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.DecoratingProxy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 3.0
 */
public class AuditingHints {

	/**
	 * {@link RuntimeHintsRegistrar} to be applied when auditing is enabled. Can be used along with
	 * {@link org.springframework.context.annotation.ImportRuntimeHints @ImportRuntimeHints} for conditional registration.
	 *
	 * @author Christoph Strobl
	 * @since 3.0
	 */
	public static class AuditingRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			registerSpringProxy(AuditorAware.class, hints);
		}
	}

	/**
	 * {@link RuntimeHintsRegistrar} to be applied when reactive auditing is enabled. Can be used along with
	 * {@link org.springframework.context.annotation.ImportRuntimeHints @ImportRuntimeHints} for conditional registration.
	 *
	 * @author Christoph Strobl
	 * @since 3.0
	 */
	public static class ReactiveAuditingRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			registerSpringProxy(ReactiveAuditorAware.class, hints);
		}
	}

	private static void registerSpringProxy(Class<?> type, RuntimeHints runtimeHints) {

		runtimeHints.proxies().registerJdkProxy(TypeReference.of(type), TypeReference.of(SpringProxy.class),
				TypeReference.of(Advised.class), TypeReference.of(DecoratingProxy.class));
	}
}
