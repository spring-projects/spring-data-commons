/*
 * Copyright 2015-2023 the original author or authors.
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
package org.springframework.data.mapping.model;

import static org.springframework.asm.Opcodes.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.core.NativeDetector;
import org.springframework.data.mapping.FactoryMethod;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An {@link EntityInstantiator} that can generate byte code to speed-up dynamic object instantiation. Uses the
 * {@link PersistentEntity}'s {@link PreferredConstructor} to instantiate an instance of the entity by dynamically
 * generating factory methods with appropriate constructor invocations via ASM. If we cannot generate byte code for a
 * type, we gracefully fallback to the {@link ReflectionEntityInstantiator}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.11
 */
class ClassGeneratingEntityInstantiator implements EntityInstantiator {

	private static final Log LOGGER = LogFactory.getLog(ClassGeneratingEntityInstantiator.class);

	private static final Object[] EMPTY_ARGS = new Object[0];

	private final ObjectInstantiatorClassGenerator generator;

	private volatile Map<TypeInformation<?>, EntityInstantiator> entityInstantiators = new HashMap<>(32);

	private final boolean fallbackToReflectionOnError;

	/**
	 * Creates a new {@link ClassGeneratingEntityInstantiator}.
	 */
	public ClassGeneratingEntityInstantiator() {
		this(true);
	}

	/**
	 * Creates a new {@link ClassGeneratingEntityInstantiator}.
	 */
	ClassGeneratingEntityInstantiator(boolean fallbackToReflectionOnError) {
		this.generator = new ObjectInstantiatorClassGenerator();
		this.fallbackToReflectionOnError = fallbackToReflectionOnError;
	}

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

		map = new HashMap<>(map);
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

		if (Modifier.isAbstract(entity.getType().getModifiers())) {
			return MappingInstantiationExceptionEntityInstantiator.create(entity.getType());
		}

