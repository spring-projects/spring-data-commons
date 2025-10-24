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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.util.TypeScanner;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The context in which the AOT processing happens. Grants access to the {@link ConfigurableListableBeanFactory
 * beanFactory} and {@link ClassLoader}.
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
	String GENERATED_REPOSITORIES_JSON_ENABLED = "spring.aot.repositories.metadata.enabled";

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
	 * Checks if repository code generation is enabled for a given module by checking environment variables for general
	 * enablement ({@link #GENERATED_REPOSITORIES_ENABLED}) and store-specific ones following the pattern
	 * {@literal spring.aot.&lt;module-name&gt;.repositories.enabled}.
	 * <p>
	 * {@link #GENERATED_REPOSITORIES_ENABLED} acts as a main switch, if disabled, store specific flags have no effect.
	 * <p>
	 * Unset properties are considered being {@literal true}.
	 *
	 * @param moduleName name of the module. Can be {@literal null} or {@literal empty}, in which case it will only check
	 *          the general {@link #GENERATED_REPOSITORIES_ENABLED} flag.
	 * @return indicator if repository code generation is enabled.
	 * @since 4.0
	 */
	default boolean isGeneratedRepositoriesEnabled(@Nullable String moduleName) {

		Environment environment = getEnvironment();

		if (!environment.getProperty(GENERATED_REPOSITORIES_ENABLED, Boolean.class, true)) {
			return false;
		}

		if (!StringUtils.hasText(moduleName)) {
			return true;
		}

		String modulePropertyName = GENERATED_REPOSITORIES_ENABLED.replace("repositories",
				"%s.repositories".formatted(moduleName.toLowerCase(Locale.ROOT)));
		return environment.getProperty(modulePropertyName, Boolean.class, true);
	}

	/**
	 * Checks if repository metadata file writing is enabled by checking environment variables for general enablement
	 * ({@link #GENERATED_REPOSITORIES_JSON_ENABLED})
	 * <p>
	 * Unset properties are considered being {@literal true}.
	 *
	 * @return indicator if repository metadata should be written
	 * @since 4.0
	 */
	default boolean isGeneratedRepositoriesMetadataEnabled() {
		return getEnvironment().getProperty(GENERATED_REPOSITORIES_JSON_ENABLED, Boolean.class, true);
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
	 * @deprecated since 4.0 as this isn't widely used and can be easily implemented within user code.
	 */
	@Deprecated(since = "4.0", forRemoval = true)
	default TypeIntrospector introspectType(String typeName) {
		throw new UnsupportedOperationException(); // preparation for implementation removal.
	}

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
	 * @deprecated since 4.0, use {@link #getTypeScanner()} directly
	 */
	@Deprecated(since = "4.0", forRemoval = true)
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
	 * @deprecated since 4.0, use {@link #getBeanFactory()} and interact with the bean factory directly.
	 */
	@Deprecated(since = "4.0", forRemoval = true)
	default IntrospectedBeanDefinition introspectBeanDefinition(BeanReference reference) {
		return introspectBeanDefinition(reference.getBeanName());
	}

	/**
	 * Returns a {@link IntrospectedBeanDefinition} to obtain further detail about the underlying bean definition. An
	 * introspected bean definition can also point to an absent bean definition.
	 *
	 * @param beanName {@link String} containing the {@literal name} of the bean to evaluate; must not be {@literal null}.
	 * @return the introspected bean definition.
	 * @deprecated since 4.0, use {@link #getBeanFactory()} and interact with the bean factory directly.
	 */
	@Deprecated(since = "4.0", forRemoval = true)
	default IntrospectedBeanDefinition introspectBeanDefinition(String beanName) {
		throw new UnsupportedOperationException(); // preparation for implementation removal.
	}

	/**
	 * Obtain a {@link AotTypeConfiguration} for the given {@link ResolvableType} to customize the AOT processing for the
	 * given type. Repeated calls to the same type will result in merging the configuration.
	 *
	 * @param resolvableType the resolvable type to configure.
	 * @param configurationConsumer configuration consumer function.
	 * @since 4.0
	 */
	default void typeConfiguration(ResolvableType resolvableType, Consumer<AotTypeConfiguration> configurationConsumer) {
		typeConfiguration(resolvableType.toClass(), configurationConsumer);
	}

	/**
	 * Obtain a {@link AotTypeConfiguration} for the given {@link ResolvableType} to customize the AOT processing for the
	 * given type. Repeated calls to the same type will result in merging the configuration.
	 *
	 * @param type the type to configure.
	 * @param configurationConsumer configuration consumer function.
	 * @since 4.0
	 */
	void typeConfiguration(Class<?> type, Consumer<AotTypeConfiguration> configurationConsumer);

	/**
	 * Contribute type configurations to the given {@link GenerationContext}. This method is called once per
	 * {@link GenerationContext} after all type configurations have been registered.
	 *
	 * @param generationContext the context to contribute the type configurations to.
	 */
	void contributeTypeConfigurations(GenerationContext generationContext);

	/**
	 * Type-based introspector to resolve {@link Class} from a type name and to introspect the bean factory for presence
	 * of beans.
	 *
	 * @deprecated since 4.0 as this isn't widely used and can be easily implemented within user code.
	 */
	@Deprecated(since = "4.0", forRemoval = true)
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
	 *
	 * @deprecated since 4.0 as this isn't widely used and can be easily implemented within user code.
	 */
	@Deprecated(since = "4.0", forRemoval = true)
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
