package org.springframework.persistence;

import java.lang.reflect.Field;
import java.util.Set;

import org.springframework.dao.DataAccessException;

/**
 * Interface to be implemented to infer or compute foreign store
 * key values and possibly store them.
 * 
 * @author Rod Johnson
 *
 */
public interface ForeignStoreKeyManager<T> {
		
	/**
	 * Is this entity class one we can store additional
	 * state in related to fields annotated with ForeignStore.
	 * @param entityClass
	 * @param foreignStore
	 * @return
	 */
	boolean isSupportedField(Class<T> entityClass, Field foreignStore);
	
	/**
	 * 
	 * @param entity
	 * @param foreignStore
	 * @return null if not yet persistent
	 * @throws DataAccessException if the key cannot be computed or stored
	 */
	<K> K findForeignStoreKey(T entity, Field foreignStore, Class<K> keyClass) throws DataAccessException;

	/**
	 * Can be a NOP if the key is inferred
	 * @param entity
	 * @param foreignStore
	 * @param pk
	 * @throws DataAccessException
	 */
	void storeForeignStoreKey(T entity, Field foreignStore, Object pk) throws DataAccessException;
	
	/**
	 * Clear out the foreign key value
	 * Can be a NOP if the key is inferred
	 * @param entity
	 * @param foreignStore
	 * @param keyClass class of the key
	 * @throws DataAccessException
	 */
	void clearForeignStoreKey(T entity, Field foreignStore, Class<?> keyClass) throws DataAccessException;
	
	<K> Set<K> findForeignStoreKeySet(T entity, Field foreignStore, Class<K> keyClass) throws DataAccessException;
		
	void storeForeignStoreKeySet(T entity, Field foreignStore, Set<Object> keys) throws DataAccessException;
	
}
