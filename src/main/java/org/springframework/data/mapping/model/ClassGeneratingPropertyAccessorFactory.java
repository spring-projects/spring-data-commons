/*
 * Copyright 2016-2018 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A factory that can generate byte code to speed-up dynamic property access. Uses the {@link PersistentEntity}'s
 * {@link PersistentProperty} to discover the access to properties. Properties are accessed either using method handles
 * to overcome Java visibility issues or directly using field access/getter/setter calls.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @since 1.13
 */
public class ClassGeneratingPropertyAccessorFactory implements PersistentPropertyAccessorFactory {

	// Pooling of parameter arrays to prevent excessive object allocation.
	private final ThreadLocal<Object[]> argumentCache = ThreadLocal.withInitial(() -> new Object[1]);

	private volatile Map<PersistentEntity<?, ?>, Constructor<?>> constructorMap = new HashMap<>(32);
	private volatile Map<TypeInformation<?>, Class<PersistentPropertyAccessor>> propertyAccessorClasses = new HashMap<>(
			32);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.PersistentPropertyAccessorFactory#getPropertyAccessor(org.springframework.data.mapping.PersistentEntity, java.lang.Object)
	 */
	@Override
	public PersistentPropertyAccessor getPropertyAccessor(PersistentEntity<?, ?> entity, Object bean) {

		Constructor<?> constructor = constructorMap.get(entity);

		if (constructor == null) {

			Class<PersistentPropertyAccessor> accessorClass = potentiallyCreateAndRegisterPersistentPropertyAccessorClass(
					entity);
			constructor = accessorClass.getConstructors()[0];

			Map<PersistentEntity<?, ?>, Constructor<?>> constructorMap = new HashMap<>(this.constructorMap);
			constructorMap.put(entity, constructor);
			this.constructorMap = constructorMap;
		}

		Object[] args = argumentCache.get();
		args[0] = bean;

		try {
			return (PersistentPropertyAccessor) constructor.newInstance(args);
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Cannot create persistent property accessor for %s", entity), e);
		} finally {
			args[0] = null;
		}
	}

	/**
	 * Checks whether an accessor class can be generated.
	 *
	 * @param entity must not be {@literal null}.
	 * @return {@literal true} if the runtime is equal or greater to Java 1.7, we can access the ClassLoader, the property
	 *         name hash codes are unique and the type has a class loader we can use to re-inject types.
	 * @see PersistentPropertyAccessorFactory#isSupported(PersistentEntity)
	 */
	@Override
	public boolean isSupported(PersistentEntity<?, ?> entity) {

		Assert.notNull(entity, "PersistentEntity must not be null!");

		return isClassLoaderDefineClassAvailable(entity) && isTypeInjectable(entity) && hasUniquePropertyHashCodes(entity);
	}

