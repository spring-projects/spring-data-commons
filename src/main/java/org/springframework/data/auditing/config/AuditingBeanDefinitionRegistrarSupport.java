/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.data.auditing.config;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.util.StringUtils;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

/**
 * Base class that implements {@link ImportBeanDefinitionRegistrar}. Registers a {@link AuditingHandler} based on
 * the provided configuration({@link AnnotationAuditingConfiguration}).
 *
 * @author Ranie Jade Ramiso
 */
public abstract class AuditingBeanDefinitionRegistrarSupport implements ImportBeanDefinitionRegistrar {
	private final String AUDITOR_AWARE = "auditorAware";
	private final String DATE_TIME_PROVIDER = "dateTimeProvider";
	private final String MODIFY_ON_CREATE = "modifyOnCreation";
	private final String SET_DATES = "dateTimeForNow";

	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AuditingHandler.class);

		AnnotationAuditingConfiguration configuration = getConfiguration(annotationMetadata);

		if (StringUtils.hasText(configuration.getAuditorAwareRef())) {
			builder.addPropertyValue(AUDITOR_AWARE, createLazyInitTargetSourceBeanDefinition(configuration.getAuditorAwareRef()));
		}

		builder.addPropertyValue(SET_DATES, configuration.isSetDates());
		builder.addPropertyValue(MODIFY_ON_CREATE, configuration.isModifyOnCreate());

		if (StringUtils.hasText(configuration.getDateTimeProviderRef())) {
			builder.addPropertyReference(DATE_TIME_PROVIDER, configuration.getDateTimeProviderRef());
		} else {
			builder.addPropertyValue(DATE_TIME_PROVIDER, CurrentDateTimeProvider.INSTANCE);
		}

		BeanDefinition auditingHandlerDefinition = builder.getBeanDefinition();

		registry.registerBeanDefinition(BeanDefinitionReaderUtils.generateBeanName(auditingHandlerDefinition, registry),
				auditingHandlerDefinition);

		postProcess(configuration, auditingHandlerDefinition, registry);

	}

	/**
	 * Store specific implementation to properly setup auditing.
	 */
	protected abstract void postProcess(AnnotationAuditingConfiguration configuration,
										BeanDefinition auditingHandlerDefinition,
										BeanDefinitionRegistry registry);

	/**
	 * Retrieve auditing configuration information.
	 */
	protected abstract AnnotationAuditingConfiguration getConfiguration(AnnotationMetadata annotationMetadata);

	private BeanDefinition createLazyInitTargetSourceBeanDefinition(String auditorAwareRef) {

		BeanDefinitionBuilder targetSourceBuilder = rootBeanDefinition(LazyInitTargetSource.class);
		targetSourceBuilder.addPropertyValue("targetBeanName", auditorAwareRef);

		BeanDefinitionBuilder builder = rootBeanDefinition(ProxyFactoryBean.class);
		builder.addPropertyValue("targetSource", targetSourceBuilder.getBeanDefinition());

		return builder.getBeanDefinition();
	}
}
