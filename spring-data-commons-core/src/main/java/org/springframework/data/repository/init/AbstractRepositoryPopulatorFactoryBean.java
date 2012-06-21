/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.repository.init;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.Repositories;

/**
 * {@link FactoryBean} to set up a {@link ResourceReaderRepositoryPopulator} with a {@link JacksonResourceReader}.
 * 
 * @author Oliver Gierke
 */
public abstract class AbstractRepositoryPopulatorFactoryBean extends
		AbstractFactoryBean<ResourceReaderRepositoryPopulator> implements ApplicationListener<ContextRefreshedEvent> {

	private Resource[] resources;
	private RepositoryPopulator populator;

	/**
	 * Configures the {@link Resource}s to be used to load objects from and initialize the repositories eventually.
	 * 
	 * @param resources the resources to set
	 */
	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return ResourceReaderRepositoryPopulator.class;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
	 */
	@Override
	protected ResourceReaderRepositoryPopulator createInstance() throws Exception {

		ResourceReaderRepositoryPopulator initializer = new ResourceReaderRepositoryPopulator(getResourceReader());
		initializer.setResources(resources);

		this.populator = initializer;

		return initializer;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {

		if (event.equals(getBeanFactory())) {
			Repositories repositories = new Repositories(event.getApplicationContext());
			populator.populate(repositories);
		}
	}

	protected abstract ResourceReader getResourceReader();
}
