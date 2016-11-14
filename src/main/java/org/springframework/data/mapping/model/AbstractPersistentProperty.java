/*
 * Copyright 2011-2015 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Simple impementation of {@link PersistentProperty}.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public abstract class AbstractPersistentProperty<P extends PersistentProperty<P>> implements PersistentProperty<P> {

	private static final Field CAUSE_FIELD;

	static {
		CAUSE_FIELD = ReflectionUtils.findField(Throwable.class, "cause");
	}

	protected final String name;
	protected final Optional<PropertyDescriptor> propertyDescriptor;
	protected final TypeInformation<?> information;
	protected final Class<?> rawType;
	protected final Optional<Field> field;
	protected final Association<P> association;
	protected final PersistentEntity<?, P> owner;
	private final SimpleTypeHolder simpleTypeHolder;
	private final int hashCode;

	public AbstractPersistentProperty(Optional<Field> field, PropertyDescriptor propertyDescriptor,
			PersistentEntity<?, P> owner, SimpleTypeHolder simpleTypeHolder) {

		Assert.notNull(simpleTypeHolder);
		Assert.notNull(owner);

		this.name = field.map(Field::getName).orElseGet(() -> propertyDescriptor.getName());
		this.rawType = field.<Class<?>> map(Field::getType).orElseGet(() -> propertyDescriptor.getPropertyType());
		this.information = owner.getTypeInformation().getProperty(this.name);
		this.propertyDescriptor = Optional.ofNullable(propertyDescriptor);
		this.field = field;
		this.association = isAssociation() ? createAssociation() : null;
		this.owner = owner;
		this.simpleTypeHolder = simpleTypeHolder;
		this.hashCode = this.field == null ? this.propertyDescriptor.hashCode() : this.field.hashCode();
	}

	protected abstract Association<P> createAssociation();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getOwner()
	 */
	@Override
	public PersistentEntity<?, P> getOwner() {
		return owner;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getType()
	 */
	@Override
	public Class<?> getType() {
		return information.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getRawType()
	 */
	@Override
	public Class<?> getRawType() {
		return this.rawType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getTypeInformation()
	 */
	@Override
	public TypeInformation<?> getTypeInformation() {
		return information;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getPersistentEntityType()
	 */
	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityType() {

		if (!isEntity()) {
			return Collections.emptySet();
		}

		TypeInformation<?> candidate = getTypeInformationIfEntityCandidate();
		return candidate != null ? Collections.singleton(candidate) : Collections.<TypeInformation<?>> emptySet();
	}

	private TypeInformation<?> getTypeInformationIfEntityCandidate() {

		TypeInformation<?> candidate = information.getActualType();

		if (candidate == null || simpleTypeHolder.isSimpleType(candidate.getType())) {
			return null;
		}

		return candidate.isCollectionLike() || candidate.isMap() ? null : candidate;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getGetter()
	 */
	@Override
	public Optional<Method> getGetter() {
		return propertyDescriptor.map(it -> it.getReadMethod())//
				.filter(it -> rawType.isAssignableFrom(it.getReturnType()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getSetter()
	 */
	@Override
	public Optional<Method> getSetter() {
		return propertyDescriptor.map(it -> it.getWriteMethod())//
				.filter(it -> it.getParameterTypes()[0].isAssignableFrom(rawType));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getField()
	 */
	@Override
	public Optional<Field> getField() {
		return field;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getSpelExpression()
	 */
	@Override
	public Optional<String> getSpelExpression() {
		return Optional.empty();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isTransient()
	 */
	@Override
	public boolean isTransient() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return !isTransient();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isAssociation()
	 */
	@Override
	public boolean isAssociation() {
		return isAnnotationPresent(Reference.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getAssociation()
	 */
	@Override
	public Association<P> getAssociation() {
		return association;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isCollectionLike()
	 */
	@Override
	public boolean isCollectionLike() {
		return information.isCollectionLike();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isMap()
	 */
	@Override
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isArray()
	 */
	@Override
	public boolean isArray() {
		return getType().isArray();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#isEntity()
	 */
	@Override
	public boolean isEntity() {
		return !isTransient() && getTypeInformationIfEntityCandidate() != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getComponentType()
	 */
	@Override
	public Class<?> getComponentType() {

		if (!isMap() && !isCollectionLike()) {
			return null;
		}

		TypeInformation<?> componentType = information.getComponentType();
		return componentType == null ? null : componentType.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getMapValueType()
	 */
	@Override
	public Class<?> getMapValueType() {
		return isMap() ? information.getMapValueType().getType() : null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getActualType()
	 */
	@Override
	public Class<?> getActualType() {
		return information.getActualType().getType();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.mapping.MongoPersistentProperty#usePropertyAccess()
	 */
	public boolean usePropertyAccess() {
		return owner.getType().isInterface() || CAUSE_FIELD.equals(getField());
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AbstractPersistentProperty)) {
			return false;
		}

		AbstractPersistentProperty<?> that = (AbstractPersistentProperty<?>) obj;

		return this.field == null ? this.propertyDescriptor.equals(that.propertyDescriptor) : this.field.equals(that.field);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return field.map(Object::toString)//
				.orElseGet(() -> propertyDescriptor.map(Object::toString)//
						.orElseThrow(() -> new IllegalStateException("Either Field or PropertyDescriptor has to be present!")));
	}
}
