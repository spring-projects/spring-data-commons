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

import java.io.Serializable;
import java.util.Map.Entry;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;

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
 */
public class DomainClassPropertyEditorRegistrar implements PropertyEditorRegistrar, ApplicationContextAware {

	private Repositories repositories = Repositories.NONE;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.PropertyEditorRegistrar#registerCustomEditors(org.springframework.beans.PropertyEditorRegistry)
	 */
	public void registerCustomEditors(PropertyEditorRegistry registry) {

		for (Entry<EntityInformation<Object, Serializable>, CrudRepository<Object, Serializable>> entry : repositories) {

			EntityInformation<Object, Serializable> entityInformation = entry.getKey();
			CrudRepository<Object, Serializable> repository = entry.getValue();

			DomainClassPropertyEditor<Object, Serializable> editor = new DomainClassPropertyEditor<Object, Serializable>(
					repository, entityInformation, registry);

			registry.registerCustomEditor(entityInformation.getJavaType(), editor);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext context) {
		this.repositories = new Repositories(context);
	}
}
