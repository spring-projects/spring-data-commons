/*
 * Copyright 2014 the original author or authors.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An {@link EntityInstantiator} that can generate byte code to speed-up dynamic object instantiation.
 * <p>
 * Uses the {@link PersistentEntity}'s {@link PreferredConstructor} to instantiate an instance of the entity by
 * dynamically generating factory methods with appropriate constructor invocations via ASM. Since we cannot support
 * every use case with byte code generation we gracefully fall-back to the {@link ReflectionEntityInstantiator}.
 * 
 * @author Thomas Darimont
 * @since 1.10
 */
public enum BytecodeGeneratingEntityInstantiator implements EntityInstantiator {

	INSTANCE;

	private static final Logger LOG = LoggerFactory.getLogger(BytecodeGeneratingEntityInstantiator.class);
	private static final Object[] EMPTY_ARRAY = new Object[0];

	private final ObjectInstantiatorClassGenerator classGenerator = ObjectInstantiatorClassGenerator.INSTANCE;
	private final Object instantiatorsLock = new Object();

	private volatile Map<InstantiatorKey, ObjectInstantiator> instantiators = new HashMap<InstantiatorKey, ObjectInstantiator>(
			32); //
	private final CopyOnWriteArraySet<Class<?>> badInstantiatorsTypes = new CopyOnWriteArraySet<Class<?>>();

	/* (non-Javadoc)
	 * @see org.springframework.data.convert.EntityInstantiator#createInstance(org.springframework.data.mapping.PersistentEntity, org.springframework.data.mapping.model.ParameterValueProvider)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		if (shouldUseReflectiveEntityInstantiator(entity)) {
			return ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider);
		}

		Object[] args = extractInvocationArguments(provider, entity.getPersistenceConstructor());

		try {
			return (T) createInternal(entity, args);
		} catch (CouldNotCreateObjectInstantiatorException ex) {
			return ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider);
		}
	}

	/**
	 * @param provider
	 * @param prefCtor
	 * @return
	 */
	private <P extends PersistentProperty<P>, T> Object[] extractInvocationArguments(ParameterValueProvider<P> provider,
			PreferredConstructor<? extends T, P> prefCtor) {

		if (provider == null || prefCtor == null || !prefCtor.hasParameters()) {
			return EMPTY_ARRAY;
		}

		List<Object> params = new ArrayList<Object>();
		for (Parameter<?, P> parameter : prefCtor.getParameters()) {
			params.add(provider.getParameterValue(parameter));
		}

		return params.toArray();
	}

	/**
	 * Determines whether we should fall back to the {@link ReflectionEntityInstantiator} for the given
	 * {@link PersistentEntity}.
	 * 
	 * @param entity
	 * @param constructor
	 * @return
	 */
	private <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> boolean shouldUseReflectiveEntityInstantiator(
			E entity) {

		Class<? extends T> type = entity.getType();
		if (type.isInterface() //
				|| type.isArray() //
				|| !Modifier.isPublic(type.getModifiers()) //
				|| type.getName().startsWith("java.lang") //
				|| (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())) //
				|| ClassUtils.isCglibProxyClass(type) //
				// beware COWAS.contains(..) takes O(n) time but is thread-safe, we expect a very small list.
				|| badInstantiatorsTypes.contains(entity.getType())) { //
			return true;
		}

		PreferredConstructor<? extends T, P> persistenceConstructor = entity.getPersistenceConstructor();
		if (persistenceConstructor == null || !Modifier.isPublic(persistenceConstructor.getConstructor().getModifiers())) {
			return true;
		}

