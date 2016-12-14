/*
 * Copyright 2008-2013 the original author or authors.
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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;

/**
 * Unit test for {@link DomainClassPropertyEditorRegistrar}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("deprecation")
public class DomainClassPropertyEditorRegistrarUnitTests {

	@Mock PropertyEditorRegistry registry;

	DomainClassPropertyEditorRegistrar registrar;
	GenericApplicationContext context;
	DomainClassPropertyEditor<Entity, Serializable> reference;

	@Before
	public void setup() {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DummyRepositoryFactoryBean.class);
		builder.addConstructorArgValue(EntityRepository.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("provider", builder.getBeanDefinition());

		context = new GenericApplicationContext(factory);
		context.refresh();
		registrar = new DomainClassPropertyEditorRegistrar();
	}

	@Test
	public void addsRepositoryForEntityIfAvailableInAppContext() throws Exception {

		registrar.setApplicationContext(context);
		registrar.registerCustomEditors(registry);

		verify(registry).registerCustomEditor(eq(Entity.class), any(DomainClassPropertyEditor.class));
	}

	@Test
	public void doesNotAddDaoAtAllIfNoDaosFound() throws Exception {

		registrar.registerCustomEditors(registry);

		verify(registry, never()).registerCustomEditor(eq(Entity.class), any(DomainClassPropertyEditor.class));
	}

	static class Entity {

	}

	static interface EntityRepository extends CrudRepository<Entity, Serializable> {

	}
}
