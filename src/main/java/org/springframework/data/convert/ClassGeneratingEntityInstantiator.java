/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.convert;

import static org.springframework.asm.Opcodes.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.MappingInstantiationException;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An {@link EntityInstantiator} that can generate byte code to speed-up dynamic object instantiation. Uses the
 * {@link PersistentEntity}'s {@link PreferredConstructor} to instantiate an instance of the entity by dynamically
 * generating factory methods with appropriate constructor invocations via ASM. If we cannot generate byte code for a
 * type, we gracefully fall-back to the {@link ReflectionEntityInstantiator}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Phillip Webb
 * @since 1.11
 */
public class ClassGeneratingEntityInstantiator implements EntityInstantiator {

	private final ObjectInstantiatorClassGenerator generator;

	private volatile Map<TypeInformation<?>, EntityInstantiator> entityInstantiators = new HashMap<TypeInformation<?>, EntityInstantiator>(
			32);

	/**
	 * Creates a new {@link ClassGeneratingEntityInstantiator}.
	 */
	public ClassGeneratingEntityInstantiator() {
		this.generator = new ObjectInstantiatorClassGenerator();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.EntityInstantiator#createInstance(org.springframework.data.mapping.PersistentEntity, org.springframework.data.mapping.model.ParameterValueProvider)
	 */
	@Override
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		EntityInstantiator instantiator = this.entityInstantiators.get(entity.getTypeInformation());

		if (instantiator == null) {
			instantiator = potentiallyCreateAndRegisterEntityInstantiator(entity);
		}

		return instantiator.createInstance(entity, provider);
	}

	/**
	 * @param entity
	 * @return
	 */
	private synchronized EntityInstantiator potentiallyCreateAndRegisterEntityInstantiator(
			PersistentEntity<?, ?> entity) {

		Map<TypeInformation<?>, EntityInstantiator> map = this.entityInstantiators;
		EntityInstantiator instantiator = map.get(entity.getTypeInformation());

		if (instantiator != null) {
			return instantiator;
		}

		instantiator = createEntityInstantiator(entity);

		map = new HashMap<TypeInformation<?>, EntityInstantiator>(map);
		map.put(entity.getTypeInformation(), instantiator);

		this.entityInstantiators = map;

		return instantiator;
	}

	/**
	 * @param entity
	 * @return
	 */
	private EntityInstantiator createEntityInstantiator(PersistentEntity<?, ?> entity) {

		if (shouldUseReflectionEntityInstantiator(entity)) {
			return ReflectionEntityInstantiator.INSTANCE;
		}

		try {
			return new EntityInstantiatorAdapter(createObjectInstantiator(entity));
		} catch (Throwable ex) {
			return ReflectionEntityInstantiator.INSTANCE;
		}
	}

	/**
	 * @param entity
	 * @return
	 */
	private boolean shouldUseReflectionEntityInstantiator(PersistentEntity<?, ?> entity) {

		Class<?> type = entity.getType();

		if (type.isInterface() //
				|| type.isArray() //
				|| !Modifier.isPublic(type.getModifiers()) //
				|| (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())) //
				|| ClassUtils.isCglibProxyClass(type)) { //
			return true;
		}

