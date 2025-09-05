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
package org.springframework.data.repository.cdi;

import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import org.apache.webbeans.context.AbstractContext;
import org.mockito.Mockito;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;

/**
 * Dummy extension of {@link CdiRepositoryExtensionSupport} to allow integration tests. Will create mocks for repository
 * interfaces being found.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class DummyCdiExtension extends CdiRepositoryExtensionSupport {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

		afterBeanDiscovery.addContext(new MyCustomScope());

		for (var type : getRepositoryTypes()) {

			var bean = new DummyCdiRepositoryBean(type.getValue(), type.getKey(), beanManager,
					Optional.of(getCustomImplementationDetector()));
			registerBean(bean);
			afterBeanDiscovery.addBean(bean);
		}
	}

	static class DummyCdiRepositoryBean<T> extends CdiRepositoryBean<T> {

		public DummyCdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager) {
			super(qualifiers, repositoryType, beanManager);
		}

		DummyCdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager,
				Optional<CustomRepositoryImplementationDetector> detector) {
			super(qualifiers, repositoryType, beanManager, detector);
		}

		@Override
		public Class<? extends Annotation> getScope() {
			return MyScope.class;
		}

		@Override
		protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {

			var mock = Mockito.mock(repositoryType);

			return create(() -> new DummyRepositoryFactory(mock), repositoryType);
		}
	}

	@NormalScope
	@Retention(RetentionPolicy.RUNTIME)
	@interface MyScope {

	}

	@SuppressWarnings("serial")
	static class MyCustomScope extends AbstractContext {

		MyCustomScope() {
			super(MyScope.class);
			setActive(true);
		}

		@Override
		protected void setComponentInstanceMap() {
			componentInstanceMap = new HashMap<>();
		}

		@Override
		public boolean isActive() {
			return true;
		}
	}
}
