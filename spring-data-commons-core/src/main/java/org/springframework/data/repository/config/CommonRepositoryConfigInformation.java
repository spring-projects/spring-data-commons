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

import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.transaction.PlatformTransactionManager;
import org.w3c.dom.Element;


/**
 * Interface for shared repository information.
 *
 * @author Oliver Gierke
 */
public interface CommonRepositoryConfigInformation {

	/**
	 * Returns the element the repository information is derived from.
	 *
	 * @return
	 */
	Element getSource();


	/**
	 * Returns the base package.
	 *
	 * @return
	 */
	String getBasePackage();


	/**
	 * Returns the suffix to use for implementation bean lookup or class
	 * detection.
	 *
	 * @return
	 */
	String getRepositoryImplementationSuffix();


	/**
	 * Returns the configured repository factory class.
	 *
	 * @return
	 */
	String getRepositoryFactoryBeanClassName();


	/**
	 * Returns the bean name of the {@link PlatformTransactionManager} to be
	 * used. Returns {@literal null} if no reference has been configured
	 * explicitly.
	 *
	 * @return
	 */
	String getTransactionManagerRef();


	/**
	 * Returns the strategy finder methods should be resolved.
	 *
	 * @return
	 */
	Key getQueryLookupStrategyKey();

	/**
	 * Returns the location of the properties file to contain named queries.
	 * 
	 * @return
	 */
	String getNamedQueriesLocation();
}