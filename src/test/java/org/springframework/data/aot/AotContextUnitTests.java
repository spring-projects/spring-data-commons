/*
 * Copyright 2025 the original author or authors.
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

import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;

import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.aot.types.Address;
import org.springframework.data.aot.types.Customer;
import org.springframework.data.aot.types.EmptyType1;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.StringUtils;

/**
 * Unit tests for {@link AotContext}.
 *
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AotContextUnitTests {

    @Mock BeanFactory beanFactory;

    @Mock AotMappingContext mappingContext;

    MockEnvironment mockEnvironment = new MockEnvironment();

    @Test // GH-2595
    void shouldContributeAccessorByDefault() {

        contributeAccessor(Address.class);
        verify(mappingContext).contribute(Address.class);
    }

    @Test // GH-2595
    void shouldConsiderDisabledAccessors() {

        mockEnvironment.setProperty("spring.aot.data.accessors.enabled", "false");

        contributeAccessor(Address.class);

        verifyNoInteractions(mappingContext);
    }

    @Test // GH-2595
    void shouldApplyExcludeFilters() {

        mockEnvironment.setProperty("spring.aot.data.accessors.exclude",
            Customer.class.getName() + " , " + EmptyType1.class.getName());

        contributeAccessor(Address.class, Customer.class, EmptyType1.class);

        verify(mappingContext).contribute(Address.class);
        verifyNoMoreInteractions(mappingContext);
    }

    @Test // GH-2595
    void shouldApplyIncludeExcludeFilters() {

        mockEnvironment.setProperty("spring.aot.data.accessors.include", Customer.class.getPackageName() + ".Add*");
        mockEnvironment.setProperty("spring.aot.data.accessors.exclude", Customer.class.getPackageName() + ".**");

        contributeAccessor(Address.class, Customer.class, EmptyType1.class);

        verify(mappingContext).contribute(Address.class);
        verifyNoMoreInteractions(mappingContext);
    }

    private void contributeAccessor(Class<?>... classes) {

        DefaultAotContext context = new DefaultAotContext(beanFactory, mockEnvironment);

        for (Class<?> aClass : classes) {
            context.typeConfiguration(aClass, AotTypeConfiguration::contributeAccessors);
        }

        context.typeConfigurations().forEach(it -> it.contribute(mockEnvironment, new TestGenerationContext()));
    }

    @ParameterizedTest // GH-3322
    @CsvSource({ //
        "'spring.aot.repositories.enabled', '', '', '', true", //
        "'spring.aot.repositories.enabled', 'true', '', '', true", //
        "'spring.aot.repositories.enabled', 'false', '', '', false", //
        "'spring.aot.repositories.enabled', '', 'commons', 'true', true", //
        "'spring.aot.repositories.enabled', 'true', 'commons', 'true', true", //
        "'spring.aot.repositories.enabled', '', 'commons', 'false', false", //
        "'spring.aot.repositories.enabled', 'false', 'commons', 'true', false" //
    })
    void considersEnvironmentSettingsForGeneratedRepositories(String generalFlag, String generalValue, String storeName,
        String storeValue, boolean enabled) {

        MockAotContext ctx = new MockAotContext();
        if (StringUtils.hasText(generalFlag) && StringUtils.hasText(generalValue)) {
            ctx.withProperty(generalFlag, generalValue);
        }
        if (StringUtils.hasText(storeName) && StringUtils.hasText(storeValue)) {
            ctx.withProperty("spring.aot.%s.repositories.enabled".formatted(storeName), storeValue);
        }

        Assertions.assertThat(ctx.isGeneratedRepositoriesEnabled(storeName)).isEqualTo(enabled);
    }

    static class MockAotContext implements AotContext {

        private final MockEnvironment environment;

        public MockAotContext() {
            this.environment = new MockEnvironment();
        }

        MockAotContext withProperty(String key, String value) {
            environment.setProperty(key, value);
            return this;
        }

        @Override
        public ConfigurableListableBeanFactory getBeanFactory() {
            return Mockito.mock(ConfigurableListableBeanFactory.class);
        }

        @Override
        public TypeIntrospector introspectType(String typeName) {
            return Mockito.mock(TypeIntrospector.class);
        }

        @Override
        public IntrospectedBeanDefinition introspectBeanDefinition(String beanName) {
            return Mockito.mock(IntrospectedBeanDefinition.class);
        }

        @Override
        public void typeConfiguration(Class<?> type, Consumer<AotTypeConfiguration> configurationConsumer) {

        }

        @Override
        public Collection<AotTypeConfiguration> typeConfigurations() {
            return List.of();
        }

        @Override
        public Environment getEnvironment() {
            return environment;
        }
    }
}
