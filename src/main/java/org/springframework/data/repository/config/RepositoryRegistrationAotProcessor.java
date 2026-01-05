/*
 * Copyright 2022-present the original author or authors.
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
package org.springframework.data.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.aot.AotContext;
import org.springframework.data.aot.AotTypeConfiguration;
import org.springframework.data.projection.EntityProjectionIntrospector;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.TypeContributor;
import org.springframework.data.util.TypeUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanRegistrationAotProcessor} responsible processing and providing AOT configuration for repositories.
 * <p>
 * Processes {@link RepositoryFactoryBeanSupport repository factory beans} to provide generic type information to the
 * AOT tooling to allow deriving target type from the {@link RootBeanDefinition bean definition}. If generic types do
 * not match due to customization of the factory bean by the user, at least the target repository type is provided via
 * the {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE}.
 * <p>
 * With {@link #registerRepositoryCompositionHints(AotRepositoryContext, GenerationContext)} (specifically
 * {@link #configureTypeContribution(Class, AotContext)} and {@link #contributeAotRepository(AotRepositoryContext)},
 * stores can provide custom logic for contributing additional (e.g. reflection) configuration. By default, reflection
 * configuration will be added for types reachable from the repository declaration and query methods as well as all used
 * {@link Annotation annotations} from the {@literal org.springframework.data} namespace.
 * <p>
 * The processor is typically configured via {@link RepositoryConfigurationExtension#getRepositoryAotProcessor()} and
 * gets added by the {@link org.springframework.data.repository.config.RepositoryConfigurationDelegate}.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @author Mark Paluch
 * @since 3.0
 */
