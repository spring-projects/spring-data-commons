/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.mapping.model;

import static org.springframework.asm.Opcodes.*;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Modifier;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;

/**
 * Utility methods used for ASM-based class generation during runtime.
 *
 * @author Mark Paluch
 */
@UtilityClass
class BytecodeUtil {

	/**
	 * Returns the appropriate autoboxing type.
	 */
	static Class<?> autoboxType(Class<?> unboxed) {

		if (unboxed.equals(Boolean.TYPE)) {
			return Boolean.class;
		}

		if (unboxed.equals(Byte.TYPE)) {
			return Byte.class;
		}

		if (unboxed.equals(Character.TYPE)) {
			return Character.class;
		}

		if (unboxed.equals(Double.TYPE)) {
			return Double.class;
		}

		if (unboxed.equals(Float.TYPE)) {
			return Float.class;
		}

		if (unboxed.equals(Integer.TYPE)) {
			return Integer.class;
		}

		if (unboxed.equals(Long.TYPE)) {
			return Long.class;
		}

		if (unboxed.equals(Short.TYPE)) {
			return Short.class;
		}

		if (unboxed.equals(Void.TYPE)) {
			return Void.class;
		}

		return unboxed;
	}

	/**
	 * Auto-box/Auto-unbox primitives to object and vice versa.
	 *
	 * @param in the input type
	 * @param out the expected output type
	 * @param visitor
	 */
	static void autoboxIfNeeded(Class<?> in, Class<?> out, MethodVisitor visitor) {

		if (in.equals(Boolean.class) && out.equals(Boolean.TYPE)) {
			visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
		}

		if (in.equals(Boolean.TYPE) && out.equals(Boolean.class)) {
			visitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
		}

		if (in.equals(Byte.class) && out.equals(Byte.TYPE)) {
			visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
		}

		if (in.equals(Byte.TYPE) && out.equals(Byte.class)) {
			visitor.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
		}

		if (in.equals(Character.class) && out.equals(Character.TYPE)) {
			visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
		}

		if (in.equals(Character.TYPE) && out.equals(Character.class)) {
			visitor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
		}

		if (in.equals(Double.class) && out.equals(Double.TYPE)) {
			visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
		}

		if (in.equals(Double.TYPE) && out.equals(Double.class)) {
			visitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
		}

		if (in.equals(Float.class) && out.equals(Float.TYPE)) {
			visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
		}

		if (in.equals(Float.TYPE) && out.equals(Float.class)) {
			visitor.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
		}

		if (in.equals(Integer.class) && out.equals(Integer.TYPE)) {
			visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
		}

		if (in.equals(Integer.TYPE) && out.equals(Integer.class)) {
			visitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
		}

		if (in.equals(Long.class) && out.equals(Long.TYPE)) {
			visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
		}

		if (in.equals(Long.TYPE) && out.equals(Long.class)) {
			visitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
		}

		if (in.equals(Short.class) && out.equals(Short.TYPE)) {
			visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
		}

		if (in.equals(Short.TYPE) && out.equals(Short.class)) {
			visitor.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
		}
	}

	/**
	 * Checks whether the class is accessible by inspecting modifiers (i.e. whether the class is {@code private}).
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 * @see Modifier#isPrivate(int)
	 */
	static boolean isAccessible(Class<?> type) {
		return isAccessible(type.getModifiers());
	}

	/**
	 * Checks whether the class is accessible by inspecting modifiers (i.e. whether the class is {@code private}).
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 * @see Modifier#isPrivate(int)
	 */
	static boolean isAccessible(int modifiers) {
		return !Modifier.isPrivate(modifiers);
	}

	/**
	 * Checks whether the modifiers express {@literal default} (not
	 * {@literal private}/{@literal protected}/{@literal public}).
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 * @see Modifier#isPrivate(int)
	 */
	static boolean isDefault(int modifiers) {
		return !(Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPublic(modifiers));
	}

	/**
	 * Create a reference type name in the form of {@literal Ljava/lang/Object;}.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	static String referenceName(Class<?> type) {
		return type.isArray() ? Type.getInternalName(type) : referenceName(Type.getInternalName(type));
	}

	static String referenceName(String internalTypeName) {
		return String.format("L%s;", internalTypeName);
	}

	/**
	 * Returns the signature type for a {@link Class} including primitives.
	 *
	 * @param type must not be {@literal null}
	 * @return
	 */
	static String signatureTypeName(Class<?> type) {

		if (type.equals(Boolean.TYPE)) {
			return "Z";
		}

		if (type.equals(Byte.TYPE)) {
			return "B";
		}

		if (type.equals(Character.TYPE)) {
			return "C";
		}

		if (type.equals(Double.TYPE)) {
			return "D";
		}

		if (type.equals(Float.TYPE)) {
			return "F";
		}

		if (type.equals(Integer.TYPE)) {
			return "I";
		}

		if (type.equals(Long.TYPE)) {
			return "J";
		}

		if (type.equals(Short.TYPE)) {
			return "S";
		}

		if (type.equals(Void.TYPE)) {
			return "V";
		}

		return referenceName(type);
	}
}
