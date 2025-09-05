/*
 * Copyright 2011-2025 the original author or authors.
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

	private static final String NO_QUERY_FOUND = "No query with name %s found; Make sure you call hasQuery(…) before calling this method";

	public static final NamedQueries EMPTY = new PropertiesBasedNamedQueries(new Properties());

	private final Properties properties;

	public PropertiesBasedNamedQueries(Properties properties) {
		this.properties = properties;
	}

	@Override
	public boolean hasQuery(String queryName) {

		Assert.hasText(queryName, "Query name must not be null or empty");

		return properties.containsKey(queryName);
	}

	@Override
	public String getQuery(String queryName) {

		Assert.hasText(queryName, "Query name must not be null or empty");

		String query = properties.getProperty(queryName);

		if (query == null) {
			throw new IllegalArgumentException(String.format(NO_QUERY_FOUND, queryName));
		}

		return query;
	}
}
