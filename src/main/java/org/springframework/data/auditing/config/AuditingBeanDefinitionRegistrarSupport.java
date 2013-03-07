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

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import java.lang.annotation.Annotation;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link ImportBeanDefinitionRegistrar} that serves as a base class for store specific implementations for
 * configuring audit support. Registers a {@link AuditingHandler} based on the provided configuration(
 * {@link AnnotationAuditingConfiguration}).
 * 
 * @author Ranie Jade Ramiso
 * @author Thomas Darimont
 */
public abstract class AuditingBeanDefinitionRegistrarSupport implements ImportBeanDefinitionRegistrar {

	private static final String DEFAULT_AUDITOR_AWARE_BEAN_NAME = "auditorProvider";
	private static final String AUDITOR_AWARE = "auditorAware";
	private static final String DATE_TIME_PROVIDER = "dateTimeProvider";
	private static final String MODIFY_ON_CREATE = "modifyOnCreation";
	private static final String SET_DATES = "dateTimeForNow";

	/**
	 * @param annotationMetadata, must not be {@literal null}.
	 * @param registry, must not be {@literal null}.
	 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata,
	 *      org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

		Assert.notNull(annotationMetadata, "annotationMetadata must not be null!");
		Assert.notNull(annotationMetadata, "registry must not be null!");

		AbstractBeanDefinition ahbd = registerAuditHandlerBeanDefinition(registry, getConfiguration(annotationMetadata));
		registerAuditListenerBeanDefinition(ahbd, registry);
	}

	/**
	 * Registers an appropriate BeanDefinition for an {@link AuditingHandler}.
	 * 
	 * @param registry, must not be {@literal null}.
	 * @param configuration, must not be {@literal null}.
	 * @return
	 */
	private AbstractBeanDefinition registerAuditHandlerBeanDefinition(BeanDefinitionRegistry registry,
			AnnotationAuditingConfiguration configuration) {

		Assert.notNull(registry, "registry must not be null!");
		Assert.notNull(configuration, "registry must not be null!");

		AbstractBeanDefinition ahbd = getAuditHandlerBeanDefinitionBuilder(configuration).getBeanDefinition();
		registry.registerBeanDefinition(BeanDefinitionReaderUtils.generateBeanName(ahbd, registry), ahbd);
		return ahbd;
	}

	/**
	 * Creates a {@link BeanDefinitionBuilder} to ease the definition of store specific {@link AuditingHandler}
	 * implementations.
	 * 
	 * @param configuration, must not be {@literal null}.
	 * @return
	 */
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AnnotationAuditingConfiguration configuration) {

		Assert.notNull(configuration, "configuration must not be null!");

		return configureDefaultAuditHandlerAttributes(configuration,
				BeanDefinitionBuilder.rootBeanDefinition(AuditingHandler.class));
	}

	/**
	 * Configures the given {@link BeanDefinitionBuilder} with the default attributes from the given
	 * {@link AnnotationAuditingConfiguration}.
	 * 
	 * @param configuration
	 * @param builder
	 * @return the builder with the audit attributes configured.
	 */
	protected BeanDefinitionBuilder configureDefaultAuditHandlerAttributes(AnnotationAuditingConfiguration configuration,
			BeanDefinitionBuilder builder) {

		if (StringUtils.hasText(configuration.getAuditorAwareRef())) {
			builder.addPropertyValue(AUDITOR_AWARE,
					createLazyInitTargetSourceBeanDefinition(configuration.getAuditorAwareRef()));
		} else {
			builder.addPropertyReference(AUDITOR_AWARE, DEFAULT_AUDITOR_AWARE_BEAN_NAME);
		}

		builder.addPropertyValue(SET_DATES, configuration.isSetDates());
		builder.addPropertyValue(MODIFY_ON_CREATE, configuration.isModifyOnCreate());

		if (StringUtils.hasText(configuration.getDateTimeProviderRef())) {
			builder.addPropertyReference(DATE_TIME_PROVIDER, configuration.getDateTimeProviderRef());
		} else {
			builder.addPropertyValue(DATE_TIME_PROVIDER, CurrentDateTimeProvider.INSTANCE);
		}

		return builder;
	}

	/**
	 * Retrieve auditing configuration information.
	 * 
	 * @param annotationMetadata
	 * @return
	 */
	protected AnnotationAuditingConfiguration getConfiguration(AnnotationMetadata annotationMetadata) {
		return new AnnotationAuditingConfigurationSupport(annotationMetadata, getAnnotation());
	}

	/**
	 * @return the annotation to use for the configuration of the auditing feature.
	 */
	protected abstract Class<? extends Annotation> getAnnotation();

	/**
	 * @param auditingHandlerDefinition
	 * @param registry
	 */
	protected abstract void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
			BeanDefinitionRegistry registry);

	/**
	 * Registers the given {@link AbstractBeanDefinition} as a singleton infrastructure bean under the given id.
	 * 
	 * @param def
	 * @param id
	 * @param registry
	 */
	protected void registerInfrastructureBeanWithId(AbstractBeanDefinition def, String id, BeanDefinitionRegistry registry) {

		def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		def.setScope("singleton");
		registry.registerBeanDefinition(id, def);
	}

	private BeanDefinition createLazyInitTargetSourceBeanDefinition(String auditorAwareRef) {

		BeanDefinitionBuilder targetSourceBuilder = rootBeanDefinition(LazyInitTargetSource.class);
		targetSourceBuilder.addPropertyValue("targetBeanName", auditorAwareRef);

		BeanDefinitionBuilder builder = rootBeanDefinition(ProxyFactoryBean.class);
		builder.addPropertyValue("targetSource", targetSourceBuilder.getBeanDefinition());

		return builder.getBeanDefinition();
	}
}
