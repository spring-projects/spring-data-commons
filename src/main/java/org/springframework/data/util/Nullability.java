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
package org.springframework.data.util;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNullApi;
import org.springframework.lang.Nullable;

/**
 * Provides access to nullability declarations of methods and parameters, usually obtained from a source such as a
 * {@link Class} or {@link Method}.
 * <p>
 * An application expresses nullability rules ideally expressed on the top-most element such as the package to let all
 * inner elements participate in the defaults. Individual elements such as methods or parameters can be annotated with
 * non-null annotations to express deviation from the default rule.
 * <p>
 * Nullability can be defined on various levels: Methods, (inner) classes, packages. We consider these as declaration
 * anchors. Introspection of nullability traverses declaration anchor trees in their logical order (i.e. a class
 * contains methods, an enclosing class contains inner classes, a package contains classes) to inherit nullability rules
 * if the particular method or parameter does not declare nullability rules.
 * <p>
 * By default (no annotation use), a package and its types are considered allowing {@literal null} values in return
 * values and method parameters. Nullability rules are expressed by annotating a package with annotations such as
 * Spring's {@link NonNullApi}. All types of the package inherit the package rule. Subpackages do not inherit
 * nullability rules and must be annotated themselves.
 *
 * <pre class="code">
 * &#64;org.springframework.lang.NonNullApi
 * package com.example;
 * </pre>
 *
 * {@link Nullable} selectively permits {@literal null} values for method return values or method parameters by
 * annotating the method respectively the parameters:
 *
 * <pre class="code">
 * public class ExampleClass {
 *
 * 	String shouldNotReturnNull(@Nullable String acceptsNull, String doesNotAcceptNull) {
 * 		// …
 * 	}
 *
 * 	&#64;Nullable
 * 	String nullableReturn(String parameter) {
 * 		// …
 * 	}
 * }
 * </pre>
 *
 * Note that nullability is also expressed through using specific types. Primitives ({@code int}, {@code char} etc.) are
 * non-nullable by definition and cannot be {@code null}. {@code void}/{@code Void} types are {@code null}-only types.
 * <p>
 * {@code javax.annotation.Nonnull} is suitable for composition of meta-annotations and expresses via
 * {@code javax.annotation.Nonnull#when()} in which cases non-nullability is applicable. Nullability introspection
 * considers the following mechanisms:
 * <ul>
 * <li>Spring's {@link NonNullApi}, {@link Nullable}, and {@link org.springframework.lang.NonNull}.</li>
 * <li>JSR-305 {@code javax.annotation.Nonnull} and meta-annotations.</li>
 * <li><a href="https://https://jspecify.dev/">JSpecify</a>, a newly designed specification to opt-in for non-null by
 * default through {@code org.jspecify.annotations.NullMarked} and {@code org.jspecify.annotations.Nullable}.</li>
 * </ul>
 * <p>
 * A component might be interested on whether nullability is declared and if so, whether the particular element is
 * nullable or non-null.
 * <p>
 * Here are some typical examples:
 *
 * <pre class="code">
 * // is an nullability declared for a Method return type
 * Nullability nullability = Nullability.forMethodReturnType(method);
 * nullability.isDeclared();
 * nullability.isNullable();
 * nullability.isNonNull();
 *
 * // introspect multiple elements for their nullability in the scope of a class/package.
 * Nullability.Introspector introspector = Nullability.introspect(NonNullOnPackage.class);
 * Nullability nullability = introspector.forReturnType(method);
 * </pre>
 * <p>
 * <b>NOTE: The Nullability API is primarily intended for framework components that want to introspect nullability
 * declarations, for example to validate input or output.</b>
 *
 * @author Mark Paluch
 * @see NonNullApi
 * @see Nullable
 */
public interface Nullability {

	/**
	 * Determine if nullability declaration is present on the source.
	 *
	 * @return {@code true} if the source (or any of its declaration anchors) defines nullability rules.
	 */
	boolean isDeclared();

	/**
	 * Determine if the source is nullable.
	 *
	 * @return {@code true} if the source (or any of its declaration anchors) is nullable.
	 */
	boolean isNullable();

	/**
	 * Determine if the source is non-nullable.
	 *
	 * @return {@code true} if the source (or any of its declaration anchors) is non-nullable.
	 */
	boolean isNonNull();

	/**
	 * Creates a new {@link MethodNullability} instance by introspecting the {@link Method} return type.
	 *
	 * @param method the source method.
	 * @return a {@code Nullability} instance containing the element's nullability declaration.
	 */
	static MethodNullability forMethod(Method method) {
		return new NullabilityIntrospector(method.getDeclaringClass(), true).forMethod(method);
	}

	/**
	 * Creates a new {@link Nullability} instance by introspecting the {@link MethodParameter}.
	 *
	 * @param parameter the source method parameter.
	 * @return a {@code Nullability} instance containing the element's nullability declaration.
	 */
	static Nullability forParameter(MethodParameter parameter) {
		return new NullabilityIntrospector(parameter.getContainingClass(), false).forParameter(parameter);
	}

