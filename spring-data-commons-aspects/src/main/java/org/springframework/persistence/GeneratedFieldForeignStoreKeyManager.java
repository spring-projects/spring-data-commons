package org.springframework.persistence;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;

/**
 * Stores keys in generated additional persistent fields
 * that Roo will add. e.g.
 * 
 * <pre>
 * atForeignStore
 * Person Person;
 * 
 * long person_id;
 * 
 * </pre>
 * @author 
 *
 */
public class GeneratedFieldForeignStoreKeyManager extends
		OrderedForeignStoreKeyManager<Roo_GeneratedForeignStoreKeys> {
	
	private final Log log = LogFactory.getLog(getClass());


	@Override
	public Object findForeignStoreKey(Roo_GeneratedForeignStoreKeys entity, Field foreignStore, Class requiredClass)
			throws DataAccessException {
		String methodName = "get" + propertyName(foreignStore);
		Object key = RooConventionEntityOperations.invokeNoArgMethod(entity.getClass(), methodName, entity);
		log.info("FIND foreign store property " + foreignStore + " <- Entity generated String property [" + methodName + "] returned [" + key + "]");
		return key;
	}

	@Override
	public void storeForeignStoreKey(Roo_GeneratedForeignStoreKeys entity, Field foreignStore,
			Object key) throws DataAccessException {
		String methodName = "set" + propertyName(foreignStore);
		RooConventionEntityOperations.invoke(entity.getClass(), methodName, entity, new Class<?>[] { key.getClass()}, key);		
		log.info("STORE foreign store property " + foreignStore + " -> Entity generated String property [" + methodName + "] with key value [" + key + "]");
	}

	@Override
	public void clearForeignStoreKey(Roo_GeneratedForeignStoreKeys entity, Field foreignStore, Class keyClass) throws DataAccessException {
		String methodName = "set" + propertyName(foreignStore);
		RooConventionEntityOperations.invoke(entity.getClass(), methodName, entity, new Class<?>[] { keyClass }, null);		
		log.info("CKEAR foreign store property " + foreignStore + " -> Entity generated String property [" + methodName + "]");
	}

	
	@Override
	public boolean isSupportedField(Class clazz, Field f) {
		// Check for marker interface
		return Roo_GeneratedForeignStoreKeys.class.isAssignableFrom(clazz);
	}
	
	
	private String propertyName(Field f) {
		return "_" + f.getName() + "_Id";
	}

}
