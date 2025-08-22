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
package org.springframework.data.repository.aot.generate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Delegate to decorate AOT {@code BeanDefinition} properties during AOT processing. Adds a {@link CodeBlock} for the
 * fragment function that resolves {@link RepositoryContributor#requiredArgs()} from the {@link BeanFactory} and
 * provides them to the generated repository fragment.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 4.0
 */
public class AotRepositoryBeanDefinitionPropertiesDecorator {

	private static final Map<ResolvableType, String> RESERVED_TYPES;

	private final Supplier<CodeBlock> inheritedProperties;
	private final RepositoryContributor repositoryContributor;

	static {

		RESERVED_TYPES = new LinkedHashMap<>(2);
		RESERVED_TYPES.put(ResolvableType.forClass(BeanFactory.class), "beanFactory");
		RESERVED_TYPES.put(ResolvableType.forClass(RepositoryFactoryBeanSupport.FragmentCreationContext.class), "context");
	}

	/**
	 * @param inheritedProperties bean definition code (containing properties and such) already added via another
	 *          component.
	 * @param repositoryContributor the contributor providing the actual AOT repository implementation.
	 * @throws IllegalArgumentException if {@link RepositoryContributor#getContributedTypeName()} is not set.
	 */
	public AotRepositoryBeanDefinitionPropertiesDecorator(Supplier<CodeBlock> inheritedProperties,
			RepositoryContributor repositoryContributor) {

		Assert.notNull(repositoryContributor.getContributedTypeName(), "Contributed type name must not be null");

		this.inheritedProperties = inheritedProperties;
		this.repositoryContributor = repositoryContributor;
	}

	/**
	 * Generate a decorated code block for bean properties.
	 * <p>
	 * <strong>NOTE:</strong> the {@link RepositoryContributor} must be able to provide the to be
	 * {@link RepositoryContributor#getContributedTypeName() type name} of the generated repository implementation and
	 * needs to have potential constructor arguments resolved.
	 *
	 * @return the decorated code block.
	 */
	public CodeBlock decorate() {

		CodeBlock.Builder builder = CodeBlock.builder();

		// bring in properties as usual
		builder.add(inheritedProperties.get());

		MethodSpec.Builder callbackMethod = MethodSpec.methodBuilder("getRepositoryFragments").addModifiers(Modifier.PUBLIC)
				.returns(RepositoryComposition.RepositoryFragments.class);

		for (Entry<ResolvableType, String> entry : RESERVED_TYPES.entrySet()) {
			callbackMethod.addParameter(entry.getKey().toClass(), entry.getValue());
		}

		callbackMethod.addCode(buildCallbackBody());

		TypeSpec repositoryFragmentsFunction = TypeSpec.anonymousClassBuilder("")
				.superclass(RepositoryFactoryBeanSupport.RepositoryFragmentsFunction.class).addMethod(callbackMethod.build())
				.build();

		builder.addStatement("beanDefinition.getPropertyValues().addPropertyValue($S, $L)", "repositoryFragmentsFunction",
				repositoryFragmentsFunction);

		return builder.build();
	}

	private CodeBlock buildCallbackBody() {

		CodeBlock.Builder callback = CodeBlock.builder();

		for (Entry<String, ResolvableType> entry : repositoryContributor.requiredArgs().entrySet()) {

			TypeName argumentType = TypeName.get(entry.getValue().getType());
			String reservedArgumentName = RESERVED_TYPES.get(entry.getValue());
			if (reservedArgumentName == null) {
				callback.addStatement("$1T $2L = beanFactory.getBean($1T.class)", argumentType, entry.getKey());
			} else {

				if (reservedArgumentName.equals(entry.getKey())) {
					continue;
				}

				callback.addStatement("$T $L = $L", argumentType, entry.getKey(), reservedArgumentName);
			}
		}

		callback.addStatement("return $T.just(new $L($L))", RepositoryComposition.RepositoryFragments.class,
				repositoryContributor.getContributedTypeName().getCanonicalName(),
				StringUtils.collectionToDelimitedString(repositoryContributor.requiredArgs().keySet(), ", "));

		return callback.build();
	}

}