	/**
	 * Creates a new {@link Nullability} instance by introspecting the {@link Method} return type.
	 *
	 * @param method the source method.
	 * @return a {@code Nullability} instance containing the element's nullability declaration.
	 */
	static Nullability forMethodReturnType(Method method) {
		return new NullabilityIntrospector(method.getDeclaringClass(), false).forReturnType(method);
	}

	/**
	 * Creates a new {@link Nullability} instance by introspecting the {@link Parameter method parameter}.
	 *
	 * @param parameter the source method parameter.
	 * @return a {@code Nullability} instance containing the element's nullability declaration.
	 */
	static Nullability forParameter(Parameter parameter) {
		return new NullabilityIntrospector(parameter.getDeclaringExecutable().getDeclaringClass(), false)
				.forParameter(parameter);
	}

	/**
	 * Creates introspector using the given {@link Class} as declaration anchor.
	 *
	 * @param cls the source class.
	 * @return a {@code Introspector} instance considering nullability declarations from the {@link Class} and package.
	 */
	static Introspector introspect(Class<?> cls) {
		return new NullabilityIntrospector(cls, true);
	}

	/**
	 * Creates introspector using the given {@link Package} as declaration anchor.
	 *
	 * @param pkg the source package.
	 * @return a {@code Introspector} instance considering nullability declarations from package.
	 */
	static Introspector introspect(Package pkg) {
		return new NullabilityIntrospector(pkg, true);
	}

	/**
	 * Nullability introspector to introspect multiple elements within the context of their source container.
	 */
	interface Introspector {

		/**
		 * Returns whether nullability rules are defined for the given {@link ElementType}.
		 *
		 * @param elementType the element type to check.
		 * @return {@code true} if nullability is declared for the given element type; {@code false} otherwise.
		 */
		boolean isDeclared(ElementType elementType);

		/**
		 * Creates a new {@link MethodNullability} instance by introspecting the {@link Method}.
		 * <p>
		 * If the method parameter does not declare any nullability rules, then introspection falls back to the source
		 * container that was used to create the introspector.
		 *
		 * @param method the source method.
		 * @return a {@code Nullability} instance containing the element's nullability declaration.
		 */
		MethodNullability forMethod(Method method);

		/**
		 * Creates a new {@link Nullability} instance by introspecting the {@link MethodParameter}.
		 * <p>
		 * If the method parameter does not declare any nullability rules, then introspection falls back to the source
		 * container that was used to create the introspector.
		 *
		 * @param parameter the source method parameter.
		 * @return a {@code Nullability} instance containing the element's nullability declaration.
		 */
		default Nullability forParameter(MethodParameter parameter) {
			return parameter.getParameterIndex() == -1 ? forReturnType(parameter.getMethod())
					: forParameter(parameter.getParameter());
		}

		/**
		 * Creates a new {@link Nullability} instance by introspecting the {@link Method} return type.
		 * <p>
		 * If the method parameter does not declare any nullability rules, then introspection falls back to the source
		 * container that was used to create the introspector.
		 *
		 * @param method the source method.
		 * @return a {@code Nullability} instance containing the element's nullability declaration.
		 */
		Nullability forReturnType(Method method);

		/**
		 * Creates a new {@link Nullability} instance by introspecting the {@link Parameter}.
		 * <p>
		 * If the method parameter does not declare any nullability rules, then introspection falls back to the source
		 * container that was used to create the introspector.
		 *
		 * @param parameter the source method parameter.
		 * @return a {@code Nullability} instance containing the element's nullability declaration.
		 */
		Nullability forParameter(Parameter parameter);

	}

	/**
	 * Nullability introspector to introspect multiple elements within the context of their source container. Inherited
	 * nullability methods nullability of the method return type.
	 */
	interface MethodNullability extends Nullability {

		/**
		 * Returns a {@link Nullability} instance for the method return type.
		 *
		 * @return a {@link Nullability} instance for the method return type.
		 */
		default Nullability forReturnType() {
			return this;
		}

		/**
		 * Returns a {@link Nullability} instance for a method parameter.
		 *
		 * @param parameter the method parameter.
		 * @return a {@link Nullability} instance for a method parameter.
		 * @throws IllegalArgumentException if the method parameter is not defined by the underlying method.
		 */
		Nullability forParameter(Parameter parameter);

		/**
		 * Returns a {@link Nullability} instance for a method parameter by index.
		 *
		 * @param index the method parameter index.
		 * @return a {@link Nullability} instance for a method parameter.
		 * @throws IndexOutOfBoundsException if the method parameter index is out of bounds.
		 */
		Nullability forParameter(int index);

		/**
		 * Returns a {@link Nullability} instance for a method parameter.
		 *
		 * @param parameter the method parameter.
		 * @return a {@link Nullability} instance for a method parameter.
		 * @throws IllegalArgumentException if the method parameter is not defined by the underlying method.
		 */
		default Nullability forParameter(MethodParameter parameter) {
			return parameter.getParameterIndex() == -1 ? forReturnType() : forParameter(parameter.getParameter());
		}

		/**
		 * Returns the method parameter count.
		 *
		 * @return the method parameter count.
		 */
		int getParameterCount();

	}

}
