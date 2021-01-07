/*
 * Copyright 2008-2021 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.util.TxUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * Extension of {@link RepositoryFactoryBeanSupport} to add transactional capabilities to the repository proxy. Will
 * register a {@link TransactionalRepositoryProxyPostProcessor} that in turn adds a {@link TransactionInterceptor} to
 * the repository proxy to be created.
 *
 * @author Oliver Gierke
 */
public abstract class TransactionalRepositoryFactoryBeanSupport<T extends Repository<S, ID>, S, ID>
		extends RepositoryFactoryBeanSupport<T, S, ID> implements BeanFactoryAware {

	private String transactionManagerName = TxUtils.DEFAULT_TRANSACTION_MANAGER;
	private @Nullable RepositoryProxyPostProcessor txPostProcessor;
	private @Nullable RepositoryProxyPostProcessor exceptionPostProcessor;
	private boolean enableDefaultTransactions = true;

	/**
	 * Creates a new {@link TransactionalRepositoryFactoryBeanSupport} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	protected TransactionalRepositoryFactoryBeanSupport(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/**
	 * Setter to configure which transaction manager to be used. We have to use the bean name explicitly as otherwise the
	 * qualifier of the {@link org.springframework.transaction.annotation.Transactional} annotation is used. By explicitly
	 * defining the transaction manager bean name we favour let this one be the default one chosen.
	 *
	 * @param transactionManager
	 */
	public void setTransactionManager(String transactionManager) {
		this.transactionManagerName = transactionManager == null ? TxUtils.DEFAULT_TRANSACTION_MANAGER : transactionManager;
	}

	/**
	 * Configures whether to enable the default transactions configured at the repository base implementation class.
	 *
	 * @param enableDefaultTransactions the enableDefaultTransactions to set
	 */
	public void setEnableDefaultTransactions(boolean enableDefaultTransactions) {
		this.enableDefaultTransactions = enableDefaultTransactions;
	}

	/**
	 * Delegates {@link RepositoryFactorySupport} creation to {@link #doCreateRepositoryFactory()} and applies the
	 * {@link TransactionalRepositoryProxyPostProcessor} to the created instance.
	 *
	 * @see org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport #createRepositoryFactory()
	 */
	@Override
	protected final RepositoryFactorySupport createRepositoryFactory() {

		RepositoryFactorySupport factory = doCreateRepositoryFactory();

		RepositoryProxyPostProcessor exceptionPostProcessor = this.exceptionPostProcessor;

		if (exceptionPostProcessor != null) {
			factory.addRepositoryProxyPostProcessor(exceptionPostProcessor);
		}

		RepositoryProxyPostProcessor txPostProcessor = this.txPostProcessor;

		if (txPostProcessor != null) {
			factory.addRepositoryProxyPostProcessor(txPostProcessor);
		}

		return factory;
	}

	/**
	 * Creates the actual {@link RepositoryFactorySupport} instance.
	 *
	 * @return
	 */
	protected abstract RepositoryFactorySupport doCreateRepositoryFactory();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {

		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory);

		super.setBeanFactory(beanFactory);

		ListableBeanFactory listableBeanFactory = (ListableBeanFactory) beanFactory;
		this.txPostProcessor = new TransactionalRepositoryProxyPostProcessor(listableBeanFactory, transactionManagerName,
				enableDefaultTransactions);
		this.exceptionPostProcessor = new PersistenceExceptionTranslationRepositoryProxyPostProcessor(listableBeanFactory);
	}
}
