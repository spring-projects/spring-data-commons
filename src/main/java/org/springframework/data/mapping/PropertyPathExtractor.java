/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.mapping;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

// Property path extractor using SerializedLambda
class PropertyPathExtractor {

	private static final Map<ClassLoader, Map<Object, PropertyPathInformation>> lambdas = new WeakHashMap<>();

	public static PropertyPathInformation getPropertyPathInformation(TypedPropertyPath<?, ?> lambda) {

		Map<Object, PropertyPathInformation> lambdaMap;
		synchronized (lambdas) {
			lambdaMap = lambdas.computeIfAbsent(lambda.getClass().getClassLoader(), k -> new ConcurrentReferenceHashMap<>());
		}

		return lambdaMap.computeIfAbsent(lambda, o -> extractPath(lambda.getClass().getClassLoader(), lambda));
	}

	record PropertyPathInformation(TypeInformation<?> owner, TypeInformation<?> propertyType, Property property) {

		public static PropertyPathInformation ofInvokeVirtual(ClassLoader classLoader, SerializedLambda lambda)
				throws ClassNotFoundException {
			return ofInvokeVirtual(classLoader, Type.getObjectType(lambda.getImplClass()), lambda.getImplMethodName());
		}

		public static PropertyPathInformation ofInvokeVirtual(ClassLoader classLoader, Type ownerType, String name)
				throws ClassNotFoundException {
			Class<?> owner = ClassUtils.forName(ownerType.getClassName(), classLoader);
			return ofInvokeVirtual(owner, name);
		}

		public static PropertyPathInformation ofInvokeVirtual(Class<?> owner, String methodName) {

			Method method = ReflectionUtils.findMethod(owner, methodName);
			if (method == null) {
				throw new IllegalArgumentException("Method %s.%s() not found".formatted(owner.getName(), methodName));
			}
			PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method);

			if (descriptor == null) {
				throw new IllegalArgumentException(
						"Cannot find PropertyDescriptor from method %s.%s".formatted(owner.getName(), methodName));
			}

			return new PropertyPathInformation(TypeInformation.of(owner),
					TypeInformation.of(ResolvableType.forMethodReturnType(method, owner)),
					Property.of(TypeInformation.of(owner), descriptor));
		}

		public static PropertyPathInformation ofFieldAccess(ClassLoader classLoader, Type ownerType, String name,
				Type fieldType) throws ClassNotFoundException {

			Class<?> owner = ClassUtils.forName(ownerType.getClassName(), classLoader);
			Class<?> type = ClassUtils.forName(fieldType.getClassName(), classLoader);

			return ofFieldAccess(owner, name, type);
		}

