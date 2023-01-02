/*
 * Copyright 2022-2023 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeCollector;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.WildcardTypeName;
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
 * <pre class="code">
 * public static InstanceSupplier&lt;ManagedTypes&gt; instance() {
 *   return (registeredBean) -> {
 *     var types = List.of("com.example.A", "com.example.B");
 *     return ManagedTypes.ofStream(types.stream().map(it -> ClassUtils.forName(it, registeredBean.getBeanFactory().getBeanClassLoader())));
 *   }
 * }
 * </pre>
 *
 * @author John Blum
 * @author Christoph Strobl
 * @author Mark Paluch
 * @see org.springframework.beans.factory.aot.BeanRegistrationAotContribution
 * @since 3.0
 */
class ManagedTypesRegistrationAotContribution implements RegisteredBeanAotContribution {

	private final ManagedTypes managedTypes;
	private final Lazy<List<Class<?>>> sourceTypes;
	private final BiConsumer<ResolvableType, GenerationContext> contributionAction;
	private final RegisteredBean source;

	public ManagedTypesRegistrationAotContribution(ManagedTypes managedTypes, RegisteredBean registeredBean,
			BiConsumer<ResolvableType, GenerationContext> contributionAction) {

		this.managedTypes = managedTypes;
		this.sourceTypes = Lazy.of(managedTypes::toList);
		this.contributionAction = contributionAction;
		this.source = registeredBean;
	}

	@Override
	public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {

		List<Class<?>> types = sourceTypes.get();

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

		ManagedTypesInstanceCodeFragment fragment = new ManagedTypesInstanceCodeFragment(sourceTypes.get(), source,
				codeFragments);
		return fragment.canGenerateCode() ? fragment : codeFragments;
	}

	@Override
	public RegisteredBean getSource() {
		return source;
	}

	/**
	 * Class used to generate the fragment of code needed to define a {@link ManagedTypes} bean from previously discovered
	 * managed types.
	 */
	static class ManagedTypesInstanceCodeFragment extends BeanRegistrationCodeFragmentsDecorator {

		public static final ResolvableType LIST_TYPE = ResolvableType.forType(List.class);
		public static final ResolvableType MANAGED_TYPES_TYPE = ResolvableType.forType(ManagedTypes.class);
		private final List<Class<?>> sourceTypes;
		private final RegisteredBean source;
		private final Lazy<Method> instanceMethod = Lazy.of(this::findInstanceFactory);

		private static final TypeName WILDCARD = WildcardTypeName.subtypeOf(Object.class);
		private static final TypeName CLASS_OF_ANY = ParameterizedTypeName.get(ClassName.get(Class.class), WILDCARD);
		private static final TypeName LIST_OF_ANY = ParameterizedTypeName.get(ClassName.get(List.class), CLASS_OF_ANY);

		protected ManagedTypesInstanceCodeFragment(List<Class<?>> sourceTypes, RegisteredBean source,
				BeanRegistrationCodeFragments codeFragments) {

			super(codeFragments);

			this.sourceTypes = sourceTypes;
			this.source = source;
		}

		@Override
		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
				BeanRegistrationCode beanRegistrationCode, Executable constructorOrFactoryMethod,
				boolean allowDirectSupplierShortcut) {

			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("Instance",
					this::generateInstanceFactory);

			return CodeBlock.of("$T.$L()", beanRegistrationCode.getClassName(), generatedMethod.getName());
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

		void generateInstanceFactory(Builder method) {

			boolean allSourceTypesVisible = sourceTypes.stream().allMatch(it -> AccessControl.forClass(it).isPublic());

			ParameterizedTypeName targetTypeName = ParameterizedTypeName.get(InstanceSupplier.class, source.getBeanClass());

			method.addJavadoc("Get the bean instance for '$L'.", source.getBeanName());
			method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
			method.returns(targetTypeName);

			CodeBlock.Builder builder = CodeBlock.builder().add("return ").beginControlFlow("(registeredBean -> ");

			if (sourceTypes.isEmpty()) {
				builder.addStatement("$T types = $T.emptyList()", LIST_OF_ANY, Collections.class);
			} else {

				TypeName variableTypeName;
				if (allSourceTypesVisible) {
					variableTypeName = LIST_OF_ANY;
				} else {
					variableTypeName = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
				}
				builder.addStatement("$T types = $T.of($L)", variableTypeName, List.class,
						toCodeBlock(sourceTypes, allSourceTypesVisible));
			}

			if (allSourceTypesVisible) {
				builder.addStatement("$T managedTypes = $T.fromIterable($L)", ManagedTypes.class, ManagedTypes.class, "types");
			} else {
				builder.add(CodeBlock.builder()
						.beginControlFlow("$T managedTypes = $T.fromStream(types.stream().map(it ->", ManagedTypes.class,
								ManagedTypes.class)
						.beginControlFlow("try")
						.addStatement("return $T.forName(it, registeredBean.getBeanFactory().getBeanClassLoader())",
								ClassUtils.class)
						.nextControlFlow("catch ($T e)", ClassNotFoundException.class)
						.addStatement("throw new $T($S, e)", IllegalArgumentException.class, "Cannot to load type").endControlFlow()
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

		private CodeBlock toCodeBlock(List<Class<?>> values, boolean allPublic) {

			if (allPublic) {
				return CodeBlock.join(values.stream().map(value -> CodeBlock.of("$T.class", value)).toList(), ", ");
			}
			return CodeBlock.join(values.stream().map(value -> CodeBlock.of("$S", value.getName())).toList(), ", ");
		}

		@Nullable
		private Method findInstanceFactory() {

			for (Method beanMethod : ReflectionUtils.getDeclaredMethods(source.getBeanClass())) {

				if (!isInstanceFactory(beanMethod)) {
					continue;
				}

				ResolvableType parameterType = ResolvableType.forMethodParameter(beanMethod, 0, source.getBeanClass());

				if (parameterType.isAssignableFrom(LIST_TYPE) || parameterType.isAssignableFrom(MANAGED_TYPES_TYPE)) {
					return beanMethod;
				}
			}

			return null;
		}

		private static boolean isInstanceFactory(Method beanMethod) {
			return beanMethod.getParameterCount() == 1 //
					&& java.lang.reflect.Modifier.isPublic(beanMethod.getModifiers()) //
					&& java.lang.reflect.Modifier.isStatic(beanMethod.getModifiers());
		}
	}
}
