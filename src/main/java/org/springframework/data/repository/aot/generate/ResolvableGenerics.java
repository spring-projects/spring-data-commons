/*
 * Copyright 2025-present the original author or authors.
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
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.core.TypeInformation;

/**
 * Value object to determine whether generics in a given {@link Method} can be resolved. Resolvable generics are e.g.
 * declared on the method level (unbounded type variables, type variables using class boundaries). Considers collections
 * and map types.
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

	/**
	 * Checks if a given {@link Parameter} can be resolved fully. Unresolvable or unwanted type signatures should fall
	 * back to using raw types.
	 *
	 * @param parameter
	 * @return
	 */
	public boolean isFullyResolvableParameter(Parameter parameter) {

		MethodParameter methodParameter = MethodParameter.forParameter(parameter).withContainingClass(implClass);
		ResolvableType resolvableType = ResolvableType.forMethodParameter(methodParameter);

		return testGenericType(resolvableType, o -> {

			if (o instanceof WildcardType wt) {
				return !isResolvable(wt.getLowerBounds()) || !isResolvable(wt.getUpperBounds());
			}

			if (o instanceof ParameterizedType pt) {
				return isResolvable(pt.getActualTypeArguments());
			}

			return unwantedMethodVariables.contains(o);
		});
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
