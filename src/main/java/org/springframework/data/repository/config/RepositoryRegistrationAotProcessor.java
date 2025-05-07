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
package org.springframework.data.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
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
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.util.TypeContributor;
import org.springframework.util.Assert;

/**
 * {@link BeanRegistrationAotProcessor} responsible processing and providing AOT configuration for repositories.
 * <p>
 * Processes {@link RepositoryFactoryBeanSupport repository factory beans} to provide generic type information to the
 * AOT tooling to allow deriving target type from the {@link RootBeanDefinition bean definition}. If generic types do
 * not match due to customization of the factory bean by the user, at least the target repository type is provided via
 * the {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE}.
 * </p>
 * <p>
 * With {@link RepositoryRegistrationAotProcessor#contribute(AotRepositoryContext, GenerationContext)}, stores can
 * provide custom logic for contributing additional (eg. reflection) configuration. By default, reflection configuration
 * will be added for types reachable from the repository declaration and query methods as well as all used
 * {@link Annotation annotations} from the {@literal org.springframework.data} namespace.
 * </p>
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

	private final Log logger = LogFactory.getLog(getClass());

	private @Nullable ConfigurableListableBeanFactory beanFactory;

	private Environment environment = new StandardEnvironment();

	private Map<String, RepositoryConfiguration<?>> configMap = Collections.emptyMap();

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean bean) {
		return isRepositoryBean(bean) ? newRepositoryRegistrationAotContribution(bean) : null;
	}

	@Nullable
	protected RepositoryContributor contribute(AotRepositoryContext repositoryContext, GenerationContext generationContext) {

		repositoryContext.getResolvedTypes().stream()
				.filter(it -> !RepositoryRegistrationAotContribution.isJavaOrPrimitiveType(it))
				.forEach(it -> contributeType(it, generationContext));

		repositoryContext.getResolvedAnnotations().stream()
				.filter(RepositoryRegistrationAotProcessor::isSpringDataManagedAnnotation).map(MergedAnnotation::getType)
				.forEach(it -> contributeType(it, generationContext));

		return null;
	}

	/**
	 * Processes the repository's domain and alternative domain types to consider {@link Reflective} annotations used on
	 * it.
	 *
	 * @param repositoryContext must not be {@literal null}.
	 * @param generationContext must not be {@literal null}.
	 */
	private void registerReflectiveForAggregateRoot(AotRepositoryContext repositoryContext,
			GenerationContext generationContext) {

		RepositoryInformation information = repositoryContext.getRepositoryInformation();
		ReflectiveRuntimeHintsRegistrar registrar = new ReflectiveRuntimeHintsRegistrar();
		RuntimeHints hints = generationContext.getRuntimeHints();

		Stream.concat(Stream.of(information.getDomainType()), information.getAlternativeDomainTypes().stream())
				.forEach(it -> registrar.registerRuntimeHints(hints, it));
	}

	private boolean isRepositoryBean(RegisteredBean bean) {
		return getConfigMap().containsKey(bean.getBeanName());
	}

	protected @Nullable RepositoryRegistrationAotContribution newRepositoryRegistrationAotContribution(
			RegisteredBean repositoryBean) {

		RepositoryRegistrationAotContribution contribution = RepositoryRegistrationAotContribution.load(this,
				repositoryBean);

		// cannot contribute a repository bean.
		if (contribution == null) {
			return null;
		}

		//TODO: add the hook for customizing bean initialization code here!

		return contribution.withModuleContribution((repositoryContext, generationContext) -> {
			registerReflectiveForAggregateRoot(repositoryContext, generationContext);
			return contribute(repositoryContext, generationContext);
		});
	}

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

	protected @Nullable RepositoryConfiguration<?> getRepositoryMetadata(RegisteredBean bean) {
		return getConfigMap().get(bean.getBeanName());
	}

	protected void contributeType(Class<?> type, GenerationContext generationContext) {
		TypeContributor.contribute(type, it -> true, generationContext);
	}

	protected Log getLogger() {
		return this.logger;
	}

	protected void logDebug(String message, Object... arguments) {
		logAt(Log::isDebugEnabled, Log::debug, message, arguments);
	}

	protected void logTrace(String message, Object... arguments) {
		logAt(Log::isTraceEnabled, Log::trace, message, arguments);
	}

	private void logAt(Predicate<Log> logLevelPredicate, BiConsumer<Log, String> logOperation, String message,
			Object... arguments) {

		Log logger = getLogger();

		if (logLevelPredicate.test(logger)) {
			logOperation.accept(logger, String.format(message, arguments));
		}
	}

	private static boolean isSpringDataManagedAnnotation(MergedAnnotation<?> annotation) {

		return isSpringDataType(annotation.getType())
				|| annotation.getMetaTypes().stream().anyMatch(RepositoryRegistrationAotProcessor::isSpringDataType);
	}

	private static boolean isSpringDataType(Class<?> type) {
		return type.getPackageName().startsWith(TypeContributor.DATA_NAMESPACE);
	}

}
