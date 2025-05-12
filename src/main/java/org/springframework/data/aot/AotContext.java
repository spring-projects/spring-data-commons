/*
 * Copyright 2022-2025 the original author or authors.
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.util.TypeScanner;
import org.springframework.util.Assert;

/**
 * The context in which the AOT processing happens. Grants access to the {@link ConfigurableListableBeanFactory
 * beanFactory} and {@link ClassLoader}. Holds a few convenience methods to check if a type
 * {@link TypeIntrospector#isTypePresent() is present} and allows resolution of them through {@link TypeIntrospector}
 * and {@link IntrospectedBeanDefinition}.
 * <p>
 * Mainly for internal use within the framework.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @author Mark Paluch
 * @since 3.0
 * @see BeanFactory
 */
public interface AotContext extends EnvironmentCapable {

	String GENERATED_REPOSITORIES_ENABLED = "spring.aot.repositories.enabled";

	/**
	 * Create an {@link AotContext} backed by the given {@link BeanFactory}.
	 *
	 * @param beanFactory reference to the {@link BeanFactory}; must not be {@literal null}.
	 * @return a new instance of {@link AotContext}.
	 * @see BeanFactory
	 */
	static AotContext from(BeanFactory beanFactory) {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		return new DefaultAotContext(beanFactory, new StandardEnvironment());
	}

	/**
	 * Create an {@link AotContext} backed by the given {@link BeanFactory}.
	 *
	 * @param beanFactory reference to the {@link BeanFactory}; must not be {@literal null}.
	 * @param environment reference to the {@link Environment}; must not be {@literal null}.
	 * @return a new instance of {@link AotContext}.
	 * @see BeanFactory
	 */
	static AotContext from(BeanFactory beanFactory, Environment environment) {

		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(environment, "Environment must not be null");

		return new DefaultAotContext(beanFactory, environment);
	}

	/**
	 * Returns a reference to the {@link ConfigurableListableBeanFactory} backing this {@link AotContext}.
	 *
	 * @return a reference to the {@link ConfigurableListableBeanFactory} backing this {@link AotContext}.
	 * @see ConfigurableListableBeanFactory
	 */
	ConfigurableListableBeanFactory getBeanFactory();

	/**
	 * Returns the {@link ClassLoader} used by this {@link AotContext} to resolve {@link Class types}. By default, this is
	 * the same {@link ClassLoader} used by the {@link BeanFactory} to resolve {@link Class types} declared in bean
	 * definitions.
	 *
	 * @return the {@link ClassLoader} used by this {@link AotContext} to resolve {@link Class types}.
	 * @see ConfigurableListableBeanFactory#getBeanClassLoader()
	 */
	@Nullable
	default ClassLoader getClassLoader() {
		return getBeanFactory().getBeanClassLoader();
	}

	/**
	 * Returns the required {@link ClassLoader} used by this {@link AotContext} to resolve {@link Class types}. By
	 * default, this is the same {@link ClassLoader} used by the {@link BeanFactory} to resolve {@link Class types}
	 * declared in bean definitions.
	 *
	 * @return the {@link ClassLoader} used by this {@link AotContext} to resolve {@link Class types}.
	 * @throws IllegalStateException if no {@link ClassLoader} is available.
	 */
	default ClassLoader getRequiredClassLoader() {

		ClassLoader loader = getClassLoader();

		if (loader == null) {
			throw new IllegalStateException("Required ClassLoader is not available");
		}

		return loader;
	}

	/**
	 * Returns a {@link TypeIntrospector} to obtain further detail about a {@link Class type} given its fully-qualified
	 * type name
	 *
	 * @param typeName {@link String name} of the {@link Class type} to evaluate; must not be {@literal null}.
	 * @return the type introspector for further type-based introspection.
	 */
	TypeIntrospector introspectType(String typeName);

	/**
	 * Returns a new {@link TypeScanner} used to scan for {@link Class types} that will be contributed to the AOT
	 * processing infrastructure.
	 *
	 * @return a {@link TypeScanner} used to scan for {@link Class types} that will be contributed to the AOT processing
	 *         infrastructure.
	 * @see TypeScanner
	 */
	default TypeScanner getTypeScanner() {
		return TypeScanner.typeScanner(getRequiredClassLoader());
	}

	/**
	 * Scans for {@link Class types} in the given {@link String named packages} annotated with the store-specific
	 * {@link Annotation identifying annotations}.
	 *
	 * @param identifyingAnnotations {@link Collection} of {@link Annotation Annotations} identifying store-specific model
	 *          {@link Class types}; must not be {@literal null}.
	 * @param packageNames {@link Collection} of {@link String package names} to scan.
	 * @return a {@link Set} of {@link Class types} found during the scan.
	 * @see #getTypeScanner()
	 */
	default Set<Class<?>> scanPackageForTypes(Collection<Class<? extends Annotation>> identifyingAnnotations,
			Collection<String> packageNames) {

		return getTypeScanner().scanPackages(packageNames).forTypesAnnotatedWith(identifyingAnnotations).collectAsSet();
	}

