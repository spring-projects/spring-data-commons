package org.springframework.persistence;

import java.lang.reflect.Field;

import org.springframework.dao.DataAccessException;

/**
 * Interface to be implemented for each persistence technology,
 * handling operations for the relevant entity type.
 * Parameters: Key=K, Entity class=E
 * 
 * @author Rod Johnson
 */
public interface EntityOperations<K,E> {
		
	/**
	 * Is this clazz supported by the current EntityOperations?
	 * @param entityClass
	 * @param fs ForeignStore annotation, may be null
	 * @return
	 */
	boolean supports(Class<?> entityClass, RelatedEntity fs);
	
	/**
	 * Return null if not found
	 * @param <T>
	 * @param entityClass
	 * @param pk
	 * @return
	 * @throws DataAccessException
	 */
	E findEntity(Class<E> entityClass, K pk) throws DataAccessException;
	
	/**
	 * Find the unique key for the given entity whose class this EntityOperations
	 * understands. For example, it might be the id property value.
	 * @param entity
	 * @return
	 * @throws DataAccessException
	 */
	K findUniqueKey(E entity) throws DataAccessException;
	
	/**
	 * 
	 * @param entityClass
	 * @return the type of the unique key for this supported entity
	 * @throws DataAccessException
	 */
	Class<?> uniqueKeyType(Class<K> entityClass) throws DataAccessException;
	
	boolean isTransient(E entity) throws DataAccessException;
	
	/**
	 * Persist. Will cause key to be non-null.
	 * @param owner Persistent root entity, which has the RelatedEntity field
	 * @param entity
	 * @param f Foreign store field for entity being persisted
	 * @param fs ForeignStore annotation
	 * @throws DataAccessException
	 * @return the new unique key
	 */
	K makePersistent(Object owner, E entity, Field f, RelatedEntity fs) throws DataAccessException;
	
	/**
	 * Is this type of entity transactional?
	 * @return
	 */
	boolean isTransactional();
	
	/**
	 * Should the field be cached in the entity? For some entity types
	 * such as streams, there should be no caching, and the value
	 * should be retrieved from the persistent store every time.
	 * @return
	 */
	boolean cacheInEntity();

}
