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

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.aot.generate.AotRepositoryBeanDefinitionPropertiesDecorator;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationAotContribution} used to contribute repository registrations.
 *
 * @author John Blum
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.0
 */
public class RepositoryRegistrationAotContribution implements BeanRegistrationAotContribution {

	private final AotRepositoryContext context;
	private final BeanRegistrationAotContribution aotContribution;
	private final @Nullable RepositoryContributor repositoryContribution;

	RepositoryRegistrationAotContribution(AotRepositoryContext context, BeanRegistrationAotContribution aotContribution,
			@Nullable RepositoryContributor repositoryContribution) {

		this.context = context;
		this.aotContribution = aotContribution;
		this.repositoryContribution = repositoryContribution;
	}

	public RepositoryInformation getRepositoryInformation() {
		return context.getRepositoryInformation();
	}

	@Override
	public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {

		aotContribution.applyTo(generationContext, beanRegistrationCode);

		if (this.repositoryContribution != null) {
			this.repositoryContribution.contribute(generationContext);
		}
	}

	@Override
	public BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(GenerationContext generationContext,
			BeanRegistrationCodeFragments codeFragments) {

		return new BeanRegistrationCodeFragmentsDecorator(codeFragments) {

			@Override
			public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
					Predicate<String> attributeFilter) {

				Supplier<CodeBlock> inheritedProperties = () -> super.generateSetBeanDefinitionPropertiesCode(generationContext,
						beanRegistrationCode, beanDefinition, attributeFilter);

				if (repositoryContribution == null) { // no aot implementation -> go on as
					return inheritedProperties.get();
				}

				AotRepositoryBeanDefinitionPropertiesDecorator decorator = new AotRepositoryBeanDefinitionPropertiesDecorator(
						inheritedProperties, repositoryContribution);

				return decorator.decorate();
			}
		};
	}

}