	/**
	 * Returns a {@link IntrospectedBeanDefinition} to obtain further detail about the underlying bean definition. An
	 * introspected bean definition can also point to an absent bean definition.
	 *
	 * @param reference {@link BeanReference} to the managed bean.
	 * @return the introspected bean definition.
	 */
	default IntrospectedBeanDefinition introspectBeanDefinition(BeanReference reference) {
		return introspectBeanDefinition(reference.getBeanName());
	}

	/**
	 * Returns a {@link IntrospectedBeanDefinition} to obtain further detail about the underlying bean definition. An
	 * introspected bean definition can also point to an absent bean definition.
	 *
	 * @param beanName {@link String} containing the {@literal name} of the bean to evaluate; must not be {@literal null}.
	 * @return the introspected bean definition.
	 */
	IntrospectedBeanDefinition introspectBeanDefinition(String beanName);

	/**
	 * Type-based introspector to resolve {@link Class} from a type name and to introspect the bean factory for presence
	 * of beans.
	 */
	interface TypeIntrospector {

		/**
		 * Determines whether @link Class type} is present on the application classpath.
		 *
		 * @return {@literal true} if the {@link Class type} is present on the application classpath.
		 * @see #getClassLoader()
		 */
		boolean isTypePresent();

		/**
		 * Resolves the required {@link String named} {@link Class type}.
		 *
		 * @return a resolved {@link Class type} for the given.
		 * @throws TypeNotPresentException if the {@link Class type} cannot be found.
		 */
		Class<?> resolveRequiredType() throws TypeNotPresentException;

		/**
		 * Resolves the {@link Class type} if present.
		 *
		 * @return an {@link Optional} value containing the {@link Class type} if the type is present on the application
		 *         classpath.
		 * @see #isTypePresent()
		 * @see #resolveRequiredType()
		 * @see java.util.Optional
		 */
		Optional<Class<?>> resolveType();

		/**
		 * Determines whether the {@link Class type} is declared on the application classpath and performs the given,
		 * required {@link Consumer action} if present.
		 *
		 * @param action {@link Consumer} defining the action to perform on the resolved {@link Class type}; must not be
		 *          {@literal null}.
		 * @see java.util.function.Consumer
		 * @see #resolveType()
		 */
		default void ifTypePresent(Consumer<Class<?>> action) {
			resolveType().ifPresent(action);
		}

		/**
		 * Determines whether the associated bean factory contains at least one bean of this type.
		 *
		 * @return {@literal true} if the {@link Class type} is present on the application classpath.
		 */
		boolean hasBean();

		/**
		 * Return a {@link List} containing bean names that implement this type.
		 *
		 * @return a {@link List} of bean names. The list is empty if the bean factory does not hold any beans of this type.
		 */
		List<String> getBeanNames();

	}

	/**
	 * Interface defining introspection methods for bean definitions.
	 */
	interface IntrospectedBeanDefinition {

		/**
		 * Determines whether a bean definition identified by the given, required {@link String name} is present.
		 *
		 * @return {@literal true} if the bean definition identified by the given, required {@link String name} registered
		 *         with.
		 */
		boolean isPresent();

		/**
		 * Determines whether a bean identified by the given, required {@link String name} is a
		 * {@link org.springframework.beans.factory.FactoryBean}.
		 *
		 * @return {@literal true} if the bean identified by the given, required {@link String name} is a
		 *         {@link org.springframework.beans.factory.FactoryBean}.
		 */
		boolean isFactoryBean();

		/**
		 * Gets the {@link BeanDefinition} for the given, required {@link String named bean}.
		 *
		 * @return the {@link BeanDefinition} for the given, required {@link String named bean}.
		 * @throws NoSuchBeanDefinitionException if a {@link BeanDefinition} cannot be found for the {@link String named
		 *           bean}.
		 * @see BeanDefinition
		 */
		BeanDefinition getBeanDefinition() throws NoSuchBeanDefinitionException;

		/**
		 * Gets the {@link RootBeanDefinition} for the given, required {@link String bean name}.
		 *
		 * @return the {@link RootBeanDefinition} for the given, required {@link String bean name}.
		 * @throws NoSuchBeanDefinitionException if a {@link BeanDefinition} cannot be found for the {@link String named
		 *           bean}.
		 * @throws IllegalStateException if the bean is not a {@link RootBeanDefinition root bean}.
		 * @see RootBeanDefinition
		 */
		RootBeanDefinition getRootBeanDefinition() throws NoSuchBeanDefinitionException;

		/**
		 * Resolves the {@link BeanDefinition bean's} defined {@link Class type}.
		 *
		 * @return the {@link Class type} of the {@link BeanReference referenced bean} if defined; may be {@literal null}.
		 * @see BeanReference
		 */
		@Nullable
		Class<?> resolveType();

	}

}