		return false;
	}

	/**
	 * Crates a new instance for the given {@link PersistentEntity} with the given
	 * 
	 * @param args .
	 * @param entity
	 * @param args
	 * @return
	 */
	private <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> Object createInternal(
			E entity, Object... args) {

		Class<? extends T> type = entity.getType();
		Constructor<? extends T> constructor = entity.getPersistenceConstructor().getConstructor();
		InstantiatorKey key = new InstantiatorKey(type, constructor);

		return getOrCreateInstantiator(key).create(args);
	}

	private ObjectInstantiator getOrCreateInstantiator(InstantiatorKey key) {

		Map<InstantiatorKey, ObjectInstantiator> map = this.instantiators;

		ObjectInstantiator instantiator = map.get(key);

		if (instantiator == null) {
			synchronized (instantiatorsLock) {

				map = this.instantiators;
				instantiator = map.get(key);

				if (instantiator == null) {

					instantiator = createObjectInstantiator(key);

					map = new HashMap<InstantiatorKey, ObjectInstantiator>(map);
					map.put(key, instantiator);

					this.instantiators = map;
				}
			}
		}

		return instantiator;
	}

	/**
	 * Creates a dynamically generated {@link ObjectInstantiator} for the given {@link InstantiatorKey}. There will always
	 * be exactly one {@link ObjectInstantiator} instance per {@link InstantiatorKey}.
	 * <p>
	 * Transparently handles the case when an {@link ObjectInstantiator} for the type of the given {@link InstantiatorKey}
	 * couldn't be created. Further attempts to create an instance of this type will be routed to
	 * {@link ReflectionEntityInstantiator} as well.
	 * 
	 * @param key
	 * @return
	 * @throws CouldNotCreateObjectInstantiatorException if the {@link ObjectInstantiator} couldn't be created.
	 */
	private ObjectInstantiator createObjectInstantiator(InstantiatorKey key)
			throws CouldNotCreateObjectInstantiatorException {

		try {
			return (ObjectInstantiator) classGenerator.generateCustomInstantiatorClass(key).newInstance();
		} catch (Exception e) {

			if (LOG.isWarnEnabled()) {
				LOG.warn(
						"Couldn't create a custom ObjectInstantiator for {}. We'll continue to use an ReflectiveEntityInstantiator for this type now. Problem was: {}",
						key, e.getMessage());
			}
			badInstantiatorsTypes.add(key.getType());

			throw new CouldNotCreateObjectInstantiatorException(e);
		}
	}

	/**
	 * A key that forms a unique tuple for a type and constructor combination.
	 * <p>
	 * 
	 * @author Thomas Darimont
	 */
	static class InstantiatorKey {

		private final Class<?> type;
		private final Constructor<?> constructor;

		InstantiatorKey(Class<?> type, Constructor<?> constructor) {
			this.type = type;
			this.constructor = constructor;
		}

		public Class<?> getType() {
			return type;
		}

		public Constructor<?> getConstructor() {
			return constructor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			InstantiatorKey that = (InstantiatorKey) o;

			if (constructor != null ? !constructor.equals(that.constructor) : that.constructor != null)
				return false;
			if (!type.equals(that.type))
				return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + (constructor != null ? constructor.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "InstantiatorKey [type=" + type + ", constructor=" + constructor + "]";
		}
	}

	/**
	 * A {@link ClassLoader} that can load classes from {@code byte[]} representations.
	 * 
	 * @author Thomas Darimont
	 */
	static class ByteArrayClassLoader extends ClassLoader {

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

	/**
	 * @author Thomas Darimont
	 */
	public static interface ObjectInstantiator {

		Object create(Object... args);
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
	 * 		public Object create(Object... args) {
	 * 			return new ObjCtor1ParamString((String) args[0]);
	 * 		}
	 * 	}
	 * }
	 * </pre>
	 * 
	 * @author Thomas Darimont
	 */
	enum ObjectInstantiatorClassGenerator {

		INSTANCE;

		private static final String INIT = "<init>";
		private static final String TAG = "_Instantiator_";
		private static final String JAVA_LANG_OBJECT = "java/lang/Object";
		private static final String CREATE_METHOD_NAME = "create";

		private static final String[] IMPLEMENTED_INTERFACES = new String[] { Type
				.getInternalName(ObjectInstantiator.class) };

		private final ByteArrayClassLoader classLoader;

		private ObjectInstantiatorClassGenerator() {

			this.classLoader = AccessController.doPrivileged(new PrivilegedAction<ByteArrayClassLoader>() {
				public ByteArrayClassLoader run() {
					return new ByteArrayClassLoader(ObjectInstantiatorClassGenerator.class.getClassLoader());
				}
			});
		}

		/**
		 * Generate a new class for the given {@link InstantiatorKey}.
		 * 
		 * @param key
		 * @return
		 */
		public Class<?> generateCustomInstantiatorClass(InstantiatorKey key) {

			String className = generateClassName(key);
			byte[] bytecode = generateBytecode(className, key);

			return classLoader.loadClass(className, bytecode);
		}

		/**
		 * @param key
		 * @return
		 */
		private String generateClassName(InstantiatorKey key) {
			return key.getType().getName() + TAG + Integer.toString(key.hashCode(), 36);
		}

		/**
		 * Generate a new class for the given {@link InstantiatorKey}.
		 * 
		 * @param key
		 * @return
		 */
		public byte[] generateBytecode(String internalClassName, InstantiatorKey key) {

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			cw.visit(Opcodes.V1_6, ACC_PUBLIC + ACC_SUPER, internalClassName.replace('.', '/'), null, JAVA_LANG_OBJECT,
					IMPLEMENTED_INTERFACES);

			visitDefaultConstructor(cw);

			visitCreateMethod(cw, key);

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

		private void visitCreateMethod(ClassWriter cw, InstantiatorKey key) {

			String objectClassResourcePath = Type.getInternalName(key.getType());

			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, CREATE_METHOD_NAME,
					"([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, objectClassResourcePath);
			mv.visitInsn(DUP);

			Constructor<?> ctor = key.getConstructor();
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
			mv.visitMethodInsn(INVOKESPECIAL, objectClassResourcePath, INIT, Type.getConstructorDescriptor(ctor), false);

			mv.visitInsn(ARETURN);
			mv.visitMaxs(0, 0); // (0, 0) = computed via ClassWriter.COMPUTE_MAXS
			mv.visitEnd();
		}

		private static void visitArrayIndex(MethodVisitor mv, int idx) {

			if (idx >= 0 && idx < 6) {
				mv.visitInsn(ICONST_0 + idx);
				return;
			}

			mv.visitLdcInsn(idx);
		}

		/**
		 * Insert any necessary cast and value call to convert from a boxed type to a primitive value
		 * 
		 * @param mv the method visitor into which instructions should be inserted
		 * @param ch the primitive type desired as output
		 * @param stackDescriptor the descriptor of the type on top of the stack copied from
		 *          org.springframework.expression.spel.CodeFlow#insertUnboxInsns.
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
	}

	/**
	 * @author Thomas Darimont
	 */
	public static class CouldNotCreateObjectInstantiatorException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public CouldNotCreateObjectInstantiatorException(Throwable cause) {
			super(cause);
		}
	}
}
