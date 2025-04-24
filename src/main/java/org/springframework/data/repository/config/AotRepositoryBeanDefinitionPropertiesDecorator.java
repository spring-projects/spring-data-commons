/*
 * Copyright 2025 the original author or authors.
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

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.TypeName;
import org.springframework.util.StringUtils;

/**
 * Delegate to decorate AOT {@code BeanDefinition} properties.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 4.0
 */
class AotRepositoryBeanDefinitionPropertiesDecorator {

	private final Supplier<CodeBlock> inheritedProperties;
	private final RepositoryContributor repositoryContributor;

	public AotRepositoryBeanDefinitionPropertiesDecorator(Supplier<CodeBlock> inheritedProperties,
			RepositoryContributor repositoryContributor) {
		this.inheritedProperties = inheritedProperties;
		this.repositoryContributor = repositoryContributor;
	}

	/**
	 * Generate a decorated code block for bean properties.
	 *
	 * @return the decorated code block.
	 */
	public CodeBlock decorate() {

		CodeBlock.Builder builder = CodeBlock.builder();
		// bring in properties as usual
		builder.add(inheritedProperties.get());

		builder.add("beanDefinition.getPropertyValues().addPropertyValue(\"repositoryFragmentsFunction\", new $T() {\n",
				RepositoryFactoryBeanSupport.RepositoryFragmentsFunction.class);
		builder.indent();
		builder.add("public $T getRepositoryFragments($T beanFactory, $T context) {\n",
				RepositoryComposition.RepositoryFragments.class, BeanFactory.class,
				RepositoryFactoryBeanSupport.FragmentCreationContext.class);
		builder.indent();

		for (Map.Entry<String, TypeName> entry : repositoryContributor.requiredArgs().entrySet()) {

			if (entry.getValue().equals(TypeName.get(RepositoryFactoryBeanSupport.FragmentCreationContext.class))) {

				if (!entry.getKey().equals("context")) {
					builder.addStatement("$T $L = context", entry.getValue(), entry.getKey(), entry.getValue());
				}

			} else {
				builder.addStatement("$T $L = beanFactory.getBean($T.class)", entry.getValue(), entry.getKey(),
						entry.getValue());
			}
		}

		builder.addStatement("return RepositoryComposition.RepositoryFragments.just(new $L($L))",
				repositoryContributor.getContributedTypeName(),
				StringUtils.collectionToDelimitedString(repositoryContributor.requiredArgs().keySet(), ", "));
		builder.unindent();
		builder.add("}\n");
		builder.unindent();
		builder.add("});\n");

		return builder.build();
	}

}
