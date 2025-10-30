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
package org.springframework.data.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility to parse Single Abstract Method (SAM) method references and lambdas to extract the owning type and the member
 * that is being accessed from it.
 *
 * @author Mark Paluch
 */
class SamParser {

	/**
	 * System property that instructs Spring Data to filter stack traces of exceptions thrown during SAM parsing.
	 */
	public static final String FILTER_STACK_TRACE = "spring.date.sam-parser.filter-stacktrace";

	/**
	 * System property that instructs Spring Data to include suppressed exceptions during SAM parsing.
	 */
	public static final String INCLUDE_SUPPRESSED_EXCEPTIONS = "spring.date.sam-parser.include-suppressed-exceptions";

	private static final Log LOGGER = LogFactory.getLog(SamParser.class);

	private static final boolean filterStackTrace = isEnabled(FILTER_STACK_TRACE, true);
	private static final boolean includeSuppressedExceptions = isEnabled(INCLUDE_SUPPRESSED_EXCEPTIONS, false);

	private final List<Class<?>> entryPoints;

	private static boolean isEnabled(String property, boolean defaultValue) {

		String value = SpringProperties.getProperty(property);
		if (StringUtils.hasText(value)) {
			return Boolean.parseBoolean(value);
		}

		return defaultValue;
	}

	SamParser(Class<?>... entryPoints) {
		this.entryPoints = Arrays.asList(entryPoints);
	}

	public MemberReference parse(ClassLoader classLoader, Object lambdaObject) {

		try {
			// Use serialization to extract method reference info
			SerializedLambda lambda = serialize(lambdaObject);

			if (lambda.getImplMethodKind() == MethodHandleInfo.REF_newInvokeSpecial
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeSpecial) {
				InvalidDataAccessApiUsageException e = new InvalidDataAccessApiUsageException(
						"Method reference must not be a constructor call");

				if (filterStackTrace) {
					e.setStackTrace(filterStackTrace(e.getStackTrace(), null));
				}
				throw e;
			}

			// method handle
			if ((lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeInterface)
					&& !lambda.getImplMethodName().startsWith("lambda$")) {
				return MethodInformation.ofInvokeVirtual(classLoader, lambda);
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
			throw new InvalidDataAccessApiUsageException("Cannot extract property path", e);
		}

		throw new InvalidDataAccessApiUsageException("Cannot extract property path from: " + lambdaObject
				+ ". The given value is not a Lambda and not a Method Reference.");
	}

	private static SerializedLambda serialize(Object lambda) {

		try {
			Method method = lambda.getClass().getDeclaredMethod("writeReplace");
			method.setAccessible(true);
			return (SerializedLambda) method.invoke(lambda);
		} catch (ReflectiveOperationException e) {
			throw new InvalidDataAccessApiUsageException(
					"Not a lambda: " + (lambda instanceof Enum<?> ? lambda.getClass().getName() + "#" + lambda : lambda), e);
		}
	}

	class LambdaClassVisitor extends ClassVisitor {

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

		public MemberReference getPropertyPathInformation(SerializedLambda lambda) {
			return methodVisitor.resolve(lambda);
		}
	}

	class LambdaMethodVisitor extends MethodVisitor {

		private static final Pattern HEX_PATTERN = Pattern.compile("[0-9a-f]+");

		private static final Set<String> BOXING_TYPES = Set.of(Type.getInternalName(Integer.class),
				Type.getInternalName(Long.class), Type.getInternalName(Short.class), Type.getInternalName(Byte.class),
				Type.getInternalName(Float.class), Type.getInternalName(Double.class), Type.getInternalName(Character.class),
				Type.getInternalName(Boolean.class));

		private static final String BOXING_METHOD = "valueOf";

		private final ClassLoader classLoader;
		private final Type owningType;
		private int line;
		List<MemberReference> memberReferences = new ArrayList<>();
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
					"Lambda expressions may only contain method calls to getters, record components, or field access", null));
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {

			if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) {
				errors.add(new ParseError(line, "Setting a field not allowed", null));
				return;
			}

