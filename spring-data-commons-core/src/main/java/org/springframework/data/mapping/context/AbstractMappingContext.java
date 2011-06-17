/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.context;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.event.MappingContextEvent;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.MutablePersistentEntity;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.validation.Validator;

/**
 * Base class to build mapping metadata and thus create instances of {@link PersistentEntity} and
 * {@link PersistentProperty}.
 * 
 * @param E the concrete {@link PersistentEntity} type the {@link MappingContext} implementation creates
 * @param P the concrete {@link PersistentProperty} type the {@link MappingContext} implementation creates
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Oliver Gierke
 */
public abstract class AbstractMappingContext<E extends MutablePersistentEntity<?, P>, P extends PersistentProperty<P>>
		implements MappingContext<E, P>, InitializingBean, ApplicationEventPublisherAware {

	private static final Set<String> UNMAPPED_FIELDS = new HashSet<String>(Arrays.asList("class", "this$0"));

	private ApplicationEventPublisher applicationEventPublisher;
	private ConcurrentMap<TypeInformation<?>, E> persistentEntities = new ConcurrentHashMap<TypeInformation<?>, E>();
	private ConcurrentMap<E, List<Validator>> validators = new ConcurrentHashMap<E, List<Validator>>();
	private List<Class<?>> customSimpleTypes = new ArrayList<Class<?>>();
	private Set<? extends Class<?>> initialEntitySet = new HashSet<Class<?>>();
	private boolean strict = false;
	private SimpleTypeHolder simpleTypeHolder = new SimpleTypeHolder();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * Sets the {@link Set} of types to populate the context initially.
	 * 
	 * @param initialEntitySet
	 */
	public void setInitialEntitySet(Set<? extends Class<?>> initialEntitySet) {
		this.initialEntitySet = initialEntitySet;
	}

	/**
	 * Configures whether the {@link MappingContext} is in strict mode which means, that it will throw
	 * {@link MappingException}s in case one tries to lookup a {@link PersistentEntity} not already in the context. This
	 * defaults to {@literal false} so that unknown types will be transparently added to the MappingContext if not known
	 * in advance.
	 * 
	 * @param strict
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	/**
	 * Configures the {@link SimpleTypeHolder} to be used by the {@link MappingContext}. Allows customization of what
	 * types will be regarded as simple types and thus not recursively analysed. Setting this to {@literal null} will
	 * reset the context to use the default {@link SimpleTypeHolder}.
	 * 
	 * @param simpleTypes
	 */
	public void setSimpleTypeHolder(SimpleTypeHolder simpleTypes) {
		this.simpleTypeHolder = simpleTypes == null ? new SimpleTypeHolder() : simpleTypes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MappingContext#getPersistentEntities()
	 */
	public Collection<E> getPersistentEntities() {
		return persistentEntities.values();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MappingContext#getPersistentEntity(java.lang.Class)
	 */
	public E getPersistentEntity(Class<?> type) {
		return getPersistentEntity(ClassTypeInformation.from(type));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MappingContext#getPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	public E getPersistentEntity(TypeInformation<?> type) {

		E entity = persistentEntities.get(type);

		if (entity != null) {
			return entity;
		}

		if (strict) {
			throw new MappingException("Unknown persistent entity " + type);
		}

		return addPersistentEntity(type);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getPersistentPropertyPath(java.lang.Class, java.lang.String)
	 */
	public <T> Iterable<P> getPersistentPropertyPath(Class<T> type, String path) {

		Iterator<String> parts = Arrays.asList(path.split("\\.")).iterator();
		List<P> result = new ArrayList<P>();
		E current = getPersistentEntity(type);

		while (parts.hasNext()) {
			String name = parts.next();
			P property = current.getPersistentProperty(name);

			if (property == null) {
				throw new IllegalArgumentException(String.format("No property %s found on %s!", name, current.getName()));
			}

			result.add(property);
			current = getPersistentEntity(property.getTypeInformation().getActualType());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getEntityValidators(org.springframework.data.mapping.PersistentEntity)
	 */
	public List<Validator> getEntityValidators(E entity) {
		return validators.get(entity);
	}

	/**
	 * Adds the given type to the {@link MappingContext}.
	 * 
	 * @param type
	 * @return
	 */
	protected E addPersistentEntity(Class<?> type) {

		return addPersistentEntity(ClassTypeInformation.from(type));
	}

	/**
	 * Adds the given {@link TypeInformation} to the {@link MappingContext}.
	 * 
	 * @param typeInformation
	 * @return
	 */
	protected E addPersistentEntity(TypeInformation<?> typeInformation) {

		E persistentEntity = persistentEntities.get(typeInformation);

		if (persistentEntity != null) {
			return persistentEntity;
		}

		Class<?> type = typeInformation.getType();

		try {
			final E entity = createPersistentEntity(typeInformation);

			// Eagerly cache the entity as we might have to find it during recursive lookups.
			persistentEntities.put(entity.getTypeInformation(), entity);

			BeanInfo info = Introspector.getBeanInfo(type);

			final Map<String, PropertyDescriptor> descriptors = new HashMap<String, PropertyDescriptor>();
			for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
				descriptors.put(descriptor.getName(), descriptor);
			}

			ReflectionUtils.doWithFields(type, new FieldCallback() {

				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

					PropertyDescriptor descriptor = descriptors.get(field.getName());

					ReflectionUtils.makeAccessible(field);
					P property = createPersistentProperty(field, descriptor, entity, simpleTypeHolder);

					if (property.isTransient()) {
						return;
					}

					entity.addPersistentProperty(property);

					if (property.isAssociation()) {
						entity.addAssociation(property.getAssociation());
					}

					if (property.isIdProperty()) {
						entity.setIdProperty(property);
					}

					TypeInformation<?> nestedType = getNestedTypeToAdd(property, entity);
					if (nestedType != null) {
						addPersistentEntity(nestedType);
					}
				}
			}, new ReflectionUtils.FieldFilter() {
				public boolean matches(Field field) {
					return !Modifier.isStatic(field.getModifiers()) && !UNMAPPED_FIELDS.contains(field.getName());
				}
			});

			entity.verify();

			// Inform listeners
			if (null != applicationEventPublisher) {
				applicationEventPublisher.publishEvent(new MappingContextEvent<E, P>(entity, typeInformation));
			}

			return entity;
		} catch (IntrospectionException e) {
			throw new MappingException(e.getMessage(), e);
		}
	}

	/**
	 * Returns a potential nested type tha needs to be added when adding the given property in the course of adding a
	 * {@link PersistentEntity}. Will return the property's {@link TypeInformation} directly if it is a potential entity,
	 * a collections component type if it's a collection as well as the value type of a {@link Map} if it's a map
	 * property.
	 * 
	 * @param property
	 * @return the TypeInformation to be added as {@link PersistentEntity} or {@literal

	 */
	private TypeInformation<?> getNestedTypeToAdd(P property, PersistentEntity<?, P> entity) {

		if (entity.getType().equals(property.getRawType())) {
			return null;
		}

		TypeInformation<?> typeInformation = property.getTypeInformation();

		if (customSimpleTypes.contains(typeInformation.getType())) {
			return null;
		}

		if (property.isEntity()) {
			return typeInformation;
		}

		if (property.isCollection()) {
			return getTypeInformationIfNotSimpleType(getComponentTypeRecursively(typeInformation));
		}

		if (property.isMap()) {
			return getTypeInformationIfNotSimpleType(typeInformation.getMapValueType());
		}

		return null;
	}

	private TypeInformation<?> getComponentTypeRecursively(TypeInformation<?> typeInformation) {
		TypeInformation<?> componentType = typeInformation.getComponentType();

		if (componentType == null) {
			return null;
		}

		return componentType.isCollectionLike() ? getComponentTypeRecursively(componentType) : componentType;
	}

	private TypeInformation<?> getTypeInformationIfNotSimpleType(TypeInformation<?> information) {
		return information == null || simpleTypeHolder.isSimpleType(information.getType()) ? null : information;
	}

	/**
	 * Creates the concrete {@link PersistentEntity} instance.
	 * 
	 * @param <T>
	 * @param typeInformation
	 * @return
	 */
	protected abstract <T> E createPersistentEntity(TypeInformation<T> typeInformation);

	/**
	 * Creates the concrete instance of {@link PersistentProperty}.
	 * 
	 * @param field
	 * @param descriptor
	 * @param owner
	 * @param simpleTypeHolder
	 * @return
	 */
	protected abstract P createPersistentProperty(Field field, PropertyDescriptor descriptor, E owner,
			SimpleTypeHolder simpleTypeHolder);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		for (Class<?> initialEntity : initialEntitySet) {
			addPersistentEntity(initialEntity);
		}
	}
}
