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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.core.MethodParameter;

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
	 * Creates a new {@link Nullability} instance by introspecting the {@link MethodParameter}.
	 *
	 * @param parameter the source method parameter.
	 * @return a {@code Nullability} instance containing the element's nullability declaration.
	 */
	static Nullability from(MethodParameter parameter) {
		return introspect(parameter.getContainingClass()).forParameter(parameter);
	}

	/**
	 * Creates a new {@link Nullability} instance by introspecting the {@link Method} return type.
	 *
	 * @param method the source method.
	 * @return a {@code Nullability} instance containing the element's nullability declaration.
	 */
	static Nullability forMethodReturnType(Method method) {
		return introspect(method.getDeclaringClass()).forReturnType(method);
	}

	/**
	 * Creates a new {@link Nullability} instance by introspecting the {@link Parameter method parameter}.
	 *
	 * @param parameter the source method parameter.
	 * @return a {@code Nullability} instance containing the element's nullability declaration.
	 */
	static Nullability forMethodParameter(Parameter parameter) {
		return introspect(parameter.getDeclaringExecutable().getDeclaringClass()).forParameter(parameter);
	}

	/**
	 * Creates introspector using the given {@link Class} as declaration anchor.
	 *
	 * @param cls the source class.
	 * @return a {@code Introspector} instance considering nullability declarations from the {@link Class} and package.
	 */
	static Introspector introspect(Class<?> cls) {
		return new NullabilityIntrospector(cls);
	}

	/**
	 * Creates introspector using the given {@link Package} as declaration anchor.
	 *
	 * @param pkg the source package.
	 * @return a {@code Introspector} instance considering nullability declarations from package.
	 */
	static Introspector introspect(Package pkg) {
		return new NullabilityIntrospector(pkg);
	}

	/**
	 * Nullability introspector to introspect multiple elements within the context of their source container.
	 */
	interface Introspector {

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
		 * Creates a new {@link Nullability} instance by introspecting the {@link MethodParameter}.
		 * <p>
		 * If the method parameter does not declare any nullability rules, then introspection falls back to the source
		 * container that was used to create the introspector.
		 *
		 * @param parameter the source method parameter.
		 * @return a {@code Nullability} instance containing the element's nullability declaration.
		 */
		Nullability forParameter(Parameter parameter);

	}

}
