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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.aot.generate.AotRepositoryFragmentMetadata.ConstructorArgument;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Delegate to decorate AOT {@code BeanDefinition} properties during AOT processing. Adds a {@link CodeBlock} for the
 * fragment function that resolves {@link RepositoryContributor#getAotFragmentMetadata()} from the {@link BeanFactory}
 * and provides them to the generated repository fragment.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 4.0
 */
public class AotRepositoryBeanDefinitionPropertiesDecorator {

	static final Map<ResolvableType, String> RESERVED_TYPES;

	private final Supplier<CodeBlock> inheritedProperties;
	private final RepositoryContributor repositoryContributor;

	static {

		RESERVED_TYPES = new LinkedHashMap<>(3);
		RESERVED_TYPES.put(ResolvableType.forClass(BeanDefinition.class), "beanDefinition");
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

		ReflectionUtils.doWithMethods(RepositoryFactoryBeanSupport.RepositoryFragmentsFunction.class, it -> {

			for (int i = 0; i < it.getParameterCount(); i++) {

				MethodParameter parameter = new MethodParameter(it, i);
				parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());

				callbackMethod.addParameter(parameter.getParameterType(), parameter.getParameterName());
			}

		}, method -> method.getName().equals("getRepositoryFragments"));

		callbackMethod.addCode(buildCallbackBody());

		TypeSpec repositoryFragmentsFunction = TypeSpec.anonymousClassBuilder("")
				.superclass(RepositoryFactoryBeanSupport.RepositoryFragmentsFunction.class).addMethod(callbackMethod.build())
				.build();

		builder.addStatement("beanDefinition.getPropertyValues().addPropertyValue($S, $L)", "repositoryFragmentsFunction",
				repositoryFragmentsFunction);

		return builder.build();
	}

	private CodeBlock buildCallbackBody() {

		Assert.state(repositoryContributor.getContributedTypeName() != null, "ContributedTypeName must not be null");

		CodeBlock.Builder callback = CodeBlock.builder();
		List<Object> arguments = new ArrayList<>();

		for (Entry<String, ConstructorArgument> entry : repositoryContributor.getAotFragmentMetadata()
				.getConstructorArguments().entrySet()) {

			ConstructorArgument argument = entry.getValue();
			AotRepositoryConstructorBuilder.ParameterOrigin parameterOrigin = argument.parameterOrigin();

			String ref = parameterOrigin.getReference();
			CodeBlock codeBlock = parameterOrigin.getCodeBlock();

			if (StringUtils.hasText(ref)) {
				arguments.add(ref);
				if (!codeBlock.isEmpty()) {
					callback.add(codeBlock);
				}
			} else {
				arguments.add(codeBlock);
			}
		}

		List<Object> args = new ArrayList<>();
		args.add(RepositoryComposition.RepositoryFragments.class);
		args.add(repositoryContributor.getContributedTypeName().getName());
		args.addAll(arguments);

		callback.addStatement("return $T.just(new $L(%s%s))".formatted("$L".repeat(arguments.isEmpty() ? 0 : 1),
				", $L".repeat(Math.max(0, arguments.size() - 1))), args.toArray());

		return callback.build();
	}

}
