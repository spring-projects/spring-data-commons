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
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link BeanRegistrationAotProcessor} responsible processing and providing AOT configuration for repositories.
 * <p>
 * Processes {@link RepositoryFactoryBeanSupport repository factory beans} to provide generic type information to
 * the AOT tooling to allow deriving target type from the {@link RootBeanDefinition bean definition}. If generic types
 * do not match due to customization of the factory bean by the user, at least the target repository type is provided
 * via the {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE}.
 * </p>
 * <p>
 * With {@link RepositoryRegistrationAotContribution#contribute(AotRepositoryContext, GenerationContext)}, stores
 * can provide custom logic for contributing additional (eg. reflection) configuration. By default, reflection
 * configuration will be added for types reachable from the repository declaration and query methods as well as
 * all used {@link Annotation annotations} from the {@literal org.springframework.data} namespace.
 * </p>
 * The processor is typically configured via {@link RepositoryConfigurationExtension#getRepositoryAotProcessor()}
 * and gets added by the {@link org.springframework.data.repository.config.RepositoryConfigurationDelegate}.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.aot.BeanRegistrationAotProcessor
 * @since 3.0.0
 */
@SuppressWarnings("unused")
public class RepositoryRegistrationAotProcessor implements BeanRegistrationAotProcessor, BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;

	private final Log logger = LogFactory.getLog(getClass());

	private Map<String, RepositoryMetadata<?>> configMap;

	@Nullable
	@Override
	public BeanRegistrationAotContribution processAheadOfTime(@NonNull RegisteredBean bean) {

		return isRepositoryBean(bean)
				? newRepositoryRegistrationAotContribution(bean)
				: null;
	}

	private boolean isRepositoryBean(RegisteredBean bean) {
		return bean != null && getConfigMap().containsKey(bean.getBeanName());
	}

	protected RepositoryRegistrationAotContribution newRepositoryRegistrationAotContribution(
			@NonNull RegisteredBean repositoryBean) {

		RepositoryRegistrationAotContribution contribution =
				RepositoryRegistrationAotContribution.fromProcessor(this).forBean(repositoryBean);

		return contribution.withModuleContribution(contribution::contribute);
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {

		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				() -> "AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);

		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@NonNull
	protected ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	public void setConfigMap(@Nullable Map<String, RepositoryMetadata<?>> configMap) {
		this.configMap = configMap;
	}

	@NonNull
	public Map<String, RepositoryMetadata<?>> getConfigMap() {
		return nullSafeMap(this.configMap);
	}

	@NonNull
	private <K, V> Map<K, V> nullSafeMap(@Nullable Map<K, V> map) {
		return map != null ? map : Collections.emptyMap();
	}

	@Nullable
	protected RepositoryMetadata<?> getRepositoryMetadata(@NonNull RegisteredBean bean) {
		return getConfigMap().get(nullSafeBeanName(bean));
	}

	private String nullSafeBeanName(@Nullable RegisteredBean bean) {

		String beanName = bean != null ? bean.getBeanName() : null;

		return StringUtils.hasText(beanName) ? beanName : "";
	}

	@NonNull
	protected Log getLogger() {
		return this.logger;
	}

	private void logAt(Predicate<Log> logLevelPredicate, BiConsumer<Log, String> logOperation,
			String message, Object... arguments) {

		Log logger = getLogger();

		if (logLevelPredicate.test(logger)) {
			logOperation.accept(logger, String.format(message, arguments));
		}
	}

	protected void logDebug(String message, Object... arguments) {
		logAt(Log::isDebugEnabled, Log::debug, message, arguments);
	}

	protected void logTrace(String message, Object... arguments) {
		logAt(Log::isTraceEnabled, Log::trace, message, arguments);
	}
}
