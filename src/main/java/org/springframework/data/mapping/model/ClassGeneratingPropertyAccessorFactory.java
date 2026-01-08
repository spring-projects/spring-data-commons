/*
 * Copyright 2016-present the original author or authors.
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
import static org.springframework.data.mapping.model.BytecodeUtil.*;

import kotlin.reflect.KParameter;
import kotlin.reflect.KParameter.Kind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.KotlinCopyMethod.KotlinCopyByProperty;
import org.springframework.data.mapping.model.KotlinValueUtils.ValueBoxing;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A factory that can generate byte code to speed-up dynamic property access. Uses the {@link PersistentEntity}'s
 * {@link PersistentProperty} to discover the access to properties. Properties are accessed either using method handles
 * to overcome Java visibility issues or directly using field access/getter/setter calls.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Johannes Englmeier
 * @since 1.13
 */
public class ClassGeneratingPropertyAccessorFactory implements PersistentPropertyAccessorFactory {

	// Pooling of parameter arrays to prevent excessive object allocation.
	private final ThreadLocal<Object[]> argumentCache = ThreadLocal.withInitial(() -> new Object[1]);

	private volatile Map<PersistentEntity<?, ?>, Constructor<?>> constructorMap = new HashMap<>(32);
	private volatile Map<TypeInformation<?>, Class<PersistentPropertyAccessor<?>>> propertyAccessorClasses = new HashMap<>(
			32);

	private final ConcurrentLruCache<PersistentProperty<?>, Function<@Nullable Object, @Nullable Object>> wrapperCache = new ConcurrentLruCache<>(
			256, KotlinValueBoxingAdapter::getWrapper);

	@Override
	public <T> PersistentPropertyAccessor<T> getPropertyAccessor(PersistentEntity<?, ?> entity, T bean) {

		Constructor<?> constructor = getPropertyAccessorConstructor(entity);

		Object[] args = argumentCache.get();
		args[0] = bean;

		try {

			PersistentPropertyAccessor<T> accessor = (PersistentPropertyAccessor<T>) constructor.newInstance(args);

			if (KotlinDetector.isKotlinType(entity.getType())) {
				return new KotlinValueBoxingAdapter<>(entity, accessor, wrapperCache);
			}

			return accessor;
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Cannot create persistent property accessor for %s", entity), e);
		} finally {
			args[0] = null;
		}
	}

