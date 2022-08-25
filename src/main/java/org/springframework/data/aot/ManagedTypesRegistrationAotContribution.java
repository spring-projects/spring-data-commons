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

import javax.lang.model.element.Modifier;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.springframework.aot.generate.AccessVisibility;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.cglib.core.VisibilityPredicate;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanRegistrationAotContribution} used to contribute a {@link ManagedTypes} registration.
 *
 * @author John Blum
 * @see org.springframework.beans.factory.aot.BeanRegistrationAotContribution
 * @since 3.0.0
 */
public class ManagedTypesRegistrationAotContribution implements BeanRegistrationAotContribution {

	private final AotContext aotContext;
	private final ManagedTypes managedTypes;
	private final BiConsumer<ResolvableType, GenerationContext> contributionAction;
	private final Class<?> beanType;

	public ManagedTypesRegistrationAotContribution(AotContext aotContext, @Nullable ManagedTypes managedTypes, Class<?> sourceType,
			BiConsumer<ResolvableType, GenerationContext> contributionAction) {

		this.aotContext = aotContext;
		this.managedTypes = managedTypes;
		this.contributionAction = contributionAction;
		this.beanType = sourceType;
	}

	protected AotContext getAotContext() {
		return this.aotContext;
	}

	protected ManagedTypes getManagedTypes() {
		return managedTypes == null ? ManagedTypes.empty() : managedTypes;
	}

	@Override
	public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {

		List<Class<?>> types = getManagedTypes().toList();

		if (!types.isEmpty()) {
			TypeCollector.inspect(types).forEach(type -> contributionAction.accept(type, generationContext));
		}

	}

	@Override
	public BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(GenerationContext generationContext, BeanRegistrationCodeFragments codeFragments) {

		if(managedTypes == null) {
			return codeFragments;
		}

		return new ManagedTypesInstance(getManagedTypes(), beanType, codeFragments);
	}

	static class ManagedTypesInstance extends BeanRegistrationCodeFragments {

		private ManagedTypes types;
		private Class<?> sourceBeanType;

		protected ManagedTypesInstance(ManagedTypes managedTypes, Class<?> beanType, BeanRegistrationCodeFragments codeFragments) {
			super(codeFragments);
			this.types = managedTypes;
			this.sourceBeanType = beanType;
		}

		@Override
		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode, Executable constructorOrFactoryMethod, boolean allowDirectSupplierShortcut) {

			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("Instance", method -> {

				List<Class<?>> sourceTypes = types.toList();
				boolean allPublic = sourceTypes.stream().allMatch(it -> AccessVisibility.PUBLIC.equals(AccessVisibility.forClass(it)));

				Class<?> beanType = ManagedTypes.class;
				ParameterizedTypeName listType = ParameterizedTypeName.get(List.class, allPublic ? Class.class : String.class);

				method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
				method.returns(beanType);
				method.addStatement("var types = $T.of($L)",// listType,
						List.class, toCodeBlock(sourceTypes, allPublic));

				if(!ObjectUtils.nullSafeEquals(sourceBeanType, beanType)) {
					String factoryMethodName = "from";
					for(Method beanMethod : ReflectionUtils.getDeclaredMethods(sourceBeanType)) {
						if(beanMethod.getParameterCount() == 1 && ObjectUtils.nullSafeEquals(beanMethod.getParameterTypes()[0], ManagedTypes.class)){
							factoryMethodName = beanMethod.getName();
						}
					}
					if (allPublic) {
						method.addStatement("return $T."+factoryMethodName+"($T.fromIterable($L))", sourceBeanType, beanType, "types");
					} else {
						method.addStatement("return $T."+factoryMethodName+"($T.fromClassNames($L))", sourceBeanType, beanType, "types");
					}
				} else {
					if (allPublic) {
						method.addStatement("return $T.fromIterable($L)", beanType, "types");
					} else {
						method.addStatement("return $T.fromClassNames($L))", beanType, "types");
					}
				}
			});

			return CodeBlock.of("() -> $T.$L()", beanRegistrationCode.getClassName(), generatedMethod.getName());
		}

		private CodeBlock toCodeBlock(List<Class<?>> values, boolean allPublic) {
			if(allPublic) {
				return CodeBlock.join(values.stream().map(value -> CodeBlock.of("$T.class", value)).toList(), ", ");
			}
			return CodeBlock.join(values.stream().map(value -> CodeBlock.of("$S", value.getName())).toList(), ", ");
		}
	}
}
