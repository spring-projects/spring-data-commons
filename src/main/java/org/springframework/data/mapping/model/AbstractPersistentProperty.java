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

import org.springframework.core.annotation.AnnotationUtils;
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
	protected final PropertyDescriptor propertyDescriptor;
	protected final TypeInformation<?> information;
	protected final Class<?> rawType;
	protected final Field field;
	protected final Association<P> association;
	protected final PersistentEntity<?, P> owner;
	private final SimpleTypeHolder simpleTypeHolder;
	private final int hashCode;

	public AbstractPersistentProperty(Field field, PropertyDescriptor propertyDescriptor, PersistentEntity<?, P> owner,
			SimpleTypeHolder simpleTypeHolder) {

		Assert.notNull(simpleTypeHolder, "SimpleTypeHolder must not be null!");
		Assert.notNull(owner, "Owner entity must not be null!");

		this.propertyDescriptor = propertyDescriptor;
		this.field = field;
		this.owner = owner;
		this.simpleTypeHolder = simpleTypeHolder;
		this.name = field == null ? propertyDescriptor.getName() : field.getName();
		this.information = owner.getTypeInformation().getProperty(this.name);
		this.rawType = this.information != null ? information.getType()
				: field == null ? propertyDescriptor.getPropertyType() : field.getType();
		this.association = isAssociation() ? createAssociation() : null;
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
	public Method getGetter() {

		if (propertyDescriptor == null) {
			return null;
		}

		Method getter = propertyDescriptor.getReadMethod();

		if (getter == null) {
			return null;
		}

		Class<?> returnType = owner.getTypeInformation() //
				.getReturnType(getter) //
				.getType();

		return rawType.isAssignableFrom(returnType) ? getter : null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getSetter()
	 */
	@Override
	public Method getSetter() {

		if (propertyDescriptor == null) {
			return null;
		}

		Method setter = propertyDescriptor.getWriteMethod();

		if (setter == null) {
			return null;
		}

		Class<?> parameterType = owner.getTypeInformation() //
				.getParameterTypes(setter) //
				.get(0) //
				.getType();

		return parameterType.isAssignableFrom(rawType) ? setter : null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getField()
	 */
	@Override
	public Field getField() {
		return field;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentProperty#getSpelExpression()
	 */
	@Override
	public String getSpelExpression() {
		return null;
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
		return field == null ? false : AnnotationUtils.getAnnotation(field, Reference.class) != null;
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
		return this.field == null ? this.propertyDescriptor.toString() : this.field.toString();
	}
}
