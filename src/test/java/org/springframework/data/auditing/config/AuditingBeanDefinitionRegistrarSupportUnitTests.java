/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.auditing.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.config.AutowiredPropertyMarker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.EnableAuditing;
import org.springframework.data.mapping.context.PersistentEntities;

/**
 * Unit tests for {@link AuditingBeanDefinitionRegistrarSupport}.
 *
 * @author Ranie Jade Ramiso
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Francisco Soler
 * @author Jaeyeon Kim
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class AuditingBeanDefinitionRegistrarSupportUnitTests {

	@Mock BeanDefinitionRegistry registry;

	@Test // DATCMNS-389
	void testRegisterBeanDefinitions() {

		AuditingBeanDefinitionRegistrarSupport registrar = new DummyAuditingBeanDefinitionRegistrarSupport();
		AnnotationMetadata metadata = AnnotationMetadata.introspect(SampleConfig.class);

		registrar.registerBeanDefinitions(metadata, registry);
		verify(registry, times(1)).registerBeanDefinition(anyString(), any(BeanDefinition.class));
	}

	@Test // DATACMNS-1453
	void rejectsNullAnnotationMetadata() {

		AuditingBeanDefinitionRegistrarSupport registrar = new DummyAuditingBeanDefinitionRegistrarSupport();

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> registrar.registerBeanDefinitions(null, registry));
	}

	@Test // DATACMNS-1453
	void rejectsNullRegistry() {

		AuditingBeanDefinitionRegistrarSupport registrar = new DummyAuditingBeanDefinitionRegistrarSupport();
		AnnotationMetadata metadata = AnnotationMetadata.introspect(SampleConfig.class);

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> registrar.registerBeanDefinitions(metadata, null));
	}

	@Test // GH-3177
	void setsAuditorAwareAndDateTimeProviderIfConfigured() {

		AuditingConfiguration configuration = new DummyAuditingBeanDefinitionRegistrarSupport().getConfiguration(null);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AuditingHandler.class);
		DummyAuditingBeanDefinitionRegistrarSupport registrar = new DummyAuditingBeanDefinitionRegistrarSupport();

		BeanDefinitionBuilder result = registrar.configureDefaultAuditHandlerAttributes(configuration, builder);
		AbstractBeanDefinition beanDefinition = result.getBeanDefinition();

		assertThat(beanDefinition.getPropertyValues().contains("auditorAware")).isTrue();
		assertThat(beanDefinition.getPropertyValues().contains("dateTimeProvider")).isTrue();
	}

	@Test // GH-3177
	void doesNotSetAuditorAwareAndDateTimeProviderIfNotConfigured() {

		AuditingConfiguration configuration = new AuditingConfiguration() {
			@Override
			public String getAuditorAwareRef() {
				return "";
			}

			@Override
			public boolean isSetDates() {
				return true;
			}

			@Override
			public String getDateTimeProviderRef() {
				return "";
			}

			@Override
			public boolean isModifyOnCreate() {
				return true;
			}
		};

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AuditingHandler.class);
		DummyAuditingBeanDefinitionRegistrarSupport registrar = new DummyAuditingBeanDefinitionRegistrarSupport();

		BeanDefinitionBuilder result = registrar.configureDefaultAuditHandlerAttributes(configuration, builder);
		AbstractBeanDefinition beanDefinition = result.getBeanDefinition();

		assertThat(beanDefinition.getPropertyValues().get("auditorAware")).isEqualTo(AutowiredPropertyMarker.INSTANCE);
		assertThat(beanDefinition.getPropertyValues().get("dateTimeProvider")).isEqualTo(AutowiredPropertyMarker.INSTANCE);
	}

	@Test // GH-3177
	void optionalAutowiringShouldConsiderOptionalProperties() {

		AuditingConfiguration configuration = new AuditingConfiguration() {
			@Override
			public String getAuditorAwareRef() {
				return "";
			}

			@Override
			public boolean isSetDates() {
				return true;
			}

			@Override
			public String getDateTimeProviderRef() {
				return "";
			}

			@Override
			public boolean isModifyOnCreate() {
				return true;
			}
		};

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AuditingHandler.class);
		builder.addConstructorArgValue(PersistentEntities.of());
		DummyAuditingBeanDefinitionRegistrarSupport registrar = new DummyAuditingBeanDefinitionRegistrarSupport();

		BeanDefinitionBuilder result = registrar.configureDefaultAuditHandlerAttributes(configuration, builder);

		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("auditingHandler", result.getBeanDefinition());
		context.refresh();

		AuditingHandler handler = context.getBean(AuditingHandler.class);

		assertThat(handler).extracting("auditorAware").isEqualTo(Optional.empty());
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
				@Override
				public String getAuditorAwareRef() {
					return "auditor";
				}

				@Override
				public boolean isSetDates() {
					return true;
				}

				@Override
				public String getDateTimeProviderRef() {
					return "dateTimeProvider";
				}

				@Override
				public boolean isModifyOnCreate() {
					return true;
				}
			};
		}

		@Override
		protected String getAuditingHandlerBeanName() {
			return "auditingHandler";
		}
	}
}
