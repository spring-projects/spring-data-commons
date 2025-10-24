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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.aot.AotProcessingException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.QTypeContributor;
import org.springframework.data.util.TypeContributor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default {@link AotContext} implementation.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.0
 */
@SuppressWarnings("removal")
class DefaultAotContext implements AotContext {

	private final AotMappingContext mappingContext;
	private final ConfigurableListableBeanFactory factory;

	private final Map<Class<?>, ContextualTypeConfiguration> typeConfigurations = new HashMap<>();
	private final Environment environment;
	private final ReflectiveRuntimeHintsRegistrar reflectiveRuntimeHintsRegistrar = new ReflectiveRuntimeHintsRegistrar();

	public DefaultAotContext(BeanFactory beanFactory, Environment environment) {
		this(beanFactory, environment, new AotMappingContext());
	}

	DefaultAotContext(BeanFactory beanFactory, Environment environment, AotMappingContext mappingContext) {
		this.factory = beanFactory instanceof ConfigurableListableBeanFactory cbf ? cbf
				: new DefaultListableBeanFactory(beanFactory);
		this.environment = environment;
		this.mappingContext = mappingContext;
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		return factory;
	}

	@Override
	public Environment getEnvironment() {
		return environment;
	}

	@Override
	public TypeIntrospector introspectType(String typeName) {
		return new DefaultTypeIntrospector(typeName);
	}

	@Override
	public IntrospectedBeanDefinition introspectBeanDefinition(String beanName) {
		return new DefaultIntrospectedBeanDefinition(beanName);
	}

	@Override
	public void typeConfiguration(Class<?> type, Consumer<AotTypeConfiguration> configurationConsumer) {
		configurationConsumer.accept(typeConfigurations.computeIfAbsent(type, it -> new ContextualTypeConfiguration(type)));
	}

	@Override
	public void contributeTypeConfigurations(GenerationContext generationContext) {
		typeConfigurations.forEach((type, configuration) -> {
			configuration.contribute(this.environment, generationContext);
		});
	}

	@SuppressWarnings("removal")
	class DefaultTypeIntrospector implements TypeIntrospector {

		private final String typeName;

		DefaultTypeIntrospector(String typeName) {
			this.typeName = typeName;
		}

		@Override
		public boolean isTypePresent() {
			return ClassUtils.isPresent(typeName, getClassLoader());
		}

		@Override
		public Class<?> resolveRequiredType() throws TypeNotPresentException {
			try {
				return ClassUtils.forName(typeName, getClassLoader());
			} catch (ClassNotFoundException cause) {
				throw new TypeNotPresentException(typeName, cause);
			}
		}

		@Override
		public Optional<Class<?>> resolveType() {
			return isTypePresent() ? Optional.of(resolveRequiredType()) : Optional.empty();
		}

		@Override
		public boolean hasBean() {
			return !getBeanNames().isEmpty();
		}

		@Override
		public List<String> getBeanNames() {
			return isTypePresent() ? Arrays.asList(factory.getBeanNamesForType(resolveRequiredType()))
					: Collections.emptyList();
		}
	}

	@SuppressWarnings("removal")
	class DefaultIntrospectedBeanDefinition implements IntrospectedBeanDefinition {

		private final String beanName;

		DefaultIntrospectedBeanDefinition(String beanName) {
			this.beanName = beanName;
		}

		@Override
		public boolean isPresent() {
			return factory.containsBeanDefinition(beanName);
		}

		@Override
		public boolean isFactoryBean() {
			return factory.isFactoryBean(beanName);
		}

		@Override
		public BeanDefinition getBeanDefinition() throws NoSuchBeanDefinitionException {
			return factory.getBeanDefinition(beanName);
		}

		@Override
		public RootBeanDefinition getRootBeanDefinition() throws NoSuchBeanDefinitionException {
			BeanDefinition beanDefinition = getBeanDefinition();

			if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
				return rootBeanDefinition;
			}

			throw new IllegalStateException(String.format("%s is not a root bean", beanName));
		}

