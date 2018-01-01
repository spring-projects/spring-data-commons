/*
 * Copyright 2013-2018 the original author or authors.
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
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link FactoryBean} to set up a {@link ResourceReaderRepositoryPopulator} with a {@link Jackson2ResourceReader}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.6
 */
public class Jackson2RepositoryPopulatorFactoryBean extends AbstractRepositoryPopulatorFactoryBean {

	private @Nullable ObjectMapper mapper;

	/**
	 * Configures the {@link ObjectMapper} to be used.
	 *
	 * @param mapper can be {@literal null}.
	 */
	public void setMapper(@Nullable ObjectMapper mapper) {
		this.mapper = mapper;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.init.AbstractRepositoryPopulatorFactoryBean#getResourceReader()
	 */
	@Override
	protected ResourceReader getResourceReader() {
		return new Jackson2ResourceReader(mapper);
	}
}
