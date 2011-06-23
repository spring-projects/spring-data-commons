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

import static org.springframework.util.StringUtils.*;

import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.util.Assert;
import org.w3c.dom.Element;


/**
 * Base class for {@link SingleRepositoryConfigInformation} implementations. So
 * these implementations will capture information for XML elements manually
 * configuring a single repository bean.
 *
 * @author Oliver Gierke
 */
public abstract class ParentDelegatingRepositoryConfigInformation<T extends CommonRepositoryConfigInformation>
		implements SingleRepositoryConfigInformation<T> {

	private final T parent;


	/**
	 * Creates a new {@link ParentDelegatingRepositoryConfigInformation} with
	 * the given {@link CommonRepositoryConfigInformation} as parent.
	 *
	 * @param parent
	 */
	public ParentDelegatingRepositoryConfigInformation(T parent) {

		Assert.notNull(parent);
		this.parent = parent;
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.RepositoryInformation#
			 * getParent()
			 */
	protected T getParent() {

		return parent;
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.CommonRepositoryInformation
			 * #getBasePackage()
			 */
	public String getBasePackage() {

		return parent.getBasePackage();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.RepositoryInformation#
			 * getImplementationClassName()
			 */
	public String getImplementationClassName() {

		return capitalize(getBeanId()) + getRepositoryImplementationSuffix();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.RepositoryInformation#
			 * getImplementationBeanName()
			 */
	public String getImplementationBeanName() {

		return getBeanId() + getRepositoryImplementationSuffix();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.RepositoryInformation#
			 * autodetectCustomImplementation()
			 */
	public boolean autodetectCustomImplementation() {

		return true;
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.RepositoryInformation#
			 * getCustomImplementationRef()
			 */
	public String getCustomImplementationRef() {

		return getBeanId() + getRepositoryImplementationSuffix();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.CommonRepositoryInformation
			 * #getSource()
			 */
	public Element getSource() {

		return parent.getSource();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.repository.config.CommonRepositoryConfigInformation
			 * #getRepositoryImplementationSuffix()
			 */
	public String getRepositoryImplementationSuffix() {

		return parent.getRepositoryImplementationSuffix();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.repository.config.CommonRepositoryConfigInformation
			 * #getRepositoryFactoryBeanClassName()
			 */
	public String getRepositoryFactoryBeanClassName() {

		return parent.getRepositoryFactoryBeanClassName();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.RepositoryInformation#
			 * getTransactionManagerRef()
			 */
	public String getTransactionManagerRef() {

		return parent.getTransactionManagerRef();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.jpa.repository.config.RepositoryInformation#
			 * getQueryLookupStrategyKey()
			 */
	public Key getQueryLookupStrategyKey() {

		return parent.getQueryLookupStrategyKey();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.CommonRepositoryConfigInformation#getNamedQueriesLocation()
	 */
	public String getNamedQueriesLocation() {
		return parent.getNamedQueriesLocation();
	}
}
