/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.aot;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * The context in which the AOT processing happens. Grants access to the {@link ConfigurableListableBeanFactory
 * beanFactory} and {@link ClassLoader}. Holds a few convenience methods to check if a type
 * {@link #isTypePresent(String) is present} and allows resolution of them. <strong>WARNING:</strong> Unstable internal
 * API!
 *
 * @author Christoph Strobl
 */
public interface AotContext {

	/**
	 * Create an {@link AotContext} backed by the given {@link BeanFactory}.
	 *
	 * @param beanFactory must not be {@literal null}.
	 * @return new instance of {@link AotContext}.
	 */
	static AotContext context(BeanFactory beanFactory) {

		Assert.notNull(beanFactory, "BeanFactory must not be null!");

		return new AotContext() {

			private final ConfigurableListableBeanFactory bf = beanFactory instanceof ConfigurableListableBeanFactory
					? (ConfigurableListableBeanFactory) beanFactory
					: new DefaultListableBeanFactory(beanFactory);

			@Override
			public ConfigurableListableBeanFactory getBeanFactory() {
				return bf;
			}
		};
	}

	ConfigurableListableBeanFactory getBeanFactory();

	default ClassLoader getClassLoader() {
		return getBeanFactory().getBeanClassLoader();
	}

	default boolean isTypePresent(String typeName) {
		return ClassUtils.isPresent(typeName, getBeanFactory().getBeanClassLoader());
	}

	default TypeScanner getTypeScanner() {
		return new TypeScanner(getClassLoader());
	}

	default Set<Class<?>> scanPackageForTypes(Collection<Class<? extends Annotation>> identifyingAnnotations,
			Collection<String> packageNames) {
		return getTypeScanner().scanForTypesAnnotatedWith(identifyingAnnotations).inPackages(packageNames);
	}

	default Optional<Class<?>> resolveType(String typeName) {

		if (!isTypePresent(typeName)) {
			return Optional.empty();
		}
		return Optional.of(resolveRequiredType(typeName));
	}

	default Class<?> resolveRequiredType(String typeName) throws TypeNotPresentException {
		try {
			return ClassUtils.forName(typeName, getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new TypeNotPresentException(typeName, e);
		}
	}

	@Nullable
	default Class<?> resolveType(BeanReference beanReference) {
		return getBeanFactory().getType(beanReference.getBeanName(), false);
	}

	default BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getBeanDefinition(beanName);
	}

	default RootBeanDefinition getRootBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {

		BeanDefinition val = getBeanFactory().getBeanDefinition(beanName);
		if (!(val instanceof RootBeanDefinition)) {
			throw new IllegalStateException(String.format("%s is not a root bean", beanName));
		}
		return RootBeanDefinition.class.cast(val);
	}

	default boolean isFactoryBean(String beanName) {
		return getBeanFactory().isFactoryBean(beanName);
	}

	default boolean isTransactionManagerPresent() {

		return resolveType("org.springframework.transaction.TransactionManager") //
				.map(it -> !ObjectUtils.isEmpty(getBeanFactory().getBeanNamesForType(it))) //
				.orElse(false);
	}

	default void ifTypePresent(String typeName, Consumer<Class<?>> action) {
		resolveType(typeName).ifPresent(action);
	}

	default void ifTransactionManagerPresent(Consumer<String[]> beanNamesConsumer) {

		ifTypePresent("org.springframework.transaction.TransactionManager", txMgrType -> {
			String[] txMgrBeanNames = getBeanFactory().getBeanNamesForType(txMgrType);
			if (!ObjectUtils.isEmpty(txMgrBeanNames)) {
				beanNamesConsumer.accept(txMgrBeanNames);
			}
		});
	}
}
