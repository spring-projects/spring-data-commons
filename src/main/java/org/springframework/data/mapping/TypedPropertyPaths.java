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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

/**
 * Utility class to parse and resolve {@link TypedPropertyPath} instances.
 */
class TypedPropertyPaths {

	private static final Map<ClassLoader, Map<Object, PropertyPathInformation>> lambdas = new WeakHashMap<>();
	private static final Map<ClassLoader, Map<TypedPropertyPath, ResolvedTypedPropertyPath<?, ?>>> resolved = new WeakHashMap<>();

	/**
	 * Retrieve {@link PropertyPathInformation} for a given {@link TypedPropertyPath}.
	 */
	public static PropertyPathInformation getPropertyPathInformation(TypedPropertyPath<?, ?> lambda) {

		Map<Object, PropertyPathInformation> cache;
		synchronized (lambdas) {
			cache = lambdas.computeIfAbsent(lambda.getClass().getClassLoader(), k -> new ConcurrentReferenceHashMap<>());
		}
		Map<Object, PropertyPathInformation> lambdaMap = cache;

		return lambdaMap.computeIfAbsent(lambda, o -> extractPath(lambda.getClass().getClassLoader(), lambda));
	}

	/**
	 * Compose a {@link TypedPropertyPath} by appending {@code next}.
	 */
	public static <T, M, R> TypedPropertyPath<T, R> compose(TypedPropertyPath<T, M> owner, TypedPropertyPath<M, R> next) {
		return new ComposedPropertyPath<>(owner, next);
	}

	/**
	 * Resolve a {@link TypedPropertyPath} into a {@link ResolvedTypedPropertyPath}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <P, T> TypedPropertyPath<T, P> of(TypedPropertyPath<T, P> lambda) {

		if (lambda instanceof ComposedPropertyPath<?, ?, ?> || lambda instanceof ResolvedTypedPropertyPath<?, ?>) {
			return lambda;
		}

		Map<TypedPropertyPath, ResolvedTypedPropertyPath<?, ?>> cache;
		synchronized (resolved) {
			cache = resolved.computeIfAbsent(lambda.getClass().getClassLoader(), k -> new ConcurrentReferenceHashMap<>());
		}

		return (TypedPropertyPath<T, P>) cache.computeIfAbsent(lambda,
				o -> new ResolvedTypedPropertyPath(o, getPropertyPathInformation(lambda)));
	}

	/**
	 * Value object holding information about a property path segment.
	 *
	 * @param owner
	 * @param propertyType
	 * @param property
	 */
	record PropertyPathInformation(TypeInformation<?> owner, TypeInformation<?> propertyType, String property) {

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
				String propertyName;

				if (methodName.startsWith("is")) {
					propertyName = Introspector.decapitalize(methodName.substring(2));
				} else if (methodName.startsWith("get")) {
					propertyName = Introspector.decapitalize(methodName.substring(3));
				} else {
					propertyName = methodName;
				}

				TypeInformation<?> fallback = TypeInformation.of(owner).getProperty(propertyName);
				if (fallback != null) {
						return new PropertyPathInformation(TypeInformation.of(owner), fallback,
								propertyName);
				}

				throw new IllegalArgumentException(
						"Cannot find PropertyDescriptor from method %s.%s".formatted(owner.getName(), methodName));
			}

