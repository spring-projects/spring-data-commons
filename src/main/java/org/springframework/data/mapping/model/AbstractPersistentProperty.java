/*
 * Copyright 2011-2025 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.ClassUtils;
import org.springframework.data.util.KotlinReflectionUtils;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;

/**
 * Simple implementation of {@link PersistentProperty}.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public abstract class AbstractPersistentProperty<P extends PersistentProperty<P>> implements PersistentProperty<P> {

	private static final Field CAUSE_FIELD;
	private static final @Nullable Class<?> ASSOCIATION_TYPE;

	static {

		CAUSE_FIELD = ReflectionUtils.getRequiredField(Throwable.class, "cause");
		ASSOCIATION_TYPE = ClassUtils.loadIfPresent("org.jmolecules.ddd.types.Association",
				AbstractPersistentProperty.class.getClassLoader());
	}

	private final String name;
	private final TypeInformation<?> information;
	private final Class<?> rawType;
	private final Lazy<Association<P>> association;
	private final PersistentEntity<?, P> owner;
	private final Property property;
	private final Lazy<Integer> hashCode;
	private final Lazy<Boolean> usePropertyAccess;
	private final Lazy<Set<TypeInformation<?>>> entityTypeInformation;

	private final Lazy<Boolean> isAssociation;
	private final Lazy<TypeInformation<?>> associationTargetType;

	private final @Nullable Method getter;
	private final @Nullable Method setter;
	private final @Nullable Field field;
	private final @Nullable Method wither;
	private final Lazy<Boolean> readable;
	private final boolean immutable;

	public AbstractPersistentProperty(Property property, PersistentEntity<?, P> owner,
			SimpleTypeHolder simpleTypeHolder) {

		Assert.notNull(simpleTypeHolder, "SimpleTypeHolder must not be null");
		Assert.notNull(owner, "Owner entity must not be null");

		this.name = property.getName();
		this.information = owner.getTypeInformation().getRequiredProperty(getName());
		this.rawType = this.information.getType();
		this.property = property;
		this.association = Lazy.of(() -> isAssociation() ? createAssociation() : null);
		this.owner = owner;

		this.hashCode = Lazy.of(property::hashCode);
		this.usePropertyAccess = Lazy.of(() -> owner.getType().isInterface() || CAUSE_FIELD.equals(getField()));

		this.isAssociation = Lazy.of(() -> ASSOCIATION_TYPE != null && ASSOCIATION_TYPE.isAssignableFrom(rawType));
		this.associationTargetType = ASSOCIATION_TYPE == null //
				? Lazy.empty() //
				: Lazy.of(() -> Optional.of(getTypeInformation()) //
						.map(it -> it.getSuperTypeInformation(ASSOCIATION_TYPE)) //
						.map(TypeInformation::getComponentType) //
						.orElse(null));

		this.entityTypeInformation = Lazy.of(() -> detectEntityTypes(simpleTypeHolder));

		this.getter = property.getGetter().orElse(null);
		this.setter = property.getSetter().orElse(null);
		this.field = property.getField().orElse(null);
		this.wither = property.getWither().orElse(null);

		this.immutable = setter == null && (field == null || Modifier.isFinal(field.getModifiers()));
		this.readable = Lazy.of(() -> {

			if (setter != null) {
				return true;
			}

			if (wither != null) {
				return true;
			}

			if (KotlinReflectionUtils.isDataClass(owner.getType()) && KotlinCopyMethod.hasKotlinCopyMethodFor(this)) {
				return true;
			}

			if (field != null && !Modifier.isFinal(field.getModifiers())) {
				return true;
			}

			return false;
		});
	}

	protected abstract Association<P> createAssociation();

	@Override
	public PersistentEntity<?, P> getOwner() {
		return this.owner;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getType() {
		return information.getType();
	}

	@Override
	public Class<?> getRawType() {
		return this.rawType;
	}

	@Override
	public TypeInformation<?> getTypeInformation() {
		return information;
	}

	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityTypeInformation() {
		return entityTypeInformation.get();
	}

	@Override
	public @Nullable Method getGetter() {
		return this.getter;
	}

	@Override
	public @Nullable Method getSetter() {
		return this.setter;
	}

	@Override
	public @Nullable Method getWither() {
		return this.wither;
	}

	@Override
	public @Nullable Field getField() {
		return this.field;
	}

	@Override
	public @Nullable String getSpelExpression() {
		return null;
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	@Override
	public boolean isWritable() {
		return !isTransient();
	}

	@Override
	public boolean isReadable() {
		return !isTransient() && readable.get();
	}

	@Override
	public boolean isImmutable() {
		return immutable;
	}

	@Override
	public boolean isAssociation() {
		return isAssociation.get();
	}

	@Override
	public @Nullable Association<P> getAssociation() {
		return association.orElse(null);
	}

	@Override
	public @Nullable Class<?> getAssociationTargetType() {

		TypeInformation<?> result = getAssociationTargetTypeInformation();

		return result != null ? result.getType() : null;
	}

	@Override
	public @Nullable TypeInformation<?> getAssociationTargetTypeInformation() {
		return associationTargetType.getNullable();
	}

	@Override
	public boolean isCollectionLike() {
		return information.isCollectionLike();
	}

	@Override
	public boolean isMap() {
		return information.isMap();
	}

	@Override
	public boolean isArray() {
		return getType().isArray();
	}

	@Override
	public boolean isEntity() {
		return !isTransient() && !entityTypeInformation.get().isEmpty();
	}

	@Override
	public @Nullable Class<?> getComponentType() {
		return isMap() || isCollectionLike() ? information.getRequiredComponentType().getType() : null;
	}

	@Override
	public @Nullable Class<?> getMapValueType() {

		if (isMap()) {

			TypeInformation<?> mapValueType = information.getMapValueType();

			if (mapValueType != null) {
				return mapValueType.getType();
			}
		}

		return null;
	}

	@Override
	public Class<?> getActualType() {
		return getActualTypeInformation().getType();
	}

	@Override
	public boolean usePropertyAccess() {
		return usePropertyAccess.get();
	}

	protected Property getProperty() {
		return this.property;
	}

	protected TypeInformation<?> getActualTypeInformation() {

		TypeInformation<?> targetType = associationTargetType.getNullable();
		return targetType == null ? information.getRequiredActualType() : targetType;
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AbstractPersistentProperty<?> that)) {
			return false;
		}

		return this.property.equals(that.property);
	}

	@Override
	public int hashCode() {
		return this.hashCode.get();
	}

	@Override
	public String toString() {
		return property.toString();
	}

	private Set<TypeInformation<?>> detectEntityTypes(SimpleTypeHolder simpleTypes) {

		TypeInformation<?> typeToStartWith = getAssociationTargetTypeInformation();
		typeToStartWith = typeToStartWith == null ? information : typeToStartWith;

		Set<TypeInformation<?>> result = detectEntityTypes(typeToStartWith);

		return result.stream().filter(it -> !simpleTypes.isSimpleType(it.getType()))
				.filter(it -> !it.getType().equals(ASSOCIATION_TYPE)).collect(Collectors.toSet());
	}

	private Set<TypeInformation<?>> detectEntityTypes(@Nullable TypeInformation<?> source) {

		if (source == null) {
			return Collections.emptySet();
		}

		Set<TypeInformation<?>> result = new HashSet<>();

		if (source.isMap()) {
			result.addAll(detectEntityTypes(source.getComponentType()));
		}

		TypeInformation<?> actualType = source.getActualType();

		if (source.equals(actualType)) {
			result.add(source);
		} else {
			result.addAll(detectEntityTypes(actualType));
		}

		return result;
	}
}
