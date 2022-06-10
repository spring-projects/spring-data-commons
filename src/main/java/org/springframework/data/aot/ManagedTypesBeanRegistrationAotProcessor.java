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

import java.util.Collections;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BeanRegistrationAotProcessor} handling {@link #getModuleIdentifier() module} {@link ManagedTypes} instances.
 * This AOT processor allows store specific handling of discovered types to be registered.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.aot.BeanRegistrationAotProcessor
 * @since 3.0
 */
public class ManagedTypesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	private final Log logger = LogFactory.getLog(getClass());
	@Nullable private String moduleIdentifier;

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(@NonNull RegisteredBean registeredBean) {

		if (!isMatch(registeredBean.getBeanClass(), registeredBean.getBeanName())) {
			return null;
		}

		BeanFactory beanFactory = registeredBean.getBeanFactory();
		return contribute(AotContext.from(registeredBean.getBeanFactory()),
				beanFactory.getBean(registeredBean.getBeanName(), ManagedTypes.class));
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

	/**
	 * Hook to provide a customized flavor of {@link BeanRegistrationAotContribution}. By overriding this method calls to
	 * {@link #contributeType(ResolvableType, GenerationContext)} might no longer be issued.
	 *
	 * @param aotContext never {@literal null}.
	 * @param managedTypes never {@literal null}.
	 * @return new instance of {@link ManagedTypesBeanRegistrationAotProcessor} or {@literal null} if nothing to do.
	 */
	@Nullable
	protected BeanRegistrationAotContribution contribute(AotContext aotContext, ManagedTypes managedTypes) {
		return new ManagedTypesRegistrationAotContribution(aotContext, managedTypes, this::contributeType);
	}

	/**
	 * Hook to contribute configuration for a given {@literal type}.
	 *
	 * @param type never {@literal null}.
	 * @param generationContext never {@literal null}.
	 */
	protected void contributeType(ResolvableType type, GenerationContext generationContext) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Contributing type information for [%s]", type.getType()));
		}

		Set<String> annotationNamespaces = Collections.singleton(TypeContributor.DATA_NAMESPACE);

		TypeContributor.contribute(type.toClass(), annotationNamespaces, generationContext);

		TypeUtils.resolveUsedAnnotations(type.toClass()).forEach(
				annotation -> TypeContributor.contribute(annotation.getType(), annotationNamespaces, generationContext));
	}

	public void setModuleIdentifier(@Nullable String moduleIdentifier) {
		this.moduleIdentifier = moduleIdentifier;
	}

	@Nullable
	public String getModuleIdentifier() {
		return this.moduleIdentifier;
	}
}