			return new PropertyPathInformation(TypeInformation.of(owner),
					TypeInformation.of(ResolvableType.forMethodReturnType(method, owner)),
					descriptor.getName());
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
					TypeInformation.of(ResolvableType.forField(field, owner)), field.getName());
		}
	}

	public static <T, R> PropertyPathInformation extractPath(ClassLoader classLoader, TypedPropertyPath<T, R> path) {

		try {
			// Use serialization to extract method reference info
			SerializedLambda lambda = getSerializedLambda(path);

			if (lambda.getImplMethodKind() == MethodHandleInfo.REF_newInvokeSpecial
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeSpecial) {
				InvalidDataAccessApiUsageException e = new InvalidDataAccessApiUsageException(
						"Method reference must not be a constructor call");
				e.setStackTrace(filterStackTrace(e.getStackTrace()));
				throw e;
			}

			// method handle
			if ((lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeInterface)
					&& !lambda.getImplMethodName().startsWith("lambda$")) {
				return PropertyPathInformation.ofInvokeVirtual(classLoader, lambda);
			}

			if (lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual) {

				String implClass = Type.getObjectType(lambda.getImplClass()).getClassName();

				Type owningType = Type.getArgumentTypes(lambda.getImplMethodSignature())[0];
				String classFileName = implClass.replace('.', '/') + ".class";
				InputStream classFile = ClassLoader.getSystemResourceAsStream(classFileName);
				if (classFile == null) {
					throw new IllegalStateException("Cannot find class file '%s' for lambda analysis.".formatted(classFileName));
				}

				try (classFile) {

					ClassReader cr = new ClassReader(classFile);
					LambdaClassVisitor classVisitor = new LambdaClassVisitor(classLoader, lambda.getImplMethodName(), owningType);
					cr.accept(classVisitor, ClassReader.SKIP_FRAMES);
					return classVisitor.getPropertyPathInformation(lambda);
				}
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
			throw new InvalidDataAccessApiUsageException(
					"Not a lambda: " + (lambda instanceof Enum<?> ? lambda.getClass().getName() + "#" + lambda : lambda), e);
		}
	}

	static class LambdaClassVisitor extends ClassVisitor {

		private final ClassLoader classLoader;
		private final String implMethodName;
		private final Type owningType;
		private @Nullable LambdaMethodVisitor methodVisitor;

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

	static class ResolvedTypedPropertyPath<T, P> implements TypedPropertyPath<T, P> {

		private final TypedPropertyPath<T, P> function;
		private final PropertyPathInformation information;
		private final List<PropertyPath> list;

		ResolvedTypedPropertyPath(TypedPropertyPath<T, P> function, PropertyPathInformation information) {
			this.function = function;
			this.information = information;
			this.list = List.of(this);
		}

		@Override
		public @Nullable P get(T obj) {
			return function.get(obj);
		}

		@Override
		public TypeInformation<?> getOwningType() {
			return information.owner();
		}

		@Override
		public String getSegment() {
			return information.property();
		}

		@Override
		public TypeInformation<?> getTypeInformation() {
			return information.propertyType();
		}

		@Override
		public Iterator<PropertyPath> iterator() {
			return list.iterator();
		}

		@Override
		public Stream<PropertyPath> stream() {
			return list.stream();
		}

		@Override
		public List<PropertyPath> toList() {
			return list;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj == null || obj.getClass() != this.getClass())
				return false;
			var that = (ResolvedTypedPropertyPath) obj;
			return Objects.equals(this.function, that.function) && Objects.equals(this.information, that.information);
		}

		@Override
		public int hashCode() {
			return Objects.hash(function, information);
		}

		@Override
		public String toString() {
			return information.owner().getType().getSimpleName() + "." + toDotPath();
		}
	}

	static class LambdaMethodVisitor extends MethodVisitor {

		private final ClassLoader classLoader;
		private final Type owningType;
		private int line;

		private static final Set<String> BOXING_TYPES = Set.of(Type.getInternalName(Integer.class),
				Type.getInternalName(Long.class), Type.getInternalName(Short.class), Type.getInternalName(Byte.class),
				Type.getInternalName(Float.class), Type.getInternalName(Double.class), Type.getInternalName(Character.class),
				Type.getInternalName(Boolean.class));

		private static final String BOXING_METHOD = "valueOf";

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

			if (opcode >= Opcodes.DUP && opcode <= Opcodes.DUP2_X2) {
				return;
			}

			visitLdcInsn("");
		}

		@Override
		public void visitLdcInsn(Object value) {
			errors.add(new ParseError(line,
					"Lambda expressions for Typed property path declaration may only contain method calls to getters, record components, and field access",
					null));
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

			if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
				errors.add(new ParseError(line, "Lambda must not call constructor", null));
				return;
			}

			int count = Type.getArgumentCount(descriptor);

			if (count != 0) {

				if (BOXING_TYPES.contains(owner) && name.equals(BOXING_METHOD)) {
					return;
				}

				errors.add(new ParseError(line, "Property path extraction requires calls to no-arg getters"));
				return;
			}

			Type ownerType = Type.getObjectType(owner);
			if (!ownerType.equals(this.owningType)) {
				errors.add(new ParseError(line,
						"Cannot derive a property path from method call '%s' on a different owning type. Expected owning type: %s, but was: %s"
								.formatted(name, this.owningType.getClassName(), ownerType.getClassName())));
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

					e.setStackTrace(filterStackTrace(lambda, e.getStackTrace(), methodName));

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

		private StackTraceElement[] filterStackTrace(SerializedLambda lambda, StackTraceElement[] stackTrace,
				String methodName) {

			int filterIndex = findEntryPoint(stackTrace);

			if (filterIndex != -1) {

				StackTraceElement[] copy = new StackTraceElement[(stackTrace.length - filterIndex) + 1];
				System.arraycopy(stackTrace, filterIndex, copy, 1, stackTrace.length - filterIndex);

				StackTraceElement userCode = copy[1];
				StackTraceElement synthetic = createSynthetic(lambda, methodName, userCode);
				copy[0] = synthetic;
				return copy;
			}

			return stackTrace;
		}

		private StackTraceElement createSynthetic(SerializedLambda lambda, String methodName, StackTraceElement userCode) {
			Type type = Type.getObjectType(lambda.getCapturingClass());
			StackTraceElement synthetic = new StackTraceElement(null, userCode.getModuleName(), userCode.getModuleVersion(),
					type.getClassName(), methodName, ClassUtils.getShortName(type.getClassName()) + ".java",
					errors.iterator().next().line);
			return synthetic;
		}
	}

	private static StackTraceElement[] filterStackTrace(StackTraceElement[] stackTrace) {

		int filterIndex = findEntryPoint(stackTrace);

		if (filterIndex != -1) {

			StackTraceElement[] copy = new StackTraceElement[(stackTrace.length - filterIndex)];
			System.arraycopy(stackTrace, filterIndex, copy, 0, stackTrace.length - filterIndex);
			return copy;
		}

		return stackTrace;
	}

	private static int findEntryPoint(StackTraceElement[] stackTrace) {

		int filterIndex = -1;

		for (int i = 0; i < stackTrace.length; i++) {

			if (stackTrace[i].getClassName().equals(TypedPropertyPaths.class.getName())
					|| stackTrace[i].getClassName().equals(TypedPropertyPath.class.getName())
					|| stackTrace[i].getClassName().equals(ComposedPropertyPath.class.getName())
					|| stackTrace[i].getClassName().equals(PropertyPath.class.getName())) {
				filterIndex = i;
			}
		}

		return filterIndex;
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

	record ComposedPropertyPath<T, M, R>(TypedPropertyPath<T, M> first, TypedPropertyPath<M, R> second,
			String dotPath) implements TypedPropertyPath<T, R> {

		ComposedPropertyPath(TypedPropertyPath<T, M> first, TypedPropertyPath<M, R> second) {
			this(first, second, first.toDotPath() + "." + second.toDotPath());
		}

		@Override
		public @Nullable R get(T obj) {
			M intermediate = first.get(obj);
			return intermediate != null ? second.get(intermediate) : null;
		}

		@Override
		public TypeInformation<?> getOwningType() {
			return first.getOwningType();
		}

		@Override
		public String getSegment() {
			return first().getSegment();
		}

		@Override
		public PropertyPath getLeafProperty() {
			return second.getLeafProperty();
		}

		@Override
		public TypeInformation<?> getTypeInformation() {
			return first.getTypeInformation();
		}

		@Override
		public PropertyPath next() {
			return second;
		}

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public String toDotPath() {
			return dotPath;
		}

		@Override
		public Stream<PropertyPath> stream() {
			return second.stream();
		}

		@Override
		public Iterator<PropertyPath> iterator() {
			return second.iterator();
		}

		@Override
		public String toString() {
			return getOwningType().getType().getSimpleName() + "." + toDotPath();
		}
	}
}
