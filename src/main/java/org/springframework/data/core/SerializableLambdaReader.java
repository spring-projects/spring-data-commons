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

import kotlin.jvm.JvmClassMappingKt;
import kotlin.jvm.internal.PropertyReference;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
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
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.KotlinDetector;
import org.springframework.core.SpringProperties;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.MemberDescriptor.KPropertyPathDescriptor;
import org.springframework.data.core.MemberDescriptor.KPropertyReferenceDescriptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Reader to extract references to fields and methods from serializable lambda expressions and method references using
 * lambda serialization and bytecode analysis. Allows introspection of property access patterns expressed through
 * functional interfaces without executing the lambda's behavior. Their declarative nature makes method references in
 * general and a constrained subset of lambda expressions suitable to declare property references in the sense of Java
 * Beans properties. Although lambdas and method references are primarily used in a declarative functional programming
 * models to express behavior, lambda serialization allows for further introspection such as parsing the lambda method
 * bytecode for property or member access information and not taking the functional behavior into account.
 * <p>
 * The actual interface is not constrained by a base type, however the object must:
 * <ul>
 * <li>Implement a functional Java interface</li>
 * <li>Must be the top-level lambda (i.e. not wrapped or a functional composition)</li>
 * <li>Implement Serializable (either through the actual interface or through inference)</li>
 * <li>Declare a single method to be implemented</li>
 * <li>Accept a single method argument and return a value</li>
 * </ul>
 * Ideally, the interface has a similar format to {@link Function}, for example:
 *
 * <pre class="code">
 * interface XtoYFunction<X, Y> (optional: extends Serializable) {
 *   Y &lt;method-name&gt;(X someArgument);
 * }
 * </pre>
 * <p>
 * <strong>Supported patterns</strong>
 * <ul>
 * <li>Method references: {@code Person::getName}</li>
 * <li>Property access lambdas: {@code person -> person.getName()}</li>
 * <li>Field access lambdas: {@code person -> person.name}</li>
 * </ul>
 * <strong>Unsupported patterns</strong>
 * <ul>
 * <li>Constructor references: {@code Person::new}</li>
 * <li>Methods with arguments: {@code person -> person.setAge(25)}</li>
 * <li>Lambda expressions that do more than property access, e.g. {@code person -> { person.setAge(25); return
 * person.getName(); }}</li>
 * <li>Arithmetic operations, arbitrary calls</li>
 * <li>Functional composition: {@code Function.andThen(...)}</li>
 * </ul>
 *
 * @author Mark Paluch
 * @since 4.1
 */
class SerializableLambdaReader {

	/**
	 * System property that instructs Spring Data to filter stack traces of exceptions thrown during SAM parsing.
	 */
	public static final String FILTER_STACK_TRACE = "spring.data.lambda-reader.filter-stacktrace";

	/**
	 * System property that instructs Spring Data to include suppressed exceptions during SAM parsing.
	 */
	public static final String INCLUDE_SUPPRESSED_EXCEPTIONS = "spring.data.lambda-reader.include-suppressed-exceptions";

	private static final Log LOGGER = LogFactory.getLog(SerializableLambdaReader.class);
	private static final boolean filterStackTrace = isEnabled(FILTER_STACK_TRACE, true);
	private static final boolean includeSuppressedExceptions = isEnabled(INCLUDE_SUPPRESSED_EXCEPTIONS, false);

	private final List<Class<?>> entryPoints;

	SerializableLambdaReader(Class<?>... entryPoints) {
		this.entryPoints = Arrays.asList(entryPoints);
	}

	private static boolean isEnabled(String property, boolean defaultValue) {

		String value = SpringProperties.getProperty(property);
		return StringUtils.hasText(value) ? Boolean.parseBoolean(value) : defaultValue;
	}