public class RepositoryRegistrationAotProcessor
		implements BeanRegistrationAotProcessor, BeanFactoryAware, EnvironmentAware, EnvironmentCapable {

	private static final String KOTLIN_COROUTINE_REPOSITORY_TYPE_NAME = "org.springframework.data.repository.kotlin.CoroutineCrudRepository";

	private static final List<TypeReference> KOTLIN_REFLECTION_TYPE_REFERENCES = List.of(
			TypeReference.of("org.springframework.data.repository.kotlin.CoroutineCrudRepository"),
			TypeReference.of(Repository.class), //
			TypeReference.of(Iterable.class), //
			TypeReference.of("kotlinx.coroutines.flow.Flow"), //
			TypeReference.of("kotlin.collections.Iterable"), //
			TypeReference.of("kotlin.Unit"), //
			TypeReference.of("kotlin.Long"), //
			TypeReference.of("kotlin.Boolean"));

	private final Log logger = LogFactory.getLog(getClass());

	private @Nullable ConfigurableListableBeanFactory beanFactory;

	private Environment environment = new StandardEnvironment();

	private Map<String, RepositoryConfiguration<?>> configMap = Collections.emptyMap();

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				() -> "AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);

		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * Setter for the config map. See {@code RepositoryConfigurationDelegate#registerAotComponents}.
	 *
	 * @param configMap
	 */
	@SuppressWarnings("unused")
	public void setConfigMap(Map<String, RepositoryConfiguration<?>> configMap) {
		this.configMap = configMap;
	}

	public Map<String, RepositoryConfiguration<?>> getConfigMap() {
		return this.configMap;
	}

	protected ConfigurableListableBeanFactory getBeanFactory() {

		if (this.beanFactory == null) {
			throw new IllegalStateException(
					"No BeanFactory available. Make sure to set the BeanFactory before using this processor.");
		}

		return this.beanFactory;
	}

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean bean) {

		if (!isRepositoryBean(bean)) {
			return null;
		}

		RepositoryConfiguration<?> repositoryMetadata = getRepositoryMetadata(bean);
		AotRepositoryContext repositoryContext = potentiallyCreateContext(environment, bean);

		if (repositoryMetadata == null || repositoryContext == null) {
			return null;
		}

		BeanRegistrationAotContribution contribution = (generationContext, beanRegistrationCode) -> {

			registerRepositoryCompositionHints(repositoryContext, generationContext);
			configureTypeContributions(repositoryContext, generationContext);

			repositoryContext.contributeTypeConfigurations(generationContext);
		};

		return new RepositoryRegistrationAotContribution(repositoryContext, contribution,
				contributeAotRepository(repositoryContext));
	}

	/**
	 * Contribute repository-specific hints, e.g. for repository proxy, base implementation, fragments. Customization hook
	 * for subclasses that wish to customize repository hint contribution.
	 *
	 * @param repositoryContext the repository context.
	 * @param generationContext the generation context.
	 * @since 4.0
	 */
	protected void registerRepositoryCompositionHints(AotRepositoryContext repositoryContext,
			GenerationContext generationContext) {

		RepositoryInformation repositoryInformation = repositoryContext.getRepositoryInformation();

		if (logger.isTraceEnabled()) {
			logger.trace(
					"Contributing repository information for [%s]".formatted(repositoryInformation.getRepositoryInterface()));
		}

		// Native hints for repository proxy
		repositoryContext.typeConfiguration(repositoryInformation.getRepositoryInterface(),
				config -> config.forReflectiveAccess(MemberCategory.INVOKE_PUBLIC_METHODS).repositoryProxy());

		// Native hints for reflective base implementation access
		repositoryContext.typeConfiguration(repositoryInformation.getRepositoryBaseClass(), config -> config
				.forReflectiveAccess(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));

		// Repository Fragments
		registerFragmentsHints(repositoryInformation.getFragments(), generationContext);

		// Kotlin
		if (isKotlinCoroutineRepository(repositoryInformation)) {
			generationContext.getRuntimeHints().reflection().registerTypes(KOTLIN_REFLECTION_TYPE_REFERENCES, hint -> {});
		}
	}

	/**
	 * Register type-specific hints and AOT artifacts for domain types, reachable types, projection interfaces derived
	 * from query method return types, and annotations from {@literal org.springframework.data} packages.
	 *
	 * @param repositoryContext the repository context.
	 * @param generationContext the generation context.
	 * @since 4.0
	 */
	protected void configureTypeContributions(AotRepositoryContext repositoryContext,
			GenerationContext generationContext) {

		RepositoryInformation information = repositoryContext.getRepositoryInformation();

		configureDomainTypeContributions(repositoryContext, generationContext);

		// Repository query methods
		information.getQueryMethods().stream().map(information::getReturnedDomainClass).filter(Class::isInterface)
				.forEach(type -> {
					if (EntityProjectionIntrospector.ProjectionPredicate.typeHierarchy().test(type,
							information.getDomainType())) {
						repositoryContext.typeConfiguration(type, AotTypeConfiguration::usedAsProjectionInterface);
					}
				});

		repositoryContext.getResolvedAnnotations().stream()
				.filter(RepositoryRegistrationAotProcessor::isSpringDataManagedAnnotation).map(MergedAnnotation::getType)
				.forEach(it -> contributeType(it, generationContext));
	}

	/**
	 * Customization hook for subclasses that wish to customize domain type hint contributions.
	 * <p>
	 * Type hints are registered for the domain, alternative domain types, and types reachable from there
	 * ({@link AotRepositoryContext#getResolvedTypes()})
	 *
	 * @param repositoryContext the repository context.
	 * @param generationContext the generation context.
	 * @since 4.0
	 */
	private void configureDomainTypeContributions(AotRepositoryContext repositoryContext,
			GenerationContext generationContext) {

		RepositoryInformation information = repositoryContext.getRepositoryInformation();

		Stream.concat(Stream.of(information.getDomainType()), information.getAlternativeDomainTypes().stream())
				.forEach(it -> {
					configureTypeContribution(it, repositoryContext);
				});

		// Domain types my be part of this, but it also contains reachable ones.
		repositoryContext.getResolvedTypes().stream()
				.filter(it -> TypeContributor.isPartOf(it, Set.of(information.getDomainType().getPackageName())))
				.forEach(it -> configureTypeContribution(it, repositoryContext));

		repositoryContext.getResolvedTypes().stream().filter(it -> !isJavaOrPrimitiveType(it))
				.forEach(it -> contributeType(it, generationContext));
	}

	/**
	 * Customization hook to configure the {@link TypeContributor} used to register the given {@literal type}.
	 *
	 * @param type the class to configure the contribution for.
	 * @param aotContext AOT context for type configuration.
	 * @since 4.0
	 */
	protected void configureTypeContribution(Class<?> type, AotContext aotContext) {
		aotContext.typeConfiguration(type, config -> config.forDataBinding().contributeAccessors().forQuerydsl());
	}

	/**
	 * This method allows for the creation to be overridden by subclasses.
	 *
	 * @param repositoryContext the context for the repository being processed.
	 * @return a {@link RepositoryContributor} to contribute store-specific AOT artifacts or {@literal null} to skip
	 *         store-specific AOT contributions.
	 * @since 4.0
	 */
	@Nullable
	protected RepositoryContributor contributeAotRepository(AotRepositoryContext repositoryContext) {
		return null;
	}

	private boolean isRepositoryBean(RegisteredBean bean) {
		return getConfigMap().containsKey(bean.getBeanName());
	}

	private RepositoryConfiguration<?> getRepositoryMetadata(RegisteredBean bean) {

		RepositoryConfiguration<?> configuration = getConfigMap().get(bean.getBeanName());

		if (configuration == null) {
			throw new IllegalArgumentException("No configuration for bean [%s]".formatted(bean.getBeanName()));
		}

		return configuration;
	}

	private void contributeType(Class<?> type, GenerationContext context) {
		TypeContributor.contribute(type, it -> true, context);
	}

	private void registerFragmentsHints(Iterable<RepositoryFragment<?>> fragments, GenerationContext contribution) {
		fragments.forEach(it -> registerFragmentHints(it, contribution));
	}

	private static void registerFragmentHints(RepositoryFragment<?> fragment, GenerationContext context) {

		Class<?> repositoryFragmentType = fragment.getSignatureContributor();
		Optional<Class<?>> implementation = fragment.getImplementationClass();

		registerReflectiveHints(repositoryFragmentType, context);

		implementation.ifPresent(typeToRegister -> registerReflectiveHints(typeToRegister, context));
	}

	private static void registerReflectiveHints(Class<?> typeToRegister, GenerationContext context) {

		context.getRuntimeHints().reflection().registerType(typeToRegister, hint -> {

			hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);

			if (!typeToRegister.isInterface()) {
				hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			}
		});
	}

	private @Nullable AotRepositoryContext potentiallyCreateContext(Environment environment, RegisteredBean bean) {

		RepositoryBeanDefinitionReader reader = new RepositoryBeanDefinitionReader(bean);
		RepositoryConfiguration<?> configuration = reader.getConfiguration();
		RepositoryConfigurationExtensionSupport extension = reader.getConfigurationExtension();

		if (configuration == null || extension == null) {
			logger.warn(
					"Cannot create AotRepositoryContext for bean [%s]. No RepositoryConfiguration/RepositoryConfigurationExtension. Please make sure to register the repository bean through @Enable…Repositories."
							.formatted(bean.getBeanName()));
			return null;
		}
		RepositoryInformation repositoryInformation = reader.getRepositoryInformation();
		DefaultAotRepositoryContext repositoryContext = new DefaultAotRepositoryContext(bean, repositoryInformation,
				extension.getModuleName(), AotContext.from(bean.getBeanFactory(), environment),
				configuration.getConfigurationSource());

		repositoryContext.setIdentifyingAnnotations(extension.getIdentifyingAnnotations());

		return repositoryContext;
	}

	private static boolean isKotlinCoroutineRepository(RepositoryInformation repositoryInformation) {

		Class<?> coroutineRepository = org.springframework.data.util.ClassUtils.loadIfPresent(
				KOTLIN_COROUTINE_REPOSITORY_TYPE_NAME, repositoryInformation.getRepositoryInterface().getClassLoader());

		return coroutineRepository != null
				&& ClassUtils.isAssignable(coroutineRepository, repositoryInformation.getRepositoryInterface());
	}

	private static boolean isSpringDataManagedAnnotation(MergedAnnotation<?> annotation) {

		return isSpringDataType(annotation.getType())
				|| annotation.getMetaTypes().stream().anyMatch(RepositoryRegistrationAotProcessor::isSpringDataType);
	}

	private static boolean isSpringDataType(Class<?> type) {
		return type.getPackageName().startsWith(TypeContributor.DATA_NAMESPACE);
	}

	private static boolean isJavaOrPrimitiveType(Class<?> type) {
		return ClassUtils.isPrimitiveOrWrapper(type) //
				|| ClassUtils.isPrimitiveArray(type) //
				|| TypeUtils.type(type).isPartOf("java");
	}

}
