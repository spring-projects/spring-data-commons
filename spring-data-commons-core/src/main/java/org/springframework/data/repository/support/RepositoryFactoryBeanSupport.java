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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.util.TxUtils;
import org.springframework.util.Assert;


/**
 * Adapter for Springs {@link FactoryBean} interface to allow easy setup of
 * repository factories via Spring configuration.
 * 
 * @author Oliver Gierke
 * @param <T> the type of the repository
 */
public abstract class RepositoryFactoryBeanSupport<T extends Repository<?, ?>>
        implements FactoryBean<T>, InitializingBean, BeanFactoryAware {

    private RepositoryFactorySupport factory;

    private Key queryLookupStrategyKey;
    private Class<? extends T> repositoryInterface;
    private Object customImplementation;

    private String transactionManagerName = TxUtils.DEFAULT_TRANSACTION_MANAGER;
    private RepositoryProxyPostProcessor txPostProcessor;


    /**
     * Setter to inject the repository interface to implement.
     * 
     * @param repositoryInterface the repository interface to set
     */
    @Required
    public void setRepositoryInterface(Class<T> repositoryInterface) {

        Assert.notNull(repositoryInterface);
        this.repositoryInterface = repositoryInterface;
    }


    public void setQueryLookupStrategyKey(Key queryLookupStrategyKey) {

        this.queryLookupStrategyKey = queryLookupStrategyKey;
    }


    /**
     * Setter to configure which transaction manager to be used. We have to use
     * the bean name explicitly as otherwise the qualifier of the
     * {@link org.springframework.transaction.annotation.Transactional}
     * annotation is used. By explicitly defining the transaction manager bean
     * name we favour let this one be the default one chosen.
     * 
     * @param transactionManager
     */
    public void setTransactionManager(String transactionManager) {

        this.transactionManagerName =
                transactionManager == null ? TxUtils.DEFAULT_TRANSACTION_MANAGER
                        : transactionManager;
    }


    /**
     * Setter to inject a custom repository implementation.
     * 
     * @param customImplementation
     */
    public void setCustomImplementation(Object customImplementation) {

        this.customImplementation = customImplementation;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public T getObject() {

        return factory.getRepository(repositoryInterface, customImplementation);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    @SuppressWarnings("unchecked")
    public Class<? extends T> getObjectType() {

        return (Class<? extends T>) (null == repositoryInterface ? Repository.class
                : repositoryInterface);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {

        return true;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() {

        this.factory = createRepositoryFactory();
        this.factory.setQueryLookupStrategyKey(queryLookupStrategyKey);
        this.factory.validate(repositoryInterface, customImplementation);
        this.factory.addRepositoryProxyPostProcessor(txPostProcessor);
    }


    /**
     * Create the actual {@link RepositoryFactorySupport} instance.
     * 
     * @return
     */
    protected abstract RepositoryFactorySupport createRepositoryFactory();


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org
     * .springframework.beans.factory.BeanFactory)
     */
    public void setBeanFactory(BeanFactory beanFactory) {

        Assert.isInstanceOf(ListableBeanFactory.class, beanFactory);

        this.txPostProcessor =
                new TransactionalRepositoryProxyPostProcessor(
                        (ListableBeanFactory) beanFactory,
                        transactionManagerName);
    }
}
