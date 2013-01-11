/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.repository.cdi;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;

import org.mockito.Mockito;

/**
 * Dummy extension of {@link CdiRepositoryExtensionSupport} to allow integration tests. Will create mocks for repository
 * interfaces being found.
 * 
 * @author Oliver Gierke
 */
public class DummyCdiExtension extends CdiRepositoryExtensionSupport {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
		for (Entry<Class<?>, Set<Annotation>> type : getRepositoryTypes()) {
			afterBeanDiscovery.addBean(new DummyCdiRepositoryBean(type.getValue(), type.getKey(), beanManager));
		}
	}

	static class DummyCdiRepositoryBean<T> extends CdiRepositoryBean<T> {

		public DummyCdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager) {
			super(qualifiers, repositoryType, beanManager);
		}

		public Class<? extends Annotation> getScope() {
			return MyScope.class;
		}

		@Override
		protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {
			return Mockito.mock(repositoryType);
		}
	}

	@NormalScope
	@Retention(RetentionPolicy.RUNTIME)
	@interface MyScope {

	}
}
