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
 * @author Oliver Gierke
 */
public interface GlobalRepositoryConfigInformation<T extends SingleRepositoryConfigInformation<?>>
        extends CommonRepositoryConfigInformation {

    /**
     * Returns the
     * 
     * @param interfaceName
     * @return
     */
    T getAutoconfigRepositoryInformation(String interfaceName);


    /**
     * Returns all {@link SingleRepositoryConfigInformation} instances used for
     * manual configuration.
     * 
     * @return
     */
    Iterable<T> getSingleRepositoryConfigInformations();


    /**
     * Returns whether to consider manual configuration. If this returns true,
     * clients should use {@link #getSingleRepositoryConfigInformations()} to
     * lookup configuration information for individual repository beans.
     * 
     * @return
     */
    boolean configureManually();


    /**
     * Returns the base interface to use
     * 
     * @return
     */
    Class<?> getRepositoryBaseInterface();
}