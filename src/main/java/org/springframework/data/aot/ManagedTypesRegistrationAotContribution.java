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

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.AccessVisibility;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.data.util.Lazy;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanRegistrationAotContribution} used to contribute a {@link ManagedTypes} registration.
 * <p>
 * Will try to resolve bean definition arguments if possible and fall back to resolving the bean from the context if
 * that is not possible. To avoid duplicate invocations of potential scan operations hidden by the {@link ManagedTypes}
 * instance the {@link BeanRegistrationAotContribution} will write custom instantiation code via
 * {@link BeanRegistrationAotContribution#customizeBeanRegistrationCodeFragments(GenerationContext, BeanRegistrationCodeFragments)}.
 * The generated code resolves potential factory methods accepting either a {@link ManagedTypes} instance, or a
 * {@link List} of either {@link Class} or {@link String} (classname) values.
 * 
 * <pre>
 * <code>
 * public static InstanceSupplier&lt;ManagedTypes&gt; instance() {
 *   return (registeredBean) -> {
 *     var types = List.of("com.example.A", "com.example.B");
 *     return ManagedTypes.ofStream(types.stream().map(it -> ClassUtils.resolveClassName(it, registeredBean.getBeanFactory().getBeanClassLoader())));
 *   }
 * }
 * </code>
 * </pre>
 *
 * @author John Blum
 * @author Christoph Strobl
 * @see org.springframework.beans.factory.aot.BeanRegistrationAotContribution
 * @since 3.0.0
 */
public class ManagedTypesRegistrationAotContribution implements RegisteredBeanAotContribution {

	private final AotContext aotContext;
	private final ManagedTypes managedTypes;
	private final BiConsumer<ResolvableType, GenerationContext> contributionAction;
	private final RegisteredBean source;

	public ManagedTypesRegistrationAotContribution(AotContext aotContext, @Nullable ManagedTypes managedTypes,
			RegisteredBean registeredBean, BiConsumer<ResolvableType, GenerationContext> contributionAction) {

		this.aotContext = aotContext;
		this.managedTypes = managedTypes;
		this.contributionAction = contributionAction;
		this.source = registeredBean;
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
	public BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(GenerationContext generationContext,
			BeanRegistrationCodeFragments codeFragments) {

		if (managedTypes == null) {
			return codeFragments;
		}

		ManagedTypesInstanceCodeFragment fragment = new ManagedTypesInstanceCodeFragment(getManagedTypes(), source,
				codeFragments);
		return fragment.canGenerateCode() ? fragment : codeFragments;
	}

	@Override
	public RegisteredBean getSource() {
		return source;
	}

	static class ManagedTypesInstanceCodeFragment extends BeanRegistrationCodeFragments {

		private ManagedTypes sourceTypes;
		private RegisteredBean source;
		private Lazy<Method> instanceMethod = Lazy.of(this::findInstanceFactory);

		protected ManagedTypesInstanceCodeFragment(ManagedTypes managedTypes, RegisteredBean source,
				BeanRegistrationCodeFragments codeFragments) {

			super(codeFragments);

			this.sourceTypes = managedTypes;
			this.source = source;
		}

		/**
		 * @return {@literal true} if the instance method code can be generated. {@literal false} otherwise.
		 */
		boolean canGenerateCode() {

			if (ObjectUtils.nullSafeEquals(source.getBeanClass(), ManagedTypes.class)) {
				return true;
			}
			return instanceMethod.getNullable() != null;
		}

		@Override
		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
				BeanRegistrationCode beanRegistrationCode, Executable constructorOrFactoryMethod,
				boolean allowDirectSupplierShortcut) {

			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("Instance",
					this::generateInstanceFactory);

			return CodeBlock.of("$T.$L()", beanRegistrationCode.getClassName(), generatedMethod.getName());
		}

		private CodeBlock toCodeBlock(List<Class<?>> values, boolean allPublic) {

			if (allPublic) {
				return CodeBlock.join(values.stream().map(value -> CodeBlock.of("$T.class", value)).toList(), ", ");
			}
			return CodeBlock.join(values.stream().map(value -> CodeBlock.of("$S", value.getName())).toList(), ", ");
		}

		private Method findInstanceFactory() {

			for (Method beanMethod : ReflectionUtils.getDeclaredMethods(source.getBeanClass())) {

				if (beanMethod.getParameterCount() == 1 && java.lang.reflect.Modifier.isPublic(beanMethod.getModifiers())
						&& java.lang.reflect.Modifier.isStatic(beanMethod.getModifiers())) {
					ResolvableType parameterType = ResolvableType.forMethodParameter(beanMethod, 0, source.getBeanClass());
					if (parameterType.isAssignableFrom(ResolvableType.forType(List.class))
							|| parameterType.isAssignableFrom(ResolvableType.forType(ManagedTypes.class))) {
						return beanMethod;
					}
				}
			}
			return null;
		}

		void generateInstanceFactory(Builder method) {

			List<Class<?>> sourceTypes = this.sourceTypes.toList();
			boolean allSourceTypesVisible = sourceTypes.stream()
					.allMatch(it -> AccessVisibility.PUBLIC.equals(AccessVisibility.forClass(it)));

			ParameterizedTypeName targetTypeName = ParameterizedTypeName.get(InstanceSupplier.class, source.getBeanClass());

			method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
			method.returns(targetTypeName);

			CodeBlock.Builder builder = CodeBlock.builder().add("return ").beginControlFlow("(registeredBean -> ");

			builder.addStatement("var types = $T.of($L)", List.class, toCodeBlock(sourceTypes, allSourceTypesVisible));

			if (allSourceTypesVisible) {
				builder.addStatement("var managedTypes = $T.fromIterable($L)", ManagedTypes.class, "types");
			} else {
				builder.add(CodeBlock.builder()
						.beginControlFlow("var managedTypes = $T.fromStream(types.stream().map(it ->", ManagedTypes.class)
						.addStatement("return $T.resolveClassName(it, registeredBean.getBeanFactory().getBeanClassLoader())",
								ClassUtils.class)
						.endControlFlow("))").build());
			}
			if (ObjectUtils.nullSafeEquals(source.getBeanClass(), ManagedTypes.class)) {
				builder.add("return managedTypes");
			} else {
				Method instanceFactoryMethod = instanceMethod.get();
				if (ResolvableType.forMethodParameter(instanceFactoryMethod, 0)
						.isAssignableFrom(ResolvableType.forType(ManagedTypes.class))) {
					builder.addStatement("return $T.$L($L)", instanceFactoryMethod.getDeclaringClass(),
							instanceFactoryMethod.getName(), "managedTypes");

				} else {
					builder.addStatement("return $T.$L($L.toList())", instanceFactoryMethod.getDeclaringClass(),
							instanceFactoryMethod.getName(), "managedTypes");
				}
			}
			builder.endControlFlow(")");
			method.addCode(builder.build());
		}
	}
}