		@Override
		public @Nullable Class<?> resolveType() {
			return factory.getType(beanName, false);
		}
	}

	class ContextualTypeConfiguration implements AotTypeConfiguration {

		private final Class<?> type;
		private boolean forDataBinding = false;
		private final Set<MemberCategory> categories = new HashSet<>(5);
		private boolean contributeAccessors = false;
		private boolean forQuerydsl = false;
		private final List<List<TypeReference>> proxies = new ArrayList<>();

		ContextualTypeConfiguration(Class<?> type) {
			this.type = type;
		}

		@Override
		public AotTypeConfiguration forDataBinding() {
			this.forDataBinding = true;
			return this;
		}

		@Override
		public AotTypeConfiguration forReflectiveAccess(MemberCategory... categories) {
			this.categories.addAll(Arrays.asList(categories));
			return this;
		}

		@Override
		public AotTypeConfiguration contributeAccessors() {
			this.contributeAccessors = true;
			return this;
		}

		@Override
		public AotTypeConfiguration proxyInterface(List<TypeReference> interfaces) {
			this.proxies.add(interfaces);
			return this;
		}

		@Override
		public AotTypeConfiguration forQuerydsl() {
			this.forQuerydsl = true;
			return this;
		}

		public void contribute(Environment environment, GenerationContext generationContext) {

			try {
				doContribute(environment, generationContext);
			} catch (RuntimeException e) {
				throw new AotProcessingException("Cannot contribute Ahead-of-Time optimizations for '" + type.getName() + "'",
						e);
			}
		}

		private void doContribute(Environment environment, GenerationContext generationContext) {

			if (!this.categories.isEmpty()) {
				generationContext.getRuntimeHints().reflection().registerType(this.type,
						categories.toArray(MemberCategory[]::new));
			}

			// check types for presence of @Reflective annotation
			reflectiveRuntimeHintsRegistrar.registerRuntimeHints(generationContext.getRuntimeHints(), type);

			if (contributeAccessors) {

				AccessorContributionConfiguration configuration = AccessorContributionConfiguration.of(environment);
				if (configuration.shouldContributeAccessors(type)) {
					mappingContext.contribute(type);
				}
			}

			if (forDataBinding) {
				TypeContributor.contribute(type, Set.of(TypeContributor.DATA_NAMESPACE), generationContext);
			}

			if (forQuerydsl) {
				QTypeContributor.contributeEntityPath(type, generationContext, factory.getBeanClassLoader());
			}

			if (!proxies.isEmpty()) {
				for (List<TypeReference> proxyInterfaces : proxies) {
					generationContext.getRuntimeHints().proxies().registerJdkProxy(
							Stream.concat(Stream.of(TypeReference.of(type)), proxyInterfaces.stream()).toArray(TypeReference[]::new));
				}
			}
		}

	}

	/**
	 * Configuration for accessor to determine whether accessors should be contributed for a given type.
	 */
	private record AccessorContributionConfiguration(boolean enabled, Lazy<String> include, Lazy<String> exclude) {

		/**
		 * {@code boolean }Environment property to enable/disable accessor contribution. Enabled by default.
		 */
		public static final String ACCESSORS_ENABLED = "spring.aot.data.accessors.enabled";

		/**
		 * {@code String} Environment property to define Ant-style include patterns (comma-separated) matching package names
		 * (e.g. {@code com.acme.**}) or type names inclusion. Inclusion pattern matches are evaluated before exclusions for
		 * broad exclusion and selective inclusion.
		 */
		public static final String INCLUDE_PATTERNS = "spring.aot.data.accessors.include";

		/**
		 * {@code String} Environment property to define Ant-style exclude patterns (comma-separated) matching package names
		 * (e.g. {@code com.acme.**}) or type names exclusion. Exclusion pattern matches are evaluated after inclusions for
		 * broad exclusion and selective inclusion.
		 */
		public static final String EXCLUDE_PATTERNS = "spring.aot.data.accessors.exclude";

		private static final AntPathMatcher antPathMatcher = new AntPathMatcher(".");

		private AccessorContributionConfiguration(boolean enabled, Supplier<String> include, Supplier<String> exclude) {
			this(enabled, Lazy.of(include), Lazy.of(exclude));
		}

		public static AccessorContributionConfiguration of(Environment environment) {
			return new AccessorContributionConfiguration(environment.getProperty(ACCESSORS_ENABLED, Boolean.class, true),
					() -> environment.getProperty(INCLUDE_PATTERNS, String.class, ""),
					() -> environment.getProperty(EXCLUDE_PATTERNS, String.class, ""));
		}

		boolean shouldContributeAccessors(Class<?> type) {

			if (!enabled) {
				return false;
			}

			if (StringUtils.hasText(include.get())) {

				String[] includes = include.get().split(",");

				for (String includePattern : includes) {
					if (antPathMatcher.match(includePattern.trim(), type.getName())) {
						return true;
					}
				}
			}

			if (StringUtils.hasText(exclude.get())) {

				String[] excludes = exclude.get().split(",");

				for (String excludePattern : excludes) {
					if (antPathMatcher.match(excludePattern.trim(), type.getName())) {
						return false;
					}
				}
			}

			return true;
		}

	}

}
