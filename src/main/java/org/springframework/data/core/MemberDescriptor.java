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
package org.springframework.data.core;

import kotlin.reflect.KProperty1;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.springframework.asm.Type;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Interface representing a member reference such as a field or method.
 *
 * @author Mark Paluch
 * @since 4.1
 */
interface MemberDescriptor {

	/**
	 * @return class owning the member, can be the declaring class or a subclass.
	 */
	Class<?> getOwner();

	/**
	 * @return the member (field or method).
	 */
	Member getMember();

	/**
	 * @return field type or method return type.
	 */
	ResolvableType getType();

	/**
	 * Create {@link MethodDescriptor} from a serialized lambda representing a method reference.
	 */
	static MethodDescriptor ofMethodReference(ClassLoader classLoader, SerializedLambda lambda)
			throws ClassNotFoundException {
		return ofMethod(classLoader, Type.getObjectType(lambda.getImplClass()).getClassName(), lambda.getImplMethodName());
	}

	/**
	 * Create {@link MethodDescriptor} from owner type and method name.
	 */
	static MethodDescriptor ofMethod(ClassLoader classLoader, String ownerClassName, String name)
			throws ClassNotFoundException {
		Class<?> owner = ClassUtils.forName(ownerClassName, classLoader);
		return MethodDescriptor.create(owner, name);
	}

	/**
	 * Create {@link MethodDescriptor.FieldDescriptor} from owner type, field name and field type.
	 */
	static MethodDescriptor.FieldDescriptor ofField(ClassLoader classLoader, String ownerClassName, String name,
			String fieldType) throws ClassNotFoundException {

		Class<?> owner = ClassUtils.forName(ownerClassName, classLoader);
		Class<?> type = ClassUtils.forName(fieldType, classLoader);

		return FieldDescriptor.create(owner, name, type);
	}

	/**
	 * Value object describing a {@link Method} in the context of an owning class.
	 */
	record MethodDescriptor(Class<?> owner, Method method) implements MemberDescriptor {

		static MethodDescriptor create(Class<?> owner, String methodName) {
			Method method = ReflectionUtils.findMethod(owner, methodName);
			if (method == null) {
				throw new IllegalArgumentException("Method '%s.%s()' not found".formatted(owner.getName(), methodName));
			}
			return new MethodDescriptor(owner, method);
		}

		@Override
		public Class<?> getOwner() {
			return owner();
		}

		@Override
		public Method getMember() {
			return method();
		}

		@Override
		public ResolvableType getType() {
			return ResolvableType.forMethodReturnType(method(), owner());
		}

	}

	/**
	 * Value object describing a {@link Field} in the context of an owning class.
	 */
	record FieldDescriptor(Class<?> owner, Field field) implements MemberDescriptor {

		static FieldDescriptor create(Class<?> owner, String fieldName, Class<?> fieldType) {

			Field field = ReflectionUtils.findField(owner, fieldName, fieldType);
			if (field == null) {
				throw new IllegalArgumentException("Field '%s.%s' not found".formatted(owner.getName(), fieldName));
			}
			return new FieldDescriptor(owner, field);
		}

		@Override
		public Class<?> getOwner() {
			return owner();
		}

		@Override
		public Field getMember() {
			return field();
		}

		@Override
		public ResolvableType getType() {
			return ResolvableType.forField(field(), owner());
		}

	}

	interface KotlinMemberDescriptor extends MemberDescriptor {

		KProperty1<?, ?> getKotlinProperty();

	}

	/**
	 * Value object describing a Kotlin property in the context of an owning class.
	 */
	record KPropertyReferenceDescriptor(Class<?> owner, KProperty1<?, ?> property) implements KotlinMemberDescriptor {

		static KPropertyReferenceDescriptor create(Class<?> owner, KProperty1<?, ?> property) {
			return new KPropertyReferenceDescriptor(owner, property);
		}

		@Override
		public KProperty1<?, ?> getKotlinProperty() {
			return property();
		}

		@Override
		public Class<?> getOwner() {
			return owner();
		}

		@Override
		public Member getMember() {

			Method javaGetter = ReflectJvmMapping.getJavaGetter(property());
			if (javaGetter != null) {
				return javaGetter;
			}

			Field javaField = ReflectJvmMapping.getJavaField(property());

			if (javaField != null) {
				return javaField;
			}

			throw new IllegalStateException("Cannot resolve member for property '%s'".formatted(property().getName()));
		}

		@Override
		public ResolvableType getType() {

			Member member = getMember();

			if (member instanceof Method m) {
				return ResolvableType.forMethodReturnType(m, owner());
			}

			return ResolvableType.forField((Field) member, owner());
		}

	}

	/**
	 * Value object describing a Kotlin property in the context of an owning class.
	 */
	record KPropertyPathDescriptor(KProperty1<?, ?> property) implements KotlinMemberDescriptor {

		static KPropertyPathDescriptor create(KProperty1<?, ?> propertyReference) {
			return new KPropertyPathDescriptor(propertyReference);
		}

		@Override
		public KProperty1<?, ?> getKotlinProperty() {
			return property();
		}

		@Override
		public Class<?> getOwner() {
			return getMember().getDeclaringClass();
		}

		@Override
		public Member getMember() {

			Method javaGetter = ReflectJvmMapping.getJavaGetter(property());
			if (javaGetter != null) {
				return javaGetter;
			}

			Field javaField = ReflectJvmMapping.getJavaField(property());

			if (javaField != null) {
				return javaField;
			}

			throw new IllegalStateException("Cannot resolve member for property '%s'".formatted(property().getName()));
		}

		@Override
		public ResolvableType getType() {

			Member member = getMember();

			if (member instanceof Method m) {
				return ResolvableType.forMethodReturnType(m, getOwner());
			}

			return ResolvableType.forField((Field) member, getOwner());
		}

	}

}
