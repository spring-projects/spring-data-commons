/*
 * Copyright 2024 the original author or authors.
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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;

/**
 * Builder style creator for AOT repository fragments.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class AotRepositoryCreator {

	private static final Log logger = LogFactory.getLog(AotRepositoryCreator.class);

	private final RepositoryInformation repositoryInformation;
	private final String moduleName;
	private final ProjectionFactory projectionFactory;
	private final AotRepositoryFragmentMetadata generationMetadata;

	private @Nullable RepositoryConstructorBuilder constructorBuilder;
	private Consumer<AotRepositoryClassBuilder> classCustomizer;

	private AotRepositoryCreator(RepositoryInformation repositoryInformation, String moduleName,
			ProjectionFactory projectionFactory) {

		this.repositoryInformation = repositoryInformation;
		this.moduleName = moduleName;
		this.projectionFactory = projectionFactory;

		this.generationMetadata = new AotRepositoryFragmentMetadata();
		this.classCustomizer = (builder) -> {};
	}

	/**
	 * Create a new {@code AotRepositoryBuilder} for the given {@link RepositoryInformation}.
	 *
	 * @param information must not be {@literal null}.
	 * @param moduleName must not be {@literal null}.
	 * @param projectionFactory must not be {@literal null}.
	 * @return
	 */
	static AotRepositoryCreator forRepository(RepositoryInformation information, String moduleName,
			ProjectionFactory projectionFactory) {
		return new AotRepositoryCreator(information, moduleName, projectionFactory);
	}

	/**
	 * Get the {@link ClassName} for the AOT repository fragment.
	 *
	 * @return the {@link ClassName} for the AOT repository fragment.
	 */
	ClassName getClassName() {
		return ClassName.get(packageName(),
				"%sImpl".formatted(repositoryInformation.getRepositoryInterface().getSimpleName()));
	}

	String packageName() {
		return repositoryInformation.getRepositoryInterface().getPackageName();
	}

	AotRepositoryFragmentMetadata getRepositoryMetadata() {
		return generationMetadata;
	}

	RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	ProjectionFactory getProjectionFactory() {
		return projectionFactory;
	}

	/**
	 * Create the AOT repository fragment and add constructors, methods and fields to the given {@link TypeSpec.Builder}.
	 *
	 * @param target the target {@link TypeSpec.Builder} to which the AOT repository fragment will be added.
	 * @return an
	 */
	AotBundle create(TypeSpec.Builder target) {

		List<AotRepositoryMethod> methodMetadata = new ArrayList<>();

		target.addModifiers(Modifier.PUBLIC) //
				.addJavadoc("AOT generated $L repository implementation for {@link $T}.\n", moduleName,
						repositoryInformation.getRepositoryInterface());

		// create the constructor
		target.addMethod(buildConstructor());

		generationMetadata.getMethods().values().forEach(localMethod -> {

			MethodContributor<? extends QueryMethod> methodContributor = localMethod.methodContributor();
			AotQueryMethodGenerationContext context = new AotQueryMethodGenerationContext(repositoryInformation,
					localMethod.source(), methodContributor.getQueryMethod(), generationMetadata);

			MethodSpec methodSpec = methodContributor.contribute(context);
			if (methodSpec != null) {
				target.addMethod(methodSpec);
			}

			// TODO: decouple json from method building and get rid of methodMetadata here?
			methodMetadata.add(new AotRepositoryMethod(localMethod.source().getName(), localMethod.source().toGenericString(),
					methodContributor.getMetadata(), null));
		});

		generationMetadata.getDelegateMethods().values().forEach(delegateMethod -> {

			String signature = delegateMethod.fragment() != null
					? delegateMethod.fragment().getSignatureContributor().getName()
					: delegateMethod.source().getDeclaringClass().getName();
			String implementation = delegateMethod.fragment() != null
					? delegateMethod.fragment().getImplementationClass().map(Class::getName).orElse(null)
					: null;
			QueryMetadata query = delegateMethod.methodContributor() != null
					? delegateMethod.methodContributor().getMetadata()
					: null;

			methodMetadata.add(new AotRepositoryMethod(delegateMethod.source().getName(), signature, query,
					new AotFragmentTarget(signature, implementation)));
		});

		// write fields at the end so we make sure to capture things added by methods
		generationMetadata.getFields().values().stream()
				.map(field -> FieldSpec.builder(field.fieldType().getType(), field.fieldName(), field.modifiers()).build())
				.forEach(target::addField);

		// finally customize the file itself
		this.classCustomizer.accept(customizer -> {

			Assert.notNull(customizer, "ClassCustomizer must not be null");
			customizer.customize(target);
		});

		return new AotBundle(repositoryInformation.getRepositoryInterface(),
				Lazy.of(() -> getAotRepositoryMetadata(methodMetadata)));
	}

	private MethodSpec buildConstructor() {
		return constructorBuilder != null ? constructorBuilder.buildConstructor()
				: MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
	}

	private AotRepositoryMetadata getAotRepositoryMetadata(List<AotRepositoryMethod> methodMetadata) {

		AotRepositoryMetadata.RepositoryType repositoryType = repositoryInformation.isReactiveRepository()
				? AotRepositoryMetadata.RepositoryType.REACTIVE
				: AotRepositoryMetadata.RepositoryType.IMPERATIVE;

		String jsonModuleName = moduleName != null ? moduleName.replaceAll("Reactive", "").trim() : null;

		return new AotRepositoryMetadata(repositoryInformation.getRepositoryInterface().getName(), jsonModuleName,
				repositoryType, methodMetadata);
	}

	/**
	 * Configure a {@link AotRepositoryConstructorBuilder} customizer.
	 *
	 * @param classCustomizer must not be {@literal null}.
	 * @return {@code this}.
	 */
	AotRepositoryCreator customizeClass(Consumer<AotRepositoryClassBuilder> classCustomizer) {
		this.classCustomizer = classCustomizer;
		return this;
	}

	/**
	 * Configure a {@link AotRepositoryConstructorBuilder} customizer.
	 *
	 * @param constructorCustomizer must not be {@literal null}.
	 * @return {@code this}.
	 */
	@SuppressWarnings("NullAway")
	AotRepositoryCreator customizeConstructor(Consumer<AotRepositoryConstructorBuilder> constructorCustomizer) {

		if (constructorBuilder != null) {
			constructorBuilder.dispose();
		}

		RepositoryConstructorBuilder constructorBuilder = new RepositoryConstructorBuilder(generationMetadata);
		constructorCustomizer.accept(constructorBuilder);
		this.constructorBuilder = constructorBuilder;
		return this;
	}

	/**
	 * Contribute repository methods using {@link MethodContributor} factory.
	 *
	 * @param methodContributorFactory must not be {@literal null}.
	 */
	void contributeMethods(@Nullable MethodContributorFactory methodContributorFactory) {

		Arrays.stream(repositoryInformation.getRepositoryInterface().getMethods())
				.sorted(Comparator.<Method, String> comparing(it -> it.getDeclaringClass().getName()) //
						.thenComparing(Method::getName) //
						.thenComparing(Method::getParameterCount) //
						.thenComparing(Method::toString))
				.forEach(method -> {

					try {
						contributeMethod(method, methodContributorFactory);
					} catch (RuntimeException e) {
						if (logger.isErrorEnabled()) {
							logger.error("Failed to contribute Repository method [%s.%s]"
									.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName()), e);
						}
					}
				});
	}

	private void contributeMethod(Method method, @Nullable MethodContributorFactory contributorFactory) {

		if (repositoryInformation.isCustomMethod(method)
				|| (repositoryInformation.isBaseClassMethod(method) && !repositoryInformation.isQueryMethod(method))) {

			RepositoryComposition repositoryComposition = repositoryInformation.getRepositoryComposition();
			RepositoryFragment<?> fragment = repositoryComposition.findFragment(method);

			if (fragment != null) {
				generationMetadata.addDelegateMethod(method, fragment);
				return;
			}
		}

		if (method.isBridge() || method.isDefault() || java.lang.reflect.Modifier.isStatic(method.getModifiers())) {

			if (logger.isTraceEnabled()) {
				logger.trace("Skipping %s method [%s.%s] contribution".formatted(
						(method.isBridge() ? "bridge" : method.isDefault() ? "default" : "static"),
						repositoryInformation.getRepositoryInterface().getName(), method.getName()));
			}
			return;
		}

		if (!repositoryInformation.isQueryMethod(method)) {

			if (logger.isTraceEnabled()) {
				logger.trace("Skipping method [%s.%s] contribution, not a query method"
						.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName()));
			}
			return;
		}

		if (contributorFactory == null) {

			if (logger.isTraceEnabled()) {
				logger.trace("Skipping method [%s.%s] contribution, no MethodContributorFactory available"
						.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName()));
			}
			return;
		}

		MethodContributor<? extends QueryMethod> contributor = contributorFactory.create(method);

		if (contributor == null) {

			if (logger.isTraceEnabled()) {
				logger.trace("Skipping method [%s.%s] contribution, no MethodContributor available"
						.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName()));
			}

			return;
		}

		if (ResolvableGenerics.of(method, repositoryInformation.getRepositoryInterface()).hasUnresolvableGenerics()) {

			if (logger.isTraceEnabled()) {
				logger.trace(
						"Skipping implementation method [%s.%s] contribution. Method uses generics that currently cannot be resolved."
								.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName()));
			}

			generationMetadata.addDelegateMethod(method, contributor);
			return;
		}

		if (!contributor.contributesMethodSpec() || repositoryInformation.isReactiveRepository()) {

			if (repositoryInformation.isReactiveRepository() && logger.isTraceEnabled()) {
				logger.trace(
						"Skipping implementation method [%s.%s] contribution. AOT repositories are not supported for reactive repositories."
								.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName()));
			}

			if (!contributor.contributesMethodSpec() && logger.isTraceEnabled()) {
				logger.trace(
						"Skipping implementation method [%s.%s] contribution. Spring Data %s did not provide a method implementation."
								.formatted(repositoryInformation.getRepositoryInterface().getName(), method.getName(), moduleName));
			}

			generationMetadata.addDelegateMethod(method, contributor);
			return;
		}

		generationMetadata.addRepositoryMethod(method, contributor);
	}

	/**
	 * Value object to determine whether generics in a given {@link Method} can be resolved. Resolvable generics are e.g.
	 * declared on the method level (unbounded type variables, type variables using class boundaries). Considers
	 * collections and map types.
	 * <p>
	 * Considers resolvable:
	 * <ul>
	 * <li>Unbounded method-level type parameters {@code <T> T foo(Class<T>)}</li>
	 * <li>Bounded method-level type parameters that resolve to a class
	 * {@code <T extends Serializable> T foo(Class<T>)}</li>
	 * <li>Simple references to interface variables {@code T foo(), List<T> foo(…)}</li>
	 * <li>Unbounded wildcards {@code User foo(GeoJson<?>)}</li>
	 * </ul>
	 * Considers non-resolvable:
	 * <ul>
	 * <li>Parametrized bounds referring to known variables on method-level type parameters
	 * {@code <P extends T> T foo(Class<T>), List<? super T> foo()}</li>
	 * <li>Generally unresolvable generics</li>
	 * </ul>
	 * </p>
	 *
	 * @author Mark Paluch
	 */
	record ResolvableGenerics(Method method, Class<?> implClass, Set<Type> resolvableTypeVariables,
			Set<Type> unwantedMethodVariables) {

		/**
		 * Create a new {@code ResolvableGenerics} object for the given {@link Method}.
		 *
		 * @param method
		 * @return
		 */
		public static ResolvableGenerics of(Method method, Class<?> implClass) {
			return new ResolvableGenerics(method, implClass, getResolvableTypeVariables(method),
					getUnwantedMethodVariables(method));
		}

		private static Set<Type> getResolvableTypeVariables(Method method) {

			Set<Type> simpleTypeVariables = new HashSet<>();

			for (TypeVariable<Method> typeParameter : method.getTypeParameters()) {
				if (isClassBounded(typeParameter.getBounds())) {
					simpleTypeVariables.add(typeParameter);
				}
			}

			return simpleTypeVariables;
		}

		private static Set<Type> getUnwantedMethodVariables(Method method) {

			Set<Type> unwanted = new HashSet<>();

			for (TypeVariable<Method> typeParameter : method.getTypeParameters()) {
				if (!isClassBounded(typeParameter.getBounds())) {
					unwanted.add(typeParameter);
				}
			}
			return unwanted;
		}

		/**
		 * Check whether the {@link Method} has unresolvable generics when being considered in the context of the
		 * implementation class.
		 *
		 * @return
		 */
		public boolean hasUnresolvableGenerics() {

			ResolvableType resolvableType = ResolvableType.forMethodReturnType(method, implClass);

			if (isUnresolvable(resolvableType)) {
				return true;
			}

			for (int i = 0; i < method.getParameterCount(); i++) {
				if (isUnresolvable(ResolvableType.forMethodParameter(method, i, implClass))) {
					return true;
				}
			}

			return false;
		}

		private boolean isUnresolvable(TypeInformation<?> typeInformation) {
			return isUnresolvable(typeInformation.toResolvableType());
		}

		private boolean isUnresolvable(ResolvableType resolvableType) {

			if (isResolvable(resolvableType)) {
				return false;
			}

			if (isUnwanted(resolvableType)) {
				return true;
			}

			if (resolvableType.isAssignableFrom(Class.class)) {
				return isUnresolvable(resolvableType.getGeneric(0));
			}

			TypeInformation<?> typeInformation = TypeInformation.of(resolvableType);
			if (typeInformation.isMap() || typeInformation.isCollectionLike()) {

				for (ResolvableType type : resolvableType.getGenerics()) {
					if (isUnresolvable(type)) {
						return true;
					}
				}

				return false;
			}

			if (typeInformation.getActualType() != null && typeInformation.getActualType() != typeInformation) {
				return isUnresolvable(typeInformation.getRequiredActualType());
			}

			return resolvableType.hasUnresolvableGenerics();
		}

		private boolean isResolvable(Type[] types) {

			for (Type type : types) {

				if (resolvableTypeVariables.contains(type)) {
					continue;
				}

				if (isClass(type)) {
					continue;
				}

				return false;
			}

			return true;
		}

		private boolean isResolvable(ResolvableType resolvableType) {

			return testGenericType(resolvableType, it -> {

				if (resolvableTypeVariables.contains(it)) {
					return true;
				}

				if (it instanceof WildcardType wt) {
					return isClassBounded(wt.getLowerBounds()) && isClassBounded(wt.getUpperBounds());
				}

				return false;
			});
		}

		private boolean isUnwanted(ResolvableType resolvableType) {

			return testGenericType(resolvableType, o -> {

				if (o instanceof WildcardType wt) {
					return !isResolvable(wt.getLowerBounds()) || !isResolvable(wt.getUpperBounds());
				}

				return unwantedMethodVariables.contains(o);
			});
		}

		private static boolean testGenericType(ResolvableType resolvableType, Predicate<Type> predicate) {

			if (predicate.test(resolvableType.getType())) {
				return true;
			}

			ResolvableType[] generics = resolvableType.getGenerics();
			for (ResolvableType generic : generics) {
				if (testGenericType(generic, predicate)) {
					return true;
				}
			}

			return false;
		}

		private static boolean isClassBounded(Type[] bounds) {

			for (Type bound : bounds) {

				if (isClass(bound)) {
					continue;
				}

				return false;
			}

			return true;
		}

		private static boolean isClass(Type type) {
			return type instanceof Class;
		}

	}

	/**
	 * Factory interface to conditionally create {@link MethodContributor} instances. An implementation may decide whether
	 * to return a {@link MethodContributor} or {@literal null}, if no method (code or metadata) should be contributed.
	 */
	public interface MethodContributorFactory {

		/**
		 * Apply customization ot the AOT repository fragment constructor.
		 *
		 * @param method the method to be contributed.
		 * @return the {@link MethodContributor} to be used. Can be {@literal null} if the method and method metadata should
		 *         not be contributed.
		 */
		@Nullable
		MethodContributor<? extends QueryMethod> create(Method method);

	}

	record AotBundle(Class<?> sourceRepository, Lazy<AotRepositoryMetadata> metadata) {

		String repositoryJsonFileName() {
			return sourceRepository.getName().replace('.', '/') + ".json";
		}
	}

}
