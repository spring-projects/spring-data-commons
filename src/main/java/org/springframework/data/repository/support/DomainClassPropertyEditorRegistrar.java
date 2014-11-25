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

import java.io.Serializable;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Simple helper class to use Hades DAOs to provide {@link java.beans.PropertyEditor}s for domain classes. To get this
 * working configure a {@link org.springframework.web.bind.support.ConfigurableWebBindingInitializer} for your
 * {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter} and register the
 * {@link DomainClassPropertyEditorRegistrar} there: <code>
 * &lt;bean class="org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter"&gt;
 * &lt;property name="webBindingInitializer"&gt;
 * &lt;bean class="org.springframework.web.bind.support.ConfigurableWebBindingInitializer"&gt;
 * &lt;property name="propertyEditorRegistrars"&gt;
 * &lt;bean class="org.springframework.data.extensions.beans.DomainClassPropertyEditorRegistrar" /&gt;
 * &lt;/property&gt;
 * &lt;/bean&gt;
 * &lt;/property&gt;
 * &lt;/bean&gt;
 * </code> Make sure this bean declaration is in the {@link ApplicationContext} created by the {@link DispatcherServlet}
 * whereas the repositories need to be declared in the root
 * {@link org.springframework.web.context.WebApplicationContext}.
 * 
 * @author Oliver Gierke
 * @deprecated use {@link DomainClassConverter} instead, will be removed in 1.10
 */
@Deprecated
public class DomainClassPropertyEditorRegistrar implements PropertyEditorRegistrar, ApplicationContextAware {

	private Repositories repositories = Repositories.NONE;
	private RepositoryInvokerFactory repositoryInvokerFactory;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.PropertyEditorRegistrar#registerCustomEditors(org.springframework.beans.PropertyEditorRegistry)
	 */
	public void registerCustomEditors(PropertyEditorRegistry registry) {

		for (Class<?> domainClass : repositories) {

			RepositoryInformation repositoryInformation = repositories.getRepositoryInformationFor(domainClass);
			RepositoryInvoker invoker = repositoryInvokerFactory.getInvokerFor(domainClass);

			DomainClassPropertyEditor<Object, Serializable> editor = new DomainClassPropertyEditor<Object, Serializable>(
					invoker, repositories.getEntityInformationFor(repositoryInformation.getDomainType()), registry);

			registry.registerCustomEditor(repositoryInformation.getDomainType(), editor);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext context) {

		this.repositories = new Repositories(context);
		this.repositoryInvokerFactory = new DefaultRepositoryInvokerFactory(repositories);
	}
}
