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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.util.ReflectionUtils;

/**
 * {@link EntityInstantiator} that uses the {@link PersistentEntity}'s {@link PreferredConstructor} to instantiate an
 * instance of the entity by dynamically generating factory methods via ASM. Since we cannot support every use case with byte
 * code generation we gracefully fall-back to the {@link ReflectionEntityInstantiator}.
 * 
 * @author Thomas Darimont
 * @since 1.10
 */
public enum BytecodeGeneratingEntityInstantiator implements EntityInstantiator {

	INSTANCE;

	private final ObjectCreatorClassGenerator classGenerator = ObjectCreatorClassGenerator.INSTANCE;

	private volatile Map<CreatorKey, ObjectCreator> creators = new HashMap<CreatorKey, ObjectCreator>(32);

	@SuppressWarnings("unchecked")
	@Override
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		Class<? extends T> type = entity.getType();

		if (shouldUseReflectiveEntityInstantiator(entity)) {
			return ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider);
		}

		PreferredConstructor<? extends T, P> ctor = entity.getPersistenceConstructor();

		List<Object> params = new ArrayList<Object>();
		if (null != provider && ctor.hasParameters()) {
			for (Parameter<?, P> parameter : ctor.getParameters()) {
				params.add(provider.getParameterValue(parameter));
			}
		}

		Object[] args = params.toArray();
		Constructor<?> ctorToUse = ctor != null && Modifier.isPublic(ctor.getConstructor().getModifiers()) ? ctor
				.getConstructor() : findAppropriateConstructorForArguments(type, args);

		if (ctorToUse == null || !assertConstructorTypesAreCompatibleWithArgTypes(args, ctorToUse)) {
			return ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider);
		}

		return (T) createInternal(entity.getClass(), ctor.getConstructor(), args);
	}

	private boolean assertConstructorTypesAreCompatibleWithArgTypes(Object[] args, Constructor<?> ctorToUse) {

		for (int i = 0; ctorToUse != null && i < args.length; i++) {
			Class<?>[] ctorParamTypes = ctorToUse.getParameterTypes();

			// ignore null arguments
			if (args[i] == null) {
				continue;
			}

			// types are not compatible
			if (!args[i].getClass().isAssignableFrom(ctorParamTypes[i])) {
				return false;
			}
		}

		return true;
	}

	private <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> boolean shouldUseReflectiveEntityInstantiator(
			E entity) {

		Class<? extends T> type = entity.getType();

		PreferredConstructor<? extends T, P> persistenceCtor = entity.getPersistenceConstructor();

		return type.getName().startsWith("java.lang") //
				|| type.isInterface() //
				|| type.isArray() //
				|| !Modifier.isPublic(type.getModifiers()) //
				|| (persistenceCtor != null //
				// && persistenceCtor.getConstructor().getAnnotation(PersistenceConstructor.class) != null //
				&& !Modifier.isPublic(persistenceCtor.getConstructor().getModifiers())) //
		;
	}

	@SuppressWarnings("unchecked")
	private <T> Constructor<T> findAppropriateConstructorForArguments(Class<T> clazz, Object[] args) {

		Constructor<T> result = null;

		ctorLoop: for (Constructor<?> ctorCandidate : clazz.getDeclaredConstructors()) {

			if (ctorCandidate.getParameterCount() == args.length) {

				Class<?>[] parameterTypes = ctorCandidate.getParameterTypes();
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

		CreatorKey key = new CreatorKey(clazz, ctor);

		Map<CreatorKey, ObjectCreator> map = this.creators;
		ObjectCreator creator = map.get(key);

		if (creator == null) {
			creator = createObjectCreator(key);

			map = new HashMap<CreatorKey, ObjectCreator>(map);
			map.put(key, creator);

			this.creators = map;
		}

		// System.out.println("Use creator to construct new object: " + creator);

		return creator.create(args);
	}

	private ObjectCreator createObjectCreator(CreatorKey key) {

		// System.out.println("######Generate class: " + key);

		try {
			return (ObjectCreator) classGenerator.generateClass(key).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	static class CreatorKey {

		private final Class<?> type;
		private final Constructor<?> ctor;

		CreatorKey(Class<?> type, Constructor<?> ctor) {
			this.type = type;
			this.ctor = ctor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			CreatorKey that = (CreatorKey) o;

			if (ctor != null ? !ctor.equals(that.ctor) : that.ctor != null)
				return false;
			if (!type.equals(that.type))
				return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + (ctor != null ? ctor.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "CreatorKey [type=" + type + ", ctor=" + ctor + "]";
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	static class ByteArrayClassLoader extends ClassLoader {

		public ByteArrayClassLoader(ClassLoader parent) {
			super(parent);
		}

		public Class<?> loadClass(String name, byte[] bytes) {

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
	public static interface ObjectCreator {

		Object create(Object... args);
	}

	/**
	 * A dynamic factory class generator.
	 * 
	 * This code generator will lazily generate a custom factory class implementing the {@link ObjectCreator} interface
	 * for every publicly accessed constructor variant.
	 *  
	 * Given a class {@code ObjCtor1ParamString} like:
	 * <pre> {@code
	 *  public class ObjCtor1ParamString extends ObjCtorNoArgs {
	 *
	 *		public final String param1;
	 *
	 *		public ObjCtor1ParamString(String param1) {
	 *			this.param1 = param1;
	 *		}
	 *	}}</pre>
	 *
	 * The following factory class {@code ObjCtor1ParamString_Creator_asdf} is generated:
	 * <pre> {@code
	 *  public class ObjCtor1ParamString_Creator_asdf implements ObjectCreator{
	 *
	 *		public Object create(Object... args) {
	 *		   return new ObjCtor1ParamString((String)args[0]);
	 *		}
	 *	}}</pre>
	 * 
	 * @author Thomas Darimont
	 */
	enum ObjectCreatorClassGenerator {
		
		INSTANCE;

		private static final String[] INTERNAL_OBJECT_CREATOR_INTERFACE_NAME = new String[] { Type
				.getInternalName(ObjectCreator.class) };

		private final ByteArrayClassLoader classLoader = new ByteArrayClassLoader(
				ObjectCreatorClassGenerator.class.getClassLoader());

		/**
		 * Generate a new class for the given {@link CreatorKey}.
		 * 
		 * @param key
		 * @return
		 */
		public Class<?> generateClass(CreatorKey key) {

			Constructor<?> ctor = key.ctor;

			Class<?> objectClass = ctor.getDeclaringClass();

			String creatorSuffix = "_Creator_" + Integer.toString(key.hashCode(), 36);
			String creatorClassResourcePath = Type.getInternalName(objectClass) + creatorSuffix;
			String entityTypeResourcePath = Type.getInternalName(objectClass);

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			MethodVisitor mv;

			cw.visit(Opcodes.V1_6, ACC_PUBLIC + ACC_SUPER, creatorClassResourcePath, null, "java/lang/Object",
					INTERNAL_OBJECT_CREATOR_INTERFACE_NAME);

			{
				mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);

				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
				mv.visitInsn(RETURN);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitLocalVariable("this", "L" + creatorClassResourcePath + ";", null, l0, l1, 0);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}

			{
				mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, "create", "([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitTypeInsn(NEW, entityTypeResourcePath);
				mv.visitInsn(DUP);

				if (ctor.getParameterCount() == 0) {
					mv.visitMethodInsn(INVOKESPECIAL, entityTypeResourcePath, "<init>", "()V", false);
				} else {

					for (int i = 0; i < ctor.getParameterCount(); i++) {

						mv.visitVarInsn(ALOAD, 1);
						mv.visitIntInsn(BIPUSH, i);

						mv.visitInsn(AALOAD);
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(ctor.getParameterTypes()[i]));
					}

					mv.visitMethodInsn(INVOKESPECIAL, entityTypeResourcePath, "<init>", Type.getConstructorDescriptor(ctor),
							false);
				}

				mv.visitInsn(ARETURN);

				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitLocalVariable("this", "L" + creatorClassResourcePath + ";", null, l0, l1, 0);
				mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l1, 1);

				mv.visitMaxs(0, 0); // computed via ClassWriter.COMPUTE_MAXS
				mv.visitEnd();
			}

			cw.visitEnd();

			return classLoader.loadClass(objectClass.getName() + creatorSuffix, cw.toByteArray());
		}
	}
}