		return entity.getPersistenceConstructor()//
				.map(PreferredConstructor::getConstructor)//
				.map(Constructor::getModifiers)//
				.map(modifier -> !Modifier.isPublic(modifier)).orElse(false);
	}

	/**
	 * Creates a dynamically generated {@link ObjectInstantiator} for the given {@link InstantiatorKey}. There will always
	 * be exactly one {@link ObjectInstantiator} instance per {@link PersistentEntity}.
	 * <p>
	 *
	 * @param entity
	 * @return
	 */
	private ObjectInstantiator createObjectInstantiator(PersistentEntity<?, ?> entity) {

		try {
			return (ObjectInstantiator) this.generator.generateCustomInstantiatorClass(entity).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adapter to forward an invocation of the {@link EntityInstantiator} API to an {@link ObjectInstantiator}.
	 *
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 */
	private static class EntityInstantiatorAdapter implements EntityInstantiator {

		private static final Object[] EMPTY_ARRAY = new Object[0];

		private final ObjectInstantiator instantiator;

		/**
		 * Creates a new {@link EntityInstantiatorAdapter} for the given {@link ObjectInstantiator}.
		 *
		 * @param instantiator must not be {@literal null}.
		 */
		public EntityInstantiatorAdapter(ObjectInstantiator instantiator) {
			this.instantiator = instantiator;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.convert.EntityInstantiator#createInstance(org.springframework.data.mapping.PersistentEntity, org.springframework.data.mapping.model.ParameterValueProvider)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
				ParameterValueProvider<P> provider) {

			Object[] params = extractInvocationArguments(entity.getPersistenceConstructor(), provider);

			try {
				return (T) instantiator.newInstance(params);
			} catch (Exception e) {
				throw new MappingInstantiationException(Optional.of(entity), Arrays.asList(params), e);
			}
		}

		/**
		 * Extracts the arguments required to invoce the given constructor from the given {@link ParameterValueProvider}.
		 *
		 * @param constructor can be {@literal null}.
		 * @param provider can be {@literal null}.
		 * @return
		 */
		private <P extends PersistentProperty<P>, T> Object[] extractInvocationArguments(
				Optional<? extends PreferredConstructor<? extends T, P>> constructor, ParameterValueProvider<P> provider) {

			return constructor.map(it -> {

				if (provider == null || !it.hasParameters()) {
					return EMPTY_ARRAY;
				}

				return it.getParameters().stream()//
						.map(parameter -> provider.getParameterValue(parameter))//
						.toArray();

			}).orElse(EMPTY_ARRAY);
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	private static interface ObjectInstantiator {

		Object newInstance(Object... args);
	}

	/**
	 * Generates a new {@link ObjectInstantiator} class for the given custom class.
	 * <p>
	 * This code generator will generate a custom factory class implementing the {@link ObjectInstantiator} interface for
	 * every publicly accessed constructor variant.
	 * <p>
	 * Given a class {@code ObjCtor1ParamString} like:
	 *
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	public class ObjCtor1ParamString extends ObjCtorNoArgs {
	 *
	 * 		public final String param1;
	 *
	 * 		public ObjCtor1ParamString(String param1) {
	 * 			this.param1 = param1;
	 * 		}
	 * 	}
	 * }
	 * </pre>
	 *
	 * The following factory class {@code ObjCtor1ParamString_Instantiator_asdf} is generated:
	 *
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	public class ObjCtor1ParamString_Instantiator_asdf implements ObjectInstantiator {
	 *
	 * 		public Object newInstance(Object... args) {
	 * 			return new ObjCtor1ParamString((String) args[0]);
	 * 		}
	 * 	}
	 * }
	 * </pre>
	 *
	 * @author Thomas Darimont
	 */
	static class ObjectInstantiatorClassGenerator {

		private static final String INIT = "<init>";
		private static final String TAG = "_Instantiator_";
		private static final String JAVA_LANG_OBJECT = "java/lang/Object";
		private static final String CREATE_METHOD_NAME = "newInstance";

		private static final String[] IMPLEMENTED_INTERFACES = new String[] {
				Type.getInternalName(ObjectInstantiator.class) };

		private final ByteArrayClassLoader classLoader;

		private ObjectInstantiatorClassGenerator() {

			this.classLoader = AccessController.doPrivileged(
					(PrivilegedAction<ByteArrayClassLoader>) () -> new ByteArrayClassLoader(ClassUtils.getDefaultClassLoader()));
		}

		/**
		 * Generate a new class for the given {@link InstantiatorKey}.
		 *
		 * @param key
		 * @return
		 */
		public Class<?> generateCustomInstantiatorClass(PersistentEntity<?, ?> entity) {

			String className = generateClassName(entity);
			byte[] bytecode = generateBytecode(className, entity);

			return classLoader.loadClass(className, bytecode);
		}

		/**
		 * @param key
		 * @return
		 */
		private String generateClassName(PersistentEntity<?, ?> entity) {
			return entity.getType().getName() + TAG + Integer.toString(entity.hashCode(), 36);
		}

		/**
		 * Generate a new class for the given {@link InstantiatorKey}.
		 *
		 * @param key
		 * @return
		 */
		public byte[] generateBytecode(String internalClassName, PersistentEntity<?, ?> entity) {

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			cw.visit(Opcodes.V1_6, ACC_PUBLIC + ACC_SUPER, internalClassName.replace('.', '/'), null, JAVA_LANG_OBJECT,
					IMPLEMENTED_INTERFACES);

			visitDefaultConstructor(cw);

			visitCreateMethod(cw, entity);

			cw.visitEnd();

			return cw.toByteArray();
		}

		private void visitDefaultConstructor(ClassWriter cw) {

			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, INIT, "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, INIT, "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(0, 0); // (0, 0) = computed via ClassWriter.COMPUTE_MAXS
			mv.visitEnd();
		}

		/**
		 * Inserts the bytecode definition for the create method for the given {@link PersistentEntity}.
		 *
		 * @param cw
		 * @param entity
		 */
		private void visitCreateMethod(ClassWriter cw, PersistentEntity<?, ?> entity) {

			String entityTypeResourcePath = Type.getInternalName(entity.getType());

			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, CREATE_METHOD_NAME,
					"([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, entityTypeResourcePath);
			mv.visitInsn(DUP);

			entity.getPersistenceConstructor().ifPresent(constructor -> {

				Constructor<?> ctor = constructor.getConstructor();
				Class<?>[] parameterTypes = ctor.getParameterTypes();

				for (int i = 0; i < parameterTypes.length; i++) {

					mv.visitVarInsn(ALOAD, 1);

					visitArrayIndex(mv, i);

					mv.visitInsn(AALOAD);

					if (parameterTypes[i].isPrimitive()) {
						insertUnboxInsns(mv, Type.getType(parameterTypes[i]).toString().charAt(0), "");
					} else {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(parameterTypes[i]));
					}
				}

				mv.visitMethodInsn(INVOKESPECIAL, entityTypeResourcePath, INIT, Type.getConstructorDescriptor(ctor), false);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(0, 0); // (0, 0) = computed via ClassWriter.COMPUTE_MAXS
				mv.visitEnd();
			});
		}

		/**
		 * Insert an appropriate value on the stack for the given index value {@code idx}.
		 *
		 * @param mv
		 * @param idx
		 */
		private static void visitArrayIndex(MethodVisitor mv, int idx) {

			if (idx >= 0 && idx < 6) {
				mv.visitInsn(ICONST_0 + idx);
				return;
			}

			mv.visitLdcInsn(idx);
		}

		/**
		 * Insert any necessary cast and value call to convert from a boxed type to a primitive value.
		 * <p>
		 * Taken from Spring Expression 4.1.2 {@code org.springframework.expression.spel.CodeFlow#insertUnboxInsns}.
		 *
		 * @param mv the method visitor into which instructions should be inserted
		 * @param ch the primitive type desired as output
		 * @param stackDescriptor the descriptor of the type on top of the stack
		 */
		private static void insertUnboxInsns(MethodVisitor mv, char ch, String stackDescriptor) {

			switch (ch) {
				case 'Z':
					if (!stackDescriptor.equals("Ljava/lang/Boolean")) {
						mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
					break;
				case 'B':
					if (!stackDescriptor.equals("Ljava/lang/Byte")) {
						mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
					break;
				case 'C':
					if (!stackDescriptor.equals("Ljava/lang/Character")) {
						mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
					break;
				case 'D':
					if (!stackDescriptor.equals("Ljava/lang/Double")) {
						mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
					break;
				case 'F':
					if (!stackDescriptor.equals("Ljava/lang/Float")) {
						mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
					break;
				case 'I':
					if (!stackDescriptor.equals("Ljava/lang/Integer")) {
						mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
					break;
				case 'J':
					if (!stackDescriptor.equals("Ljava/lang/Long")) {
						mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
					break;
				case 'S':
					if (!stackDescriptor.equals("Ljava/lang/Short")) {
						mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
					break;
				default:
					throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + ch + "'");
			}
		}

		/**
		 * A {@link ClassLoader} that can load classes from {@code byte[]} representations.
		 *
		 * @author Thomas Darimont
		 */
		private class ByteArrayClassLoader extends ClassLoader {

			public ByteArrayClassLoader(ClassLoader parent) {
				super(parent);
			}

			/**
			 * Tries to load a class given {@code byte[]}.
			 *
			 * @param name must not be {@literal null}
			 * @param bytes must not be {@literal null}
			 * @return
			 */
			public Class<?> loadClass(String name, byte[] bytes) {

				Assert.notNull(name, "name must not be null");
				Assert.notNull(bytes, "bytes must not be null");

				try {
					Class<?> clazz = findClass(name);
					if (clazz != null) {
						return clazz;
					}
				} catch (ClassNotFoundException ignore) {}

				return defineClass(name, bytes, 0, bytes.length);
			}
		}
	}
}
