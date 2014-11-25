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

import static org.springframework.data.keyvalue.core.KeySpaceUtils.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Basic implementation of {@link KeyValueOperations}.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.10
 */
public class KeyValueTemplate implements KeyValueOperations {

	private static final PersistenceExceptionTranslator DEFAULT_PERSISTENCE_EXCEPTION_TRANSLATOR = new KeyValuePersistenceExceptionTranslator();

	private final KeyValueAdapter adapter;
	private final ConcurrentHashMap<Class<?>, String> keySpaceCache = new ConcurrentHashMap<Class<?>, String>();
	private final MappingContext<? extends PersistentEntity<?, ? extends PersistentProperty<?>>, ? extends PersistentProperty<?>> mappingContext;
	private final IdentifierGenerator identifierGenerator;
	private PersistenceExceptionTranslator exceptionTranslator = DEFAULT_PERSISTENCE_EXCEPTION_TRANSLATOR;

	/**
	 * Create new {@link KeyValueTemplate} using the given {@link KeyValueAdapter} with a default
	 * {@link KeyValueMappingContext}.
	 * 
	 * @param adapter must not be {@literal null}.
	 */
	public KeyValueTemplate(KeyValueAdapter adapter) {
		this(adapter, new KeyValueMappingContext());
	}

	/**
	 * Create new {@link KeyValueTemplate} using the given {@link KeyValueAdapter} and {@link MappingContext}.
	 * 
	 * @param adapter must not be {@literal null}.
	 * @param mappingContext must not be {@literal null}.
	 */
	@SuppressWarnings("rawtypes")
	public KeyValueTemplate(
			KeyValueAdapter adapter,
			MappingContext<? extends PersistentEntity<?, ? extends PersistentProperty>, ? extends PersistentProperty<?>> mappingContext) {

		Assert.notNull(adapter, "Adapter must not be null!");
		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.adapter = adapter;
		this.mappingContext = mappingContext;
		this.identifierGenerator = DefaultIdentifierGenerator.INSTANCE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#insert(java.lang.Object)
	 */
	@Override
	public <T> T insert(T objectToInsert) {

		PersistentEntity<?, ?> entity = this.mappingContext.getPersistentEntity(ClassUtils.getUserClass(objectToInsert));

		GeneratingIdAccessor generatingIdAccessor = new GeneratingIdAccessor(entity.getPropertyAccessor(objectToInsert),
				entity.getIdProperty(), identifierGenerator);
		Object id = generatingIdAccessor.getOrGenerateIdentifier();

		insert((Serializable) id, objectToInsert);
		return objectToInsert;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#insert(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public void insert(final Serializable id, final Object objectToInsert) {

		Assert.notNull(id, "Id for object to be inserted must not be null!");
		Assert.notNull(objectToInsert, "Object to be inserted must not be null!");

		execute(new KeyValueCallback<Void>() {

			@Override
			public Void doInKeyValue(KeyValueAdapter adapter) {

				String typeKey = resolveKeySpace(objectToInsert.getClass());

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

		update((Serializable) entity.getIdentifierAccessor(objectToUpdate).getIdentifier(), objectToUpdate);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#update(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public void update(final Serializable id, final Object objectToUpdate) {

		Assert.notNull(id, "Id for object to be inserted must not be null!");
		Assert.notNull(objectToUpdate, "Object to be updated must not be null!");

		execute(new KeyValueCallback<Void>() {

			@Override
			public Void doInKeyValue(KeyValueAdapter adapter) {
				adapter.put(id, objectToUpdate, resolveKeySpace(objectToUpdate.getClass()));
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#findAllOf(java.lang.Class)
	 */
	@Override
	public <T> List<T> findAllOf(final Class<T> type) {

		Assert.notNull(type, "Type to fetch must not be null!");

		return execute(new KeyValueCallback<List<T>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<T> doInKeyValue(KeyValueAdapter adapter) {

				Collection<?> x = adapter.getAllOf(resolveKeySpace(type));

				if (getKeySpace(type) == null) {
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
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#findById(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public <T> T findById(final Serializable id, final Class<T> type) {

		Assert.notNull(id, "Id for object to be inserted must not be null!");
		Assert.notNull(type, "Type to fetch must not be null!");

		return execute(new KeyValueCallback<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T doInKeyValue(KeyValueAdapter adapter) {

				Object result = adapter.get(id, resolveKeySpace(type));

				if (result == null || getKeySpace(type) == null || typeCheck(type, result)) {
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

		Assert.notNull(type, "Type to delete must not be null!");

		final String typeKey = resolveKeySpace(type);

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

		return delete((Serializable) entity.getIdentifierAccessor(objectToDelete).getIdentifier(), type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#delete(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public <T> T delete(final Serializable id, final Class<T> type) {

		Assert.notNull(id, "Id for object to be inserted must not be null!");
		Assert.notNull(type, "Type to delete must not be null!");

		return execute(new KeyValueCallback<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T doInKeyValue(KeyValueAdapter adapter) {
				return (T) adapter.delete(id, resolveKeySpace(type));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#count(java.lang.Class)
	 */
	@Override
	public long count(Class<?> type) {

		Assert.notNull(type, "Type for count must not be null!");
		return findAllOf(type).size();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#execute(org.springframework.data.keyvalue.core.KeyValueCallback)
	 */
	@Override
	public <T> T execute(KeyValueCallback<T> action) {

		Assert.notNull(action, "KeyValueCallback must not be null!");

		try {
			return action.doInKeyValue(this.adapter);
		} catch (RuntimeException e) {
			throw resolveExceptionIfPossible(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueOperations#find(org.springframework.data.keyvalue.core.query.KeyValueQuery, java.lang.Class)
	 */
	@Override
	public <T> List<T> find(final KeyValueQuery<?> query, final Class<T> type) {

		return execute(new KeyValueCallback<List<T>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<T> doInKeyValue(KeyValueAdapter adapter) {

				Collection<?> result = adapter.find(query, resolveKeySpace(type));

				if (getKeySpace(type) == null) {
					return new ArrayList<T>((Collection<T>) result);
				}

				List<T> filtered = new ArrayList<T>();

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
				return adapter.count(query, resolveKeySpace(type));
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() throws Exception {
		this.adapter.clear();
	}

	/**
	 * Set the {@link PersistenceExceptionTranslator} used for converting {@link RuntimeException}.
	 * 
	 * @param exceptionTranslator must not be {@literal null}.
	 */
	public void setExceptionTranslator(PersistenceExceptionTranslator exceptionTranslator) {

		Assert.notNull(exceptionTranslator, "ExceptionTranslator must not be null.");
		this.exceptionTranslator = exceptionTranslator;
	}

	protected String resolveKeySpace(Class<?> type) {

		Class<?> userClass = ClassUtils.getUserClass(type);

		String potentialAlias = keySpaceCache.get(userClass);

		if (potentialAlias != null) {
			return potentialAlias;
		}

		String keySpaceString = null;
		Object keySpace = getKeySpace(type);

		if (keySpace != null) {
			keySpaceString = keySpace.toString();
		}

		if (!StringUtils.hasText(keySpaceString)) {
			keySpaceString = userClass.getName();
		}

		keySpaceCache.put(userClass, keySpaceString);
		return keySpaceString;
	}

	private static boolean typeCheck(Class<?> requiredType, Object candidate) {
		return candidate == null ? true : ClassUtils.isAssignable(requiredType, candidate.getClass());
	}

	private RuntimeException resolveExceptionIfPossible(RuntimeException e) {

		DataAccessException translatedException = exceptionTranslator.translateExceptionIfPossible(e);
		return translatedException != null ? translatedException : e;
	}
}
