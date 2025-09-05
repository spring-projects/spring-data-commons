/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.repository.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSourceUnitTests.Person;
import org.springframework.data.repository.sample.SampleAnnotatedRepository;

/**
 * Unit tests for {@link RepositoryComponentProvider}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class RepositoryComponentProviderUnitTests {

	BeanDefinitionRegistry registry = mock(BeanDefinitionRegistry.class);

	@Test
	void findsAnnotatedRepositoryInterface() {

		var provider = new RepositoryComponentProvider(Collections.emptyList(), registry);
		var components = provider.findCandidateComponents("org.springframework.data.repository.sample");

		assertThat(components).hasSize(4);
		assertThat(components).extracting(BeanDefinition::getBeanClassName)
				.contains(SampleAnnotatedRepository.class.getName());
	}

	@Test
	void limitsFoundRepositoriesToIncludeFiltersOnly() {

		List<? extends TypeFilter> filters = Collections.singletonList(new AssignableTypeFilter(MyOtherRepository.class));

		var provider = new RepositoryComponentProvider(filters, registry);
		var components = provider.findCandidateComponents("org.springframework.data.repository");

		assertThat(components).hasSize(1);
		assertThat(components).extracting(BeanDefinition::getBeanClassName).contains(MyOtherRepository.class.getName());
	}

	@Test // DATACMNS-90
	void shouldConsiderNestedRepositoryInterfacesIfEnabled() {

		var provider = new RepositoryComponentProvider(Collections.emptyList(), registry);
		provider.setConsiderNestedRepositoryInterfaces(true);

		var components = provider.findCandidateComponents("org.springframework.data.repository.config");
		var nestedRepositoryClassName = "org.springframework.data.repository.config.RepositoryComponentProviderUnitTests$MyNestedRepositoryDefinition";

		assertThat(components.size()).isGreaterThanOrEqualTo(1);
		assertThat(components).extracting(BeanDefinition::getBeanClassName).contains(nestedRepositoryClassName);
	}

	@Test // DATACMNS-1098
	void rejectsNullBeanDefinitionRegistry() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new RepositoryComponentProvider(Collections.emptyList(), null));
	}

	@Test // DATACMNS-1098
	void exposesBeanDefinitionRegistry() {

		var provider = new RepositoryComponentProvider(Collections.emptyList(), registry);

		assertThat(provider.getRegistry()).isEqualTo(registry);
	}

	interface MyNestedRepositoryDefinition extends Repository<Person, Long> {}
}
