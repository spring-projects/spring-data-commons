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
package org.springframework.data.repository.support;

import java.io.Serializable;

import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.Repository;
import org.springframework.util.Assert;


/**
 * Abstract base class for generic repositories. Captures information about the
 * domain class to be managed.
 * 
 * @author Oliver Gierke
 * @param <T> the type of entity to be handled
 */
public abstract class RepositorySupport<T, ID extends Serializable> implements
        Repository<T, ID> {

    private final Class<T> domainClass;
    private final IsNewAware isNewStrategy;


    /**
     * Creates a new {@link RepositorySupport}.
     * 
     * @param domainClass
     */
    public RepositorySupport(Class<T> domainClass) {

        Assert.notNull(domainClass);
        this.domainClass = domainClass;
        this.isNewStrategy = createIsNewStrategy(domainClass);
        Assert.notNull(isNewStrategy);
    }


    /**
     * Returns the domain class to handle.
     * 
     * @return the domain class
     */
    protected Class<T> getDomainClass() {

        return domainClass;
    }


    /**
     * Return whether the given entity is to be regarded as new. Default
     * implementation will inspect the given domain class and use either
     * {@link PersistableEntityInformation} if the class implements
     * {@link Persistable} or {@link ReflectiveEntityInformation} otherwise.
     * 
     * @param entity
     * @return
     */
    protected abstract IsNewAware createIsNewStrategy(Class<?> domainClass);


    /**
     * Returns the strategy how to determine whether an entity is to be regarded
     * as new.
     * 
     * @return the isNewStrategy
     */
    protected IsNewAware getIsNewStrategy() {

        return isNewStrategy;
    }
}