	private static boolean isClassLoaderDefineClassAvailable(PersistentEntity<?, ?> entity) {

		try {
			return ReflectionUtils.findMethod(entity.getType().getClassLoader().getClass(), "defineClass", String.class,
					byte[].class, Integer.TYPE, Integer.TYPE, ProtectionDomain.class) != null;
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isTypeInjectable(PersistentEntity<?, ?> entity) {

		Class<?> type = entity.getType();
		return type.getClassLoader() != null
				&& (type.getPackage() == null || !type.getPackage().getName().startsWith("java"));
	}

	private boolean hasUniquePropertyHashCodes(PersistentEntity<?, ?> entity) {

		Set<Integer> hashCodes = new HashSet<>();
		AtomicInteger propertyCount = new AtomicInteger();

		entity.doWithProperties((SimplePropertyHandler) property -> {

			hashCodes.add(property.getName().hashCode());
			propertyCount.incrementAndGet();
		});

		entity.doWithAssociations((SimpleAssociationHandler) association -> {

			if (association.getInverse() != null) {

				hashCodes.add(association.getInverse().getName().hashCode());
				propertyCount.incrementAndGet();
			}
		});

		return hashCodes.size() == propertyCount.get();
	}

	/**
	 * @param entity must not be {@literal null}.
	 */
	private synchronized Class<PersistentPropertyAccessor> potentiallyCreateAndRegisterPersistentPropertyAccessorClass(
			PersistentEntity<?, ?> entity) {

		Map<TypeInformation<?>, Class<PersistentPropertyAccessor>> map = this.propertyAccessorClasses;
		Class<PersistentPropertyAccessor> propertyAccessorClass = map.get(entity.getTypeInformation());

		if (propertyAccessorClass != null) {
			return propertyAccessorClass;
		}

		propertyAccessorClass = createAccessorClass(entity);

		map = new HashMap<>(map);
		map.put(entity.getTypeInformation(), propertyAccessorClass);

		this.propertyAccessorClasses = map;

		return propertyAccessorClass;
	}

	@SuppressWarnings("unchecked")
	private Class<PersistentPropertyAccessor> createAccessorClass(PersistentEntity<?, ?> entity) {

		try {
			return (Class<PersistentPropertyAccessor>) PropertyAccessorClassGenerator.generateCustomAccessorClass(entity);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates {@link PersistentPropertyAccessor} classes to access properties of a {@link PersistentEntity}. This code
	 * uses {@code private final static} held method handles which perform about the speed of native method invocations
	 * for property access which is restricted due to Java rules (such as private fields/methods) or private inner
	 * classes. All other scoped members (package default, protected and public) are accessed via field or property access
	 * to bypass reflection overhead. That's only possible if the type and the member access is possible from another
	 * class within the same package and class loader. Mixed access (MethodHandle/getter/setter calls) is possible as
	 * well. Accessing properties using generated accessors imposes some constraints:
	 * <ul>
	 * <li>Runtime must be Java 7 or higher</li>
	 * <li>The generated accessor decides upon generation whether to use field or property access for particular
	 * properties. It's not possible to change the access method once the accessor class is generated.</li>
	 * <li>Property names and their {@link String#hashCode()} must be unique within a {@link PersistentEntity}.</li>
	 * </ul>
	 * These constraints apply to retain the performance gains, otherwise the generated code has to decide which method
	 * (field/property) has to be used. The {@link String#hashCode()} rule originates in dispatching of to the appropriate
	 * {@link java.lang.invoke.MethodHandle}. This is done by {@code LookupSwitch} which is a O(1) operation but requires
	 * a constant input. {@link String#hashCode()} may change but since we run in the same VM, no evil should happen.
	 *
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	public class PersonWithId_Accessor_zd4wnl implements PersistentPropertyAccessor {
	 * 		private final Object bean;
	 * 		private static final MethodHandle $id_fieldGetter;
	 * 		private static final MethodHandle $id_fieldSetter;
	 * 		// ...
	 * 		public PersonWithId_Accessor_zd4wnl(Object bean) {
	 * 			this.bean = bean;
	 *        }
	 * 		static {
	 * 			Method getter;
	 * 			Method setter;
	 * 			MethodHandles.Lookup lookup = MethodHandles.lookup();
	 * 			Class class_1 = Class.forName("org.springframework.data.mapping.Person");
	 * 			Class class_2 = Class.forName("org.springframework.data.mapping.PersonWithId");
	 * 			Field field = class_2.getDeclaredField("id");
	 * 			field.setAccessible(true);
	 * 			$id_fieldGetter = lookup.unreflectGetter(field);
	 * 			$id_fieldSetter = lookup.unreflectSetter(field);
	 * 			// ...
	 *        }
	 * 		public Object getBean() {
	 * 			return this.bean;
	 *        }
	 * 		public void setProperty(PersistentProperty<?> property, Object value) {
	 * 			Object bean = this.bean;
	 * 			switch (property.getName().hashCode()) {
	 * 				case 3355:
	 * 					$id_fieldSetter.invoke(bean, value);
	 * 					return;
	 * 				// ...
	 *            }
	 * 			throw new UnsupportedOperationException(
	 * 					String.format("No MethodHandle to set property %s", new Object[] { property }));
	 *        }
	 * 		 public Object getProperty(PersistentProperty<?> property){
	 * 			Object bean = this.bean;
	 * 			switch (property.getName().hashCode()) {
	 * 				case 3355:
	 * 					return id_fieldGetter..invoke(bean);
	 * 				case 3356:
	 * 					return bean.getField();
	 * 					// ...
	 * 				case 3357:
	 * 					return bean.field;
	 * 					// ...
	 * 			throw new UnsupportedOperationException(
	 * 					String.format("No MethodHandle to get property %s", new Object[] { property }));
	 *        }
	 * }
	 * </pre>
	 *
	 * @author Mark Paluch
	 */
	static class PropertyAccessorClassGenerator {

		private static final String INIT = "<init>";
		private static final String CLINIT = "<clinit>";
		private static final String TAG = "_Accessor_";
		private static final String JAVA_LANG_OBJECT = "java/lang/Object";
		private static final String JAVA_LANG_STRING = "java/lang/String";
		private static final String JAVA_LANG_REFLECT_METHOD = "java/lang/reflect/Method";
		private static final String JAVA_LANG_INVOKE_METHOD_HANDLE = "java/lang/invoke/MethodHandle";
		private static final String JAVA_LANG_CLASS = "java/lang/Class";
		private static final String BEAN_FIELD = "bean";
		private static final String THIS_REF = "this";
		private static final String PERSISTENT_PROPERTY = "org/springframework/data/mapping/PersistentProperty";
		private static final String SET_ACCESSIBLE = "setAccessible";
		private static final String JAVA_LANG_REFLECT_FIELD = "java/lang/reflect/Field";
		private static final String JAVA_LANG_INVOKE_METHOD_HANDLES = "java/lang/invoke/MethodHandles";
		private static final String JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP = "java/lang/invoke/MethodHandles$Lookup";
		private static final String JAVA_LANG_UNSUPPORTED_OPERATION_EXCEPTION = "java/lang/UnsupportedOperationException";

		private static final String[] IMPLEMENTED_INTERFACES = new String[] {
				Type.getInternalName(PersistentPropertyAccessor.class) };

		/**
		 * Generate a new class for the given {@link PersistentEntity}.
		 */
		static Class<?> generateCustomAccessorClass(PersistentEntity<?, ?> entity) {

			String className = generateClassName(entity);
			byte[] bytecode = generateBytecode(className.replace('.', '/'), entity);
			Class<?> type = entity.getType();

			try {
				return ReflectUtils.defineClass(className, bytecode, type.getClassLoader(), type.getProtectionDomain());
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		/**
		 * Generate a new class for the given {@link PersistentEntity}.
		 */
		static byte[] generateBytecode(String internalClassName, PersistentEntity<?, ?> entity) {

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			cw.visit(Opcodes.V1_6, ACC_PUBLIC + ACC_SUPER, internalClassName, null, JAVA_LANG_OBJECT, IMPLEMENTED_INTERFACES);

			List<PersistentProperty<?>> persistentProperties = getPersistentProperties(entity);

			visitFields(entity, persistentProperties, cw);
			visitDefaultConstructor(entity, internalClassName, cw);
			visitStaticInitializer(entity, persistentProperties, internalClassName, cw);
			visitBeanGetter(entity, internalClassName, cw);
			visitSetProperty(entity, persistentProperties, internalClassName, cw);
			visitGetProperty(entity, persistentProperties, internalClassName, cw);

			cw.visitEnd();

			return cw.toByteArray();
		}

		private static List<PersistentProperty<?>> getPersistentProperties(PersistentEntity<?, ?> entity) {

			final List<PersistentProperty<?>> persistentProperties = new ArrayList<>();

			entity.doWithAssociations((SimpleAssociationHandler) association -> {
				if (association.getInverse() != null) {
					persistentProperties.add(association.getInverse());
				}
			});

			entity.doWithProperties((SimplePropertyHandler) property -> persistentProperties.add(property));

			return persistentProperties;
		}

		/**
		 * Generates field declarations for private-visibility properties.
		 *
		 * <pre>
		 * {
		 * 	&#064;code
		 * 	private final Object bean;
		 * 	private static final MethodHandle $id_fieldGetter;
		 * 	private static final MethodHandle $id_fieldSetter;
		 * 	// ...
		 * }
		 * </pre>
		 */
		private static void visitFields(PersistentEntity<?, ?> entity, List<PersistentProperty<?>> persistentProperties,
				ClassWriter cw) {

			cw.visitInnerClass(JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP, JAVA_LANG_INVOKE_METHOD_HANDLES, "Lookup",
					ACC_PRIVATE + ACC_FINAL + ACC_STATIC);

			boolean accessibleType = isAccessible(entity);

			if (accessibleType) {
				cw.visitField(ACC_PRIVATE + ACC_FINAL, BEAN_FIELD, referenceName(Type.getInternalName(entity.getType())), null,
						null).visitEnd();
			} else {
				cw.visitField(ACC_PRIVATE + ACC_FINAL, BEAN_FIELD, referenceName(JAVA_LANG_OBJECT), null, null).visitEnd();
			}

			for (PersistentProperty<?> property : persistentProperties) {

				if (generateMethodHandle(entity, property.getSetter())) {
					cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, setterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
				}

				if (generateMethodHandle(entity, property.getGetter())) {
					cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, getterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
				}

				if (generateSetterMethodHandle(entity, property.getField())) {

					cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, fieldSetterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
					cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, fieldGetterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
				}
			}
		}

		/**
		 * Generates the default constructor.
		 *
		 * <pre>
		 * {
		 * 		&#064;code
		 * 		public PersonWithId_Accessor_zd4wnl(PersonWithId bean) {
		 * 			this.bean = bean;
		 *      }
		 * }
		 * </pre>
		 */
		private static void visitDefaultConstructor(PersistentEntity<?, ?> entity, String internalClassName,
				ClassWriter cw) {

			// public EntityAccessor(Entity bean) or EntityAccessor(Object bean)
			MethodVisitor mv;
			boolean accessibleType = isAccessible(entity);

			if (accessibleType) {
				mv = cw.visitMethod(ACC_PUBLIC, INIT, String.format("(%s)V", referenceName(entity.getType())), null, null);
			} else {
				mv = cw.visitMethod(ACC_PUBLIC, INIT, String.format("(%s)V", referenceName(JAVA_LANG_OBJECT)), null, null);
			}

			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, INIT, "()V", false);

			// Assert.notNull(bean)
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn("Bean must not be null!");
			mv.visitMethodInsn(INVOKESTATIC, "org/springframework/util/Assert", "notNull",
					String.format("(%s%s)V", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_STRING)), false);

			// this.bean = bean
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);

			if (accessibleType) {
				mv.visitFieldInsn(PUTFIELD, internalClassName, BEAN_FIELD, referenceName(entity.getType()));
			} else {
				mv.visitFieldInsn(PUTFIELD, internalClassName, BEAN_FIELD, referenceName(JAVA_LANG_OBJECT));
			}

			mv.visitInsn(RETURN);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitLocalVariable(THIS_REF, referenceName(internalClassName), null, l0, l3, 0);

			if (accessibleType) {
				mv.visitLocalVariable(BEAN_FIELD, referenceName(Type.getInternalName(entity.getType())), null, l0, l3, 1);
			} else {
				mv.visitLocalVariable(BEAN_FIELD, referenceName(JAVA_LANG_OBJECT), null, l0, l3, 1);
			}

			mv.visitMaxs(2, 2);
		}

		/**
		 * Generates the static initializer block.
		 *
		 * <pre>
		 * 		&#064;code
		 * 		static {
		 * 			Method getter;
		 * 			Method setter;
		 * 			MethodHandles.Lookup lookup = MethodHandles.lookup();
		 * 			Class class_1 = Class.forName("org.springframework.data.mapping.Person");
		 * 			Class class_2 = Class.forName("org.springframework.data.mapping.PersonWithId");
		 * 			Field field = class_2.getDeclaredField("id");
		 * 			field.setAccessible(true);
		 * 			$id_fieldGetter = lookup.unreflectGetter(field);
		 * 			$id_fieldSetter = lookup.unreflectSetter(field);
		 *  		// ...
		 *        }
		 * </pre>
		 */
		private static void visitStaticInitializer(PersistentEntity<?, ?> entity,
				List<PersistentProperty<?>> persistentProperties, String internalClassName, ClassWriter cw) {

			MethodVisitor mv = cw.visitMethod(ACC_STATIC, CLINIT, "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			mv.visitLabel(l0);

			// lookup = MethodHandles.lookup()
			mv.visitMethodInsn(INVOKESTATIC, JAVA_LANG_INVOKE_METHOD_HANDLES, "lookup",
					String.format("()%s", referenceName(JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP)), false);
			mv.visitVarInsn(ASTORE, 0);

			List<Class<?>> entityClasses = getPropertyDeclaratingClasses(persistentProperties);

			for (Class<?> entityClass : entityClasses) {

				mv.visitLdcInsn(entityClass.getName());
				mv.visitMethodInsn(INVOKESTATIC, JAVA_LANG_CLASS, "forName",
						String.format("(%s)%s", referenceName(JAVA_LANG_STRING), referenceName(JAVA_LANG_CLASS)), false);
				mv.visitVarInsn(ASTORE, classVariableIndex4(entityClasses, entityClass));
			}

			for (PersistentProperty<?> property : persistentProperties) {

				if (property.usePropertyAccess()) {

					if (generateMethodHandle(entity, property.getGetter())) {
						visitPropertyGetterInitializer(property, mv, entityClasses, internalClassName);
					}

					if (generateMethodHandle(entity, property.getSetter())) {
						visitPropertySetterInitializer(property, mv, entityClasses, internalClassName);
					}
				}

				if (generateSetterMethodHandle(entity, property.getField())) {
					visitFieldGetterSetterInitializer(property, mv, entityClasses, internalClassName);
				}
			}

			mv.visitLabel(l1);
			mv.visitInsn(RETURN);

			mv.visitLocalVariable("lookup", referenceName(JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP), null, l0, l1, 0);
			mv.visitLocalVariable("field", referenceName(JAVA_LANG_REFLECT_FIELD), null, l0, l1, 1);
			mv.visitLocalVariable("setter", referenceName(JAVA_LANG_REFLECT_METHOD), null, l0, l1, 2);
			mv.visitLocalVariable("getter", referenceName(JAVA_LANG_REFLECT_METHOD), null, l0, l1, 3);

			for (Class<?> entityClass : entityClasses) {

				int index = classVariableIndex4(entityClasses, entityClass);
				mv.visitLocalVariable(String.format("class_%d", index), referenceName(JAVA_LANG_CLASS), null, l0, l1, index);
			}

			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		/**
		 * Retrieve all classes which are involved in property/getter/setter declarations as these elements may be
		 * distributed across the type hierarchy.
		 */
		@SuppressWarnings("null")
		private static List<Class<?>> getPropertyDeclaratingClasses(List<PersistentProperty<?>> persistentProperties) {

			return persistentProperties.stream().flatMap(property -> {
				return Optionals
						.toStream(Optional.ofNullable(property.getField()), Optional.ofNullable(property.getGetter()),
								Optional.ofNullable(property.getSetter()))

						// keep it a lambda to infer the correct types, preventing
						// LambdaConversionException: Invalid receiver type class java.lang.reflect.AccessibleObject; not a subtype
						// of implementation type interface java.lang.reflect.Member
						.map(it -> it.getDeclaringClass());

			}).collect(Collectors.collectingAndThen(Collectors.toSet(), it -> new ArrayList<>(it)));

		}

		/**
		 * Generate property getter initializer.
		 */
		private static void visitPropertyGetterInitializer(PersistentProperty<?> property, MethodVisitor mv,
				List<Class<?>> entityClasses, String internalClassName) {

			// getter = <entity>.class.getDeclaredMethod()
			Method getter = property.getGetter();

			if (getter != null) {

				mv.visitVarInsn(ALOAD, classVariableIndex4(entityClasses, getter.getDeclaringClass()));
				mv.visitLdcInsn(getter.getName());
				mv.visitInsn(ICONST_0);
				mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_CLASS);

				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_CLASS, "getDeclaredMethod", String.format("(%s[%s)%s",
						referenceName(JAVA_LANG_STRING), referenceName(JAVA_LANG_CLASS), referenceName(JAVA_LANG_REFLECT_METHOD)),
						false);
				mv.visitVarInsn(ASTORE, 3);

				// getter.setAccessible(true)
				mv.visitVarInsn(ALOAD, 3);
				mv.visitInsn(ICONST_1);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_REFLECT_METHOD, SET_ACCESSIBLE, "(Z)V", false);

				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 3);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP, "unreflect", String.format("(%s)%s",
						referenceName(JAVA_LANG_REFLECT_METHOD), referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE)), false);
			}

			if (getter == null) {
				mv.visitInsn(ACONST_NULL);
			}

			mv.visitFieldInsn(PUTSTATIC, internalClassName, getterName(property),
					referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
		}

		/**
		 * Generate property setter initializer.
		 */
		private static void visitPropertySetterInitializer(PersistentProperty<?> property, MethodVisitor mv,
				List<Class<?>> entityClasses, String internalClassName) {

			// setter = <entity>.class.getDeclaredMethod()
			Method setter = property.getSetter();

			if (setter != null) {

				mv.visitVarInsn(ALOAD, classVariableIndex4(entityClasses, setter.getDeclaringClass()));
				mv.visitLdcInsn(setter.getName());

				mv.visitInsn(ICONST_1);
				mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_CLASS);
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_0);

				Class<?> parameterType = setter.getParameterTypes()[0];

				if (parameterType.isPrimitive()) {
					mv.visitFieldInsn(GETSTATIC, Type.getInternalName(autoboxType(setter.getParameterTypes()[0])), "TYPE",
							referenceName(JAVA_LANG_CLASS));
				} else {
					mv.visitLdcInsn(Type.getType(referenceName(parameterType)));
				}

				mv.visitInsn(AASTORE);

				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_CLASS, "getDeclaredMethod", String.format("(%s[%s)%s",
						referenceName(JAVA_LANG_STRING), referenceName(JAVA_LANG_CLASS), referenceName(JAVA_LANG_REFLECT_METHOD)),
						false);
				mv.visitVarInsn(ASTORE, 2);

				// setter.setAccessible(true)
				mv.visitVarInsn(ALOAD, 2);
				mv.visitInsn(ICONST_1);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_REFLECT_METHOD, SET_ACCESSIBLE, "(Z)V", false);

				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP, "unreflect", String.format("(%s)%s",
						referenceName(JAVA_LANG_REFLECT_METHOD), referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE)), false);
			}

