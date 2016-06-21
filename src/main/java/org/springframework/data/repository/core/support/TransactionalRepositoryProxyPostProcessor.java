/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.data.repository.core.support;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.transaction.annotation.Ejb3TransactionAnnotationParser;
import org.springframework.transaction.annotation.JtaTransactionAnnotationParser;
import org.springframework.transaction.annotation.SpringTransactionAnnotationParser;
import org.springframework.transaction.annotation.TransactionAnnotationParser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link RepositoryProxyPostProcessor} to add transactional behaviour to repository proxies. Adds a
 * {@link PersistenceExceptionTranslationInterceptor} as well as an annotation based {@link TransactionInterceptor} to
 * the proxy.
 * 
 * @author Oliver Gierke
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

		Assert.notNull(beanFactory);
		Assert.notNull(transactionManagerName);

		this.beanFactory = beanFactory;
		this.transactionManagerName = transactionManagerName;
		this.enableDefaultTransactions = enableDefaultTransaction;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryProxyPostProcessor#postProcess(org.springframework.aop.framework.ProxyFactory, org.springframework.data.repository.core.RepositoryInformation)
	 */
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {

		CustomAnnotationTransactionAttributeSource transactionAttributeSource = new CustomAnnotationTransactionAttributeSource();
		transactionAttributeSource.setRepositoryInformation(repositoryInformation);
		transactionAttributeSource.setEnableDefaultTransactions(enableDefaultTransactions);

		TransactionInterceptor transactionInterceptor = new TransactionInterceptor(null, transactionAttributeSource);
		transactionInterceptor.setTransactionManagerBeanName(transactionManagerName);
		transactionInterceptor.setBeanFactory(beanFactory);
		transactionInterceptor.afterPropertiesSet();

		factory.addAdvice(transactionInterceptor);
	}

	// The section below contains copies of two core Spring classes that slightly modify the algorithm transaction
	// configuration is discovered. The original Spring implementation favours the implementation class' transaction
	// configuration over one declared at an interface. As we need to provide the capability to override transaction
	// configuration of the implementation at the interface level we pretty much invert this logic to inspect the
	// originally invoked method first before digging down into the implementation class.
	//
	// Unfortunately the Spring classes do not allow modifying this algorithm easily. That's why we have to copy the two
	// classes 1:1. Only modifications done are inside
	// AbstractFallbackTransactionAttributeSource#computeTransactionAttribute(Method, Class<?>).

	/**
	 * Implementation of the {@link org.springframework.transaction.interceptor.TransactionAttributeSource} interface for
	 * working with transaction metadata in JDK 1.5+ annotation format.
	 * <p>
	 * This class reads Spring's JDK 1.5+ {@link Transactional} annotation and exposes corresponding transaction
	 * attributes to Spring's transaction infrastructure. Also supports JTA 1.2's and EJB3's
	 * {@link javax.ejb.TransactionAttribute} annotation (if present). This class may also serve as base class for a
	 * custom TransactionAttributeSource, or get customized through {@link TransactionAnnotationParser} strategies.
	 *
	 * @author Colin Sampaleanu
	 * @author Juergen Hoeller
	 * @since 1.2
	 * @see Transactional
	 * @see TransactionAnnotationParser
	 * @see SpringTransactionAnnotationParser
	 * @see Ejb3TransactionAnnotationParser
	 * @see org.springframework.transaction.interceptor.TransactionInterceptor#setTransactionAttributeSource
	 * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean#setTransactionAttributeSource
	 */
	@SuppressWarnings("serial")
	static class CustomAnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource
			implements Serializable {

		private static final boolean jta12Present = ClassUtils.isPresent("javax.transaction.Transactional",
				CustomAnnotationTransactionAttributeSource.class.getClassLoader());

		private static final boolean ejb3Present = ClassUtils.isPresent("javax.ejb.TransactionAttribute",
				CustomAnnotationTransactionAttributeSource.class.getClassLoader());

		private final boolean publicMethodsOnly;

		private final Set<TransactionAnnotationParser> annotationParsers;

		/**
		 * Create a default CustomAnnotationTransactionAttributeSource, supporting public methods that carry the
		 * {@code Transactional} annotation or the EJB3 {@link javax.ejb.TransactionAttribute} annotation.
		 */
		public CustomAnnotationTransactionAttributeSource() {
			this(true);
		}

		/**
		 * Create a custom CustomAnnotationTransactionAttributeSource, supporting public methods that carry the
		 * {@code Transactional} annotation or the EJB3 {@link javax.ejb.TransactionAttribute} annotation.
		 * 
		 * @param publicMethodsOnly whether to support public methods that carry the {@code Transactional} annotation only
		 *          (typically for use with proxy-based AOP), or protected/private methods as well (typically used with
		 *          AspectJ class weaving)
		 */
		public CustomAnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
			this.publicMethodsOnly = publicMethodsOnly;
			this.annotationParsers = new LinkedHashSet<>(2);
			this.annotationParsers.add(new SpringTransactionAnnotationParser());
			if (jta12Present) {
				this.annotationParsers.add(new JtaTransactionAnnotationParser());
			}
			if (ejb3Present) {
				this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
			}
		}

		/**
		 * Create a custom CustomAnnotationTransactionAttributeSource.
		 * 
		 * @param annotationParser the TransactionAnnotationParser to use
		 */
		public CustomAnnotationTransactionAttributeSource(TransactionAnnotationParser annotationParser) {
			this.publicMethodsOnly = true;
			Assert.notNull(annotationParser, "TransactionAnnotationParser must not be null");
			this.annotationParsers = Collections.singleton(annotationParser);
		}

		/**
		 * Create a custom CustomAnnotationTransactionAttributeSource.
		 * 
		 * @param annotationParsers the TransactionAnnotationParsers to use
		 */
		public CustomAnnotationTransactionAttributeSource(TransactionAnnotationParser... annotationParsers) {
			this.publicMethodsOnly = true;
			Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
			Set<TransactionAnnotationParser> parsers = new LinkedHashSet<>(
					annotationParsers.length);
			Collections.addAll(parsers, annotationParsers);
			this.annotationParsers = parsers;
		}

		/**
		 * Create a custom CustomAnnotationTransactionAttributeSource.
		 * 
		 * @param annotationParsers the TransactionAnnotationParsers to use
		 */
		public CustomAnnotationTransactionAttributeSource(Set<TransactionAnnotationParser> annotationParsers) {
			this.publicMethodsOnly = true;
			Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
			this.annotationParsers = annotationParsers;
		}

		@Override
		protected TransactionAttribute findTransactionAttribute(Method method) {
			return determineTransactionAttribute(method);
		}

		@Override
		protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
			return determineTransactionAttribute(clazz);
		}

		/**
		 * Determine the transaction attribute for the given method or class.
		 * <p>
		 * This implementation delegates to configured {@link TransactionAnnotationParser TransactionAnnotationParsers} for
		 * parsing known annotations into Spring's metadata attribute class. Returns {@code null} if it's not transactional.
		 * <p>
		 * Can be overridden to support custom annotations that carry transaction metadata.
		 * 
		 * @param ae the annotated method or class
		 * @return TransactionAttribute the configured transaction attribute, or {@code null} if none was found
		 */
		protected TransactionAttribute determineTransactionAttribute(AnnotatedElement ae) {
			for (TransactionAnnotationParser annotationParser : this.annotationParsers) {
				TransactionAttribute attr = annotationParser.parseTransactionAnnotation(ae);
				if (attr != null) {
					return attr;
				}
			}
			return null;
		}

		/**
		 * By default, only public methods can be made transactional.
		 */
		@Override
		protected boolean allowPublicMethodsOnly() {
			return this.publicMethodsOnly;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof CustomAnnotationTransactionAttributeSource)) {
				return false;
			}
			CustomAnnotationTransactionAttributeSource otherTas = (CustomAnnotationTransactionAttributeSource) other;
			return (this.annotationParsers.equals(otherTas.annotationParsers)
					&& this.publicMethodsOnly == otherTas.publicMethodsOnly);
		}

		@Override
		public int hashCode() {
			return this.annotationParsers.hashCode();
		}
	}

	/**
	 * Abstract implementation of {@link TransactionAttributeSource} that caches attributes for methods and implements a
	 * fallback policy: 1. specific target method; 2. target class; 3. declaring method; 4. declaring class/interface.
	 * <p>
	 * Defaults to using the target class's transaction attribute if none is associated with the target method. Any
	 * transaction attribute associated with the target method completely overrides a class transaction attribute. If none
	 * found on the target class, the interface that the invoked method has been called through (in case of a JDK proxy)
	 * will be checked.
	 * <p>
	 * This implementation caches attributes by method after they are first used. If it is ever desirable to allow dynamic
	 * changing of transaction attributes (which is very unlikely), caching could be made configurable. Caching is
	 * desirable because of the cost of evaluating rollback rules.
	 * 
	 * @author Rod Johnson
	 * @author Juergen Hoeller
	 * @since 1.1
	 */
	abstract static class AbstractFallbackTransactionAttributeSource implements TransactionAttributeSource {

		/**
		 * Canonical value held in cache to indicate no transaction attribute was found for this method, and we don't need
		 * to look again.
		 */
		private final static TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute();

		/**
		 * Logger available to subclasses.
		 * <p>
		 * As this base class is not marked Serializable, the logger will be recreated after serialization - provided that
		 * the concrete subclass is Serializable.
		 */
		protected final Logger logger = LoggerFactory.getLogger(getClass());

		/**
		 * Cache of TransactionAttributes, keyed by DefaultCacheKey (Method + target Class).
		 * <p>
		 * As this base class is not marked Serializable, the cache will be recreated after serialization - provided that
		 * the concrete subclass is Serializable.
		 */
		final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<Object, TransactionAttribute>();

		private RepositoryInformation repositoryInformation;
		private boolean enableDefaultTransactions = true;

		/**
		 * @param repositoryInformation the repositoryInformation to set
		 */
		public void setRepositoryInformation(RepositoryInformation repositoryInformation) {
			this.repositoryInformation = repositoryInformation;
		}

		/**
		 * @param enableDefaultTransactions the enableDefaultTransactions to set
		 */
		public void setEnableDefaultTransactions(boolean enableDefaultTransactions) {
			this.enableDefaultTransactions = enableDefaultTransactions;
		}

		/**
		 * Determine the transaction attribute for this method invocation.
		 * <p>
		 * Defaults to the class's transaction attribute if no method attribute is found.
		 * 
		 * @param method the method for the current invocation (never <code>null</code>)
		 * @param targetClass the target class for this invocation (may be <code>null</code>)
		 * @return TransactionAttribute for this method, or <code>null</code> if the method is not transactional
		 */
		public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
			// First, see if we have a cached value.
			Object cacheKey = getCacheKey(method, targetClass);
			Object cached = this.attributeCache.get(cacheKey);
			if (cached != null) {
				// Value will either be canonical value indicating there is no transaction attribute,
				// or an actual transaction attribute.
				if (cached == NULL_TRANSACTION_ATTRIBUTE) {
					return null;
				} else {
					return (TransactionAttribute) cached;
				}
			} else {
				// We need to work it out.
				TransactionAttribute txAtt = computeTransactionAttribute(method, targetClass);
				// Put it in the cache.
				if (txAtt == null) {
					this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Adding transactional method '" + method.getName() + "' with attribute: " + txAtt);
					}
					this.attributeCache.put(cacheKey, txAtt);
				}
				return txAtt;
			}
		}

		/**
		 * Determine a cache key for the given method and target class.
		 * <p>
		 * Must not produce same key for overloaded methods. Must produce same key for different instances of the same
		 * method.
		 * 
		 * @param method the method (never <code>null</code>)
		 * @param targetClass the target class (may be <code>null</code>)
		 * @return the cache key (never <code>null</code>)
		 */
		protected Object getCacheKey(Method method, Class<?> targetClass) {
			return new DefaultCacheKey(method, targetClass);
		}

		/**
		 * Same signature as {@link #getTransactionAttribute}, but doesn't cache the result.
		 * {@link #getTransactionAttribute} is effectively a caching decorator for this method.
		 * 
		 * @see #getTransactionAttribute
		 */
		private TransactionAttribute computeTransactionAttribute(Method method, Class<?> targetClass) {
			// Don't allow no-public methods as required.
			if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
				return null;
			}

			// Ignore CGLIB subclasses - introspect the actual user class.
			Class<?> userClass = ClassUtils.getUserClass(targetClass);
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

			if (!enableDefaultTransactions) {
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

		/**
		 * Subclasses need to implement this to return the transaction attribute for the given method, if any.
		 * 
		 * @param method the method to retrieve the attribute for
		 * @return all transaction attribute associated with this method (or <code>null</code> if none)
		 */
		protected abstract TransactionAttribute findTransactionAttribute(Method method);

		/**
		 * Subclasses need to implement this to return the transaction attribute for the given class, if any.
		 * 
		 * @param clazz the class to retrieve the attribute for
		 * @return all transaction attribute associated with this class (or <code>null</code> if none)
		 */
		protected abstract TransactionAttribute findTransactionAttribute(Class<?> clazz);

		/**
		 * Should only public methods be allowed to have transactional semantics?
		 * <p>
		 * The default implementation returns <code>false</code>.
		 */
		protected boolean allowPublicMethodsOnly() {
			return false;
		}

		/**
		 * Default cache key for the TransactionAttribute cache.
		 */
		private static class DefaultCacheKey {

			private final Method method;

			private final Class<?> targetClass;

			public DefaultCacheKey(Method method, Class<?> targetClass) {
				this.method = method;
				this.targetClass = targetClass;
			}

			@Override
			public boolean equals(Object other) {
				if (this == other) {
					return true;
				}
				if (!(other instanceof DefaultCacheKey)) {
					return false;
				}
				DefaultCacheKey otherKey = (DefaultCacheKey) other;
				return this.method.equals(otherKey.method)
						&& ObjectUtils.nullSafeEquals(this.targetClass, otherKey.targetClass);
			}

			@Override
			public int hashCode() {
				return this.method.hashCode() * 29 + (this.targetClass != null ? this.targetClass.hashCode() : 0);
			}
		}
	}
}
