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
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * The context in which the AOT processing happens.
 *
 * Grants access to the {@link ConfigurableListableBeanFactory beanFactory} and {@link ClassLoader}. Holds a few
 * convenience methods to check if a type {@link #isTypePresent(String) is present} and allows resolution of them.
 *
 * <strong>WARNING:</strong> Unstable internal API!
 *
 * @author Christoph Strobl
 * @author John Blum
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @since 3.0
 */
public interface AotContext {

	/**
	 * Create an {@link AotContext} backed by the given {@link BeanFactory}.
	 *
	 * @param beanFactory reference to the {@link BeanFactory}; must not be {@literal null}.
	 * @return a new instance of {@link AotContext}.
	 * @see BeanFactory
	 */
	static AotContext from(@NonNull BeanFactory beanFactory) {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		return new AotContext() {

			private final ConfigurableListableBeanFactory bf = beanFactory instanceof ConfigurableListableBeanFactory
					? (ConfigurableListableBeanFactory) beanFactory
					: new DefaultListableBeanFactory(beanFactory);

			@NonNull
			@Override
			public ConfigurableListableBeanFactory getBeanFactory() {
				return bf;
			}
		};
	}

	/**
	 * Returns a reference to the {@link ConfigurableListableBeanFactory} backing this {@link AotContext}.
	 *
	 * @return a reference to the {@link ConfigurableListableBeanFactory} backing this {@link AotContext}.
	 * @see ConfigurableListableBeanFactory
	 */
	ConfigurableListableBeanFactory getBeanFactory();

	/**
	 * Returns the {@link ClassLoader} used by this {@link AotContext} to resolve {@link Class types}.
	 *
	 * By default, this is the same {@link ClassLoader} used by the {@link BeanFactory} to resolve {@link Class types}
	 * declared in bean definitions.
	 *
	 * @return the {@link ClassLoader} used by this {@link AotContext} to resolve {@link Class types}.
	 * @see ConfigurableListableBeanFactory#getBeanClassLoader()
	 */
	@Nullable
	default ClassLoader getClassLoader() {
		return getBeanFactory().getBeanClassLoader();
	}

	/**
	 * Determines whether the given {@link String named} {@link Class type} is present on the application classpath.
	 *
	 * @param typeName {@link String name} of the {@link Class type} to evaluate; must not be {@literal null}.
	 * @return {@literal true} if the given {@link String named} {@link Class type} is present
	 * on the application classpath.
	 * @see #getClassLoader()
	 */
	default boolean isTypePresent(@NonNull String typeName) {
		return ClassUtils.isPresent(typeName, getClassLoader());
	}

	/**
	 * Returns a new {@link TypeScanner} used to scan for {@link Class types} that will be contributed to the AOT
	 * processing infrastructure.
	 *
	 * @return a {@link TypeScanner} used to scan for {@link Class types} that will be contributed to the AOT
	 * processing infrastructure.
	 * @see TypeScanner
	 */
	@NonNull
	default TypeScanner getTypeScanner() {
		return new TypeScanner(getClassLoader());
	}

	/**
	 * Scans for {@link Class types} in the given {@link String named packages} annotated with the store-specific
	 * {@link Annotation identifying annotations}.
	 *
	 * @param identifyingAnnotations {@link Collection} of {@link Annotation Annotations} identifying store-specific
	 * model {@link Class types}; must not be {@literal null}.
	 * @param packageNames {@link Collection} of {@link String package names} to scan.
	 * @return a {@link Set} of {@link Class types} found during the scan.
	 * @see TypeScanner#scanForTypesAnnotatedWith(Class[])
	 * @see TypeScanner.Scanner#inPackages(Collection)
	 * @see #getTypeScanner()
	 */
	default Set<Class<?>> scanPackageForTypes(@NonNull Collection<Class<? extends Annotation>> identifyingAnnotations,
			Collection<String> packageNames) {

		return getTypeScanner().scanForTypesAnnotatedWith(identifyingAnnotations).inPackages(packageNames);
	}

	/**
	 * Resolves the required {@link String named} {@link Class type}.
	 *
	 * @param typeName {@link String} containing the {@literal fully-qualified class name} of the {@link Class type}
	 * to resolve; must not be {@literal null}.
	 * @return a resolved {@link Class type} for the given, required {@link String name}.
	 * @throws TypeNotPresentException if the {@link String named} {@link Class type} cannot be found.
	 */
	@NonNull
	default Class<?> resolveRequiredType(@NonNull String typeName) throws TypeNotPresentException {

		try {
			return ClassUtils.forName(typeName, getClassLoader());
		} catch (ClassNotFoundException cause) {
			throw new TypeNotPresentException(typeName, cause);
		}
	}

	/**
	 * Resolves the given {@link String named} {@link Class type} if present.
	 *
	 * @param typeName {@link String} containing the {@literal fully-qualified class name} of the {@link Class type}
	 * to resolve; must not be {@literal null}.
	 * @return an {@link Optional} value containing the {@link Class type}
	 * if the {@link String fully-qualified class name} is present on the application classpath.
	 * @see #isTypePresent(String)
	 * @see #resolveRequiredType(String)
	 * @see java.util.Optional
	 */
	default Optional<Class<?>> resolveType(@NonNull String typeName) {

		return isTypePresent(typeName)
				? Optional.of(resolveRequiredType(typeName))
				: Optional.empty();
	}

	/**
	 * Resolves the {@link BeanDefinition bean's} defined {@link Class type}.
	 *
	 * @param beanReference {@link BeanReference} to the managed bean.
	 * @return the {@link Class type} of the {@link BeanReference referenced bean} if defined; may be {@literal null}.
	 * @see BeanReference
	 */
	@Nullable
	default Class<?> resolveType(@NonNull BeanReference beanReference) {
		return getBeanFactory().getType(beanReference.getBeanName(), false);
	}

	/**
	 * Gets the {@link BeanDefinition} for the given, required {@link String named bean}.
	 *
	 * @param beanName {@link String} containing the {@literal name} of the bean; must not be {@literal null}.
	 * @return the {@link BeanDefinition} for the given, required {@link String named bean}.
	 * @throws NoSuchBeanDefinitionException if a {@link BeanDefinition} cannot be found for
	 * the {@link String named bean}.
	 * @see BeanDefinition
	 */
	@NonNull
	default BeanDefinition getBeanDefinition(@NonNull String beanName) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getBeanDefinition(beanName);
	}

	/**
	 * Gets the {@link RootBeanDefinition} for the given, required {@link String bean name}.
	 *
	 * @param beanName {@link String} containing the {@literal name} of the bean.
	 * @return the {@link RootBeanDefinition} for the given, required {@link String bean name}.
	 * @throws NoSuchBeanDefinitionException if a {@link BeanDefinition} cannot be found for
	 * the {@link String named bean}.
	 * @throws IllegalStateException if the bean is not a {@link RootBeanDefinition root bean}.
	 * @see RootBeanDefinition
	 */
	@NonNull
	default RootBeanDefinition getRootBeanDefinition(@NonNull String beanName) throws NoSuchBeanDefinitionException {

		BeanDefinition beanDefinition = getBeanDefinition(beanName);

		if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
			return rootBeanDefinition;
		}

		throw new IllegalStateException(String.format("%s is not a root bean", beanName));
	}

	/**
	 * Determines whether a bean identified by the given, required {@link String name} is a
	 * {@link org.springframework.beans.factory.FactoryBean}.
	 *
	 * @param beanName {@link String} containing the {@literal name} of the bean to evaluate;
	 * must not be {@literal null}.
	 * @return {@literal true} if the bean identified by the given, required {@link String name} is a
	 * {@link org.springframework.beans.factory.FactoryBean}.
	 */
	default boolean isFactoryBean(@NonNull String beanName) {
		return getBeanFactory().isFactoryBean(beanName);
	}

	/**
	 * Determines whether a Spring {@link org.springframework.transaction.TransactionManager} is present.
	 *
	 * @return {@literal true} if a Spring {@link org.springframework.transaction.TransactionManager} is present.
	 */
	default boolean isTransactionManagerPresent() {

		return resolveType("org.springframework.transaction.TransactionManager")
				.filter(it -> !ObjectUtils.isEmpty(getBeanFactory().getBeanNamesForType(it)))
				.isPresent();
	}

	/**
	 * Determines whether the given, required {@link String type name} is declared on the application classpath
	 * and performs the given, required {@link Consumer action} if present.
	 *
	 * @param typeName {@link String name} of the {@link Class type} to process; must not be {@literal null}.
	 * @param action {@link Consumer} defining the action to perform on the resolved {@link Class type};
	 * must not be {@literal null}.
	 * @see java.util.function.Consumer
	 * @see #resolveType(String)
	 */
	default void ifTypePresent(@NonNull String typeName, @NonNull Consumer<Class<?>> action) {
		resolveType(typeName).ifPresent(action);
	}

	/**
	 * Runs the given {@link Consumer action} on any {@link org.springframework.transaction.TransactionManager} beans
	 * defined in the application context.
	 *
	 * @param beanNamesConsumer {@link Consumer} defining the action to perform on
	 * the {@link org.springframework.transaction.TransactionManager} beans if present; must not be {@literal null}.
	 * @see java.util.function.Consumer
	 */
	default void ifTransactionManagerPresent(@NonNull Consumer<String[]> beanNamesConsumer) {

		ifTypePresent("org.springframework.transaction.TransactionManager", txMgrType -> {
			String[] txMgrBeanNames = getBeanFactory().getBeanNamesForType(txMgrType);
			if (!ObjectUtils.isEmpty(txMgrBeanNames)) {
				beanNamesConsumer.accept(txMgrBeanNames);
			}
		});
	}
}
