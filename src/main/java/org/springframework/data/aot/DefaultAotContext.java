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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.aot.AotMappingContext.BasicPersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.util.QTypeContributor;
import org.springframework.data.util.TypeContributor;
import org.springframework.util.ClassUtils;

/**
 * Default {@link AotContext} implementation.
 *
 * @author Mark Paluch
 * @since 3.0
 */
class DefaultAotContext implements AotContext {

	private final AotMappingContext mappingContext = new AotMappingContext();
	private final ConfigurableListableBeanFactory factory;

	// TODO: should we reuse the config or potentially have multiple ones with different settings - somehow targets the
	// filtering issue
	private final Map<TypeReference, AotTypeConfiguration> typeConfigurations = new HashMap<>();

	private final Environment environment;

	public DefaultAotContext(BeanFactory beanFactory, Environment environment) {
		factory = beanFactory instanceof ConfigurableListableBeanFactory cbf ? cbf
				: new DefaultListableBeanFactory(beanFactory);
		this.environment = environment;
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
	public InstantiationCreator instantiationCreator(TypeReference typeReference) {
		return new DefaultInstantiationCreator(introspectType(typeReference.getName()));
	}

	@Override
	public AotTypeConfiguration typeConfiguration(TypeReference typeReference) {
		return typeConfigurations.computeIfAbsent(typeReference, it -> new ContextualTypeConfiguration(typeReference));
	}

	@Override
	public Collection<AotTypeConfiguration> typeConfigurations() {
		return typeConfigurations.values();
	}

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

	class DefaultInstantiationCreator implements InstantiationCreator {

		Lazy<BasicPersistentEntity<?, BasicPersistentProperty>> entity;

		public DefaultInstantiationCreator(TypeIntrospector typeIntrospector) {
			this.entity = Lazy.of(() -> mappingContext.getPersistentEntity(typeIntrospector.resolveRequiredType()));
		}

		@Override
		public boolean isAvailable() {
			return entity.getNullable() != null;
		}

		@Override
		public void create() {

			BasicPersistentEntity<?, BasicPersistentProperty> persistentEntity = entity.getNullable();
			if (persistentEntity != null) {
				mappingContext.contribute(persistentEntity);
			}
		}
	}

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

		private final TypeReference type;
		private boolean forDataBinding = false;
		private final Set<MemberCategory> categories = new HashSet<>(5);
		private boolean generateEntityInstantiator = false;
		private boolean forQuerydsl = false;
		private final List<List<TypeReference>> proxies = new ArrayList<>();
		private Predicate<TypeReference> filter;

		ContextualTypeConfiguration(TypeReference type) {
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
		public AotTypeConfiguration generateEntityInstantiator() {
			this.generateEntityInstantiator = true;
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

		@Override
		public AotTypeConfiguration conditional(Predicate<TypeReference> filter) {

			this.filter = filter;
			return this;
		}

		@Override
		public void contribute(GenerationContext generationContext) {

			if (filter != null && !filter.test(this.type)) {
				return;
			}

			if (!this.categories.isEmpty()) {
				generationContext.getRuntimeHints().reflection().registerType(this.type,
						categories.toArray(MemberCategory[]::new));
			}

			if (generateEntityInstantiator) {
				instantiationCreator(type).create();
			}

			if (forDataBinding) {
				if (!doIfPresent(resolved -> TypeContributor.contribute(resolved, Set.of(TypeContributor.DATA_NAMESPACE),
						generationContext))) {
					generationContext.getRuntimeHints().reflection().registerType(type,
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS);
				}
			}

			if (forQuerydsl) {
				doIfPresent(
						resolved -> QTypeContributor.contributeEntityPath(resolved, generationContext, resolved.getClassLoader()));
			}

			if (!proxies.isEmpty()) {
				for (List<TypeReference> proxyInterfaces : proxies) {
					generationContext.getRuntimeHints().proxies()
							.registerJdkProxy(Stream.concat(Stream.of(type), proxyInterfaces.stream()).toArray(TypeReference[]::new));
				}
			}

		}

		private boolean doIfPresent(Consumer<Class<?>> consumer) {
			if (!ClassUtils.isPresent(type.getName(), type.getClass().getClassLoader())) {
				return false;
			}
			try {
				Class<?> resolved = ClassUtils.forName(type.getName(), type.getClass().getClassLoader());
				consumer.accept(resolved);
				return true;
			} catch (ClassNotFoundException e) {
				return false;
			}
		}
	}
}
