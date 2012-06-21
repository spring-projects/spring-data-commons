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
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} to create a {@link ResourceReaderRepositoryPopulator} using an {@link Unmarshaller}.
 * 
 * @author Oliver Gierke
 */
public class UnmarshallerRepositoryPopulatorFactoryBean extends AbstractRepositoryPopulatorFactoryBean {

	private Unmarshaller unmarshaller;

	/**
	 * Configures the {@link Unmarshaller} to be used.
	 * 
	 * @param unmarshaller the unmarshaller to set
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.init.AbstractRepositoryPopulatorFactoryBean#getResourceReader()
	 */
	@Override
	protected ResourceReader getResourceReader() {
		return new UnmarshallingResourceReader(unmarshaller);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(unmarshaller, "No Unmarshaller configured!");
		super.afterPropertiesSet();
	}
}
