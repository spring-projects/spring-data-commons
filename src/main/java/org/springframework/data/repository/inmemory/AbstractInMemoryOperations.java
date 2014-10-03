/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.inmemory;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base implementation of {@link InMemoryOperations} implementing common concerns.
 * 
 * @author Christoph Strobl
 */
public abstract class AbstractInMemoryOperations<Q extends BasicInMemoryQuery> implements InMemoryOperations {

	@SuppressWarnings("rawtypes")//
	private MappingContext<? extends PersistentEntity<?, ? extends PersistentProperty>, ? extends PersistentProperty<?>> mappingContext;

	protected AbstractInMemoryOperations() {
		this(new BasicMappingContext());
	}

	@SuppressWarnings("rawtypes")
	protected AbstractInMemoryOperations(
			MappingContext<? extends PersistentEntity<?, ? extends PersistentProperty>, ? extends PersistentProperty<?>> mappingContext) {

		this.mappingContext = mappingContext;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public <T> T create(T objectToInsert) {

		PersistentEntity<?, ? extends PersistentProperty> entity = this.mappingContext.getPersistentEntity(ClassUtils
				.getUserClass(objectToInsert));

		Serializable id = getIdResolver().readId(objectToInsert, entity);

		if (id == null) {
			id = getIdResolver().createId(entity);
			BeanWrapper.create(objectToInsert, null).setProperty(entity.getIdProperty(), id);
		}

		create(id, objectToInsert);
		return objectToInsert;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#create(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public void create(final Serializable id, final Object objectToInsert) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(objectToInsert, "Object to be inserted must not be 'null'.");

		// TODO: add transaction hook
		execute(new InMemoryCallback<Void>() {

			@Override
			public Void doInMemory(InMemoryAdapter adapter) {

				String typeKey = resolveTypeAlias(objectToInsert.getClass());

				if (adapter.contains(id, typeKey)) {
					throw new InvalidDataAccessApiUsageException("Cannot insert existing object. Please use update.");
				}

				adapter.put(id, objectToInsert, typeKey);
				return null;
			}
		});
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void update(Object objectToUpdate) {

		PersistentEntity<?, ? extends PersistentProperty> entity = this.mappingContext.getPersistentEntity(ClassUtils
				.getUserClass(objectToUpdate));

		if (!entity.hasIdProperty()) {
			throw new InvalidDataAccessApiUsageException(String.format("Cannot determine id for type %s",
					ClassUtils.getUserClass(objectToUpdate)));
		}

		Serializable id = BeanWrapper.create(objectToUpdate, null).getProperty(entity.getIdProperty(), Serializable.class);
		update(id, objectToUpdate);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#update(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public void update(final Serializable id, final Object objectToUpdate) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(objectToUpdate, "Object to be updated must not be 'null'. Use delete to remove.");

		execute(new InMemoryCallback<Void>() {

			@Override
			public Void doInMemory(InMemoryAdapter adapter) {
				adapter.put(id, objectToUpdate, resolveTypeAlias(objectToUpdate.getClass()));
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public <T> List<T> read(final Class<T> type) {

		Assert.notNull(type, "Type to fetch must not be 'null'.");

		final PersistentEntity<?, ? extends PersistentProperty> entity = mappingContext.getPersistentEntity(ClassUtils
				.getUserClass(type));

		return execute(new InMemoryCallback<List<T>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<T> doInMemory(InMemoryAdapter adapter) {
				Collection<?> x = adapter.getAllOf(resolveTypeAlias(type));

				if (entity.getTypeAlias() == null) {
					return new ArrayList<T>((Collection<T>) x);
				}

				ArrayList<T> filtered = new ArrayList<T>();
				for (Object candidate : x) {
					if (typeCheck(type, candidate)) {
						filtered.add((T) candidate);
					}
				}

				return filtered;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(java.io.Serializable, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public <T> T read(final Serializable id, final Class<T> type) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(type, "Type to fetch must not be 'null'.");

		final PersistentEntity<?, ? extends PersistentProperty> entity = mappingContext.getPersistentEntity(ClassUtils
				.getUserClass(type));

		return execute(new InMemoryCallback<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T doInMemory(InMemoryAdapter adapter) {

				Object result = adapter.get(id, resolveTypeAlias(type));

				if (result == null || entity.getTypeAlias() == null || typeCheck(type, result)) {
					return (T) result;
				}

				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#delete(java.lang.Class)
	 */
	@Override
	public void delete(final Class<?> type) {

		Assert.notNull(type, "Type to delete must not be 'null'.");

		final String typeKey = resolveTypeAlias(type);

		execute(new InMemoryCallback<Void>() {

			@Override
			public Void doInMemory(InMemoryAdapter adapter) {

				adapter.deleteAllOf(typeKey);
				return null;
			}
		});
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T delete(T objectToDelete) {

		Class<T> type = (Class<T>) ClassUtils.getUserClass(objectToDelete);
		PersistentEntity<?, ? extends PersistentProperty> entity = this.mappingContext.getPersistentEntity(type);

		return delete(getIdResolver().readId(objectToDelete, entity), type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#delete(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public <T> T delete(final Serializable id, final Class<T> type) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(type, "Type to delete must not be 'null'.");

		return execute(new InMemoryCallback<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T doInMemory(InMemoryAdapter adapter) {
				return (T) adapter.delete(id, resolveTypeAlias(type));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#count(java.lang.Class)
	 */
	@Override
	public long count(Class<?> type) {

		Assert.notNull(type, "Type for count must not be 'null'.");
		return read(type).size();
	}

	@Override
	public <T> T execute(InMemoryCallback<T> action) {

		Assert.notNull(action, "InMemoryCallback must not be 'null'.");

		try {
			return action.doInMemory(this.getAdapter());
		} catch (RuntimeException e) {

			// TODO: potentially convert runtime exception?
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(org.springframework.data.repository.inmemory.InMemoryQuery, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> read(InMemoryQuery query, Class<T> type) {
		return doRead((Q) query, type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#count(org.springframework.data.repository.inmemory.InMemoryQuery, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public long count(InMemoryQuery query, Class<?> type) {
		return doCount((Q) query, type);
	}

	@Override
	public MappingContext<?, ?> getMappingContext() {
		return this.mappingContext;
	}

	protected IdResolver getIdResolver() {
		return DefaultIdResolver.INSTANCE;
	}

	@SuppressWarnings({ "rawtypes" })
	protected String resolveTypeAlias(Class<?> type) {

		Class<?> userClass = ClassUtils.getUserClass(type);
		PersistentEntity<?, ? extends PersistentProperty> entity = this.mappingContext.getPersistentEntity(userClass);

		return entity.getTypeAlias() != null ? entity.getTypeAlias().toString() : userClass.getName();

	}

	protected abstract InMemoryAdapter getAdapter();

	protected abstract <T> List<T> doRead(Q query, Class<T> type);

	protected abstract long doCount(Q query, Class<?> type);

	@SuppressWarnings("rawtypes")
	private static void verifyIdPropertyPresent(PersistentEntity<?, ? extends PersistentProperty> entity) {

		if (!entity.hasIdProperty()) {
			throw new InvalidDataAccessApiUsageException(String.format("Cannot determine id for type %s", entity.getType()));
		}
	}

	private boolean typeCheck(Class<?> requiredType, Object candidate) {

		if (candidate == null) {
			return true;
		}
		return ClassUtils.isAssignable(requiredType, candidate.getClass());
	}

	/**
	 * @author Christoph Strobl
	 */
	public static interface IdResolver {

		/**
		 * Generates a new id for the given {@link PersistentEntity}.
		 * 
		 * @param entity must not be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("rawtypes")
		Serializable createId(PersistentEntity<?, ? extends PersistentProperty> entity);

		/**
		 * Reads the id value from the given source using information provided by {@link PersistentEntity}.
		 * 
		 * @param source must not be {@literal null}.
		 * @param entity must not be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("rawtypes")
		Serializable readId(Object source, PersistentEntity<?, ? extends PersistentProperty> entity);
	}

	/**
	 * @author Christoph Strobl
	 */
	static enum DefaultIdResolver implements IdResolver {

		INSTANCE;

		@SuppressWarnings("rawtypes")
		@Override
		public Serializable createId(PersistentEntity<?, ? extends PersistentProperty> entity) {

			if (!entity.hasIdProperty() || ClassUtils.isAssignable(String.class, entity.getIdProperty().getActualType())) {
				return UUID.randomUUID().toString();
			} else if (ClassUtils.isAssignable(Integer.class, entity.getIdProperty().getActualType())) {
				try {
					return SecureRandom.getInstanceStrong().nextInt();
				} catch (NoSuchAlgorithmException e) {
					throw new InvalidDataAccessApiUsageException("argh....", e);
				}
			} else if (ClassUtils.isAssignable(Long.class, entity.getIdProperty().getActualType())) {
				try {
					return SecureRandom.getInstanceStrong().nextLong();
				} catch (NoSuchAlgorithmException e) {
					throw new InvalidDataAccessApiUsageException("argh....", e);
				}
			}

			throw new InvalidDataAccessApiUsageException("non gereratable id type....");
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Serializable readId(Object source, PersistentEntity<?, ? extends PersistentProperty> entity) {

			verifyIdPropertyPresent(entity);

			return BeanWrapper.create(source, null).getProperty(entity.getIdProperty(), Serializable.class);
		}

	}

}
