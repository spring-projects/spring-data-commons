/*
 * Copyright 2008-2020 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link RepositoryProxyPostProcessor} to add transactional behaviour to repository proxies. Adds a
 * {@link PersistenceExceptionTranslationInterceptor} as well as an annotation based {@link TransactionInterceptor} to
 * the proxy.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class TransactionalRepositoryProxyPostProcessor implements RepositoryProxyPostProcessor {

	private final BeanFactory beanFactory;
	private final String transactionManagerName;
	private final boolean enableDefaultTransactions;

	/**
	 * Creates a new {@link TransactionalRepositoryProxyPostProcessor} using the given {@link ListableBeanFactory} and
	 * transaction manager bean name.
	 *
	 * @param beanFactory must not be {@literal null}.
	 * @param transactionManagerName must not be {@literal null} or empty.
	 * @param enableDefaultTransaction
	 */
	public TransactionalRepositoryProxyPostProcessor(ListableBeanFactory beanFactory, String transactionManagerName,
			boolean enableDefaultTransaction) {

		Assert.notNull(beanFactory, "BeanFactory must not be null!");
		Assert.notNull(transactionManagerName, "TransactionManagerName must not be null!");

		this.beanFactory = beanFactory;
		this.transactionManagerName = transactionManagerName;
		this.enableDefaultTransactions = enableDefaultTransaction;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryProxyPostProcessor#postProcess(org.springframework.aop.framework.ProxyFactory, org.springframework.data.repository.core.RepositoryInformation)
	 */
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {

		RepositoryInformationPreferringAnnotationTransactionAttributeSource transactionAttributeSource = new RepositoryInformationPreferringAnnotationTransactionAttributeSource(
				enableDefaultTransactions, repositoryInformation);

		TransactionInterceptor transactionInterceptor = new TransactionInterceptor();
		transactionInterceptor.setTransactionAttributeSource(transactionAttributeSource);
		transactionInterceptor.setTransactionManagerBeanName(transactionManagerName);
		transactionInterceptor.setBeanFactory(beanFactory);
		transactionInterceptor.afterPropertiesSet();

		factory.addAdvice(transactionInterceptor);
	}

	/**
	 * Custom implementation of {@link AnnotationTransactionAttributeSource} that that slightly modify the algorithm
	 * transaction configuration is discovered.
	 * <p>
	 * The original Spring implementation favours the implementation class' transaction configuration over one declared at
	 * an interface. As we need to provide the capability to override transaction configuration of the implementation at
	 * the interface level we pretty much invert this logic to inspect the originally invoked method first before digging
	 * down into the implementation class.
	 *
	 * @author Oliver Drotbohm
	 * @author Mark Paluch
	 */
	@SuppressWarnings({ "serial", "null" })
	static class RepositoryInformationPreferringAnnotationTransactionAttributeSource
			extends AnnotationTransactionAttributeSource implements Serializable {

		private final boolean enableDefaultTransactions;

		private final @Nullable RepositoryInformation repositoryInformation;

		/**
		 * Create a default CustomAnnotationTransactionAttributeSource, supporting public methods that carry the
		 * {@code Transactional} annotation or the EJB3 {@link javax.ejb.TransactionAttribute} annotation.
		 */
		public RepositoryInformationPreferringAnnotationTransactionAttributeSource() {
			this(true, null);
		}

		/**
		 * Create a default CustomAnnotationTransactionAttributeSource, supporting public methods that carry the
		 * {@code Transactional} annotation or the EJB3 {@link javax.ejb.TransactionAttribute} annotation.
		 */
		public RepositoryInformationPreferringAnnotationTransactionAttributeSource(boolean enableDefaultTransactions,
				@Nullable RepositoryInformation repositoryInformation) {
			super(true);
			this.enableDefaultTransactions = enableDefaultTransactions;
			this.repositoryInformation = repositoryInformation;
		}

		@Override
		protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {

			// Don't allow no-public methods as required.
			if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
				return null;
			}

			// Ignore CGLIB subclasses - introspect the actual user class.
			Class<?> userClass = targetClass != null ? ProxyUtils.getUserClass(targetClass) : targetClass;
			// The method may be on an interface, but we need attributes from the target class.
			// If the target class is null, the method will be unchanged.
			Method specificMethod = ClassUtils.getMostSpecificMethod(method, userClass);
			// If we are dealing with method with generic parameters, find the original method.
			specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

			TransactionAttribute txAtt = null;

			if (specificMethod != method) {
				// Fallback is to look at the original method.
				txAtt = findTransactionAttribute(method);
				if (txAtt != null) {
					return txAtt;
				}
				// Last fallback is the class of the original method.
				txAtt = findTransactionAttribute(method.getDeclaringClass());

				if (txAtt != null || !enableDefaultTransactions) {
					return txAtt;
				}
			}

			// Start: Implementation class check block

			// First try is the method in the target class.
			txAtt = findTransactionAttribute(specificMethod);
			if (txAtt != null) {
				return txAtt;
			}

			// Second try is the transaction attribute on the target class.
			txAtt = findTransactionAttribute(specificMethod.getDeclaringClass());
			if (txAtt != null) {
				return txAtt;
			}

			if (!enableDefaultTransactions || repositoryInformation == null) {
				return null;
			}

			// Fallback to implementation class transaction settings of nothing found
			// return findTransactionAttribute(method);
			Method targetClassMethod = repositoryInformation.getTargetClassMethod(method);

			if (targetClassMethod.equals(method)) {
				return null;
			}

			txAtt = findTransactionAttribute(targetClassMethod);
			if (txAtt != null) {
				return txAtt;
			}

			txAtt = findTransactionAttribute(targetClassMethod.getDeclaringClass());
			if (txAtt != null) {
				return txAtt;
			}

			return null;
			// End: Implementation class check block
		}
	}

}