			if (setter == null) {
				mv.visitInsn(ACONST_NULL);
			}

			mv.visitFieldInsn(PUTSTATIC, internalClassName, setterName(property),
					referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
		}

		/**
		 * Generate field getter and setter initializers.
		 */
		private static void visitFieldGetterSetterInitializer(PersistentProperty<?> property, MethodVisitor mv,
				List<Class<?>> entityClasses, String internalClassName) {

			// field = <entity>.class.getDeclaredField()

			Field field = property.getField();
			if (field != null) {

				mv.visitVarInsn(ALOAD, classVariableIndex4(entityClasses, field.getDeclaringClass()));
				mv.visitLdcInsn(field.getName());
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_CLASS, "getDeclaredField",
						String.format("(%s)%s", referenceName(JAVA_LANG_STRING), referenceName(JAVA_LANG_REFLECT_FIELD)), false);
				mv.visitVarInsn(ASTORE, 1);

				// field.setAccessible(true)
				mv.visitVarInsn(ALOAD, 1);
				mv.visitInsn(ICONST_1);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD, SET_ACCESSIBLE, "(Z)V", false);

				// $fieldGetter = lookup.unreflectGetter(field)
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP, "unreflectGetter", String.format(
						"(%s)%s", referenceName(JAVA_LANG_REFLECT_FIELD), referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE)), false);
				mv.visitFieldInsn(PUTSTATIC, internalClassName, fieldGetterName(property),
						referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));

				// $fieldSetter = lookup.unreflectSetter(field)
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP, "unreflectSetter", String.format(
						"(%s)%s", referenceName(JAVA_LANG_REFLECT_FIELD), referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE)), false);
				mv.visitFieldInsn(PUTSTATIC, internalClassName, fieldSetterName(property),
						referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
			}
		}

		private static void visitBeanGetter(PersistentEntity<?, ?> entity, String internalClassName, ClassWriter cw) {

			// public Object getBean()
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getBean", String.format("()%s", referenceName(JAVA_LANG_OBJECT)),
					null, null);
			mv.visitCode();
			Label l0 = new Label();

			// return this.bean
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);

			if (isAccessible(entity)) {
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, referenceName(entity.getType()));
			} else {
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, referenceName(JAVA_LANG_OBJECT));
			}

			mv.visitInsn(ARETURN);

			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable(THIS_REF, referenceName(internalClassName), null, l0, l1, 0);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		/**
		 * Generate {@link PersistentPropertyAccessor#getProperty(PersistentProperty)} . *
		 *
		 * <pre>
		 * {
		 * 	&#064;code
		 * 		 public Optional<? extends Object> getProperty(PersistentProperty<?> property){
		 * 			Object bean = this.bean;
		 * 			switch (property.getName().hashCode()) {
		 * 				case 3355:
		 * 					return id_fieldGetter..invoke(bean);
		 * 				case 3356:
		 * 					return bean.getField();
		 * 					// ...
		 * 				case 3357:
		 * 					return bean.field;
		 * 					// ...
		 *            }
		 * 			throw new UnsupportedOperationException(
		 * 					String.format("No MethodHandle to get property %s", new Object[] { property }));
		 *        }
		 * }
		 * </pre>
		 */
		private static void visitGetProperty(PersistentEntity<?, ?> entity,
				List<PersistentProperty<?>> persistentProperties, String internalClassName, ClassWriter cw) {

			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getProperty",
					"(Lorg/springframework/data/mapping/PersistentProperty;)Ljava/lang/Object;",
					"(Lorg/springframework/data/mapping/PersistentProperty<*>;)Ljava/lang/Object;", null);
			mv.visitCode();

			Label l0 = new Label();
			Label l1 = new Label();
			mv.visitLabel(l0);

			// Assert.notNull(property)
			visitAssertNotNull(mv);

			mv.visitVarInsn(ALOAD, 0);

			if (isAccessible(entity)) {
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, referenceName(entity.getType()));
			} else {
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, referenceName(JAVA_LANG_OBJECT));
			}
			mv.visitVarInsn(ASTORE, 2);

			visitGetPropertySwitch(entity, persistentProperties, internalClassName, mv);

			mv.visitLabel(l1);
			visitThrowUnsupportedOperationException(mv, "No accessor to get property %s");

			mv.visitLocalVariable(THIS_REF, referenceName(internalClassName), null, l0, l1, 0);
			mv.visitLocalVariable("property", referenceName(PERSISTENT_PROPERTY),
					"Lorg/springframework/data/mapping/PersistentProperty<*>;", l0, l1, 1);

			if (isAccessible(entity)) {
				mv.visitLocalVariable(BEAN_FIELD, referenceName(entity.getType()), null, l0, l1, 2);
			} else {
				mv.visitLocalVariable(BEAN_FIELD, referenceName(JAVA_LANG_OBJECT), null, l0, l1, 2);
			}

			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		/**
		 * Generate the {@code switch(hashcode) {label: }} block.
		 */
		private static void visitGetPropertySwitch(PersistentEntity<?, ?> entity,
				List<PersistentProperty<?>> persistentProperties, String internalClassName, MethodVisitor mv) {

			Map<String, PropertyStackAddress> propertyStackMap = createPropertyStackMap(persistentProperties);

			int[] hashes = new int[propertyStackMap.size()];
			Label[] switchJumpLabels = new Label[propertyStackMap.size()];
			List<PropertyStackAddress> stackmap = new ArrayList<>(propertyStackMap.values());
			Collections.sort(stackmap);

			for (int i = 0; i < stackmap.size(); i++) {

				PropertyStackAddress propertyStackAddress = stackmap.get(i);
				hashes[i] = propertyStackAddress.hash;
				switchJumpLabels[i] = propertyStackAddress.label;
			}

			Label dfltLabel = new Label();

			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEINTERFACE, PERSISTENT_PROPERTY, "getName",
					String.format("()%s", referenceName(JAVA_LANG_STRING)), true);
			mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING, "hashCode", "()I", false);
			mv.visitLookupSwitchInsn(dfltLabel, hashes, switchJumpLabels);

			for (PersistentProperty<?> property : persistentProperties) {

				mv.visitLabel(propertyStackMap.get(property.getName()).label);
				mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

				if (property.getGetter() != null || property.getField() != null) {
					visitGetProperty0(entity, property, mv, internalClassName);
				} else {
					mv.visitJumpInsn(GOTO, dfltLabel);
				}
			}

			mv.visitLabel(dfltLabel);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		}

		/**
		 * Generate property read access using a {@link java.lang.invoke.MethodHandle}.
		 * {@link java.lang.invoke.MethodHandle#invoke(Object...)} have a {@code @PolymorphicSignature} so {@code invoke} is
		 * called as if the method had the expected signature and not array/varargs.
		 */
		private static void visitGetProperty0(PersistentEntity<?, ?> entity, PersistentProperty<?> property,
				MethodVisitor mv, String internalClassName) {

			Method getter = property.getGetter();
			if (property.usePropertyAccess() && getter != null) {

				if (generateMethodHandle(entity, getter)) {
					// $getter.invoke(bean)
					mv.visitFieldInsn(GETSTATIC, internalClassName, getterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
					mv.visitVarInsn(ALOAD, 2);
					mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke",
							String.format("(%s)%s", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT)), false);
				} else {
					// bean.get...
					mv.visitVarInsn(ALOAD, 2);

					int invokeOpCode = INVOKEVIRTUAL;
					Class<?> declaringClass = getter.getDeclaringClass();
					boolean interfaceDefinition = declaringClass.isInterface();

					if (interfaceDefinition) {
						invokeOpCode = INVOKEINTERFACE;
					}

					mv.visitMethodInsn(invokeOpCode, Type.getInternalName(declaringClass), getter.getName(),
							String.format("()%s", signatureTypeName(getter.getReturnType())), interfaceDefinition);
					autoboxIfNeeded(getter.getReturnType(), autoboxType(getter.getReturnType()), mv);
				}
			} else {

				Field field = property.getRequiredField();

				if (generateMethodHandle(entity, field)) {
					// $fieldGetter.invoke(bean)
					mv.visitFieldInsn(GETSTATIC, internalClassName, fieldGetterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
					mv.visitVarInsn(ALOAD, 2);
					mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke",
							String.format("(%s)%s", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT)), false);
				} else {
					// bean.field
					mv.visitVarInsn(ALOAD, 2);
					mv.visitFieldInsn(GETFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(),
							signatureTypeName(field.getType()));
					autoboxIfNeeded(field.getType(), autoboxType(field.getType()), mv);
				}
			}

			mv.visitInsn(ARETURN);
		}

		/**
		 * Generate the {@link PersistentPropertyAccessor#setProperty(PersistentProperty, Object)} method. *
		 *
		 * <pre>
		 * {
		 * 	&#064;code
		 * 		public void setProperty(PersistentProperty<?> property, Optional<? extends Object> value) {
		 * 			Object bean = this.bean;
		 * 			switch (property.getName().hashCode()) {
		 * 				case 3355:
		 * 					$id_fieldSetter.invoke(bean, value);
		 * 					return;
		 * 				// ...
		 *            }
		 * 			throw new UnsupportedOperationException(
		 * 					String.format("No MethodHandle to set property %s", new Object[] { property }));
		 *        }
		 *    }
		 * </pre>
		 */
		private static void visitSetProperty(PersistentEntity<?, ?> entity,
				List<PersistentProperty<?>> persistentProperties, String internalClassName, ClassWriter cw) {

			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "setProperty",
					"(Lorg/springframework/data/mapping/PersistentProperty;Ljava/lang/Object;)V",
					"(Lorg/springframework/data/mapping/PersistentProperty<*>;Ljava/lang/Object;)V", null);
			mv.visitCode();

			Label l0 = new Label();
			mv.visitLabel(l0);

			visitAssertNotNull(mv);

			mv.visitVarInsn(ALOAD, 0);

			if (isAccessible(entity)) {
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, referenceName(entity.getType()));
			} else {
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, referenceName(JAVA_LANG_OBJECT));
			}

			mv.visitVarInsn(ASTORE, 3);

			visitSetPropertySwitch(entity, persistentProperties, internalClassName, mv);

			Label l1 = new Label();
			mv.visitLabel(l1);

			visitThrowUnsupportedOperationException(mv, "No accessor to set property %s");

			mv.visitLocalVariable(THIS_REF, referenceName(internalClassName), null, l0, l1, 0);
			mv.visitLocalVariable("property", "Lorg/springframework/data/mapping/PersistentProperty;",
					"Lorg/springframework/data/mapping/PersistentProperty<*>;", l0, l1, 1);
			mv.visitLocalVariable("value", referenceName(JAVA_LANG_OBJECT), null, l0, l1, 2);

			if (isAccessible(entity)) {
				mv.visitLocalVariable(BEAN_FIELD, referenceName(entity.getType()), null, l0, l1, 3);
			} else {
				mv.visitLocalVariable(BEAN_FIELD, referenceName(JAVA_LANG_OBJECT), null, l0, l1, 3);
			}

			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		/**
		 * Generate the {@code switch(hashcode) {label: }} block.
		 */
		private static void visitSetPropertySwitch(PersistentEntity<?, ?> entity,
				List<PersistentProperty<?>> persistentProperties, String internalClassName, MethodVisitor mv) {

			Map<String, PropertyStackAddress> propertyStackMap = createPropertyStackMap(persistentProperties);

			int[] hashes = new int[propertyStackMap.size()];
			Label[] switchJumpLabels = new Label[propertyStackMap.size()];
			List<PropertyStackAddress> stackmap = new ArrayList<>(propertyStackMap.values());
			Collections.sort(stackmap);

			for (int i = 0; i < stackmap.size(); i++) {
				PropertyStackAddress propertyStackAddress = stackmap.get(i);
				hashes[i] = propertyStackAddress.hash;
				switchJumpLabels[i] = propertyStackAddress.label;
			}

			Label dfltLabel = new Label();

			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEINTERFACE, PERSISTENT_PROPERTY, "getName",
					String.format("()%s", referenceName(JAVA_LANG_STRING)), true);
			mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING, "hashCode", "()I", false);
			mv.visitLookupSwitchInsn(dfltLabel, hashes, switchJumpLabels);

			for (PersistentProperty<?> property : persistentProperties) {
				mv.visitLabel(propertyStackMap.get(property.getName()).label);
				mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

				if (property.getSetter() != null || property.getField() != null) {
					visitSetProperty0(entity, property, mv, internalClassName);
				} else {
					mv.visitJumpInsn(GOTO, dfltLabel);
				}
			}

			mv.visitLabel(dfltLabel);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		}

		/**
		 * Generate property write access using a {@link java.lang.invoke.MethodHandle}. NOTE:
		 * {@link java.lang.invoke.MethodHandle#invoke(Object...)} have a {@code @PolymorphicSignature} so {@code invoke} is
		 * called as if the method had the expected signature and not array/varargs.
		 */
		private static void visitSetProperty0(PersistentEntity<?, ?> entity, PersistentProperty<?> property,
				MethodVisitor mv, String internalClassName) {

			Method setter = property.getSetter();
			if (property.usePropertyAccess() && setter != null) {

				if (generateMethodHandle(entity, setter)) {
					// $setter.invoke(bean)
					mv.visitFieldInsn(GETSTATIC, internalClassName, setterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
					mv.visitVarInsn(ALOAD, 3);
					mv.visitVarInsn(ALOAD, 2);
					mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke",
							String.format("(%s%s)V", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT)), false);
				} else {
					// bean.set...(object)
					mv.visitVarInsn(ALOAD, 3);
					mv.visitVarInsn(ALOAD, 2);

					Class<?> parameterType = setter.getParameterTypes()[0];
					mv.visitTypeInsn(CHECKCAST, Type.getInternalName(autoboxType(parameterType)));
					autoboxIfNeeded(autoboxType(parameterType), parameterType, mv);

					int invokeOpCode = INVOKEVIRTUAL;
					Class<?> declaringClass = setter.getDeclaringClass();
					boolean interfaceDefinition = declaringClass.isInterface();

					if (interfaceDefinition) {
						invokeOpCode = INVOKEINTERFACE;
					}

					mv.visitMethodInsn(invokeOpCode, Type.getInternalName(setter.getDeclaringClass()), setter.getName(),
							String.format("(%s)V", signatureTypeName(parameterType)), interfaceDefinition);
				}
			} else {

				Field field = property.getField();
				if (field != null) {
					if (generateSetterMethodHandle(entity, field)) {
						// $fieldSetter.invoke(bean, object)
						mv.visitFieldInsn(GETSTATIC, internalClassName, fieldSetterName(property),
								referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
						mv.visitVarInsn(ALOAD, 3);
						mv.visitVarInsn(ALOAD, 2);
						mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke",
								String.format("(%s%s)V", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT)), false);
					} else {
						// bean.field
						mv.visitVarInsn(ALOAD, 3);
						mv.visitVarInsn(ALOAD, 2);

						Class<?> fieldType = field.getType();

						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(autoboxType(fieldType)));
						autoboxIfNeeded(autoboxType(fieldType), fieldType, mv);
						mv.visitFieldInsn(PUTFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(),
								signatureTypeName(fieldType));
					}
				}
			}

			mv.visitInsn(RETURN);
		}

		private static void visitAssertNotNull(MethodVisitor mv) {

			// Assert.notNull(property)
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn("Property must not be null!");
			mv.visitMethodInsn(INVOKESTATIC, "org/springframework/util/Assert", "notNull",
					String.format("(%s%s)V", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_STRING)), false);
		}

		private static void visitThrowUnsupportedOperationException(MethodVisitor mv, String message) {

			// throw new UnsupportedOperationException(msg)
			mv.visitTypeInsn(NEW, JAVA_LANG_UNSUPPORTED_OPERATION_EXCEPTION);
			mv.visitInsn(DUP);
			mv.visitLdcInsn(message);
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_OBJECT);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKESTATIC, JAVA_LANG_STRING, "format",
					"(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
			mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_UNSUPPORTED_OPERATION_EXCEPTION, "<init>", "(Ljava/lang/String;)V",
					false);
			mv.visitInsn(ATHROW);
		}

		private static String fieldSetterName(PersistentProperty<?> property) {
			return String.format("$%s_fieldSetter", property.getName());
		}

		private static String fieldGetterName(PersistentProperty<?> property) {
			return String.format("$%s_fieldGetter", property.getName());
		}

		private static String setterName(PersistentProperty<?> property) {
			return String.format("$%s_setter", property.getName());
		}

		private static String getterName(PersistentProperty<?> property) {
			return String.format("$%s_getter", property.getName());
		}

		private static boolean isAccessible(PersistentEntity<?, ?> entity) {
			return isAccessible(entity.getType());
		}

		private static boolean isAccessible(Class<?> theClass) {
			return isAccessible(theClass.getModifiers());
		}

		private static boolean isAccessible(int modifiers) {
			return !Modifier.isPrivate(modifiers);
		}

		private static boolean isDefault(int modifiers) {

			return !(Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPublic(modifiers));
		}

		private static boolean generateSetterMethodHandle(PersistentEntity<?, ?> entity, @Nullable Field field) {

			if (field == null) {
				return false;
			}

			return generateMethodHandle(entity, field) || Modifier.isFinal(field.getModifiers());
		}

		/**
		 * Check whether to generate {@link java.lang.invoke.MethodHandle} access. Checks visibility rules of the member and
		 * its declaring class. Use also {@link java.lang.invoke.MethodHandle} if visibility is protected/package-default
		 * and packages of the declaring types are different.
		 */
		private static boolean generateMethodHandle(PersistentEntity<?, ?> entity, @Nullable Member member) {

			if (member == null) {
				return false;
			}

			if (isAccessible(entity)) {

				if (Modifier.isProtected(member.getModifiers()) || isDefault(member.getModifiers())) {
					if (!member.getDeclaringClass().getPackage().equals(entity.getType().getPackage())) {
						return true;
					}
				}

				if (isAccessible(member.getDeclaringClass()) && isAccessible(member.getModifiers())) {
					return false;
				}
			}

			return true;
		}

		/**
		 * Retrieves the class variable index with an offset of {@code 4}.
		 */
		private static int classVariableIndex4(List<Class<?>> list, Class<?> item) {
			return 4 + list.indexOf(item);
		}

		private static String generateClassName(PersistentEntity<?, ?> entity) {
			return entity.getType().getName() + TAG + Integer.toString(entity.hashCode(), 36);
		}
	}

	private static String referenceName(Class<?> type) {
		if (type.isArray()) {
			return Type.getInternalName(type);
		}
		return referenceName(Type.getInternalName(type));
	}

	private static String referenceName(String internalTypeName) {
		return String.format("L%s;", internalTypeName);
	}

	private static Map<String, PropertyStackAddress> createPropertyStackMap(
			List<PersistentProperty<?>> persistentProperties) {

		Map<String, PropertyStackAddress> stackmap = new HashMap<>();

		for (PersistentProperty<?> property : persistentProperties) {
			stackmap.put(property.getName(), new PropertyStackAddress(new Label(), property.getName().hashCode()));
		}
		return stackmap;
	}

	/**
	 * Returns the appropriate autoboxing type.
	 */
	private static Class<?> autoboxType(Class<?> unboxed) {

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
	private static void autoboxIfNeeded(Class<?> in, Class<?> out, MethodVisitor visitor) {

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
	 * Returns the signature type for a {@link Class} including primitives.
	 */
	private static String signatureTypeName(Class<?> type) {

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

	/**
	 * Stack map address for a particular property.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	static class PropertyStackAddress implements Comparable<PropertyStackAddress> {

		private final @NonNull Label label;
		private final int hash;

		/*
		 * (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(@SuppressWarnings("null") PropertyStackAddress o) {
			return hash < o.hash ? -1 : hash == o.hash ? 0 : 1;
		}
	}
}
