package org.springframework.data.persistence;

import org.springframework.dao.DataAccessException;

/**
 * Interface to be implemented by classes that can synchronize between data stores and ChangeSets.
 * 
 * @param <K> entity key
 * @author Rod Johnson
 */
public interface ChangeSetPersister<K> {

	String ID_KEY = "_id";

	String CLASS_KEY = "_class";

	/**
	 * TODO how to tell when not found? throw exception?
	 */
	void getPersistentState(Class<? extends ChangeSetBacked> entityClass, K key, ChangeSet changeSet)
			throws DataAccessException, NotFoundException;

	/**
	 * Return id
	 * 
	 * @param entity
	 * @param cs
	 * @return
	 * @throws DataAccessException
	 */
	K getPersistentId(ChangeSetBacked entity, ChangeSet cs) throws DataAccessException;

	/**
	 * Return key
	 * 
	 * @param entity
	 * @param cs Key may be null if not persistent
	 * @return
	 * @throws DataAccessException
	 */
	K persistState(ChangeSetBacked entity, ChangeSet cs) throws DataAccessException;

	/**
	 * Exception thrown in alternate control flow if getPersistentState finds no entity data.
	 */
	class NotFoundException extends Exception {

	}

}
