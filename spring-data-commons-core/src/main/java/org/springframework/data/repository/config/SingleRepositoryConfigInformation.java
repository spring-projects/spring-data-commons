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

/**
 * Interface to capture configuration information necessary to set up a single repository instance.
 * 
 * @author Oliver Gierke
 */
public interface SingleRepositoryConfigInformation<T extends CommonRepositoryConfigInformation> extends
		CommonRepositoryConfigInformation {

	/**
	 * Returns the bean name to be used for the repository.
	 * 
	 * @return
	 */
	String getBeanId();

	/**
	 * Returns the name of the repository interface.
	 * 
	 * @return
	 */
	String getInterfaceName();

	/**
	 * Returns the class name of a possible custom repository implementation class to detect.
	 * 
	 * @return
	 */
	String getImplementationClassName();

	/**
	 * Returns the bean name a possibly found custom implementation shall be registered under.
	 * 
	 * @return
	 */
	String getImplementationBeanName();

	/**
	 * Returns the bean reference to the custom repository implementation.
	 * 
	 * @return
	 */
	String getCustomImplementationRef();

	/**
	 * Returns whether to try to autodetect a custom implementation.
	 * 
	 * @return
	 */
	boolean autodetectCustomImplementation();
}