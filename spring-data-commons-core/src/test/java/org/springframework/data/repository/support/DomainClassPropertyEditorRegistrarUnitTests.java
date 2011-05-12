/*
 * Copyright 2008-2011 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;


/**
 * Unit test for {@link DomainClassPropertyEditorRegistrar}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DomainClassPropertyEditorRegistrarUnitTests {

	DomainClassPropertyEditorRegistrar registrar =
			new DomainClassPropertyEditorRegistrar();
	@Mock
	ApplicationContext context;
	@Mock
	PropertyEditorRegistry registry;
	@Mock
	EntityRepository repository;
	@Mock
	EntityInformation<Entity, Long> information;
	@Mock
	RepositoryFactoryInformation<Entity, Long> provider;

	DomainClassPropertyEditor<Entity, Long> reference;


	@Before
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setup() {

		when(information.getJavaType()).thenReturn(Entity.class);
		when(provider.getEntityInformation()).thenReturn(information);
		when(provider.getRepositoryInterface()).thenReturn(
				(Class) EntityRepository.class);
		Map<String, EntityRepository> map = getBeanAsMap(repository);
		when(context.getBeansOfType(EntityRepository.class)).thenReturn(map);

		reference =
				new DomainClassPropertyEditor<Entity, Long>(repository,
						information, registry);
	}


	@Test
	public void addsRepositoryForEntityIfAvailableInAppContext()
			throws Exception {

		letContextContain(provider);
		registrar.setApplicationContext(context);
		registrar.registerCustomEditors(registry);

		verify(registry).registerCustomEditor(eq(Entity.class), eq(reference));
	}


	@Test
	public void doesNotAddDaoAtAllIfNoDaosFound() throws Exception {

		letContextContain(provider);
		registrar.registerCustomEditors(registry);

		verify(registry, never()).registerCustomEditor(eq(Entity.class),
				eq(reference));
	}


	private void letContextContain(Object bean) {

		Map<String, Object> beanMap = getBeanAsMap(bean);

		when(context.getBeansOfType(argThat(is(subtypeOf(bean.getClass())))))
				.thenReturn(beanMap);
	}


	private <T> Map<String, T> getBeanAsMap(T bean) {

		Map<String, T> beanMap = new HashMap<String, T>();
		beanMap.put(bean.getClass().getName(), bean);
		return beanMap;
	}

	@SuppressWarnings("serial")
	private static class Entity implements Serializable {

	}

	private static interface EntityRepository extends CrudRepository<Entity, Long> {

	}


	private static <T> TypeSafeMatcher<Class<T>> subtypeOf(
			final Class<? extends T> type) {

		return new TypeSafeMatcher<Class<T>>() {

			public void describeTo(Description arg0) {

				arg0.appendText("not a subtype of");
			}


			@Override
			public boolean matchesSafely(Class<T> arg0) {

				return arg0.isAssignableFrom(type);
			}
		};
	}
}
