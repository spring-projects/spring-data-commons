package org.springframework.persistence;

import org.springframework.dao.DataAccessException;

/**
 * Interface to be implemented by classes that can find EntityOperations
 * implementations to handle particular classes.
 * 
 * @author Rod Johnson
 */
public interface EntityOperationsLocator {
	
	/**
	 * Find the EntityOperations for this class.
	 * @param fs ForeignStore annotation (may be null)
	 * @param entityClass
	 * @return
	 * @throws DataAccessException if no EntityOperations can be found.
	 */
	<T> EntityOperations<?,T> entityOperationsFor(Class<T> entityClass, RelatedEntity fs) throws DataAccessException;

}
