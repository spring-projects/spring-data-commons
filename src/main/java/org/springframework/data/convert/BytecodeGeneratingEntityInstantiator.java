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
import org.springframework.util.ReflectionUtils;

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

	private final ObjectInstantiatorClassGenerator classGenerator = ObjectInstantiatorClassGenerator.INSTANCE;

	private volatile Map<InstantiatorKey, ObjectInstantiator> creators = new HashMap<InstantiatorKey, ObjectInstantiator>(32);

	private final Object lock = new Object();

	/* (non-Javadoc)
	 * @see org.springframework.data.convert.EntityInstantiator#createInstance(org.springframework.data.mapping.PersistentEntity, org.springframework.data.mapping.model.ParameterValueProvider)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		Class<? extends T> type = entity.getType();
		if (shouldUseReflectiveEntityInstantiator(entity)) {
			return ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider);
		}


		PreferredConstructor<? extends T, P> prefCtor = entity.getPersistenceConstructor();
		
				List<Object> params = new ArrayList<Object>();
		if (null != provider && prefCtor.hasParameters()) {
			for (Parameter<?, P> parameter : prefCtor.getParameters()) {
				params.add(provider.getParameterValue(parameter));
			}
		}

		Object[] args = params.toArray();
		Constructor<? extends T> ctor = prefCtor.getConstructor();

		Constructor<?> ctorToUse = prefCtor != null && Modifier.isPublic(ctor.getModifiers()) ? prefCtor.getConstructor()
				: findAppropriateConstructorForArguments(type, args);

		if (ctorToUse == null || !assertConstructorTypesAreCompatibleWithArgTypes(args, ctorToUse)) {
			return ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider);
		}

		return (T) createInternal(entity.getType(), prefCtor.getConstructor(), args);
	}

	/**
	 * Ensures that the given {@link Constructor} can be used with the given {@param args}. 
	 * 
	 * @param args
	 * @param ctorToUse
	 * @return
	 */
	private boolean assertConstructorTypesAreCompatibleWithArgTypes(Object[] args, Constructor<?> ctorToUse) {

		for (int i = 0; ctorToUse != null && i < args.length; i++) {
			Class<?>[] ctorParamTypes = ctorToUse.getParameterTypes();

			if (args[i] == null) {

				// the ctor cannot be used since null cannot be boxed to a wrapper
				if (ctorParamTypes[i].isPrimitive()) {
					return false;
				}

				// ignore null arguments otherwise
				continue;
			}

			// types are not compatible
			if (!args[i].getClass().isAssignableFrom(ctorParamTypes[i])) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Determines whether we should fall back to the {@link ReflectionEntityInstantiator} for the given {@link PersistentEntity}.
	 * 
	 * @param entity
	 * @return
	 */
	private <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> boolean shouldUseReflectiveEntityInstantiator(
			E entity) {

		Class<? extends T> type = entity.getType();

		PreferredConstructor<? extends T, P> persistenceCtor = entity.getPersistenceConstructor();

		return type.getName().startsWith("java.lang") //
				|| type.isInterface() //
				|| type.isArray() //
				|| !Modifier.isPublic(type.getModifiers()) //
				|| (persistenceCtor != null //
				&& !Modifier.isPublic(persistenceCtor.getConstructor().getModifiers())) //
		;
	}

	@SuppressWarnings("unchecked")
	private <T> Constructor<T> findAppropriateConstructorForArguments(Class<T> clazz, Object[] args) {

		Constructor<T> result = null;

		ctorLoop: for (Constructor<?> ctorCandidate : clazz.getDeclaredConstructors()) {

			Class<?>[] parameterTypes = ctorCandidate.getParameterTypes();
			if (parameterTypes.length == args.length) {

				for (int i = 0; i < args.length; i++) {

					if (args[i] == null) {
						continue;
					}

					if (!parameterTypes[i].isInstance(args[i])) {
						continue ctorLoop;
					}
				}

				result = (Constructor<T>) ctorCandidate;
			}
		}

		if (result != null) {
			ReflectionUtils.makeAccessible(result);
		}

		return result;
	}

	private Object createInternal(Class<?> clazz, Constructor<?> ctor, Object... args) {

		InstantiatorKey key = new InstantiatorKey(clazz, ctor);

		Map<InstantiatorKey, ObjectInstantiator> map = this.creators;
		ObjectInstantiator creator = map.get(key);

		if (creator == null) {
			synchronized (lock) {

				map = this.creators;
				creator = map.get(key);

				if (creator == null) {

					creator = createObjectCreator(key);
					map = new HashMap<InstantiatorKey, ObjectInstantiator>(map);
					map.put(key, creator);

					this.creators = map;
				}
			}
		}

		return creator.create(args);
	}

	/**
	 * Creates a dynamically generated {@link ObjectInstantiator} for the given {@link InstantiatorKey}.
	 * 
	 * There will always be exactly one {@link ObjectInstantiator} instance per {@link InstantiatorKey}.
	 * 
	 * @param key
	 * @return
	 */
	private ObjectInstantiator createObjectCreator(InstantiatorKey key) {

		try {
			return (ObjectInstantiator) classGenerator.generateCustomInstantiatorClass(key).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A key that forms a unique tuple for a type and constructor combination.<p>
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
			return "CreatorKey [type=" + type + ", constructor=" + constructor + "]";
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
	 * The following factory class {@code ObjCtor1ParamString_Creator_asdf} is generated:
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	public class ObjCtor1ParamString_Creator_asdf implements ObjectCreator {
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
				mv.visitIntInsn(BIPUSH, i);
				mv.visitInsn(AALOAD);
				mv.visitTypeInsn(CHECKCAST, Type.getInternalName(parameterTypes[i]));
			}
			mv.visitMethodInsn(INVOKESPECIAL, objectClassResourcePath, INIT, Type.getConstructorDescriptor(ctor), false);

			mv.visitInsn(ARETURN);
			mv.visitMaxs(0, 0); // (0, 0) = computed via ClassWriter.COMPUTE_MAXS
			mv.visitEnd();
		}
	}
}
