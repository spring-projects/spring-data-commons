package org.springframework.persistence;

import java.lang.reflect.Field;

import org.springframework.dao.DataAccessException;

/**
 * Interface to be implemented by classes that can find ForeignStoreKeyManager
 * implementations to handle particular entities.
 * 
 * @author Rod Johnson
 */
public interface ForeignStoreKeyManagerLocator {
	
	/**
	 * Find the ForeignStoreKeyManager for this class.
	 * @param f field the RelatedEntity annotation is on
	 * @param entityClass
	 * @return
	 * @throws DataAccessException if no ForeignStoreKeyManager can be found.
	 */
	<T> ForeignStoreKeyManager<T> foreignStoreKeyManagerFor(Class<T> entityClass, Field f) throws DataAccessException;

}
