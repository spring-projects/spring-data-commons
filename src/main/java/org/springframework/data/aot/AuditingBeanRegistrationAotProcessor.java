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
package org.springframework.data.aot;

import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.data.aot.hint.AuditingHints;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.repository.util.ReactiveWrappers.ReactiveLibrary;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 3.0
 */
public class AuditingBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Nullable
	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {

		if (isAuditingHandler(registeredBean)) {
			return (generationContext, beanRegistrationCode) -> new AuditingHints.AuditingRuntimeHints()
					.registerHints(generationContext.getRuntimeHints(), registeredBean.getBeanFactory().getBeanClassLoader());
		}
		if (ReactiveWrappers.isAvailable(ReactiveLibrary.PROJECT_REACTOR) && isReactiveAuditorAware(registeredBean)) {
			return (generationContext, beanRegistrationCode) -> new AuditingHints.ReactiveAuditingRuntimeHints()
					.registerHints(generationContext.getRuntimeHints(), registeredBean.getBeanFactory().getBeanClassLoader());
		}
		return null;
	}

	boolean isAuditingHandler(RegisteredBean bean) {
		return ClassUtils.isAssignable(AuditorAware.class, bean.getBeanClass());
	}

	boolean isReactiveAuditorAware(RegisteredBean bean) {
		return ClassUtils.isAssignable(ReactiveAuditorAware.class, bean.getBeanClass());
	}
}