		if (fallbackToReflectionOnError) {
			try {
				return doCreateEntityInstantiator(entity);
			} catch (Throwable ex) {

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(
							String.format("Cannot create entity instantiator for %s; Falling back to ReflectionEntityInstantiator",
									entity.getName()),
							ex);
				}
				return ReflectionEntityInstantiator.INSTANCE;
			}
		}

		return doCreateEntityInstantiator(entity);
	}

	/**
	 * @param entity
	 * @return
	 */
	protected EntityInstantiator doCreateEntityInstantiator(PersistentEntity<?, ?> entity) {
		return new EntityInstantiatorAdapter(
				createObjectInstantiator(entity, entity.getInstanceCreatorMetadata()));
	}

	/**
	 * @param entity
	 * @return
	 */
	boolean shouldUseReflectionEntityInstantiator(PersistentEntity<?, ?> entity) {

		if (NativeDetector.inNativeImage()) {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("graalvm.nativeimage - fall back to reflection for %s", entity.getName()));
			}

			return true;
		}

		Class<?> type = entity.getType();

		if (type.isInterface() //
				|| type.isArray() //
				|| Modifier.isPrivate(type.getModifiers()) //
				|| type.isMemberClass() && !Modifier.isStatic(type.getModifiers()) //
				|| ClassUtils.isCglibProxyClass(type)) { //
			return true;
		}

		InstanceCreatorMetadata<? extends PersistentProperty<?>> creatorMetadata = entity.getInstanceCreatorMetadata();

		if (creatorMetadata == null) {
			return true;
		}

		if (creatorMetadata instanceof PreferredConstructor<?, ?> persistenceConstructor) {

			if (Modifier.isPrivate(persistenceConstructor.getConstructor().getModifiers())) {
				return true;
			}
		}

		if (creatorMetadata instanceof FactoryMethod<?, ?> factoryMethod) {

			if (Modifier.isPrivate(factoryMethod.getFactoryMethod().getModifiers())) {
				return true;
			}
		}

		if (!ClassUtils.isPresent(ObjectInstantiator.class.getName(), type.getClassLoader())) {
			return true;
		}

		return false;
	}

	/**
	 * Allocates an object array for instance creation.
	 *
	 * @param argumentCount
	 * @return
	 * @since 2.0
	 */
	static Object[] allocateArguments(int argumentCount) {
		return argumentCount == 0 ? EMPTY_ARGS : new Object[argumentCount];
	}

	/**
	 * Creates a dynamically generated {@link ObjectInstantiator} for the given {@link PersistentEntity} and
	 * {@link InstanceCreatorMetadata}. There will always be exactly one {@link ObjectInstantiator} instance per
	 * {@link PersistentEntity}.
	 *
	 * @param entity
	 * @param constructor
	 * @return
	 */
	ObjectInstantiator createObjectInstantiator(PersistentEntity<?, ?> entity,
			@Nullable InstanceCreatorMetadata<?> constructor) {

		try {
			return (ObjectInstantiator) this.generator.generateCustomInstantiatorClass(entity, constructor).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adapter to forward an invocation of the {@link EntityInstantiator} API to an {@link ObjectInstantiator}.
	 *
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	private static class EntityInstantiatorAdapter implements EntityInstantiator {

		private final ObjectInstantiator instantiator;

		/**
		 * Creates a new {@link EntityInstantiatorAdapter} for the given {@link ObjectInstantiator}.
		 *
		 * @param instantiator must not be {@literal null}.
		 */
		public EntityInstantiatorAdapter(ObjectInstantiator instantiator) {
			this.instantiator = instantiator;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
				ParameterValueProvider<P> provider) {

			Object[] params = extractInvocationArguments(entity.getInstanceCreatorMetadata(), provider);

			try {
				return (T) instantiator.newInstance(params);
			} catch (Exception e) {
				throw new MappingInstantiationException(entity, Arrays.asList(params), e);
			}
		}
	}

	/**
	 * Extracts the arguments required to invoke the given constructor from the given {@link ParameterValueProvider}.
	 *
	 * @param constructor can be {@literal null}.
	 * @param provider can be {@literal null}.
	 * @return
	 */
	static <P extends PersistentProperty<P>, T> Object[] extractInvocationArguments(
			@Nullable InstanceCreatorMetadata<P> constructor, ParameterValueProvider<P> provider) {

		if (constructor == null || !constructor.hasParameters()) {
			return allocateArguments(0);
		}

		Object[] params = allocateArguments(constructor.getParameterCount());

		int index = 0;
		for (Parameter<?, P> parameter : constructor.getParameters()) {
			params[index++] = provider.getParameterValue(parameter);
		}

		return params;
	}

	/**
	 * Needs to be public as otherwise the implementation class generated does not see the interface from the classloader.
	 *
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 */
	public interface ObjectInstantiator {
		Object newInstance(Object... args);
	}

	/**
	 * {@link EntityInstantiator} throwing {@link MappingInstantiationException} upon
	 * {@link #createInstance(PersistentEntity, ParameterValueProvider)}.
	 *
	 * @author Mark Paluch
	 * @since 2.5
	 */
	static class MappingInstantiationExceptionEntityInstantiator implements EntityInstantiator {

		private final Class<?> typeToCreate;

		private MappingInstantiationExceptionEntityInstantiator(Class<?> typeToCreate) {
			this.typeToCreate = typeToCreate;
		}

		public static EntityInstantiator create(Class<?> typeToCreate) {
			return new MappingInstantiationExceptionEntityInstantiator(typeToCreate);
		}

		@Override
		public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
				ParameterValueProvider<P> provider) {

			Object[] params = extractInvocationArguments(entity.getInstanceCreatorMetadata(), provider);

			throw new MappingInstantiationException(entity, Arrays.asList(params),
					new BeanInstantiationException(typeToCreate, "Class is abstract"));
		}
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
	 * @author Mark Paluch
	 */
	static class ObjectInstantiatorClassGenerator {

		private static final String INIT = "<init>";
		private static final String TAG = "_Instantiator_";
		private static final String JAVA_LANG_OBJECT = Type.getInternalName(Object.class);
		private static final String CREATE_METHOD_NAME = "newInstance";

		private static final String[] IMPLEMENTED_INTERFACES = new String[] {
				Type.getInternalName(ObjectInstantiator.class) };

		/**
		 * Generate a new class for the given {@link PersistentEntity}.
		 *
		 * @param entity
		 * @param constructor
		 * @return
		 */
		public Class<?> generateCustomInstantiatorClass(PersistentEntity<?, ?> entity,
				@Nullable InstanceCreatorMetadata<?> constructor) {

			String className = generateClassName(entity);
			Class<?> type = entity.getType();
			ClassLoader classLoader = type.getClassLoader();

			if (ClassUtils.isPresent(className, classLoader)) {

				try {
					return ClassUtils.forName(className, classLoader);
				} catch (Exception o_O) {
					throw new IllegalStateException(o_O);
				}
			}

			byte[] bytecode = generateBytecode(className, entity, constructor);

			try {
				return ReflectUtils.defineClass(className, bytecode, classLoader, type.getProtectionDomain(), type);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		/**
		 * @param entity
		 * @return
		 */
		private String generateClassName(PersistentEntity<?, ?> entity) {
			return entity.getType().getName() + TAG + Integer.toString(entity.hashCode(), 36);
		}

		/**
		 * Generate a new class for the given {@link PersistentEntity}.
		 *
		 * @param internalClassName
		 * @param entity
		 * @param entityCreator
		 * @return
		 */
		public byte[] generateBytecode(String internalClassName, PersistentEntity<?, ?> entity,
				@Nullable InstanceCreatorMetadata<?> entityCreator) {

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			cw.visit(Opcodes.V1_6, ACC_PUBLIC + ACC_SUPER, internalClassName.replace('.', '/'), null, JAVA_LANG_OBJECT,
					IMPLEMENTED_INTERFACES);

			visitDefaultConstructor(cw);

			visitCreateMethod(cw, entity, entityCreator);

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
		 * @param entityCreator
		 */
		private void visitCreateMethod(ClassWriter cw, PersistentEntity<?, ?> entity,
				@Nullable InstanceCreatorMetadata<?> entityCreator) {

			String entityTypeResourcePath = Type.getInternalName(entity.getType());

			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, CREATE_METHOD_NAME,
					"([" + BytecodeUtil.referenceName(Object.class) + ")" + BytecodeUtil.referenceName(Object.class),
					null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, entityTypeResourcePath);
			mv.visitInsn(DUP);

			if (entityCreator instanceof PreferredConstructor<?, ?> constructor) {
				visitConstructorCreation(constructor, mv, entityTypeResourcePath);
			}

			if (entityCreator instanceof FactoryMethod<?, ?> factoryMethod) {
				visitFactoryMethodCreation(factoryMethod, mv, entityTypeResourcePath);
			}

			mv.visitInsn(ARETURN);
			mv.visitMaxs(0, 0); // (0, 0) = computed via ClassWriter.COMPUTE_MAXS
			mv.visitEnd();
		}

		private static void visitConstructorCreation(PreferredConstructor<?, ?> constructor, MethodVisitor mv,
				String entityTypeResourcePath) {

			Constructor<?> ctor = constructor.getConstructor();
			Class<?>[] parameterTypes = ctor.getParameterTypes();
			List<? extends Parameter<Object, ?>> parameters = constructor.getParameters();

			visitParameterTypes(mv, parameterTypes, parameters);

			mv.visitMethodInsn(INVOKESPECIAL, entityTypeResourcePath, INIT, Type.getConstructorDescriptor(ctor), false);
		}

		private static void visitFactoryMethodCreation(FactoryMethod<?, ?> factoryMethod, MethodVisitor mv,
				String entityTypeResourcePath) {

			Method method = factoryMethod.getFactoryMethod();
			Class<?>[] parameterTypes = method.getParameterTypes();
			List<? extends Parameter<Object, ?>> parameters = factoryMethod.getParameters();

			visitParameterTypes(mv, parameterTypes, parameters);

			mv.visitMethodInsn(INVOKESTATIC, entityTypeResourcePath, method.getName(), Type.getMethodDescriptor(method),
					false);
		}

		private static void visitParameterTypes(MethodVisitor mv, Class<?>[] parameterTypes,
				List<? extends Parameter<Object, ?>> parameters) {

			for (int i = 0; i < parameterTypes.length; i++) {

				mv.visitVarInsn(ALOAD, 1);

				visitArrayIndex(mv, i);

				mv.visitInsn(AALOAD);

				if (parameterTypes[i].isPrimitive()) {

					mv.visitInsn(DUP);
					String parameterName = parameters.size() > i ? parameters.get(i).getName() : null;

					insertAssertNotNull(mv, parameterName == null ? String.format("at index %d", i) : parameterName);
					insertUnboxInsns(mv, Type.getType(parameterTypes[i]).toString().charAt(0), "");
				} else {
					mv.visitTypeInsn(CHECKCAST, Type.getInternalName(parameterTypes[i]));
				}
			}
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
		 * Insert not-{@literal null} assertion for a parameter.
		 *
		 * @param mv the method visitor into which instructions should be inserted
		 * @param parameterName name of the parameter to create the appropriate assertion message.
		 */
		private static void insertAssertNotNull(MethodVisitor mv, String parameterName) {

			// Assert.notNull(property)
			mv.visitLdcInsn(String.format("Parameter %s must not be null", parameterName));
			mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Assert.class), "notNull", String.format("(%s%s)V",
					BytecodeUtil.referenceName(JAVA_LANG_OBJECT), BytecodeUtil.referenceName(String.class)), false);
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
					if (!stackDescriptor.equals(BytecodeUtil.referenceName(Boolean.class))) {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Boolean.class));
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", "()Z", false);
					break;
				case 'B':
					if (!stackDescriptor.equals(BytecodeUtil.referenceName(Byte.class))) {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Byte.class));
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Byte.class), "byteValue", "()B", false);
					break;
				case 'C':
					if (!stackDescriptor.equals(BytecodeUtil.referenceName(Character.class))) {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Character.class));
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", "()C", false);
					break;
				case 'D':
					if (!stackDescriptor.equals(BytecodeUtil.referenceName(Double.class))) {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Double.class));
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Double.class), "doubleValue", "()D", false);
					break;
				case 'F':
					if (!stackDescriptor.equals(BytecodeUtil.referenceName(Float.class))) {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Float.class));
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Float.class), "floatValue", "()F", false);
					break;
				case 'I':
					if (!stackDescriptor.equals(BytecodeUtil.referenceName(Integer.class))) {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Integer.class));
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue", "()I", false);
					break;
				case 'J':
					if (!stackDescriptor.equals(BytecodeUtil.referenceName(Long.class))) {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Long.class));
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Long.class), "longValue", "()J", false);
					break;
				case 'S':
					if (!stackDescriptor.equals(BytecodeUtil.referenceName(Short.class))) {
						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(Short.class));
					}
					mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Short.class), "shortValue", "()S", false);
					break;
				default:
					throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + ch + "'");
			}
		}
	}
}
