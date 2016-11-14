/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.repository.config;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
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

	@Test
	public void findsAnnotatedRepositoryInterface() {

		RepositoryComponentProvider provider = new RepositoryComponentProvider(Collections.<TypeFilter> emptyList());
		Set<BeanDefinition> components = provider.findCandidateComponents("org.springframework.data.repository.sample");

		assertThat(components).hasSize(3);
		assertThat(components).extracting(BeanDefinition::getBeanClassName)
				.contains(SampleAnnotatedRepository.class.getName());
	}

	@Test
	public void limitsFoundRepositoriesToIncludeFiltersOnly() {

		List<? extends TypeFilter> filters = Arrays.asList(new AssignableTypeFilter(MyOtherRepository.class));

		RepositoryComponentProvider provider = new RepositoryComponentProvider(filters);
		Set<BeanDefinition> components = provider.findCandidateComponents("org.springframework.data.repository");

		assertThat(components).hasSize(1);
		assertThat(components).extracting(BeanDefinition::getBeanClassName).contains(MyOtherRepository.class.getName());
	}

	/**
	 * @DATACMNS-90
	 */
	@Test
	public void shouldConsiderNestedRepositoryInterfacesIfEnabled() {

		RepositoryComponentProvider provider = new RepositoryComponentProvider(Collections.<TypeFilter> emptyList());
		provider.setConsiderNestedRepositoryInterfaces(true);

		Set<BeanDefinition> components = provider.findCandidateComponents("org.springframework.data.repository.config");
		String nestedRepositoryClassName = "org.springframework.data.repository.config.RepositoryComponentProviderUnitTests$MyNestedRepository";

		assertThat(components.size()).isGreaterThanOrEqualTo(1);
		assertThat(components).extracting(BeanDefinition::getBeanClassName).contains(nestedRepositoryClassName);
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
