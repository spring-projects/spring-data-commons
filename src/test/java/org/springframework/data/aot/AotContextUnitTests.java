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

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.aot.types.Address;
import org.springframework.data.aot.types.Customer;
import org.springframework.data.aot.types.EmptyType1;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link AotContext}.
 *
 * @author Mark Paluch
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

		DefaultAotContext context = new DefaultAotContext(beanFactory, mappingContext);

		for (Class<?> aClass : classes) {
			context.typeConfiguration(aClass, AotTypeConfiguration::contributeAccessors);
		}

		context.typeConfigurations().forEach(it -> it.contribute(mockEnvironment, new TestGenerationContext()));
	}
}
