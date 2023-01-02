/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.repository.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.lang.Nullable;

/**
 * Factory bean to create {@link PropertiesBasedNamedQueries}.
 * <p>
 * Supports loading from a properties file and/or setting local properties on this FactoryBean. The created Properties
 * instance will be merged from loaded and local values. If neither a location nor local properties are set, an
 * exception will be thrown on initialization.
 * <p>
 * Can create a singleton or a new object on each request. Default is a singleton.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class PropertiesBasedNamedQueriesFactoryBean extends PropertiesLoaderSupport
		implements FactoryBean<PropertiesBasedNamedQueries>, InitializingBean {

	private boolean singleton = true;

	private @Nullable PropertiesBasedNamedQueries singletonInstance;

	/**
	 * Set whether a shared singleton {@code PropertiesBasedNamedQueries} instance should be created, or rather a new
	 * {@code PropertiesBasedNamedQueries} instance on each request.
	 * <p>
	 * Default is {@code true} (a shared singleton).
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void afterPropertiesSet() throws IOException {
		if (this.singleton) {
			this.singletonInstance = new PropertiesBasedNamedQueries(createProperties());
		}
	}

	@Override
	@Nullable
	public PropertiesBasedNamedQueries getObject() throws IOException {
		if (this.singleton) {
			return this.singletonInstance;
		} else {
			return new PropertiesBasedNamedQueries(createProperties());
		}
	}

	@Override
	public Class<PropertiesBasedNamedQueries> getObjectType() {
		return PropertiesBasedNamedQueries.class;
	}

	protected Properties createProperties() throws IOException {
		return mergeProperties();
	}
}