	/**
	 * Read the given lambda object and extract a reference to a {@link Member} such as a field or method.
	 * <p>
	 * Ideally used with an interface resembling {@link java.util.function.Function}.
	 *
	 * @param lambdaObject the actual lambda object, must be {@link java.io.Serializable}.
	 * @return the member reference.
	 * @throws InvalidDataAccessApiUsageException if the lambda object does not contain a valid property reference or hits
	 *           any of the mentioned limitations.
	 */
	public MemberDescriptor read(Object lambdaObject) {

		SerializedLambda lambda = serialize(lambdaObject);

		if (isKotlinPropertyReference(lambda)) {
			return KotlinDelegate.read(lambda);
		}

		assertNotConstructor(lambda);

		try {

			// method reference
			if ((lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeInterface)
					&& !lambda.getImplMethodName().startsWith("lambda$")) {
				return MemberDescriptor.ofMethodReference(lambdaObject.getClass().getClassLoader(), lambda);
			}

			// all other lambda forms
			if (lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual) {
				return getMemberDescriptor(lambdaObject, lambda);
			}
		} catch (ReflectiveOperationException | IOException e) {
			throw new InvalidDataAccessApiUsageException("Cannot extract method or field", e);
		}

		throw new InvalidDataAccessApiUsageException("Cannot extract method or field from: " + lambdaObject
				+ ". The given value is not a lambda or method reference.");
	}

	private void assertNotConstructor(SerializedLambda lambda) {

		if (lambda.getImplMethodKind() == MethodHandleInfo.REF_newInvokeSpecial
				|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeSpecial) {

			InvalidDataAccessApiUsageException e = new InvalidDataAccessApiUsageException(
					"Method reference must not be a constructor call");

			if (filterStackTrace) {
				e.setStackTrace(filterStackTrace(e.getStackTrace(), null));
			}
			throw e;
		}
	}

