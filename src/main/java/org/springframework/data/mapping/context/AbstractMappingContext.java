/*
 * Copyright 2011-2016 by the original author(s).
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.model.ClassGeneratingPropertyAccessorFactory;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.MutablePersistentEntity;
import org.springframework.data.mapping.model.PersistentPropertyAccessorFactory;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.Pair;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;
import org.springframework.util.StringUtils;

/**
 * Base class to build mapping metadata and thus create instances of {@link PersistentEntity} and
 * {@link PersistentProperty}.
 * <p>
 * The implementation uses a {@link ReentrantReadWriteLock} to make sure {@link PersistentEntity} are completely
 * populated before accessing them from outside.
 *
 * @param E the concrete {@link PersistentEntity} type the {@link MappingContext} implementation creates
 * @param P the concrete {@link PersistentProperty} type the {@link MappingContext} implementation creates
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Michael Hunger
 * @author Thomas Darimont
 * @author Tomasz Wysocki
 * @author Mark Paluch
 * @author Mikael Klamra
 */
public abstract class AbstractMappingContext<E extends MutablePersistentEntity<?, P>, P extends PersistentProperty<P>>
		implements MappingContext<E, P>, ApplicationEventPublisherAware, InitializingBean {

	private final Optional<E> NONE = Optional.empty();
	private final Map<TypeInformation<?>, Optional<E>> persistentEntities = new HashMap<>();
	private final PersistentPropertyAccessorFactory persistentPropertyAccessorFactory = new ClassGeneratingPropertyAccessorFactory();

	private ApplicationEventPublisher applicationEventPublisher;

	private Set<? extends Class<?>> initialEntitySet = new HashSet<Class<?>>();
	private boolean strict = false;
	private SimpleTypeHolder simpleTypeHolder = new SimpleTypeHolder();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

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

			return persistentEntities.values().stream()//
					.flatMap(it -> Optionals.toStream(it))//
					.collect(Collectors.toSet());

		} finally {
			read.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MappingContext#getPersistentEntity(java.lang.Class)
	 */
	public Optional<E> getPersistentEntity(Class<?> type) {
		return getPersistentEntity(ClassTypeInformation.from(type));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getRequiredPersistentEntity(java.lang.Class)
	 */
	@Override
	public E getRequiredPersistentEntity(Class<?> type) {

		return getPersistentEntity(type).orElseThrow(
				() -> new IllegalArgumentException(String.format("Couldn't find PersistentEntity for type %s!", type)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#hasPersistentEntityFor(java.lang.Class)
	 */
	@Override
	public boolean hasPersistentEntityFor(Class<?> type) {
		return type == null ? false : persistentEntities.containsKey(ClassTypeInformation.from(type));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.MappingContext#getPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	public Optional<E> getPersistentEntity(TypeInformation<?> type) {

		Assert.notNull(type);

		try {

			read.lock();

			Optional<E> entity = persistentEntities.get(type);

			if (entity != null) {
				return entity;
			}

		} finally {
			read.unlock();
		}

		if (!shouldCreatePersistentEntityFor(type)) {

			try {
				write.lock();
				persistentEntities.put(type, NONE);
			} finally {
				write.unlock();
			}

			return NONE;
		}

		if (strict) {
			throw new MappingException("Unknown persistent entity " + type);
		}

		return addPersistentEntity(type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getRequiredPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	public E getRequiredPersistentEntity(TypeInformation<?> type) {

		return getPersistentEntity(type)
				.orElseThrow(() -> new MappingException(String.format("Couldn't find PersistentEntity for type %s!", type)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getPersistentEntity(org.springframework.data.mapping.PersistentProperty)
	 */
	public Optional<E> getPersistentEntity(P persistentProperty) {

		Assert.notNull(persistentProperty, "PersistentProperty must not be null!");

		TypeInformation<?> typeInfo = persistentProperty.getTypeInformation();
		return getPersistentEntity(typeInfo.getActualType());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getRequiredPersistentEntity(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	public E getRequiredPersistentEntity(P persistentProperty) {

		return getPersistentEntity(persistentProperty).orElseThrow(() -> new IllegalArgumentException(
				String.format("Couldn't find PersistentEntity for type %s!", persistentProperty)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getPersistentPropertyPath(java.lang.Class, java.lang.String)
	 */
	public PersistentPropertyPath<P> getPersistentPropertyPath(PropertyPath propertyPath) {

		Assert.notNull(propertyPath, "Property path must not be null!");

		return getPersistentPropertyPath(propertyPath.toDotPath(), propertyPath.getOwningType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getPersistentPropertyPath(java.lang.String, java.lang.Class)
	 */
	public PersistentPropertyPath<P> getPersistentPropertyPath(String propertyPath, Class<?> type) {

		Assert.notNull(propertyPath, "Property path must not be null!");
		Assert.notNull(type, "Type must not be null!");

		return getPersistentPropertyPath(propertyPath, ClassTypeInformation.from(type));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.MappingContext#getPersistentPropertyPath(org.springframework.data.mapping.context.InvalidPersistentPropertyPath)
	 */
	@Override
	public PersistentPropertyPath<P> getPersistentPropertyPath(InvalidPersistentPropertyPath invalidPath) {
		return getPersistentPropertyPath(invalidPath.getResolvedPath(), invalidPath.getType());
	}

	private PersistentPropertyPath<P> getPersistentPropertyPath(String propertyPath, TypeInformation<?> type) {
		return getPersistentPropertyPath(Arrays.asList(propertyPath.split("\\.")), type);
	}

	/**
	 * Creates a {@link PersistentPropertyPath} for the given parts and {@link TypeInformation}.
	 *
	 * @param parts must not be {@literal null} or empty.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private PersistentPropertyPath<P> getPersistentPropertyPath(Collection<String> parts, TypeInformation<?> type) {

		DefaultPersistentPropertyPath<P> path = DefaultPersistentPropertyPath.empty();
		Iterator<String> iterator = parts.iterator();
		E current = getRequiredPersistentEntity(type);

		while (iterator.hasNext()) {

			String segment = iterator.next();
			final DefaultPersistentPropertyPath<P> foo = path;
			final E bar = current;

			Pair<DefaultPersistentPropertyPath<P>, E> pair = getPair(path, iterator, segment, current).orElseThrow(() -> {

				String source = StringUtils.collectionToDelimitedString(parts, ".");
				String resolvedPath = foo.toDotPath();

				return new InvalidPersistentPropertyPath(source, type, segment, resolvedPath,
						String.format("No property %s found on %s!", segment, bar.getName()));
			});

			path = pair.getFirst();
			current = pair.getSecond();
		}

		return path;
	}

	private Optional<Pair<DefaultPersistentPropertyPath<P>, E>> getPair(DefaultPersistentPropertyPath<P> path,
			Iterator<String> iterator, String segment, E entity) {

		Optional<P> persistentProperty = entity.getPersistentProperty(segment);

		return persistentProperty.map(it -> {

			TypeInformation<?> type = it.getTypeInformation().getActualType();
			return Pair.of(path.append(it), iterator.hasNext() ? getRequiredPersistentEntity(type) : entity);
		});
	}

	/**
	 * Adds the given type to the {@link MappingContext}.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	protected Optional<E> addPersistentEntity(Class<?> type) {
		return addPersistentEntity(ClassTypeInformation.from(type));
	}

	/**
	 * Adds the given {@link TypeInformation} to the {@link MappingContext}.
	 *
	 * @param typeInformation must not be {@literal null}.
	 * @return
	 */
	protected Optional<E> addPersistentEntity(TypeInformation<?> typeInformation) {

		Assert.notNull(typeInformation, "TypeInformation must not be null!");

		try {

			read.lock();

			Optional<E> persistentEntity = persistentEntities.get(typeInformation);

			if (persistentEntity != null) {
				return persistentEntity;
			}

		} finally {
			read.unlock();
		}

		Class<?> type = typeInformation.getType();

		try {

			write.lock();

			final E entity = createPersistentEntity(typeInformation);

			// Eagerly cache the entity as we might have to find it during recursive lookups.
			persistentEntities.put(typeInformation, Optional.of(entity));

			PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(type);

			final Map<String, PropertyDescriptor> descriptors = new HashMap<String, PropertyDescriptor>();
			for (PropertyDescriptor descriptor : pds) {
				descriptors.put(descriptor.getName(), descriptor);
			}

			try {

				PersistentPropertyCreator persistentPropertyCreator = new PersistentPropertyCreator(entity, descriptors);
				ReflectionUtils.doWithFields(type, persistentPropertyCreator, PersistentPropertyFilter.INSTANCE);
				persistentPropertyCreator.addPropertiesForRemainingDescriptors();

				entity.verify();

				if (persistentPropertyAccessorFactory.isSupported(entity)) {
					entity.setPersistentPropertyAccessorFactory(persistentPropertyAccessorFactory);
				}

			} catch (MappingException e) {
				persistentEntities.remove(typeInformation);
				throw e;
			}

			// Inform listeners
			if (null != applicationEventPublisher) {
				applicationEventPublisher.publishEvent(new MappingContextEvent<E, P>(this, entity));
			}

			return Optional.of(entity);

		} catch (BeansException e) {
			throw new MappingException(e.getMessage(), e);
		} finally {
			write.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentEntityAware#getManagedTypes()
	 */
	@Override
	public Collection<TypeInformation<?>> getManagedTypes() {

		try {

			read.lock();
			return Collections.unmodifiableSet(new HashSet<TypeInformation<?>>(persistentEntities.keySet()));

		} finally {
			read.unlock();
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
	protected abstract P createPersistentProperty(Property property, E owner, SimpleTypeHolder simpleTypeHolder);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
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
		private final Map<String, PropertyDescriptor> remainingDescriptors;

		/**
		 * Creates a new {@link PersistentPropertyCreator} for the given {@link PersistentEntity} and
		 * {@link PropertyDescriptor}s.
		 *
		 * @param entity must not be {@literal null}.
		 * @param descriptors must not be {@literal null}.
		 */
		private PersistentPropertyCreator(E entity, Map<String, PropertyDescriptor> descriptors) {

			Assert.notNull(entity, "PersistentEntity must not be null!");
			Assert.notNull(descriptors, "PropertyDescriptors must not be null!");

			this.entity = entity;
			this.descriptors = descriptors;
			this.remainingDescriptors = new HashMap<String, PropertyDescriptor>(descriptors);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.util.ReflectionUtils.FieldCallback#doWith(java.lang.reflect.Field)
		 */
		public void doWith(Field field) {

			String fieldName = field.getName();

			ReflectionUtils.makeAccessible(field);
			createAndRegisterProperty(Property.of(field, Optional.ofNullable(descriptors.get(fieldName))));

			this.remainingDescriptors.remove(fieldName);
		}

		/**
		 * Adds {@link PersistentProperty} instances for all suitable {@link PropertyDescriptor}s without a backing
		 * {@link Field}.
		 *
		 * @see PersistentPropertyFilter
		 */
		public void addPropertiesForRemainingDescriptors() {

			for (PropertyDescriptor descriptor : remainingDescriptors.values()) {
				if (PersistentPropertyFilter.INSTANCE.matches(descriptor)) {
					createAndRegisterProperty(Property.of(descriptor));
				}
			}
		}

		private void createAndRegisterProperty(Property input) {

			P property = createPersistentProperty(input, entity, simpleTypeHolder);

			if (property.isTransient()) {
				return;
			}

			if (!input.isFieldBacked() && !property.usePropertyAccess()) {
				return;
			}

			entity.addPersistentProperty(property);
			property.getAssociation().ifPresent(it -> entity.addAssociation(it));

			if (entity.getType().equals(property.getRawType())) {
				return;
			}

			for (TypeInformation<?> candidate : property.getPersistentEntityType()) {
				addPersistentEntity(candidate);
			}
		}
	}

	/**
	 * Filter rejecting static fields as well as artificially introduced ones. See
	 * {@link PersistentPropertyFilter#UNMAPPED_PROPERTIES} for details.
	 *
	 * @author Oliver Gierke
	 */
	static enum PersistentPropertyFilter implements FieldFilter {

		INSTANCE;

		private static final Iterable<PropertyMatch> UNMAPPED_PROPERTIES;

		static {

			Set<PropertyMatch> matches = new HashSet<>();
			matches.add(new PropertyMatch("class", null));
			matches.add(new PropertyMatch("this\\$.*", null));
			matches.add(new PropertyMatch("metaClass", "groovy.lang.MetaClass"));

			UNMAPPED_PROPERTIES = Collections.unmodifiableCollection(matches);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.util.ReflectionUtils.FieldFilter#matches(java.lang.reflect.Field)
		 */
		public boolean matches(Field field) {

			if (Modifier.isStatic(field.getModifiers())) {
				return false;
			}

			for (PropertyMatch candidate : UNMAPPED_PROPERTIES) {
				if (candidate.matches(field.getName(), field.getType())) {
					return false;
				}
			}

			return true;
		}

		/**
		 * Returns whether the given {@link PropertyDescriptor} is one to create a {@link PersistentProperty} for.
		 *
		 * @param descriptor must not be {@literal null}.
		 * @return
		 */
		public boolean matches(PropertyDescriptor descriptor) {

			Assert.notNull(descriptor, "PropertyDescriptor must not be null!");

			if (descriptor.getReadMethod() == null && descriptor.getWriteMethod() == null) {
				return false;
			}

			for (PropertyMatch candidate : UNMAPPED_PROPERTIES) {
				if (candidate.matches(descriptor.getName(), descriptor.getPropertyType())) {
					return false;
				}
			}

			return true;
		}

		/**
		 * Value object to help defining property exclusion based on name patterns and types.
		 *
		 * @since 1.4
		 * @author Oliver Gierke
		 */
		static class PropertyMatch {

			private final String namePattern;
			private final String typeName;

			/**
			 * Creates a new {@link PropertyMatch} for the given name pattern and type name. At least one of the paramters
			 * must not be {@literal null}.
			 *
			 * @param namePattern a regex pattern to match field names, can be {@literal null}.
			 * @param typeName the name of the type to exclude, can be {@literal null}.
			 */
			public PropertyMatch(String namePattern, String typeName) {

				Assert.isTrue(!(namePattern == null && typeName == null), "Either name patter or type name must be given!");

				this.namePattern = namePattern;
				this.typeName = typeName;
			}

			/**
			 * Returns whether the given {@link Field} matches the defined {@link PropertyMatch}.
			 *
			 * @param field must not be {@literal null}.
			 * @return
			 */
			public boolean matches(String name, Class<?> type) {

				if (namePattern != null && !name.matches(namePattern)) {
					return false;
				}

				if (typeName != null && !type.getName().equals(typeName)) {
					return false;
				}

				return true;
			}
		}
	}
}
