/*
 * Copyright 2008-2010 the original author or authors.
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
package org.springframework.data.repository.config;

import static org.springframework.util.ClassUtils.*;
import static org.springframework.util.StringUtils.*;

import org.springframework.util.Assert;

/**
 * A {@link SingleRepositoryConfigInformation} implementation that is not backed by an XML element but by a scanned
 * interface. As this is derived from the parent, most of the lookup logic is delegated to the parent as well.
 * 
 * @author Oliver Gierke
 */
public class AutomaticRepositoryConfigInformation<S extends CommonRepositoryConfigInformation> extends
		ParentDelegatingRepositoryConfigInformation<S> {

	private final String interfaceName;

	/**
	 * Creates a new {@link AutomaticRepositoryConfigInformation} for the given interface name and
	 * {@link CommonRepositoryConfigInformation} parent.
	 * 
	 * @param interfaceName
	 * @param parent
	 */
	public AutomaticRepositoryConfigInformation(String interfaceName, S parent) {

		super(parent);
		Assert.notNull(interfaceName);
		this.interfaceName = interfaceName;
	}

	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.repository.config.SingleRepositoryConfigInformation
			 * #getBeanId()
			 */
	public String getBeanId() {

		return uncapitalize(getShortName(interfaceName));
	}

	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.repository.config.SingleRepositoryConfigInformation
			 * #getInterfaceName()
			 */
	public String getInterfaceName() {

		return interfaceName;
	}
}
