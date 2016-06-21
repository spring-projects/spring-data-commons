/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.auditing.EnableAuditing;

/**
 * Unit tests for {@link AuditingBeanDefinitionRegistrarSupport}.
 * 
 * @author Ranie Jade Ramiso
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class AuditingBeanDefinitionRegistrarSupportUnitTests {

	@Mock BeanDefinitionRegistry registry;

	/**
	 * @see DATCMNS-389
	 */
	@Test
	public void testRegisterBeanDefinitions() {

		AuditingBeanDefinitionRegistrarSupport registrar = new DummyAuditingBeanDefinitionRegistrarSupport();
		AnnotationMetadata metadata = new StandardAnnotationMetadata(SampleConfig.class);

		registrar.registerBeanDefinitions(metadata, registry);
		verify(registry, times(1)).registerBeanDefinition(anyString(), any(BeanDefinition.class));
	}

	static class SampleConfig {}

	static class DummyAuditingBeanDefinitionRegistrarSupport extends AuditingBeanDefinitionRegistrarSupport {

		@Override
		protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
				BeanDefinitionRegistry registry) {}

		@Override
		protected Class<? extends Annotation> getAnnotation() {
			return EnableAuditing.class;
		}

		@Override
		protected AuditingConfiguration getConfiguration(AnnotationMetadata annotationMetadata) {

			return new AuditingConfiguration() {
				public String getAuditorAwareRef() {
					return "auditor";
				}

				public boolean isSetDates() {
					return true;
				}

				public String getDateTimeProviderRef() {
					return "dateTimeProvider";
				}

				public boolean isModifyOnCreate() {
					return true;
				}
			};
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#getAuditingHandlerBeanName()
		 */
		@Override
		protected String getAuditingHandlerBeanName() {
			return "auditingHandler";
		}
	}
}
