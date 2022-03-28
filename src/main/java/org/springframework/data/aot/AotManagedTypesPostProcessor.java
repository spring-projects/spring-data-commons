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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aot.generator.CodeContribution;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.data.ManagedTypes;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link AotContributingBeanPostProcessor} handling {@link #getModulePrefix() prefixed} {@link ManagedTypes} instances.
 * This allows to register store specific handling of discovered types.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
public class AotManagedTypesPostProcessor implements AotContributingBeanPostProcessor, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(AotManagedTypesPostProcessor.class);

	private BeanFactory beanFactory;

	@Nullable private String modulePrefix;

	@Nullable
	@Override
	public BeanInstantiationContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType,
			String beanName) {

		if (!ClassUtils.isAssignable(ManagedTypes.class, beanType) || !matchesPrefix(beanName)) {
			return null;
		}

		return contribute(AotContext.context(beanFactory), beanFactory.getBean(beanName, ManagedTypes.class));
	}

	/**
	 * Hook to provide a customized flavor of {@link BeanInstantiationContribution}. By overriding this method calls to
	 * {@link #contributeType(ResolvableType, CodeContribution)} might no longer be issued.
	 *
	 * @param aotContext never {@literal null}.
	 * @param managedTypes never {@literal null}.
	 * @return new instance of {@link AotManagedTypesPostProcessor} or {@literal null} if nothing to do.
	 */
	@Nullable
	protected BeanInstantiationContribution contribute(AotContext aotContext, ManagedTypes managedTypes) {
		return new ManagedTypesContribution(aotContext, managedTypes, this::contributeType);
	}

	/**
	 * Hook to contribute configuration for a given {@literal type}.
	 *
	 * @param type never {@literal null}.
	 * @param contribution never {@literal null}.
	 */
	protected void contributeType(ResolvableType type, CodeContribution contribution) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Contributing type information for %s.", type.getType()));
		}

		TypeContributor.contribute(type.toClass(), Collections.singleton(TypeContributor.DATA_NAMESPACE), contribution);
		TypeUtils.resolveUsedAnnotations(type.toClass()).forEach(annotation -> TypeContributor
				.contribute(annotation.getType(), Collections.singleton(TypeContributor.DATA_NAMESPACE), contribution));
	}

	protected boolean matchesPrefix(String beanName) {
		return StringUtils.startsWithIgnoreCase(beanName, getModulePrefix());
	}

	public String getModulePrefix() {
		return modulePrefix;
	}

	public void setModulePrefix(@Nullable String modulePrefix) {
		this.modulePrefix = modulePrefix;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	static class ManagedTypesContribution implements BeanInstantiationContribution {

		private AotContext aotContext;
		private ManagedTypes managedTypes;
		private BiConsumer<ResolvableType, CodeContribution> contributionAction;

		public ManagedTypesContribution(AotContext aotContext, ManagedTypes managedTypes,
				BiConsumer<ResolvableType, CodeContribution> contributionAction) {

			this.aotContext = aotContext;
			this.managedTypes = managedTypes;
			this.contributionAction = contributionAction;

		}

		@Override
		public void applyTo(CodeContribution contribution) {

			List<Class<?>> types = new ArrayList<>(100);
			managedTypes.forEach(types::add);
			if (types.isEmpty()) {
				return;
			}

			TypeCollector.inspect(types).forEach(type -> {
				contributionAction.accept(type, contribution);
			});
		}

		public AotContext getAotContext() {
			return aotContext;
		}
	}
}
