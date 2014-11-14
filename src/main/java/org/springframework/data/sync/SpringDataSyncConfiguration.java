/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.sync;

import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.sync.diffsync.Equivalency;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.config.DiffSyncConfigurerAdapter;

/**
 * Configuration class to register the Spring Data specific integration components for Spring Sync.
 * 
 * @author Oliver Gierke
 * @since 1.10
 */
@Configuration
public class SpringDataSyncConfiguration extends DiffSyncConfigurerAdapter {

	@Autowired List<MappingContext<?, ?>> mappingContexts;
	@Autowired(required = false) ConversionService conversionService;
	@Autowired ListableBeanFactory beanFactory;

	/**
	 * Creates a {@link Repositories} instance with meta-information about all Spring Data repositories available in the
	 * {@link ApplicationContext}.
	 * 
	 * @param factory
	 * @return
	 */
	@Bean
	public Repositories repositories() {
		return new Repositories(beanFactory);
	}

	/**
	 * Registers a special {@link Equivalency} implementation taking the Spring Data mapping information into account to
	 * lookup identifier values and compare them.
	 * 
	 * @return
	 */
	@Bean
	public PersistentEntitiesEquivalency persistentEntitiesEquivalency() {
		return new PersistentEntitiesEquivalency(new PersistentEntities(mappingContexts));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.config.DiffSyncConfigurerAdapter#addPersistenceCallbacks(org.springframework.sync.diffsync.PersistenceCallbackRegistry)
	 */
	@Override
	@SuppressWarnings({ "rawtypes" })
	public void addPersistenceCallbacks(PersistenceCallbackRegistry registry) {

		Repositories repositories = repositories();
		ConversionService service = conversionService == null ? new DefaultFormattingConversionService()
				: conversionService;

		RepositoryInvokerFactory factory = new DefaultRepositoryInvokerFactory(repositories, service);

		for (Class<?> domainType : repositories) {

			RepositoryMetadata metadata = repositories.getRepositoryInformationFor(domainType);
			RepositoryInvoker invoker = factory.getInvokerFor(domainType);

			registry.addPersistenceCallback(new RepositoryPersistenceCallback(invoker, metadata, new PersistentEntities(
					mappingContexts)));
		}
	}
}
