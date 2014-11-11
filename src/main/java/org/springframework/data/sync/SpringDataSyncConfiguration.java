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
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.core.CrudInvoker;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.support.Repositories;
import org.springframework.sync.diffsync.Equivalency;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;

/**
 * Configuration class to register the Spring Data specific integration components for Spring Sync.
 * 
 * @author Oliver Gierke
 */
@Configuration
public class SpringDataSyncConfiguration {

	@Autowired List<MappingContext<?, ?>> mappingContexts;
	@Autowired(required = false) ConversionService conversionService;

	/**
	 * Creates a {@link Repositories} instance with meta-information about all Spring Data repositories available in the
	 * {@link ApplicationContext}.
	 * 
	 * @param factory
	 * @return
	 */
	@Bean
	public Repositories repositories(ListableBeanFactory factory) {
		return new Repositories(factory);
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

	/**
	 * Registers a {@link RepositoryPersistenceCallback} for each of the repositories found in {@link Repositories}.
	 * 
	 * @param repositories
	 * @return
	 */
	@Bean
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PersistenceCallbackRegistry persistenceCallbackRegistry(Repositories repositories) {

		PersistenceCallbackRegistry registry = new PersistenceCallbackRegistry();
		ConversionService service = conversionService == null ? new GenericConversionService() : conversionService;

		for (Class<?> domainType : repositories) {

			CrudInvoker<?> invoker = repositories.getCrudInvoker(domainType);
			RepositoryMetadata metadata = repositories.getRepositoryInformationFor(domainType);
			registry.addPersistenceCallback(new RepositoryPersistenceCallback(invoker, metadata, service));
		}

		return registry;
	}
}
