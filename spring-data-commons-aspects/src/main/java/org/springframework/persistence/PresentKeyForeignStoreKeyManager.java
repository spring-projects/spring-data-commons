package org.springframework.persistence;

import java.lang.reflect.Field;

import org.springframework.dao.DataAccessException;

/**
 * ForeignStoreKeyManager implementation that uses the key of the present
 * entity.
 * 
 * @author Rod Johnson
 * 
 */
public class PresentKeyForeignStoreKeyManager extends OrderedForeignStoreKeyManager {

	private final EntityOperationsLocator eoLocator;

	public PresentKeyForeignStoreKeyManager(EntityOperationsLocator eoLocator) {
		this.eoLocator = eoLocator;
	}

	@Override
	public Object findForeignStoreKey(Object entity, Field foreignStore, Class requiredClass) throws DataAccessException {
		EntityOperations eo = eoLocator.entityOperationsFor(entity.getClass(), foreignStore.getAnnotation(RelatedEntity.class));
		return eo.findUniqueKey(entity);
	}

	@Override
	public boolean isSupportedField(Class clazz, Field foreignStore) {
		RelatedEntity fs = foreignStore.getAnnotation(RelatedEntity.class);
		return fs.sameKey();
	}

	@Override
	public void storeForeignStoreKey(Object entity, Field foreignStore, Object pk) throws DataAccessException {
		// Nothing to do
	}

	@Override
	public void clearForeignStoreKey(Object entity, Field foreignStore, Class keyClass) throws DataAccessException {
		// Nothing to do
	}

}
