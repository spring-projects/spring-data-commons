/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.aot;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.DecoratingProxy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.util.ReactiveWrappers;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanRegistrationAotProcessor} to register runtime hints for beans that implement auditor-aware interfaces to
 * enable JDK proxy creation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.0
 */
class AuditingBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {


	@Nullable
	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {

		if (isAuditingHandler(registeredBean)) {
			return (generationContext, beanRegistrationCode) -> registerSpringProxy(AuditorAware.class,
					generationContext.getRuntimeHints());
		}

		if (ReactiveWrappers.PROJECT_REACTOR_PRESENT && isReactiveAuditorAware(registeredBean)) {
			return (generationContext, beanRegistrationCode) -> registerSpringProxy(ReactiveAuditorAware.class,
					generationContext.getRuntimeHints());
		}

		return null;
	}

	private static boolean isAuditingHandler(RegisteredBean bean) {
		return ClassUtils.isAssignable(AuditorAware.class, bean.getBeanClass());
	}

	private static boolean isReactiveAuditorAware(RegisteredBean bean) {
		return ClassUtils.isAssignable(ReactiveAuditorAware.class, bean.getBeanClass());
	}

	private static void registerSpringProxy(Class<?> type, RuntimeHints runtimeHints) {

		runtimeHints.proxies().registerJdkProxy(TypeReference.of(type), TypeReference.of(SpringProxy.class),
				TypeReference.of(Advised.class), TypeReference.of(DecoratingProxy.class));
	}
}
