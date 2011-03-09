package org.springframework.persistence.support;

import java.lang.reflect.Field;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.persistence.OrderedForeignStoreKeyManager;

/**
 * ForeignStoreKeyManager implementation that backs the foreign key to a
 * ChangeSet.
 * 
 * @author Thomas Risberg
 * @author Rod Johnson
 */
public class ChangeSetForeignStoreKeyManager extends OrderedForeignStoreKeyManager<ChangeSetBacked> {

	public static final String FOREIGN_STORE_SET_PREFIX = "S";

	protected final Log log = LogFactory.getLog(getClass());

	private String fieldDelimiter = ".";
	
	private final ConversionService conversionService;
	
	public String getFieldDelimiter() {
		return fieldDelimiter;
	}

	public void setFieldDelimiter(String fieldDelimiter) {
		this.fieldDelimiter = fieldDelimiter;
	}

	@Autowired
	public ChangeSetForeignStoreKeyManager(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void clearForeignStoreKey(ChangeSetBacked entity, Field foreignStore, Class<?> keyClass) throws DataAccessException {
		String propName = propertyName(entity, foreignStore);
		entity.getChangeSet().removeProperty(propName);
		log.info("CLEAR foreign store property " + foreignStore + " <- ChangeSetBacked foreign key property [" + propName + "]");
	}

	@Override
	public <K> K findForeignStoreKey(ChangeSetBacked entity, Field foreignStore, Class<K> keyClass) throws DataAccessException {
		String propName = propertyName(entity, foreignStore);
		System.err.println("+++ " + entity.getChangeSet().getValues());
		K key = entity.getChangeSet().get(propName, keyClass, this.conversionService);
		log.info("FIND foreign store property " + foreignStore + " <- ChangeSetBacked foreign key property [" + propName + "] returned ["
				+ key + "]");
		return key;
	}

	@Override
	public boolean isSupportedField(Class<ChangeSetBacked> entityClass, Field foreignStore) {
		return ChangeSetBacked.class.isAssignableFrom(entityClass);
	}

	@Override
	public void storeForeignStoreKey(ChangeSetBacked entity, Field foreignStore, Object pk) throws DataAccessException {
		String propName = propertyName(entity, foreignStore);
		entity.getChangeSet().set(propName, pk);
		log.info("STORE foreign store property " + foreignStore + " -> ChangeSetBacked foreign key property [" + propName
				+ "] with key value [" + pk + "]");
	}

	@Override
	public <K> Set<K> findForeignStoreKeySet(ChangeSetBacked entity, Field foreignStore, Class<K> keyClass) throws DataAccessException {
		Set keySet = entity.getChangeSet().get(foreignStoreKeyName(foreignStore), Set.class, this.conversionService);
//		if (keySet != null && !keySet.isEmpty())
//			System.out.println("KeySET=**************" + keySet + ", 0th type=" + keySet.iterator().next().getClass());
		return keySet;
	}

	private String foreignStoreKeyName(Field foreignStore) {
		return FOREIGN_STORE_SET_PREFIX + getFieldDelimiter() + foreignStore.getName();
	}

	@Override
	public void storeForeignStoreKeySet(ChangeSetBacked entity, Field foreignStore, Set<Object> keys) throws DataAccessException {
		entity.getChangeSet().set(foreignStoreKeyName(foreignStore), keys);
	}

	private String propertyName(ChangeSetBacked rb, Field f) {
		return rb.getClass().getSimpleName() + getFieldDelimiter() + f.getName();
	}

}