			Type fieldType = Type.getType(descriptor);

			try {
				this.memberReferences.add(FieldInformation.create(classLoader, owningType, name, fieldType));
			} catch (ReflectiveOperationException e) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Failed to resolve field '%s.%s'".formatted(owner, name), e);
				}
				errors.add(new ParseError(line, e.getMessage()));
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

			if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
				errors.add(new ParseError(line, "Lambda must not invoke constructors", null));
				return;
			}

			int count = Type.getArgumentCount(descriptor);

			if (count != 0) {

				if (BOXING_TYPES.contains(owner) && name.equals(BOXING_METHOD)) {
					return;
				}

				errors.add(new ParseError(line, "Method references must invoke no-arg methods only"));
				return;
			}

			Type ownerType = Type.getObjectType(owner);
			if (!ownerType.equals(this.owningType)) {

				Type[] argumentTypes = Type.getArgumentTypes(descriptor);
				String signature = Arrays.stream(argumentTypes).map(Type::getClassName).collect(Collectors.joining(", "));
				errors.add(new ParseError(line,
						"Cannot derive a method reference from method invocation '%s(%s)' on a different type than the owning one.%nExpected owning type: '%s', but was: '%s'"
								.formatted(name, signature, this.owningType.getClassName(), ownerType.getClassName())));
				return;
			}

			try {
				this.memberReferences.add(MethodInformation.ofInvokeVirtual(classLoader, owningType, name));
			} catch (ReflectiveOperationException e) {

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Failed to resolve method '%s.%s'".formatted(owner, name), e);
				}
				errors.add(new ParseError(line, e.getMessage()));
			}
		}

		public MemberReference resolve(SerializedLambda lambda) {

			// TODO composite path information
			if (errors.isEmpty()) {

				if (memberReferences.isEmpty()) {
					throw new InvalidDataAccessApiUsageException("There is no method or field access");
				}

				return memberReferences.get(memberReferences.size() - 1);
			}

			if (lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual) {

				String methodName = getDeclaringMethodName(lambda);

				InvalidDataAccessApiUsageException e = new InvalidDataAccessApiUsageException(
						"Cannot resolve property path%n%nError%s:%n".formatted(errors.size() > 1 ? "s" : "") + errors.stream()
								.map(ParseError::message).map(args -> formatMessage(args)).collect(Collectors.joining()));

				if (includeSuppressedExceptions) {
					for (ParseError error : errors) {
						if (error.e != null) {
							e.addSuppressed(error.e);
						}
					}
				}

				if (filterStackTrace) {
					e.setStackTrace(
							filterStackTrace(e.getStackTrace(), userCode -> createSynthetic(lambda, methodName, userCode)));
				}

				throw e;
			}

			throw new InvalidDataAccessApiUsageException("Error resolving " + errors);
		}

		private static String formatMessage(String args) {

			String[] split = args.split("%n".formatted());
			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < split.length; i++) {

				if (i == 0) {
					builder.append("\t* %s%n".formatted(split[i]));
				} else {
					builder.append("\t  %s%n".formatted(split[i]));
				}
			}

			return builder.toString();
		}

		private static String getDeclaringMethodName(SerializedLambda lambda) {

			String methodName = lambda.getImplMethodName();
			if (methodName.startsWith("lambda$")) {
				methodName = methodName.substring("lambda$".length());

				if (methodName.contains("$")) {
					methodName = methodName.substring(0, methodName.lastIndexOf('$'));
				}

				if (methodName.contains("$")) {
					String probe = methodName.substring(methodName.lastIndexOf('$') + 1);
					if (HEX_PATTERN.matcher(probe).matches()) {
						methodName = methodName.substring(0, methodName.lastIndexOf('$'));
					}
				}
			}
			return methodName;
		}

		private StackTraceElement createSynthetic(SerializedLambda lambda, String methodName, StackTraceElement userCode) {

			Type type = Type.getObjectType(lambda.getCapturingClass());

			return new StackTraceElement(null, userCode.getModuleName(), userCode.getModuleVersion(), type.getClassName(),
					methodName, ClassUtils.getShortName(type.getClassName()) + ".java", errors.iterator().next().line);
		}
	}

	private StackTraceElement[] filterStackTrace(StackTraceElement[] stackTrace,
			@Nullable Function<StackTraceElement, StackTraceElement> syntheticSupplier) {

		int filterIndex = findEntryPoint(stackTrace);

		if (filterIndex != -1) {

			int offset = syntheticSupplier == null ? 0 : 1;

			StackTraceElement[] copy = new StackTraceElement[(stackTrace.length - filterIndex) + offset];
			System.arraycopy(stackTrace, filterIndex, copy, offset, stackTrace.length - filterIndex);

			if (syntheticSupplier != null) {
				StackTraceElement userCode = copy[1];
				StackTraceElement synthetic = syntheticSupplier.apply(userCode);
				copy[0] = synthetic;
			}
			return copy;
		}

		return stackTrace;
	}

	private int findEntryPoint(StackTraceElement[] stackTrace) {

		int filterIndex = -1;

		for (int i = 0; i < stackTrace.length; i++) {

			if (matchesEntrypoint(stackTrace[i].getClassName())) {
				filterIndex = i;
			}
		}

		return filterIndex;
	}

	private boolean matchesEntrypoint(String className) {

		if (className.equals(getClass().getName())) {
			return true;
		}

		for (Class<?> entryPoint : entryPoints) {
			if (className.equals(entryPoint.getName())) {
				return true;
			}
		}

		return false;
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

	sealed interface MemberReference permits FieldInformation, MethodInformation {

		Class<?> getOwner();

		Member getMember();

		ResolvableType getType();
	}

	/**
	 * Value object holding information about a property path segment.
	 *
	 * @param owner
	 */
	record FieldInformation(Class<?> owner, Field field) implements MemberReference {

		public static FieldInformation create(ClassLoader classLoader, Type ownerType, String name, Type fieldType)
				throws ClassNotFoundException {

			Class<?> owner = ClassUtils.forName(ownerType.getClassName(), classLoader);
			Class<?> type = ClassUtils.forName(fieldType.getClassName(), classLoader);

			return create(owner, name, type);
		}

		private static FieldInformation create(Class<?> owner, String fieldName, Class<?> fieldType) {

			Field field = ReflectionUtils.findField(owner, fieldName, fieldType);
			if (field == null) {
				throw new IllegalArgumentException("Field %s.%s() not found".formatted(owner.getName(), fieldName));
			}

			return new FieldInformation(owner, field);
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

	/**
	 * Value object holding information about a method invocation.
	 */
	record MethodInformation(Class<?> owner, Method method) implements MemberReference {

		public static MethodInformation ofInvokeVirtual(ClassLoader classLoader, SerializedLambda lambda)
				throws ClassNotFoundException {
			return ofInvokeVirtual(classLoader, Type.getObjectType(lambda.getImplClass()), lambda.getImplMethodName());
		}

		public static MethodInformation ofInvokeVirtual(ClassLoader classLoader, Type ownerType, String name)
				throws ClassNotFoundException {
			Class<?> owner = ClassUtils.forName(ownerType.getClassName(), classLoader);
			return ofInvokeVirtual(owner, name);
		}

		public static MethodInformation ofInvokeVirtual(Class<?> owner, String methodName) {

			Method method = ReflectionUtils.findMethod(owner, methodName);
			if (method == null) {
				throw new IllegalArgumentException("Method %s.%s() not found".formatted(owner.getName(), methodName));
			}
			return new MethodInformation(owner, method);
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
}