	private MemberDescriptor getMemberDescriptor(Object lambdaObject, SerializedLambda lambda) throws IOException {

		String implClass = Type.getObjectType(lambda.getImplClass()).getClassName();
		Type owningType = Type.getArgumentTypes(lambda.getImplMethodSignature())[0];
		String classFileName = implClass.replace('.', '/') + ".class";
		InputStream classFile = ClassLoader.getSystemResourceAsStream(classFileName);

		if (classFile == null) {
			throw new IllegalStateException("Cannot find class file '%s' for lambda introspection".formatted(classFileName));
		}

		try (classFile) {

			ClassReader cr = new ClassReader(classFile);
			LambdaReadingVisitor classVisitor = new LambdaReadingVisitor(lambdaObject.getClass().getClassLoader(),
					lambda.getImplMethodName(), owningType);
			cr.accept(classVisitor, ClassReader.SKIP_FRAMES);
			return classVisitor.getMemberReference(lambda);
		}
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

	private static boolean isKotlinPropertyReference(SerializedLambda lambda) {

		return KotlinDetector.isKotlinReflectPresent() //
				&& lambda.getCapturedArgCount() == 1 //
				&& lambda.getCapturedArg(0) != null //
				&& KotlinDetector.isKotlinType(lambda.getCapturedArg(0).getClass());
	}

	/**
	 * Delegate to detect and read Kotlin property references.
	 * <p>
	 * Inner class delays loading of Kotlin classes.
	 */
	static class KotlinDelegate {

		public static MemberDescriptor read(SerializedLambda lambda) {

			Object captured = lambda.getCapturedArg(0);

			if (captured instanceof PropertyReference propRef //
					&& propRef.getOwner() instanceof KClass<?> owner //
					&& captured instanceof KProperty1<?, ?> kProperty) {
				return new KPropertyReferenceDescriptor(JvmClassMappingKt.getJavaClass(owner), kProperty);
			}

			if (captured instanceof KPropertyPath<?, ?> propRef) {
				return KPropertyPathDescriptor.create(propRef);
			}

			throw new InvalidDataAccessApiUsageException("Cannot extract MemberDescriptor from: " + lambda);
		}

	}

	class LambdaReadingVisitor extends ClassVisitor {

		private final String implMethodName;
		private final LambdaMethodVisitor methodVisitor;

		public LambdaReadingVisitor(ClassLoader classLoader, String implMethodName, Type owningType) {
			super(SpringAsmInfo.ASM_VERSION);
			this.implMethodName = implMethodName;
			this.methodVisitor = new LambdaMethodVisitor(classLoader, owningType);
		}

		public MemberDescriptor getMemberReference(SerializedLambda lambda) {
			return methodVisitor.resolve(lambda);
		}

		@Override
		public @Nullable MethodVisitor visitMethod(int access, String name, String desc, String signature,
				String[] exceptions) {
			return name.equals(implMethodName) ? methodVisitor : null;
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
		private final List<MemberDescriptor> memberDescriptors = new ArrayList<>();
		private final Set<ReadingError> errors = new LinkedHashSet<>();

		public LambdaMethodVisitor(ClassLoader classLoader, Type owningType) {
			super(SpringAsmInfo.ASM_VERSION);
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

			// we don't care about stack manipulation
			if (opcode >= Opcodes.DUP && opcode <= Opcodes.DUP2_X2) {
				return;
			}

			// no-op
			if (opcode == Opcodes.NOP) {
				return;
			}

			visitLdcInsn("");
		}

		@Override
		public void visitLdcInsn(Object value) {
			errors.add(new ReadingError(line,
					"Code loads a constant. Only method calls to getters, record components, or field access allowed.", null));
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {

			if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) {
				errors.add(new ReadingError(line, String.format("Code attempts to set field '%s'", name), null));
				return;
			}

			Type fieldType = Type.getType(descriptor);

			try {
				this.memberDescriptors
						.add(MemberDescriptor.ofField(classLoader, owningType.getClassName(), name, fieldType.getClassName()));
			} catch (ReflectiveOperationException e) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Failed to resolve field '%s.%s'".formatted(owner, name), e);
				}
				errors.add(new ReadingError(line, e.getMessage()));
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

			if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
				errors.add(new ReadingError(line, "Constructor calls not supported.", null));
				return;
			}

			int count = Type.getArgumentCount(descriptor);

			if (count != 0) {

				if (BOXING_TYPES.contains(owner) && name.equals(BOXING_METHOD)) {
					return;
				}

				errors.add(new ReadingError(line, "Method references must invoke no-arg methods only"));
				return;
			}

			Type ownerType = Type.getObjectType(owner);
			if (!ownerType.equals(this.owningType)) {

				Type[] argumentTypes = Type.getArgumentTypes(descriptor);
				String signature = Arrays.stream(argumentTypes).map(Type::getClassName).collect(Collectors.joining(", "));
				errors.add(new ReadingError(line,
						"Cannot derive method reference from '%s#%s(%s)': Method calls allowed on owning type '%s' only."
								.formatted(ownerType.getClassName(), name, signature, this.owningType.getClassName())));
				return;
			}

			try {
				this.memberDescriptors.add(MemberDescriptor.ofMethod(classLoader, owningType.getClassName(), name));
			} catch (ReflectiveOperationException e) {

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Failed to resolve method '%s.%s'".formatted(owner, name), e);
				}
				errors.add(new ReadingError(line, e.getMessage()));
			}
		}

		/**
		 * Resolve a {@link MemberDescriptor} from a {@link SerializedLambda}.
		 *
		 * @param lambda the lambda to introspect.
		 * @return the resolved member descriptor.
		 */
		public MemberDescriptor resolve(SerializedLambda lambda) {

			// TODO composite path information
			if (errors.isEmpty()) {

				if (memberDescriptors.isEmpty()) {
					throw new InvalidDataAccessApiUsageException("There is no method or field access");
				}

				return memberDescriptors.get(memberDescriptors.size() - 1);
			}

			if (lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic
					|| lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual) {

				String methodName = getDeclaringMethodName(lambda);

				InvalidDataAccessApiUsageException e = new InvalidDataAccessApiUsageException(
						"Cannot resolve property path%n%nError%s:%n".formatted(errors.size() > 1 ? "s" : "") + errors.stream()
								.map(ReadingError::message).map(LambdaMethodVisitor::formatMessage).collect(Collectors.joining()));

				if (includeSuppressedExceptions) {
					for (ReadingError error : errors) {
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
					methodName, ClassUtils.getShortName(type.getClassName()) + ".java", errors.iterator().next().line());
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

	/**
	 * Value object for reading errors.
	 *
	 * @param line
	 * @param message
	 * @param e
	 */
	record ReadingError(int line, String message, @Nullable Exception e) {

		ReadingError(int line, String message) {
			this(line, message, null);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ReadingError that)) {
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
