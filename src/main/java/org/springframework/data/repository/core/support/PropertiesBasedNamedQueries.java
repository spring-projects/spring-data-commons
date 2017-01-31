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
package org.springframework.data.repository.core.support;

import java.util.Properties;

import org.springframework.data.repository.core.NamedQueries;
import org.springframework.util.Assert;

/**
 * {@link NamedQueries} implementation backed by a {@link Properties} instance.
 * 
 * @author Oliver Gierke
 */
public class PropertiesBasedNamedQueries implements NamedQueries {

	public static final NamedQueries EMPTY = new PropertiesBasedNamedQueries(new Properties());

	private final Properties properties;

	/**
	 * Creates a new {@link PropertiesBasedNamedQueries} for the given {@link Properties} instance.
	 * 
	 * @param properties
	 */
	public PropertiesBasedNamedQueries(Properties properties) {
		Assert.notNull(properties, "Properties must not be null!");
		this.properties = properties;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.NamedQueries#hasNamedQuery(java.lang.String)
	 */
	public boolean hasQuery(String queryName) {
		return properties.containsKey(queryName);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.NamedQueries#getNamedQuery(java.lang.String)
	 */
	public String getQuery(String queryName) {
		return properties.getProperty(queryName);
	}
}