	private Constructor<?> getPropertyAccessorConstructor(PersistentEntity<?, ?> entity) {

		Constructor<?> constructor = constructorMap.get(entity);

		if (constructor == null) {

			Class<PersistentPropertyAccessor<?>> accessorClass = potentiallyCreateAndRegisterPersistentPropertyAccessorClass(
					entity);
			constructor = accessorClass.getConstructors()[0];

			Map<PersistentEntity<?, ?>, Constructor<?>> constructorMap = new HashMap<>(this.constructorMap);
			constructorMap.put(entity, constructor);
			this.constructorMap = constructorMap;
		}

		return constructor;
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

		Assert.notNull(entity, "PersistentEntity must not be null");

		// already present in classloader
		if (findAccessorClass(entity) != null) {
			return true;
		}

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
				&& (type.getPackage() == null || !type.getPackage().getName().startsWith("java"))
				&& ClassUtils.isPresent(PersistentPropertyAccessor.class.getName(), type.getClassLoader())
				&& ClassUtils.isPresent(Assert.class.getName(), type.getClassLoader());
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
	protected synchronized Class<PersistentPropertyAccessor<?>> potentiallyCreateAndRegisterPersistentPropertyAccessorClass(
			PersistentEntity<?, ?> entity) {

		Map<TypeInformation<?>, Class<PersistentPropertyAccessor<?>>> map = this.propertyAccessorClasses;
		Class<PersistentPropertyAccessor<?>> propertyAccessorClass = map.get(entity.getTypeInformation());

		if (propertyAccessorClass != null) {
			return propertyAccessorClass;
		}

		propertyAccessorClass = loadOrCreateAccessorClass(entity);

		map = new HashMap<>(map);
		map.put(entity.getTypeInformation(), propertyAccessorClass);

		this.propertyAccessorClasses = map;

		return propertyAccessorClass;
	}

	@SuppressWarnings({ "unchecked" })
	private Class<PersistentPropertyAccessor<?>> loadOrCreateAccessorClass(PersistentEntity<?, ?> entity) {

		try {

			Class<?> accessorClass = findAccessorClass(entity);
			if (accessorClass != null) {
				return (Class<PersistentPropertyAccessor<?>>) accessorClass;
			}

			return (Class<PersistentPropertyAccessor<?>>) PropertyAccessorClassGenerator.generateCustomAccessorClass(entity);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static @Nullable Class<?> findAccessorClass(PersistentEntity<?, ?> entity) {

		String accessorClassName = PropertyAccessorClassGenerator.generateClassName(entity);

		return org.springframework.data.util.ClassUtils.loadIfPresent(accessorClassName, entity.getType().getClassLoader());
	}

	/**
	 * Generates {@link PersistentPropertyAccessor} classes to access properties of a {@link PersistentEntity}. This code
	 * uses {@code private static final} held method handles which perform about the speed of native method invocations
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
	 * <pre class="code">
	 * public class PersonWithId_Accessor_zd4wnl implements PersistentPropertyAccessor {
	 * 	private final Object bean;
	 * 	private static final MethodHandle $id_fieldGetter;
	 * 	private static final MethodHandle $id_fieldSetter;
	 *
	 * 	// ...
	 * 	public PersonWithId_Accessor_zd4wnl(Object bean) {
	 * 		this.bean = bean;
	 * 	}
	 *
	 * 	static {
	 * 		Method getter;
	 * 		Method setter;
	 * 		MethodHandles.Lookup lookup = MethodHandles.lookup();
	 * 		Class class_1 = Class.forName("org.springframework.data.mapping.Person");
	 * 		Class class_2 = Class.forName("org.springframework.data.mapping.PersonWithId");
	 * 		Field field = class_2.getDeclaredField("id");
	 * 		field.setAccessible(true);
	 * 		$id_fieldGetter = lookup.unreflectGetter(field);
	 * 		$id_fieldSetter = lookup.unreflectSetter(field);
	 * 		// ...
	 * 	}
	 *
	 * 	public Object getBean() {
	 * 		return this.bean;
	 * 	}
	 *
	 * 	public void setProperty(PersistentProperty<?> property, Object value) {
	 * 		Object bean = this.bean;
	 * 		switch (property.getName().hashCode()) {
	 * 			case 3355:
	 * 				$id_fieldSetter.invoke(bean, value);
	 * 				return;
	 * 			case 3357:
	 * 				this.bean = $id_wither.invoke(bean, value);
	 * 				return;
	 * 			case 3358:
	 * 				this.bean = bean.withId(value);
	 * 				return;
	 * 			case 3359:
	 * 				this.bean = PersonWithId.copy$default(bean, value, 0, null); // Kotlin
	 * 				return;
	 * 			// …
	 * 		}
	 * 		throw new UnsupportedOperationException(
	 * 				String.format("No accessor to set property %s", new Object[] { property }));
	 * 	}
	 *
	 * 	public Object getProperty(PersistentProperty<?> property) {
	 * 		Object bean = this.bean;
	 * 		switch (property.getName().hashCode()) {
	 * 			case 3355:
	 * 				return id_fieldGetter.invoke(bean);
	 * 			case 3356:
	 * 				return bean.getField();
	 * 			// …
	 * 			case 3357:
	 * 				return bean.field;
	 * 				// …
	 * 				throw new UnsupportedOperationException(
	 * 						String.format("No accessor to get property %s", new Object[] { property }));
	 * 		}
	 * 	}
	 * }
	 * </pre>
	 *
	 * @author Mark Paluch
	 */
	static class PropertyAccessorClassGenerator {

		private static final String INIT = "<init>";
		private static final String CLINIT = "<clinit>";
		private static final String TAG = "__Accessor_";
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
			Class<?> type = entity.getType();
			ClassLoader classLoader = type.getClassLoader();

			if (ClassUtils.isPresent(className, classLoader)) {

				try {
					return ClassUtils.forName(className, classLoader);
				} catch (Exception o_O) {
					throw new IllegalStateException(o_O);
				}
			}

			byte[] bytecode = generateBytecode(className.replace('.', '/'), entity);

			try {

				return ReflectUtils.defineClass(className, bytecode, classLoader, type.getProtectionDomain(), type);
			} catch (Exception o_O) {
				throw new IllegalStateException(o_O);
			}
		}

		/**
		 * Generate a new class for the given {@link PersistentEntity}.
		 */
		static byte[] generateBytecode(String internalClassName, PersistentEntity<?, ?> entity) {

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			String signature = referenceName(JAVA_LANG_OBJECT) + String.format("L%s<%s>;",
					Type.getInternalName(PersistentPropertyAccessor.class), referenceName(entity.getType()));

			cw.visit(Opcodes.V1_8, ACC_PUBLIC + ACC_SUPER, internalClassName, signature, JAVA_LANG_OBJECT,
					IMPLEMENTED_INTERFACES);

			List<PersistentProperty<?>> persistentProperties = getPersistentProperties(entity);

			visitFields(entity, persistentProperties, cw);
			visitDefaultConstructor(entity, internalClassName, cw);

			if (requiresMethodHandleSetup(entity, persistentProperties)) {
				visitStaticInitializer(entity, persistentProperties, internalClassName, cw);
			}

			visitBeanGetter(entity, internalClassName, cw);
			visitSetProperty(entity, persistentProperties, internalClassName, cw);
			visitGetProperty(entity, persistentProperties, internalClassName, cw);

			cw.visitEnd();

			return cw.toByteArray();
		}

		private static List<PersistentProperty<?>> getPersistentProperties(PersistentEntity<?, ?> entity) {

			final List<PersistentProperty<?>> persistentProperties = new ArrayList<>();

			entity.doWithAssociations((SimpleAssociationHandler) association -> {
				persistentProperties.add(association.getInverse());
			});

			entity.doWithProperties((SimplePropertyHandler) persistentProperties::add);

			return persistentProperties;
		}

		/**
		 * Generates field declarations for private-visibility properties.
		 *
		 * <pre class="code">
		 * private final Object bean;
		 * private static final MethodHandle $id_fieldGetter;
		 * private static final MethodHandle $id_fieldSetter;
		 * // …
		 * </pre>
		 */
		private static void visitFields(PersistentEntity<?, ?> entity, List<PersistentProperty<?>> persistentProperties,
				ClassWriter cw) {

			cw.visitInnerClass(JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP, JAVA_LANG_INVOKE_METHOD_HANDLES, "Lookup",
					ACC_PRIVATE + ACC_FINAL + ACC_STATIC);

			cw.visitField(ACC_PRIVATE, BEAN_FIELD, getAccessibleTypeReferenceName(entity), null, null).visitEnd();

			for (PersistentProperty<?> property : persistentProperties) {

				if (property.isImmutable()) {
					if (generateMethodHandle(entity, property.getWither())) {
						cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, witherName(property),
								referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
					}
				} else {
					if (generateMethodHandle(entity, property.getSetter())) {
						cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, setterName(property),
								referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
					}
				}

				if (generateMethodHandle(entity, property.getGetter())) {
					cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, getterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
				}

				if (generateSetterMethodHandle(entity, property.getField())) {

					if (!property.isImmutable()) {
						cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, fieldSetterName(property),
								referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
					}

					cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, fieldGetterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE), null, null).visitEnd();
				}
			}
		}

		/**
		 * Generates the default constructor.
		 *
		 * <pre class="code">
		 * public PersonWithId_Accessor_zd4wnl(PersonWithId bean) {
		 * 	this.bean = bean;
		 * }
		 * </pre>
		 */
		private static void visitDefaultConstructor(PersistentEntity<?, ?> entity, String internalClassName,
				ClassWriter cw) {

			// public EntityAccessor(Entity bean) or EntityAccessor(Object bean)
			MethodVisitor mv;

			mv = cw.visitMethod(ACC_PUBLIC, INIT, String.format("(%s)V", getAccessibleTypeReferenceName(entity)), null, null);

			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, INIT, "()V", false);

			// Assert.notNull(bean)
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn("Bean must not be null");
			mv.visitMethodInsn(INVOKESTATIC, "org/springframework/util/Assert", "notNull",
					String.format("(%s%s)V", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_STRING)), false);

