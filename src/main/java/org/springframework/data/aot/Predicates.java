/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.aot;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 * Abstract utility class containing common, reusable {@link Predicate Predicates}.
 *
 * @author John Blum
 * @see java.util.function.Predicate
 * @since 3.0
 */
// TODO: Consider moving to the org.springframework.data.util package.
@SuppressWarnings("unused")
public abstract class Predicates {

	public static final Predicate<Member> IS_ENUM_MEMBER = member -> member.getDeclaringClass().isEnum();
	public static final Predicate<Member> IS_HIBERNATE_MEMBER = member -> member.getName().startsWith("$$_hibernate");
	public static final Predicate<Member> IS_OBJECT_MEMBER = member -> Object.class.equals(member.getDeclaringClass());
	public static final Predicate<Member> IS_JAVA = member -> member.getDeclaringClass().getPackageName().startsWith("java.");
	public static final Predicate<Member> IS_NATIVE = member -> Modifier.isNative(member.getModifiers());
	public static final Predicate<Member> IS_PRIVATE = member -> Modifier.isPrivate(member.getModifiers());
	public static final Predicate<Member> IS_PROTECTED = member -> Modifier.isProtected(member.getModifiers());
	public static final Predicate<Member> IS_PUBLIC = member -> Modifier.isPublic(member.getModifiers());
	public static final Predicate<Member> IS_SYNTHETIC = Member::isSynthetic;

	public static final Predicate<Method> IS_BRIDGE_METHOD = Method::isBridge;


}
