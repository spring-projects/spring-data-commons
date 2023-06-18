/*
 * Copyright 2021-2023 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * Utility methods to work with {@link Predicate}s.
 *
 * @author Mark Paluch
 * @since 2.7
 */
public interface Predicates {

	Predicate<Member> IS_ENUM_MEMBER = member -> member.getDeclaringClass().isEnum();
	Predicate<Member> IS_HIBERNATE_MEMBER = member -> member.getName().startsWith("$$_hibernate"); // this
	// should
	// go
	// into
	// JPA
	Predicate<Member> IS_OBJECT_MEMBER = member -> Object.class.equals(member.getDeclaringClass());
	Predicate<Member> IS_JAVA = member -> member.getDeclaringClass().getPackageName().startsWith("java.");
	Predicate<Member> IS_NATIVE = member -> Modifier.isNative(member.getModifiers());
	Predicate<Member> IS_PRIVATE = member -> Modifier.isPrivate(member.getModifiers());
	Predicate<Member> IS_PROTECTED = member -> Modifier.isProtected(member.getModifiers());
	Predicate<Member> IS_PUBLIC = member -> Modifier.isPublic(member.getModifiers());
	Predicate<Member> IS_SYNTHETIC = Member::isSynthetic;

	Predicate<Class<?>> IS_KOTLIN = KotlinReflectionUtils::isSupportedKotlinClass;
	Predicate<Member> IS_STATIC = member -> Modifier.isStatic(member.getModifiers());

	Predicate<Method> IS_BRIDGE_METHOD = Method::isBridge;

	/**
	 * A {@link Predicate} that yields always {@code true}.
	 *
	 * @return a {@link Predicate} that yields always {@code true}.
	 */
	static <T> Predicate<T> isTrue() {
		return t -> true;
	}

	/**
	 * A {@link Predicate} that yields always {@code false}.
	 *
	 * @return a {@link Predicate} that yields always {@code false}.
	 */
	static <T> Predicate<T> isFalse() {
		return t -> false;
	}

	/**
	 * Returns a {@link Predicate} that represents the logical negation of {@code predicate}.
	 *
	 * @return a {@link Predicate} that represents the logical negation of {@code predicate}.
	 */
	static <T> Predicate<T> negate(Predicate<T> predicate) {

		Assert.notNull(predicate, "Predicate must not be null");
		return predicate.negate();
	}

	/**
	 * Whether to consider a {@link Constructor}. We generally exclude synthetic constructors for non-Kotlin classes.
	 *
	 * @param candidate
	 * @return
	 */
	static boolean isIncluded(Constructor<?> candidate) {
		return !isExcluded(candidate);
	}

	/**
	 * Whether to not consider a {@link Constructor}. We generally exclude synthetic constructors for non-Kotlin classes.
	 *
	 * @param candidate
	 * @return
	 */
	static boolean isExcluded(Constructor<?> candidate) {

		if (IS_KOTLIN.test(candidate.getDeclaringClass())) {
			return false;
		}

		return IS_SYNTHETIC.test(candidate);
	}
}
