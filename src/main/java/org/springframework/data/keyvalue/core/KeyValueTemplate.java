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
package org.springframework.data.keyvalue.core;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.inmemory.BasicMappingContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Basic implementation of {@link KeyValueOperations}.
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
public class KeyValueTemplate implements KeyValueOperations {

	private final KeyValueAdapter adapter;
	private ConcurrentHashMap<Class<?>, String> typeAliasMap = new ConcurrentHashMap<Class<?>, String>();

	@SuppressWarnings("rawtypes")//
	private MappingContext<? extends PersistentEntity<?, ? extends PersistentProperty>, ? extends PersistentProperty<?>> mappingContext;

	public KeyValueTemplate(KeyValueAdapter adapter) {
		this(adapter, new BasicMappingContext());
	}

	@SuppressWarnings("rawtypes")
	public KeyValueTemplate(
			KeyValueAdapter adapter,
			MappingContext<? extends PersistentEntity<?, ? extends PersistentProperty>, ? extends PersistentProperty<?>> mappingContext) {

		this.adapter = adapter;
		this.mappingContext = mappingContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#insert(java.lang.Object)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public <T> T insert(T objectToInsert) {

		PersistentEntity<?, ? extends PersistentProperty> entity = this.mappingContext.getPersistentEntity(ClassUtils
				.getUserClass(objectToInsert));

		Serializable id = getIdResolver().readId(objectToInsert, entity);

		if (id == null) {
			id = getIdResolver().createId(entity);
			BeanWrapper.create(objectToInsert, null).setProperty(entity.getIdProperty(), id);
		}

		insert(id, objectToInsert);
		return objectToInsert;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#insert(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public void insert(final Serializable id, final Object objectToInsert) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(objectToInsert, "Object to be inserted must not be 'null'.");

		execute(new KeyValueCallback<Void>() {

			@Override
			public Void doInKeyValue(KeyValueAdapter adapter) {

				String typeKey = resolveTypeAlias(objectToInsert.getClass());

				if (adapter.contains(id, typeKey)) {
					throw new InvalidDataAccessApiUsageException("Cannot insert existing object. Please use update.");
				}

				adapter.put(id, objectToInsert, typeKey);
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#update(java.lang.Object)
	 */
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
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#update(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public void update(final Serializable id, final Object objectToUpdate) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(objectToUpdate, "Object to be updated must not be 'null'. Use delete to remove.");

		execute(new KeyValueCallback<Void>() {

			@Override
			public Void doInKeyValue(KeyValueAdapter adapter) {
				adapter.put(id, objectToUpdate, resolveTypeAlias(objectToUpdate.getClass()));
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#findAllOf(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public <T> List<T> findAllOf(final Class<T> type) {

		Assert.notNull(type, "Type to fetch must not be 'null'.");

		final PersistentEntity<?, ? extends PersistentProperty> entity = mappingContext.getPersistentEntity(ClassUtils
				.getUserClass(type));

		return execute(new KeyValueCallback<List<T>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<T> doInKeyValue(KeyValueAdapter adapter) {
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
	 *(non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#findById(java.io.Serializable, java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public <T> T findById(final Serializable id, final Class<T> type) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(type, "Type to fetch must not be 'null'.");

		final PersistentEntity<?, ? extends PersistentProperty> entity = mappingContext.getPersistentEntity(ClassUtils
				.getUserClass(type));

		return execute(new KeyValueCallback<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T doInKeyValue(KeyValueAdapter adapter) {

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
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#delete(java.lang.Class)
	 */
	@Override
	public void delete(final Class<?> type) {

		Assert.notNull(type, "Type to delete must not be 'null'.");

		final String typeKey = resolveTypeAlias(type);

		execute(new KeyValueCallback<Void>() {

			@Override
			public Void doInKeyValue(KeyValueAdapter adapter) {

				adapter.deleteAllOf(typeKey);
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#delete(java.lang.Object)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> T delete(T objectToDelete) {

		Class<T> type = (Class<T>) ClassUtils.getUserClass(objectToDelete);
		PersistentEntity<?, ? extends PersistentProperty> entity = this.mappingContext.getPersistentEntity(type);

		return delete(getIdResolver().readId(objectToDelete, entity), type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#delete(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public <T> T delete(final Serializable id, final Class<T> type) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(type, "Type to delete must not be 'null'.");

		return execute(new KeyValueCallback<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T doInKeyValue(KeyValueAdapter adapter) {
				return (T) adapter.delete(id, resolveTypeAlias(type));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#count(java.lang.Class)
	 */
	@Override
	public long count(Class<?> type) {

		Assert.notNull(type, "Type for count must not be 'null'.");
		return findAllOf(type).size();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#execute(org.springframework.data.keyvalue.core.KeyValueCallback)
	 */
	@Override
	public <T> T execute(KeyValueCallback<T> action) {

		Assert.notNull(action, "InMemoryCallback must not be 'null'.");

		try {
			return action.doInKeyValue(this.adapter);
		} catch (RuntimeException e) {

			// TODO: potentially convert runtime exception?
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#find(org.springframework.data.keyvalue.core.query.KeyValueQuery, java.lang.Class)
	 */
	@Override
	public <T> List<T> find(final KeyValueQuery<?> query, final Class<T> type) {

		final PersistentEntity<?, ? extends PersistentProperty> entity = mappingContext.getPersistentEntity(ClassUtils
				.getUserClass(type));

		return execute(new KeyValueCallback<List<T>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<T> doInKeyValue(KeyValueAdapter adapter) {

				Collection<?> result = adapter.find(query, resolveTypeAlias(type));

				if (entity.getTypeAlias() == null) {
					return new ArrayList<T>((Collection<T>) result);
				}

				ArrayList<T> filtered = new ArrayList<T>();
				for (Object candidate : result) {
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
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#findAllOf(org.springframework.data.domain.Sort, java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public <T> List<T> findAllOf(Sort sort, Class<T> type) {
		return find(new KeyValueQuery(sort), type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#findInRange(int, int, java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public <T> List<T> findInRange(int offset, int rows, Class<T> type) {
		return find(new KeyValueQuery().skip(offset).limit(rows), type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#findInRange(int, int, org.springframework.data.domain.Sort, java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public <T> List<T> findInRange(int offset, int rows, Sort sort, Class<T> type) {
		return find(new KeyValueQuery(sort).skip(offset).limit(rows), type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#count(org.springframework.data.keyvalue.core.query.KeyValueQuery, java.lang.Class)
	 */
	@Override
	public long count(final KeyValueQuery<?> query, final Class<?> type) {

		return execute(new KeyValueCallback<Long>() {

			@Override
			public Long doInKeyValue(KeyValueAdapter adapter) {
				return adapter.count(query, resolveTypeAlias(type));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#getMappingContext()
	 */
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

		String potentialAlias = typeAliasMap.get(userClass);
		if (potentialAlias != null) {
			return potentialAlias;
		}

		PersistentEntity<?, ? extends PersistentProperty> entity = this.mappingContext.getPersistentEntity(userClass);

		String alias = entity.getTypeAlias() != null ? entity.getTypeAlias().toString() : userClass.getName();
		typeAliasMap.put(userClass, alias);
		return alias;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() throws Exception {
		this.adapter.clear();
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

	@SuppressWarnings("rawtypes")
	private static void verifyIdPropertyPresent(PersistentEntity<?, ? extends PersistentProperty> entity) {

		if (!entity.hasIdProperty()) {
			throw new InvalidDataAccessApiUsageException(String.format("Cannot determine id for type %s", entity.getType()));
		}
	}

}
