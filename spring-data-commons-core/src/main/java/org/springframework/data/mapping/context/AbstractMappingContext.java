/*
 * Copyright 2011-2012 by the original author(s).
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.MutablePersistentEntity;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * Base class to build mapping metadata and thus create instances of {@link PersistentEntity} and
 * {@link PersistentProperty}.
 * <p>
 * The implementation uses a {@link ReentrantReadWriteLock} to make sure {@link PersistentEntity} are completely
 * populated before accessing them from outside.
 * 
 * @param E the concrete {@link PersistentEntity} type the {@link MappingContext} implementation creates
 * @param P the concrete {@link PersistentProperty} type the {@link MappingContext} implementation creates
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Oliver Gierke
 */
public abstract class AbstractMappingContext<E extends MutablePersistentEntity<?, P>, P extends PersistentProperty<P>>
		implements MappingContext<E, P>, ApplicationContextAware, ApplicationEventPublisherAware,
		ApplicationListener<ContextRefreshedEvent> {

	private static final Set<String> UNMAPPED_FIELDS = new HashSet<String>(Arrays.asList("class", "this$0"));

	private final ConcurrentMap<TypeInformation<?>, E> persistentEntities = new ConcurrentHashMap<TypeInformation<?>, E>();

	private ApplicationContext applicationContext;
	private ApplicationEventPublisher applicationEventPublisher;

	private Set<? extends Class<?>> initialEntitySet = new HashSet<Class<?>>();
	private boolean strict = false;
	private SimpleTypeHolder simpleTypeHolder = new SimpleTypeHolder();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.applicationContext = applicationContext;

		// Default publisher
		if (this.applicationEventPublisher == null) {
			this.applicationEventPublisher = applicationContext;
		}
	}

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
		try {
			read.lock();
			return persistentEntities.values();
		} finally {
			read.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MappingContext#getPersistentEntity(java.lang.Class)
	 */
	public E getPersistentEntity(Class<?> type) {
		Assert.notNull(type);
		return getPersistentEntity(ClassTypeInformation.from(type));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MappingContext#getPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	public E getPersistentEntity(TypeInformation<?> type) {

		Assert.notNull(type);

		try {
			read.lock();
			E entity = persistentEntities.get(type);

			if (entity != null) {
				return entity;
			}

		} finally {
			read.unlock();
		}

		if (strict) {
			throw new MappingException("Unknown persistent entity " + type);
		}

		if (!shouldCreatePersistentEntityFor(type)) {
			return null;
		}

		return addPersistentEntity(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getPersistentEntity(org.springframework.data.mapping.PersistentProperty)
	 */
	public E getPersistentEntity(P persistentProperty) {

		if (persistentProperty == null) {
			return null;
		}

		try {
			read.lock();
			return persistentEntities.get(persistentProperty.getTypeInformation());
		} finally {
			read.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getPersistentPropertyPath(java.lang.Class, java.lang.String)
	 */
	public PersistentPropertyPath<P> getPersistentPropertyPath(PropertyPath propertyPath) {

		List<P> result = new ArrayList<P>();
		E current = getPersistentEntity(propertyPath.getOwningType());

		for (PropertyPath segment : propertyPath) {

			P persistentProperty = current.getPersistentProperty(segment.getSegment());

			if (persistentProperty == null) {
				throw new IllegalArgumentException(String.format("No property %s found on %s!", segment.getSegment(),
						current.getName()));
			}

			result.add(persistentProperty);

			if (segment.hasNext()) {
				current = getPersistentEntity(segment.getType());
			}
		}

		return new DefaultPersistentPropertyPath<P>(result);
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

			write.lock();

			final E entity = createPersistentEntity(typeInformation);

			// Eagerly cache the entity as we might have to find it during recursive lookups.
			persistentEntities.put(typeInformation, entity);

			BeanInfo info = Introspector.getBeanInfo(type);

			final Map<String, PropertyDescriptor> descriptors = new HashMap<String, PropertyDescriptor>();
			for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
				descriptors.put(descriptor.getName(), descriptor);
			}

			ReflectionUtils.doWithFields(type, new PersistentPropertyCreator(entity, descriptors),
					new ReflectionUtils.FieldFilter() {
						public boolean matches(Field field) {
							return !Modifier.isStatic(field.getModifiers()) && !UNMAPPED_FIELDS.contains(field.getName());
						}
					});

			try {
				entity.verify();
			} catch (MappingException e) {
				persistentEntities.remove(typeInformation);
				throw e;
			}

			// Inform listeners
			if (null != applicationEventPublisher) {
				applicationEventPublisher.publishEvent(new MappingContextEvent<E, P>(this, entity));
			}

			return entity;

		} catch (IntrospectionException e) {
			throw new MappingException(e.getMessage(), e);
		} finally {
			write.unlock();
		}
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
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {

		if (!event.getApplicationContext().equals(applicationContext)) {
			return;
		}

		initialize();
	}

	/**
	 * Initializes the mapping context. Will add the types configured through {@link #setInitialEntitySet(Set)} to the
	 * context.
	 */
	public void initialize() {

		for (Class<?> initialEntity : initialEntitySet) {
			addPersistentEntity(initialEntity);
		}
	}

	/**
	 * Returns whether a {@link PersistentEntity} instance should be created for the given {@link TypeInformation}. By
	 * default this will reject this for all types considered simple, but it might be necessary to tweak that in case you
	 * have registered custom converters for top level types (which renders them to be considered simple) but still need
	 * meta-information about them.
	 * 
	 * @param type will never be {@literal null}.
	 * @return
	 */
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
		return !simpleTypeHolder.isSimpleType(type.getType());
	}

	/**
	 * {@link FieldCallback} to create {@link PersistentProperty} instances.
	 * 
	 * @author Oliver Gierke
	 */
	private final class PersistentPropertyCreator implements FieldCallback {

		private final E entity;
		private final Map<String, PropertyDescriptor> descriptors;

		/**
		 * Creates a new {@link PersistentPropertyCreator} for the given {@link PersistentEntity} and
		 * {@link PropertyDescriptor}s.
		 * 
		 * @param entity
		 * @param descriptors
		 */
		private PersistentPropertyCreator(E entity, Map<String, PropertyDescriptor> descriptors) {
			this.entity = entity;
			this.descriptors = descriptors;
		}

		public void doWith(Field field) {

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

			if (entity.getType().equals(property.getRawType())) {
				return;
			}

			if (!property.isEntity()) {
				return;
			}

			for (TypeInformation<?> candidate : property.getPersistentEntityType()) {
				addPersistentEntity(candidate);
			}
		}
	}
}