		public static PropertyPathInformation ofFieldAccess(Class<?> owner, String fieldName, Class<?> fieldType) {

			Field field = ReflectionUtils.findField(owner, fieldName, fieldType);
			if (field == null) {
				throw new IllegalArgumentException("Field %s.%s() not found".formatted(owner.getName(), field));
			}

			return new PropertyPathInformation(TypeInformation.of(owner),
					TypeInformation.of(ResolvableType.forField(field, owner)), Property.of(TypeInformation.of(owner), field));
		}
	}

	public static <T, R> PropertyPathInformation extractPath(ClassLoader classLoader, TypedPropertyPath<T, R> path) {

		try {
			// Use serialization to extract method reference info
			SerializedLambda lambda = getSerializedLambda(path);

			// method handle
			if (lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual
					&& !lambda.getImplMethodName().startsWith("lambda$")) {
				return PropertyPathInformation.ofInvokeVirtual(classLoader, lambda);
			}

			if (lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual) {

				String implClass = Type.getObjectType(lambda.getImplClass()).getClassName();
				Type owningType = Type.getArgumentTypes(lambda.getImplMethodSignature())[0];
				ClassReader cr = new ClassReader(implClass);
				LambdaClassVisitor classVisitor = new LambdaClassVisitor(classLoader, lambda.getImplMethodName(), owningType);
				cr.accept(classVisitor, ClassReader.SKIP_FRAMES);
				return classVisitor.getPropertyPathInformation(lambda);
			}
		} catch (ReflectiveOperationException | IOException e) {
			throw new RuntimeException("Cannot extract property path", e);
		}

		throw new IllegalArgumentException(
				"Cannot extract property path from: " + path + ". The given value is not a Lambda and not a Method Reference.");
	}

	private static SerializedLambda getSerializedLambda(Object lambda) {
		try {
			Method method = lambda.getClass().getDeclaredMethod("writeReplace");
			method.setAccessible(true);
			return (SerializedLambda) method.invoke(lambda);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Not a lambda: " + lambda, e);
		}
	}

	static class LambdaClassVisitor extends ClassVisitor {

		private final ClassLoader classLoader;
		private final String implMethodName;
		private final Type owningType;
		private LambdaMethodVisitor methodVisitor;

		public LambdaClassVisitor(ClassLoader classLoader, String implMethodName, Type owningType) {
			super(Opcodes.ASM10_EXPERIMENTAL);
			this.classLoader = classLoader;
			this.implMethodName = implMethodName;
			this.owningType = owningType;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			// Capture the lambda body methods for later
			if (name.equals(implMethodName)) {

				methodVisitor = new LambdaMethodVisitor(classLoader, owningType);
				return methodVisitor;
			}

			return null;
		}

		public PropertyPathInformation getPropertyPathInformation(SerializedLambda lambda) {
			return methodVisitor.getPropertyPathInformation(lambda);
		}
	}

	static class LambdaMethodVisitor extends MethodVisitor {

		private final ClassLoader classLoader;
		private final Type owningType;
		private int line;
		List<PropertyPathInformation> propertyPathInformations = new ArrayList<>();
		Set<ParseError> errors = new LinkedHashSet<>();

		public LambdaMethodVisitor(ClassLoader classLoader, Type owningType) {
			super(Opcodes.ASM10_EXPERIMENTAL);
			this.classLoader = classLoader;
			this.owningType = owningType;
		}

		@Override
		public void visitLineNumber(int line, Label start) {
			this.line = line;
		}

		@Override
		public void visitInsn(int opcode) {

			// allow primitive and object return
			if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
				return;
			}

			errors.add(new ParseError(line, "PropertyPath lambdas may only contain method calls and field access", null));
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {

			if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) {
				errors.add(new ParseError(line, "Put field not allowed in property path lambda", null));
				return;
			}

			Type fieldType = Type.getType(descriptor);

			try {
				this.propertyPathInformations
						.add(PropertyPathInformation.ofFieldAccess(classLoader, owningType, name, fieldType));
			} catch (ClassNotFoundException e) {
				errors.add(new ParseError(line, e.getMessage()));
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

			int count = Type.getArgumentCount(descriptor);

			if (count != 0) {
				errors.add(new ParseError(line, "Property path extraction requires calls to no-arg getters"));
				return;
			}

			try {
				this.propertyPathInformations.add(PropertyPathInformation.ofInvokeVirtual(classLoader, owningType, name));
			} catch (Exception e) {
				errors.add(new ParseError(line, e.getMessage(), e));
			}
		}

		public PropertyPathInformation getPropertyPathInformation(SerializedLambda lambda) {

			if (!errors.isEmpty()) {

				if (lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic
						|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual) {

					Pattern hex = Pattern.compile("[0-9a-f]+");
					String methodName = lambda.getImplMethodName();
					if (methodName.startsWith("lambda$")) {
						methodName = methodName.substring("lambda$".length());

						if (methodName.contains("$")) {
							methodName = methodName.substring(0, methodName.lastIndexOf('$'));
						}

						if (methodName.contains("$")) {
							String probe = methodName.substring(methodName.lastIndexOf('$') + 1);
							if (hex.matcher(probe).matches()) {
								methodName = methodName.substring(0, methodName.lastIndexOf('$'));
							}
						}
					}

					InvalidDataAccessApiUsageException e = new InvalidDataAccessApiUsageException("Cannot resolve property path: "
							+ errors.stream().map(ParseError::message).collect(Collectors.joining("; ")));

					for (ParseError error : errors) {
						if (error.e != null) {
							e.addSuppressed(error.e);
						}
					}

					StackTraceElement[] stackTrace = e.getStackTrace();
					int filterIndex = -1;

					for (int i = 0; i < stackTrace.length; i++) {

						if (stackTrace[i].getClassName().equals(PropertyPathExtractor.class.getName())
								|| stackTrace[i].getClassName().equals(TypedPropertyPath.class.getName())
								|| stackTrace[i].getClassName().equals(ComposedPropertyPath.class.getName())
								|| stackTrace[i].getClassName().equals(PropertyPath.class.getName())) {
							filterIndex = i;
						}
					}

					if (filterIndex != -1) {

						StackTraceElement[] copy = new StackTraceElement[(stackTrace.length - filterIndex) + 1];
						System.arraycopy(stackTrace, filterIndex, copy, 1, stackTrace.length - filterIndex);

						Type type = Type.getObjectType(lambda.getCapturingClass());
						StackTraceElement userCode = copy[1];

						StackTraceElement synthetic = new StackTraceElement(null, userCode.getModuleName(),
								userCode.getModuleVersion(), type.getClassName(), methodName,
								ClassUtils.getShortName(type.getClassName()) + ".java", errors.iterator().next().line);
						copy[0] = synthetic;
						e.setStackTrace(copy);
					}

					throw e;

				}
				// lambda$resolvesComposedLambdaFieldAccess$d3dc5794$1

				throw new IllegalStateException("There are errors in property path lambda " + errors);
			}

			if (propertyPathInformations.isEmpty()) {
				throw new IllegalStateException("There are no property path information available");
			}

			// TODO composite path information
			return propertyPathInformations.get(propertyPathInformations.size() - 1);
		}

	}

	record ParseError(int line, String message, @Nullable Exception e) {

		ParseError(int line, String message) {
			this(line, message, null);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ParseError that)) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(e, that.e)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(message, that.message);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHash(message, e);
		}
	}
}
