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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeCollector;
import org.springframework.data.util.TypeContributor;
import org.springframework.data.util.TypeUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BeanRegistrationAotProcessor} handling {@link #getModuleIdentifier() module} {@link ManagedTypes} instances.
 * This AOT processor allows store specific handling of discovered types to be registered.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @since 3.0
 */
public class ManagedTypesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor, EnvironmentAware {

	private static final Lazy<Environment> DEFAULT_ENVIRONMENT = Lazy.of(StandardEnvironment::new);

	private final Log logger = LogFactory.getLog(getClass());

	private @Nullable String moduleIdentifier;
	private @Nullable Environment environment = null;

	public void setModuleIdentifier(@Nullable String moduleIdentifier) {
		this.moduleIdentifier = moduleIdentifier;
	}

	@Nullable
	public String getModuleIdentifier() {
		return this.moduleIdentifier;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {

		if (!isMatch(registeredBean.getBeanClass(), registeredBean.getBeanName())) {
			return null;
		}

		DefaultAotContext aotContext = new DefaultAotContext(registeredBean.getBeanFactory(), getConfiguredEnvironmentOrTryToResolveOne(registeredBean));
		return contribute(aotContext, resolveManagedTypes(registeredBean), registeredBean);
	}

	private ManagedTypes resolveManagedTypes(RegisteredBean registeredBean) {

		RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();

		if (beanDefinition.hasConstructorArgumentValues()) {

			ValueHolder indexedArgumentValue = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValue(0, null);

			if (indexedArgumentValue != null && indexedArgumentValue.getValue() instanceof Collection<?> values
					&& values.stream().allMatch(it -> it instanceof Class)) {
				return ManagedTypes.fromIterable((Collection<Class<?>>) values);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(
					String.format("ManagedTypes BeanDefinition '%s' does serve arguments. Trying to resolve bean instance.",
							registeredBean.getBeanName()));
		}

		if (registeredBean.getParent() == null) {
			try {
				return registeredBean.getBeanFactory().getBean(registeredBean.getBeanName(), ManagedTypes.class);
			} catch (BeanCreationException e) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Could not resolve ManagedTypes '%s'.", registeredBean.getBeanName()));
				}
				if (logger.isDebugEnabled()) {
					logger.debug(e);
				}
			}
		}

		return ManagedTypes.empty();
	}

	/**
	 * Hook to provide a customized flavor of {@link BeanRegistrationAotContribution}. By overriding this method calls to
	 * {@link #registerTypeHints(ResolvableType, AotContext, GenerationContext)} might no longer be issued.
	 *
	 * @param aotContext never {@literal null}.
	 * @param managedTypes never {@literal null}.
	 * @return new instance of {@link BeanRegistrationAotContribution} or {@literal null} if nothing to do.
	 */
	protected BeanRegistrationAotContribution contribute(AotContext aotContext, ManagedTypes managedTypes,
			RegisteredBean registeredBean) {
		return new ManagedTypesRegistrationAotContribution(aotContext, managedTypes, registeredBean,
				typeCollectorCustomizer(), this::registerTypeHints);
	}

	/**
	 * Customization hook to configure {@link TypeCollector}.
	 *
	 * @return a {@link Consumer} to customize the {@link TypeCollector}, must not be {@literal null}.
	 * @since 4.0
	 */
	protected Consumer<TypeCollector> typeCollectorCustomizer() {
		return typeCollector -> {};
	}

	/**
	 * Hook to contribute configuration for a given {@literal type}.
	 *
	 * @param type never {@literal null}.
	 * @param generationContext never {@literal null}.
	 */
	protected void registerTypeHints(ResolvableType type, AotContext aotContext, GenerationContext generationContext) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Contributing type information for [%s]", type.getType()));
		}

		Set<String> annotationNamespaces = Collections.singleton(TypeContributor.DATA_NAMESPACE);

		configureTypeContribution(type.toClass(), aotContext);

		TypeUtils.resolveUsedAnnotations(type.toClass()).forEach(
				annotation -> TypeContributor.contribute(annotation.getType(), annotationNamespaces, generationContext));
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

	protected boolean isMatch(@Nullable Class<?> beanType, @Nullable String beanName) {
		return matchesByType(beanType) && matchesPrefix(beanName);
	}

	protected boolean matchesByType(@Nullable Class<?> beanType) {
		return beanType != null && ClassUtils.isAssignable(ManagedTypes.class, beanType);
	}

	protected boolean matchesPrefix(@Nullable String beanName) {
		return StringUtils.startsWithIgnoreCase(beanName, getModuleIdentifier());
	}

	protected Environment getConfiguredEnvironmentOrTryToResolveOne(RegisteredBean registeredBean) {

		if (this.environment != null) {
			return this.environment;
		}

		if (registeredBean.getBeanFactory() instanceof EnvironmentCapable ec) {
			return ec.getEnvironment();
		}

		String[] beanNamesForType = registeredBean.getBeanFactory().getBeanNamesForType(Environment.class);
		if (!ObjectUtils.isEmpty(beanNamesForType)) {
			return registeredBean.getBeanFactory().getBean(beanNamesForType[0], Environment.class);
		}

		return DEFAULT_ENVIRONMENT.get();
	}

}
