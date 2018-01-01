/*
 * Copyright 2013-2018 the original author or authors.
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
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
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
 * {@link AuditingConfiguration}).
 *
 * @author Ranie Jade Ramiso
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
public abstract class AuditingBeanDefinitionRegistrarSupport implements ImportBeanDefinitionRegistrar {

	private static final String AUDITOR_AWARE = "auditorAware";
	private static final String DATE_TIME_PROVIDER = "dateTimeProvider";
	private static final String MODIFY_ON_CREATE = "modifyOnCreation";
	private static final String SET_DATES = "dateTimeForNow";

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

		Assert.notNull(annotationMetadata, "AnnotationMetadata must not be null!");
		Assert.notNull(annotationMetadata, "BeanDefinitionRegistry must not be null!");

		AbstractBeanDefinition ahbd = registerAuditHandlerBeanDefinition(registry, getConfiguration(annotationMetadata));
		registerAuditListenerBeanDefinition(ahbd, registry);
	}

	/**
	 * Registers an appropriate BeanDefinition for an {@link AuditingHandler}.
	 *
	 * @param registry must not be {@literal null}.
	 * @param configuration must not be {@literal null}.
	 * @return
	 */
	private AbstractBeanDefinition registerAuditHandlerBeanDefinition(BeanDefinitionRegistry registry,
			AuditingConfiguration configuration) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");
		Assert.notNull(configuration, "AuditingConfiguration must not be null!");

		AbstractBeanDefinition ahbd = getAuditHandlerBeanDefinitionBuilder(configuration).getBeanDefinition();
		registry.registerBeanDefinition(getAuditingHandlerBeanName(), ahbd);
		return ahbd;
	}

	/**
	 * Creates a {@link BeanDefinitionBuilder} to ease the definition of store specific {@link AuditingHandler}
	 * implementations.
	 *
	 * @param configuration must not be {@literal null}.
	 * @return
	 */
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AuditingConfiguration configuration) {

		Assert.notNull(configuration, "AuditingConfiguration must not be null!");

		return configureDefaultAuditHandlerAttributes(configuration,
				BeanDefinitionBuilder.rootBeanDefinition(AuditingHandler.class));
	}

	/**
	 * Configures the given {@link BeanDefinitionBuilder} with the default attributes from the given
	 * {@link AuditingConfiguration}.
	 *
	 * @param configuration must not be {@literal null}.
	 * @param builder must not be {@literal null}.
	 * @return the builder with the audit attributes configured.
	 */
	protected BeanDefinitionBuilder configureDefaultAuditHandlerAttributes(AuditingConfiguration configuration,
			BeanDefinitionBuilder builder) {

		if (StringUtils.hasText(configuration.getAuditorAwareRef())) {
			builder.addPropertyValue(AUDITOR_AWARE,
					createLazyInitTargetSourceBeanDefinition(configuration.getAuditorAwareRef()));
		} else {
			builder.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
		}

		builder.addPropertyValue(SET_DATES, configuration.isSetDates());
		builder.addPropertyValue(MODIFY_ON_CREATE, configuration.isModifyOnCreate());

		if (StringUtils.hasText(configuration.getDateTimeProviderRef())) {
			builder.addPropertyReference(DATE_TIME_PROVIDER, configuration.getDateTimeProviderRef());
		} else {
			builder.addPropertyValue(DATE_TIME_PROVIDER, CurrentDateTimeProvider.INSTANCE);
		}

		builder.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);

		return builder;
	}

	/**
	 * Retrieve auditing configuration from the given {@link AnnotationMetadata}.
	 *
	 * @param annotationMetadata will never be {@literal null}.
	 * @return
	 */
	protected AuditingConfiguration getConfiguration(AnnotationMetadata annotationMetadata) {
		return new AnnotationAuditingConfiguration(annotationMetadata, getAnnotation());
	}

	/**
	 * Return the annotation type to lookup configuration values from.
	 *
	 * @return must not be {@literal null}.
	 */
	protected abstract Class<? extends Annotation> getAnnotation();

	/**
	 * Register the listener to eventually trigger the {@link AuditingHandler}.
	 *
	 * @param auditingHandlerDefinition will never be {@literal null}.
	 * @param registry will never be {@literal null}.
	 */
	protected abstract void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
			BeanDefinitionRegistry registry);

	/**
	 * Return the name to be used to register the {@link AuditingHandler} under.
	 *
	 * @return
	 */
	protected abstract String getAuditingHandlerBeanName();

	/**
	 * Registers the given {@link AbstractBeanDefinition} as infrastructure bean under the given id.
	 *
	 * @param definition must not be {@literal null}.
	 * @param id must not be {@literal null} or empty.
	 * @param registry must not be {@literal null}.
	 */
	protected void registerInfrastructureBeanWithId(AbstractBeanDefinition definition, String id,
			BeanDefinitionRegistry registry) {

		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(id, definition);
	}

	private BeanDefinition createLazyInitTargetSourceBeanDefinition(String auditorAwareRef) {

		BeanDefinitionBuilder targetSourceBuilder = rootBeanDefinition(LazyInitTargetSource.class);
		targetSourceBuilder.addPropertyValue("targetBeanName", auditorAwareRef);

		BeanDefinitionBuilder builder = rootBeanDefinition(ProxyFactoryBean.class);
		builder.addPropertyValue("targetSource", targetSourceBuilder.getBeanDefinition());

		return builder.getBeanDefinition();
	}
}