			// this.bean = bean
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);

			mv.visitFieldInsn(PUTFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));

			mv.visitInsn(RETURN);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitLocalVariable(THIS_REF, referenceName(internalClassName), null, l0, l3, 0);
			mv.visitLocalVariable(BEAN_FIELD, getAccessibleTypeReferenceName(entity), null, l0, l3, 1);

			mv.visitMaxs(2, 2);
		}

		/**
		 * Generates the static initializer block.
		 *
		 * <pre class="code">
		 * static {
		 * 	Method getter;
		 * 	Method setter;
		 * 	MethodHandles.Lookup lookup = MethodHandles.lookup();
		 * 	Field field = org.springframework.data.mapping.PersonWithId.class.getDeclaredField("id");
		 * 	field.setAccessible(true);
		 * 	$id_fieldGetter = lookup.unreflectGetter(field);
		 * 	$id_fieldSetter = lookup.unreflectSetter(field);
		 * 	// …
		 * }
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

			for (PersistentProperty<?> property : persistentProperties) {

				if (property.usePropertyAccess()) {

					if (generateMethodHandle(entity, property.getGetter())) {
						visitPropertyGetterInitializer(property, mv, internalClassName);
					}

					if (generateMethodHandle(entity, property.getSetter())) {
						visitPropertySetterInitializer(property.getSetter(), property, mv, internalClassName,
								PropertyAccessorClassGenerator::setterName, 2);
					}
				}

				if (property.isImmutable() && generateMethodHandle(entity, property.getWither())) {
					visitPropertySetterInitializer(property.getWither(), property, mv, internalClassName,
							PropertyAccessorClassGenerator::witherName, 4);
				}

				if (generateSetterMethodHandle(entity, property.getField())) {
					visitFieldGetterSetterInitializer(property, mv, internalClassName);
				}
			}

			mv.visitLabel(l1);
			mv.visitInsn(RETURN);

			mv.visitLocalVariable("lookup", referenceName(JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP), null, l0, l1, 0);
			mv.visitLocalVariable("field", referenceName(JAVA_LANG_REFLECT_FIELD), null, l0, l1, 1);
			mv.visitLocalVariable("setter", referenceName(JAVA_LANG_REFLECT_METHOD), null, l0, l1, 2);
			mv.visitLocalVariable("getter", referenceName(JAVA_LANG_REFLECT_METHOD), null, l0, l1, 3);
			mv.visitLocalVariable("wither", referenceName(JAVA_LANG_REFLECT_METHOD), null, l0, l1, 4);

			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		static boolean requiresMethodHandleSetup(PersistentEntity<?, ?> entity,
				List<PersistentProperty<?>> persistentProperties) {

			for (PersistentProperty<?> property : persistentProperties) {

				if (property.usePropertyAccess()) {

					if (generateMethodHandle(entity, property.getGetter())) {
						return true;
					}

					if (generateMethodHandle(entity, property.getSetter())) {
						return true;
					}
				}

				if (property.isImmutable() && generateMethodHandle(entity, property.getWither())) {
					return true;
				}

				if (generateSetterMethodHandle(entity, property.getField())) {
					return true;
				}
			}

			return false;
		}

		/**
		 * Generate property getter initializer.
		 */
		private static void visitPropertyGetterInitializer(PersistentProperty<?> property, MethodVisitor mv,
				String internalClassName) {

			// getter = <entity>.class.getDeclaredMethod()
			Method getter = property.getGetter();

			if (getter != null) {

				mv.visitLdcInsn(Type.getObjectType(Type.getInternalName(getter.getDeclaringClass())));
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
		 * Generate property setter/wither initializer.
		 */
		private static void visitPropertySetterInitializer(@Nullable Method method, PersistentProperty<?> property,
				MethodVisitor mv, String internalClassName, Function<PersistentProperty<?>, String> setterNameFunction,
				int localVariableIndex) {

			// method = <entity>.class.getDeclaredMethod()

			if (method != null) {

				mv.visitLdcInsn(Type.getObjectType(Type.getInternalName(method.getDeclaringClass())));
				mv.visitLdcInsn(method.getName());

				mv.visitInsn(ICONST_1);
				mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_CLASS);
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_0);

				Class<?> parameterType = method.getParameterTypes()[0];

				if (parameterType.isPrimitive()) {
					mv.visitFieldInsn(GETSTATIC, Type.getInternalName(autoboxType(method.getParameterTypes()[0])), "TYPE",
							referenceName(JAVA_LANG_CLASS));
				} else {
					mv.visitLdcInsn(Type.getType(referenceName(parameterType)));
				}

				mv.visitInsn(AASTORE);

				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_CLASS, "getDeclaredMethod", String.format("(%s[%s)%s",
						referenceName(JAVA_LANG_STRING), referenceName(JAVA_LANG_CLASS), referenceName(JAVA_LANG_REFLECT_METHOD)),
						false);
				mv.visitVarInsn(ASTORE, localVariableIndex);

				// wither/setter.setAccessible(true)
				mv.visitVarInsn(ALOAD, localVariableIndex);
				mv.visitInsn(ICONST_1);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_REFLECT_METHOD, SET_ACCESSIBLE, "(Z)V", false);

				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, localVariableIndex);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP, "unreflect", String.format("(%s)%s",
						referenceName(JAVA_LANG_REFLECT_METHOD), referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE)), false);
			}

			if (method == null) {
				mv.visitInsn(ACONST_NULL);
			}

			mv.visitFieldInsn(PUTSTATIC, internalClassName, setterNameFunction.apply(property),
					referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
		}

		/**
		 * Generate field getter and setter initializers.
		 */
		private static void visitFieldGetterSetterInitializer(PersistentProperty<?> property, MethodVisitor mv,
				String internalClassName) {

			// field = <entity>.class.getDeclaredField()

			Field field = property.getField();
			if (field != null) {

				mv.visitLdcInsn(Type.getObjectType(Type.getInternalName(field.getDeclaringClass())));
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

				if (!property.isImmutable()) {

					// $fieldSetter = lookup.unreflectSetter(field)
					mv.visitVarInsn(ALOAD, 0);
					mv.visitVarInsn(ALOAD, 1);
					mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP, "unreflectSetter", String.format(
							"(%s)%s", referenceName(JAVA_LANG_REFLECT_FIELD), referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE)), false);
					mv.visitFieldInsn(PUTSTATIC, internalClassName, fieldSetterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
				}
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

			mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));

			mv.visitInsn(ARETURN);

			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable(THIS_REF, referenceName(internalClassName), null, l0, l1, 0);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		/**
		 * Generate {@link PersistentPropertyAccessor#getProperty(PersistentProperty)}.
		 *
		 * <pre class="code">
		 * public Object getProperty(PersistentProperty<?> property) {
		 * 	switch (property.getName().hashCode()) {
		 * 		case 3355:
		 * 			return id_fieldGetter.invoke(this.bean);
		 * 		case 3356:
		 * 			return this.bean.getField();
		 * 		// …
		 * 		case 3357:
		 * 			return this.bean.field;
		 * 		// …
		 * 	}
		 * 	throw new UnsupportedOperationException(
		 * 			String.format("No accessor to get property %s", new Object[] { property }));
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

			visitGetPropertySwitch(entity, persistentProperties, internalClassName, mv);

			mv.visitLabel(l1);
			visitThrowUnsupportedOperationException(mv, "No accessor to get property %s");

			mv.visitLocalVariable(THIS_REF, referenceName(internalClassName), null, l0, l1, 0);
			mv.visitLocalVariable("property", referenceName(PERSISTENT_PROPERTY),
					"Lorg/springframework/data/mapping/PersistentProperty<*>;", l0, l1, 1);

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

				PropertyStackAddress propertyStackAddress = propertyStackMap.get(property.getName());
				if (propertyStackAddress == null) {
					throw new IllegalStateException(
							"No PropertyStackAddress found for property %s".formatted(property.getName()));
				}
				mv.visitLabel(propertyStackAddress.label);
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
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
					mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke",
							String.format("(%s)%s", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT)), false);
				} else {
					// bean.get…
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));

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
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
					mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke",
							String.format("(%s)%s", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT)), false);
				} else {
					// bean.field
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
					mv.visitFieldInsn(GETFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(),
							signatureTypeName(field.getType()));
					autoboxIfNeeded(field.getType(), autoboxType(field.getType()), mv);
				}
			}

			mv.visitInsn(ARETURN);
		}

		/**
		 * Generate the {@link PersistentPropertyAccessor#setProperty(PersistentProperty, Object)} method.
		 *
		 * <pre class="code">
		 * public void setProperty(PersistentProperty<?> property, Optional<? extends Object> value) {
		 * 	switch (property.getName().hashCode()) {
		 * 		case 3355:
		 * 			$id_fieldSetter.invoke(this.bean, value);
		 * 			return;
		 * 		case 3357:
		 * 			this.bean = $id_fieldWither.invoke(this.bean, value);
		 * 			return;
		 * 		case 3358:
		 * 			this.bean = this.bean.withId(value);
		 * 			return;
		 * 		case 3359:
		 * 			this.bean = PersonWithId.copy$default(this.bean, value, 0, null); // Kotlin
		 * 			return;
		 * 		// …
		 * 	}
		 * 	throw new UnsupportedOperationException(
		 * 			String.format("No accessor to set property %s", new Object[] { property }));
		 * }
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

			visitSetPropertySwitch(entity, persistentProperties, internalClassName, mv);

			Label l1 = new Label();
			mv.visitLabel(l1);

			visitThrowUnsupportedOperationException(mv, "No accessor to set property %s");

			mv.visitLocalVariable(THIS_REF, referenceName(internalClassName), null, l0, l1, 0);
			mv.visitLocalVariable("property", "Lorg/springframework/data/mapping/PersistentProperty;",
					"Lorg/springframework/data/mapping/PersistentProperty<*>;", l0, l1, 1);
			mv.visitLocalVariable("value", referenceName(JAVA_LANG_OBJECT), null, l0, l1, 2);

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

				PropertyStackAddress propertyStackAddress = propertyStackMap.get(property.getName());

				if (propertyStackAddress == null) {
					throw new IllegalStateException(
							"No PropertyStackAddress found for property %s".formatted(property.getName()));
				}

				mv.visitLabel(propertyStackAddress.label);
				mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

				if (supportsMutation(property)) {
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
			Method wither = property.getWither();

			if (property.isImmutable()) {

				if (wither != null) {
					visitWithProperty(entity, property, mv, internalClassName, wither);
				}

				if (KotlinDetector.isKotlinType(entity.getType()) && KotlinCopyMethod.hasKotlinCopyMethodFor(property)) {
					visitKotlinCopy(entity, property, mv, internalClassName);
				}

			} else if (property.usePropertyAccess() && setter != null) {
				visitSetProperty(entity, property, mv, internalClassName, setter);
			} else {
				visitSetField(entity, property, mv, internalClassName);
			}

			mv.visitInsn(RETURN);
		}

		/**
		 * Generates:
		 *
		 * <pre class="code">
		 * this.bean = this.bean = bean.withId(value);
		 * </pre>
		 */
		private static void visitWithProperty(PersistentEntity<?, ?> entity, PersistentProperty<?> property,
				MethodVisitor mv, String internalClassName, Method wither) {

			// this. <- for later PUTFIELD
			mv.visitVarInsn(ALOAD, 0);

			if (generateMethodHandle(entity, wither)) {

				// $wither.invoke(bean)
				mv.visitFieldInsn(GETSTATIC, internalClassName, witherName(property),
						referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));

				mv.visitVarInsn(ALOAD, 2);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke", String.format("(%s%s)%s",
						referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT), getAccessibleTypeReferenceName(entity)),
						false);
				if (isAccessible(entity)) {
					mv.visitTypeInsn(CHECKCAST, Type.getInternalName(entity.getType()));
				}
			} else {

				// this.bean.set...(object)
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
				mv.visitVarInsn(ALOAD, 2);

				visitInvokeMethodSingleArg(mv, wither);
			}

			mv.visitFieldInsn(PUTFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
		}

		/**
		 * Generates:
		 *
		 * <pre class="code">
		 * this.bean = bean.copy(value);
		 * </pre>
		 *
		 * or
		 *
		 * <pre class="code">
		 * this.bean = bean.copy$default..(bean, object, MASK, null)
		 * </pre>
		 */
		private static void visitKotlinCopy(PersistentEntity<?, ?> entity, PersistentProperty<?> property, MethodVisitor mv,
				String internalClassName) {

			KotlinCopyMethod kotlinCopyMethod = KotlinCopyMethod.findCopyMethod(entity.getType())
					.orElseThrow(() -> new IllegalStateException(
							String.format("No usable .copy(…) method found in entity %s", entity.getType().getName())));

			// this. <- for later PUTFIELD
			mv.visitVarInsn(ALOAD, 0);

			if (kotlinCopyMethod.shouldUsePublicCopyMethod(entity)) {

				// PersonWithId.copy$(value)
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
				mv.visitVarInsn(ALOAD, 2);

				visitInvokeMethodSingleArg(mv, kotlinCopyMethod.getPublicCopyMethod());
			} else {

				Method copy = kotlinCopyMethod.getSyntheticCopyMethod();
				Class<?>[] parameterTypes = copy.getParameterTypes();

				// PersonWithId.copy$default..(this.bean, object, MASK, null)
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));

				KotlinCopyMethod.KotlinCopyByProperty copyByProperty = kotlinCopyMethod.forProperty(property)
						.orElseThrow(() -> new IllegalStateException(
								String.format("No usable .copy(…) method found for property %s", property)));

				for (int i = 1; i < kotlinCopyMethod.getParameterCount(); i++) {

					if (copyByProperty.getParameterPosition() == i) {

						mv.visitVarInsn(ALOAD, 2);

						mv.visitTypeInsn(CHECKCAST, Type.getInternalName(autoboxType(parameterTypes[i])));
						autoboxIfNeeded(autoboxType(parameterTypes[i]), parameterTypes[i], mv);

						continue;
					}

					visitDefaultValue(parameterTypes[i], mv);
				}

				copyByProperty.getDefaultMask().forEach(mv::visitLdcInsn);

				mv.visitInsn(Opcodes.ACONST_NULL);

				int invokeOpCode = getInvokeOp(copy, false);

				mv.visitMethodInsn(invokeOpCode, Type.getInternalName(copy.getDeclaringClass()), copy.getName(),
						getArgumentSignature(copy), false);
			}

			mv.visitFieldInsn(PUTFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
		}

		/**
		 * Generate:
		 *
		 * <pre class="code">
		 * $id_fieldSetter.invoke(bean, value);
		 * </pre>
		 *
		 * or
		 *
		 * <pre class="code">
		 * this.bean.setId(value);
		 * </pre>
		 */
		private static void visitSetProperty(PersistentEntity<?, ?> entity, PersistentProperty<?> property,
				MethodVisitor mv, String internalClassName, Method setter) {

			if (generateMethodHandle(entity, setter)) {

				// $setter.invoke(this.bean)
				mv.visitFieldInsn(GETSTATIC, internalClassName, setterName(property),
						referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
				mv.visitVarInsn(ALOAD, 2);
				mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke",
						String.format("(%s%s)V", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT)), false);
			} else {

				// this.bean.set...(object)
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
				mv.visitVarInsn(ALOAD, 2);

				visitInvokeMethodSingleArg(mv, setter);
			}
		}

		/**
		 * Generate:
		 *
		 * <pre class="code">
		 * $id_fieldSetter.invoke(this.bean, value);
		 * </pre>
		 *
		 * or
		 *
		 * <pre class="code">
		 * bean.id = value;
		 * </pre>
		 */
		private static void visitSetField(PersistentEntity<?, ?> entity, PersistentProperty<?> property, MethodVisitor mv,
				String internalClassName) {

			Field field = property.getField();
			if (field != null) {
				if (generateSetterMethodHandle(entity, field)) {
					// $fieldSetter.invoke(this.bean, object)
					mv.visitFieldInsn(GETSTATIC, internalClassName, fieldSetterName(property),
							referenceName(JAVA_LANG_INVOKE_METHOD_HANDLE));
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
					mv.visitVarInsn(ALOAD, 2);
					mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_INVOKE_METHOD_HANDLE, "invoke",
							String.format("(%s%s)V", referenceName(JAVA_LANG_OBJECT), referenceName(JAVA_LANG_OBJECT)), false);
				} else {
					// this.bean.field
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, internalClassName, BEAN_FIELD, getAccessibleTypeReferenceName(entity));
					mv.visitVarInsn(ALOAD, 2);

					Class<?> fieldType = field.getType();

					mv.visitTypeInsn(CHECKCAST, Type.getInternalName(autoboxType(fieldType)));
					autoboxIfNeeded(autoboxType(fieldType), fieldType, mv);
					mv.visitFieldInsn(PUTFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(),
							signatureTypeName(fieldType));
				}
			}
		}

		/**
		 * Creates the method signature containing parameter types (e.g. (Ljava/lang/Object)I for a method accepting
		 * {@link Object} and returning a primitive {@code int}).
		 *
		 * @param method must not be {@literal null}.
		 * @return a string representation of the given method.
		 * @since 2.1
		 */
		private static String getArgumentSignature(Method method) {

			StringBuilder result = new StringBuilder("(");
			List<String> argumentTypes = new ArrayList<>();

			for (Class<?> parameterType : method.getParameterTypes()) {

				result.append("%s");
				argumentTypes.add(signatureTypeName(parameterType));
			}

			result.append(")%s");
			argumentTypes.add(signatureTypeName(method.getReturnType()));

			return String.format(result.toString(), argumentTypes.toArray());
		}

		private static void visitAssertNotNull(MethodVisitor mv) {

			// Assert.notNull(property)
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn("Property must not be null");
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

		private static String witherName(PersistentProperty<?> property) {
			return String.format("$%s_wither", property.getName());
		}

		private static String getterName(PersistentProperty<?> property) {
			return String.format("$%s_getter", property.getName());
		}

		private static boolean isAccessible(PersistentEntity<?, ?> entity) {
			return BytecodeUtil.isAccessible(entity.getType());
		}

		private static String getAccessibleTypeReferenceName(PersistentEntity<?, ?> entity) {

			if (isAccessible(entity)) {
				return referenceName(entity.getType());
			}

			return referenceName(JAVA_LANG_OBJECT);
		}

		private static boolean generateSetterMethodHandle(PersistentEntity<?, ?> entity, @Nullable Field field) {

			if (field == null) {
				return false;
			}

			return generateMethodHandle(entity, field);
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
					Package declaringPackage = member.getDeclaringClass().getPackage();

					if (declaringPackage != null && !declaringPackage.equals(entity.getType().getPackage())) {
						return true;
					}
				}

				if (BytecodeUtil.isAccessible(member.getDeclaringClass()) && BytecodeUtil.isAccessible(member.getModifiers())) {
					return false;
				}
			}

			return true;
		}

		static String generateClassName(PersistentEntity<?, ?> entity) {
			return entity.getType().getName() + TAG + Integer.toString(Math.abs(entity.getType().getName().hashCode()), 36);
		}
	}

	private static void visitInvokeMethodSingleArg(MethodVisitor mv, Method method) {

		Class<?>[] parameterTypes = method.getParameterTypes();
		Class<?> parameterType = parameterTypes[0];
		Class<?> declaringClass = method.getDeclaringClass();
		boolean interfaceDefinition = declaringClass.isInterface();

		mv.visitTypeInsn(CHECKCAST, Type.getInternalName(autoboxType(parameterType)));
		autoboxIfNeeded(autoboxType(parameterType), parameterType, mv);

		int invokeOpCode = getInvokeOp(method, interfaceDefinition);

		mv.visitMethodInsn(invokeOpCode, Type.getInternalName(method.getDeclaringClass()), method.getName(),
				String.format("(%s)%s", signatureTypeName(parameterType), signatureTypeName(method.getReturnType())),
				interfaceDefinition);
	}

	private static int getInvokeOp(Method method, boolean interfaceDefinition) {

		int invokeOpCode = Modifier.isStatic(method.getModifiers()) ? INVOKESTATIC : INVOKEVIRTUAL;

		if (interfaceDefinition) {
			invokeOpCode = INVOKEINTERFACE;
		}
		return invokeOpCode;
	}

	private static Map<String, PropertyStackAddress> createPropertyStackMap(
			List<PersistentProperty<?>> persistentProperties) {

		Map<String, PropertyStackAddress> stackmap = new HashMap<>(persistentProperties.size());

		for (PersistentProperty<?> property : persistentProperties) {
			stackmap.put(property.getName(), new PropertyStackAddress(new Label(), property.getName().hashCode()));
		}
		return stackmap;
	}

	/**
	 * Stack map address for a particular property.
	 *
	 * @author Mark Paluch
	 */
	static class PropertyStackAddress implements Comparable<PropertyStackAddress> {

		private final Label label;
		private final int hash;

		public PropertyStackAddress(Label label, int hash) {
			this.label = label;
			this.hash = hash;
		}

		@Override
		public int compareTo(PropertyStackAddress o) {
			return Integer.compare(hash, o.hash);
		}
	}

	/**
	 * @param property the persistent property to inspect.
	 * @return {@literal true} if object mutation is supported.
	 */
	static boolean supportsMutation(PersistentProperty<?> property) {
		return (property.usePropertyAccess() && property.getSetter() != null) || property.isReadable();
	}

	/**
	 * Adapter to encapsulate Kotlin's value class boxing when properties are nullable.
	 *
	 * @param entity the entity that could use value class properties.
	 * @param delegate the property accessor to delegate to.
	 * @param wrapperCache cache for wrapping functions.
	 * @param <T>
	 * @since 3.2
	 */
	record KotlinValueBoxingAdapter<T>(PersistentEntity<?, ?> entity, PersistentPropertyAccessor<T> delegate,
			ConcurrentLruCache<PersistentProperty<?>, Function<@Nullable Object, @Nullable Object>> wrapperCache)
			implements
				PersistentPropertyAccessor<T> {

		@Override
		public void setProperty(PersistentProperty<?> property, @Nullable Object value) {
			delegate.setProperty(property, wrapperCache.get(property).apply(value));
		}

		/**
		 * Create a wrapper function if the {@link PersistentProperty} uses value classes.
		 *
		 * @param property the persistent property to inspect.
		 * @return a wrapper function to wrap a value class component into the hierarchy of value classes or
		 *         {@link Function#identity()} if wrapping is not necessary.
		 * @see KotlinValueUtils#getCopyValueHierarchy(KParameter)
		 */
		static Function<@Nullable Object, @Nullable Object> getWrapper(PersistentProperty<?> property) {

			Optional<KotlinCopyMethod> kotlinCopyMethod = KotlinCopyMethod.findCopyMethod(property.getOwner().getType())
					.filter(it -> it.supportsProperty(property));

			if (kotlinCopyMethod.isPresent()
					&& kotlinCopyMethod.filter(it -> it.forProperty(property).isPresent()).isPresent()) {
				KotlinCopyMethod copyMethod = kotlinCopyMethod.get();

				Optional<KParameter> kParameter = kotlinCopyMethod.stream()
						.flatMap(it -> it.getCopyFunction().getParameters().stream()) //
						.filter(kf -> kf.getKind() == Kind.VALUE) //
						.filter(kf -> StringUtils.hasText(kf.getName())) //
						.filter(kf -> kf.getName().equals(property.getName())) //
						.findFirst();

				ValueBoxing vh = kParameter.map(KotlinValueUtils::getCopyValueHierarchy).orElse(null);
				KotlinCopyByProperty kotlinCopyByProperty = copyMethod.forProperty(property).get();
				Method copy = copyMethod.getSyntheticCopyMethod();

				Parameter parameter = copy.getParameters()[kotlinCopyByProperty.getParameterPosition()];

				return o -> ClassUtils.isAssignableValue(parameter.getType(), o) || vh == null ? o : vh.applyWrapping(o);
			}

			return Function.identity();
		}

		@Override
		public @Nullable Object getProperty(PersistentProperty<?> property) {
			return delegate.getProperty(property);
		}

		@Override
		public T getBean() {
			return delegate.getBean();
		}
	}
}
