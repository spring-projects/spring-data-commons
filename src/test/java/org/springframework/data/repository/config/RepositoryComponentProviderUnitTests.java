/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
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
public class RepositoryComponentProviderUnitTests {

	BeanDefinitionRegistry registry = mock(BeanDefinitionRegistry.class);

	@Test
	public void findsAnnotatedRepositoryInterface() {

		RepositoryComponentProvider provider = new RepositoryComponentProvider(Collections.emptyList(), registry);
		Set<BeanDefinition> components = provider.findCandidateComponents("org.springframework.data.repository.sample");

		assertThat(components).hasSize(3);
		assertThat(components).extracting(BeanDefinition::getBeanClassName)
				.contains(SampleAnnotatedRepository.class.getName());
	}

	@Test
	public void limitsFoundRepositoriesToIncludeFiltersOnly() {

		List<? extends TypeFilter> filters = Collections.singletonList(new AssignableTypeFilter(MyOtherRepository.class));

		RepositoryComponentProvider provider = new RepositoryComponentProvider(filters, registry);
		Set<BeanDefinition> components = provider.findCandidateComponents("org.springframework.data.repository");

		assertThat(components).hasSize(1);
		assertThat(components).extracting(BeanDefinition::getBeanClassName).contains(MyOtherRepository.class.getName());
	}

	@Test // DATACMNS-90
	public void shouldConsiderNestedRepositoryInterfacesIfEnabled() {

		RepositoryComponentProvider provider = new RepositoryComponentProvider(Collections.emptyList(), registry);
		provider.setConsiderNestedRepositoryInterfaces(true);

		Set<BeanDefinition> components = provider.findCandidateComponents("org.springframework.data.repository.config");
		String nestedRepositoryClassName = "org.springframework.data.repository.config.RepositoryComponentProviderUnitTests$MyNestedRepository";

		assertThat(components.size()).isGreaterThanOrEqualTo(1);
		assertThat(components).extracting(BeanDefinition::getBeanClassName).contains(nestedRepositoryClassName);
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-1098
	public void rejectsNullBeanDefinitionRegistry() {
		new RepositoryComponentProvider(Collections.emptyList(), null);
	}

	@Test // DATACMNS-1098
	public void exposesBeanDefinitionRegistry() {

		RepositoryComponentProvider provider = new RepositoryComponentProvider(Collections.emptyList(), registry);

		assertThat(provider.getRegistry()).isEqualTo(registry);
	}

	static class BeanDefinitionOfTypeMatcher extends BaseMatcher<BeanDefinition> {

		private final Class<?> expectedType;

		private BeanDefinitionOfTypeMatcher(Class<?> expectedType) {
			this.expectedType = expectedType;
		}

		public static BeanDefinitionOfTypeMatcher beanDefinitionOfType(Class<?> expectedType) {
			return new BeanDefinitionOfTypeMatcher(expectedType);
		}

		/*
		 * (non-Javadoc)
		 * @see org.hamcrest.Matcher#matches(java.lang.Object)
		 */
		@Override
		public boolean matches(Object item) {

			if (!(item instanceof BeanDefinition)) {
				return false;
			}

			BeanDefinition definition = (BeanDefinition) item;
			return definition.getBeanClassName().equals(expectedType.getName());
		}

		/*
		 * (non-Javadoc)
		 * @see org.hamcrest.SelfDescribing#describeTo(org.hamcrest.Description)
		 */
		@Override
		public void describeTo(Description description) {}
	}

	public interface MyNestedRepository extends Repository<Person, Long> {}
}
