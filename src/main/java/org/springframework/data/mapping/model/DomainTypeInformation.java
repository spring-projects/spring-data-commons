/*
 * Copyright 2020 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructorProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
// REVIEW: The *Aware suffix is somewhat misleading. Maybe *Provider?
// REVIEW: DOMAIN typeInformation seems to be the wrong name:
// 1. It is not only for Domain types.
// 2. It is not the only variant that is for domain types
// -> ConfiguredTypeInformatin maybe?
public class DomainTypeInformation<S> extends ClassTypeInformation<S>
		implements AnnotationAware, EntityInstantiatorAware, PreferredConstructorProvider<S>,
		PersistentPropertyAccessorFactoryProvider, AccessorFunctionAware<S> {

	private final Class<S> type;

	@Nullable private final TypeInformation<?> valueType;
	@Nullable private final TypeInformation<?> keyType;

	// REVIEW: How should we handle information for Spring Data Domain types (Page, Slice, geo stuff...)
	// REVIEW: Maybe we should have specialised subtypes of DomainTypeInformation for primitives or simple types, without
	// EntitIntantiator, PreferredConstructor ...
	private DomainTypeInformation<? super S> superTypeInformation;
	private List<TypeInformation<?>> typeArguments;
	private MultiValueMap<Class<? extends Annotation>, Annotation> annotations;

	private final Fields fields;

	private DomainTypeConstructor<S> preferredConstructor;

	public DomainTypeInformation(Class<S> type) {
		this(type, null, null);
	}

	public DomainTypeInformation(Class<S> type, DomainTypeInformation<? super S> superTypeInformation) {
		this(type, superTypeInformation, null, null);
	}

	/* REVIEW:
		If we instead of creating subtypes pass a "configuration lambda" to the constructor which contains all the
		generated code for setting fields and stuff we get the following benefits:
		- DomainTypeInformation will be truly immutable.
		- Fewer classes assuming the lambdas get not compiled to classes, which should be true. Even if they get compiled
		into classes the namespace would contain fewer named classes.
		- We may do pre and postprocessing, e.g. a call to a verify method which might check that all required fields are set.
	 */
	public DomainTypeInformation(Class<S> type, @Nullable TypeInformation<?> valueType,
			@Nullable TypeInformation<?> keyType) {
		this(type, null, valueType, keyType);
	}

	private DomainTypeInformation(Class<S> type, @Nullable DomainTypeInformation<? super S> superTypeInformation,
			@Nullable TypeInformation<?> valueType, @Nullable TypeInformation<?> keyType) {

		super(type, valueType, keyType);
		this.type = type;
		this.superTypeInformation = superTypeInformation;
		this.valueType = valueType;
		this.keyType = keyType;
		// REVIEW: We can't really have multiple annotations of the same type, can we? Container annotations should be
		// handled explicitly, in order to avoid bugs.
		this.annotations = new LinkedMultiValueMap<>();
		this.typeArguments = new ArrayList<>(0);
		this.fields = new Fields(type);

		addSuperTypeFields(superTypeInformation);
		addSuperAnnotations(superTypeInformation);
	}

	protected void addField(Field<?, ? super S> field) {
		this.fields.add(field);
	}

	protected void addAnnotation(Annotation annotation) {

		// TODO: should we auto add eg. Persistent when we find a TypeAlias annotation?
		this.annotations.add(annotation.annotationType(), annotation);
	}

	protected void addTypeArguments(TypeInformation<?>... typeArguments) {
		this.typeArguments.addAll(Arrays.asList(typeArguments));
	}

	protected void setConstructor(DomainTypeConstructor<S> constructor) {
		this.preferredConstructor = constructor;
	}

	@Override
	public PreferredConstructor<S, ?> getPreferredConstructor() {
		return preferredConstructor;
	}

	private void addSuperTypeFields(@Nullable DomainTypeInformation<? super S> superType) {

		if (superType == null) {
			return;
		}

		superType.doWithFields((name, field) -> {
			addField(field);
		});
		addSuperTypeFields(superType.superTypeInformation);
	}

	private void addSuperAnnotations(@Nullable DomainTypeInformation<? super S> superType) {

		if (superType == null) {
			return;
		}

		superType.getAnnotations().forEach(this::addAnnotation);
	}

	public void doWithFields(BiConsumer<String, Field<?, S>> consumer) {
		fields.doWithFields(consumer);
	}

	@Override
	public List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor) {
		return null;
	}

	@Nullable
	@Override
	public TypeInformation<?> getProperty(String property) {

		if (!fields.hasField(property)) {
			return null;
		}

		return fields.getField(property).getTypeInformation();
	}

	@Override
	public boolean isCollectionLike() {
		return false;
	}

	@Override
	public boolean isMap() {
		return false;
	}

	@Override
	@Nullable
	public TypeInformation<?> getComponentType() {

		if(isMap()) {
			return keyType;
		}

		if(isCollectionLike()) {
			return valueType;
		}

		return null;
	}

	@Nullable
	@Override
	public TypeInformation<?> getMapValueType() {
		return valueType;
	}

	@Override
	public Class<S> getType() {
		return type;
	}

	@Override
	public ClassTypeInformation<?> getRawTypeInformation() {
		return this;
	}

	@Nullable
	@Override
	public TypeInformation<?> getActualType() {
		return valueType != null ? valueType : this;
	}

	@Override
	public TypeInformation<?> getReturnType(Method method) {
		return null;
	}

	@Override
	public List<TypeInformation<?>> getParameterTypes(Method method) {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public TypeInformation<?> getSuperTypeInformation(Class<?> superType) {
		return superTypeInformation;
	}

	@Override
	public boolean isAssignableFrom(TypeInformation<?> target) {
		return this.type.isAssignableFrom(target.getType());
	}

	@Override
	public List<TypeInformation<?>> getTypeArguments() {
		return typeArguments;
	}

	@Override
	public TypeInformation<? extends S> specialize(ClassTypeInformation<?> type) {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;

		DomainTypeInformation<?> that = (DomainTypeInformation<?>) o;

		if (!ObjectUtils.nullSafeEquals(type, that.type)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(valueType, that.valueType)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(keyType, that.keyType)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(superTypeInformation, that.superTypeInformation)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(typeArguments, that.typeArguments)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(fields, that.fields)) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(preferredConstructor, that.preferredConstructor);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(type);
		result = 31 * result + ObjectUtils.nullSafeHashCode(valueType);
		result = 31 * result + ObjectUtils.nullSafeHashCode(keyType);
		result = 31 * result + ObjectUtils.nullSafeHashCode(superTypeInformation);
		result = 31 * result + ObjectUtils.nullSafeHashCode(typeArguments);
		result = 31 * result + ObjectUtils.nullSafeHashCode(fields);
		result = 31 * result + ObjectUtils.nullSafeHashCode(preferredConstructor);
		return result;
	}

	@Nullable
	@Override
	public EntityInstantiator getEntityInstantiator() {
		return preferredConstructor != null ? preferredConstructor.getEntityInstantiator() : null;
	}

	@Override
	public List<Annotation> getAnnotations() {

		List<Annotation> all = new ArrayList<>();
		annotations.values().forEach(all::addAll);
		return all;
	}

	@Override
	public boolean hasAnnotation(Class<?> annotationType) {
		return annotations.containsKey(annotationType);
	}

	@Override
	public <T extends Annotation> List<T> findAnnotation(Class<T> annotation) {

		List<T> found = (List<T>) annotations.getOrDefault(annotation, Collections.emptyList());
		Collections.reverse(found); // reverse for bottom up structure
		return found;
	}

	@Nullable
	@Override
	public PersistentPropertyAccessorFactory getPersistentPropertyAccessorFactory() {
		return AccessorFunctionPropertyAccessorFactory.instance();
	}

	@Override
	public BiFunction<S, Object, S> getSetFunctionFor(String fieldName) {

		Field<Object, S> entityField = fields.getField(fieldName);
		if (entityField == null) {
			return null;
		}

		return entityField.getSetter();
	}

	@Override
	public Function<S, Object> getGetFunctionFor(String fieldName) {

		Field<Object, S> entityField = fields.getField(fieldName);
		if (entityField == null) {
			return null;
		}

		return entityField.getGetter();
	}
}
