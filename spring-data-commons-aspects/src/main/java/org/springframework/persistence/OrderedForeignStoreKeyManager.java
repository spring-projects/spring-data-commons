package org.springframework.persistence;

import java.lang.reflect.Field;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;

/**
 * Convenient base class for ForeignStoreKeyManager implementations that adds
 * ordering support.
 * 
 * @author Rod Johnson
 */
public abstract class OrderedForeignStoreKeyManager<T> implements ForeignStoreKeyManager<T>, Ordered {

	protected final Log log = LogFactory.getLog(getClass());

	private int order = Integer.MAX_VALUE;

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Subclasses can override if they support collection management.
	 */
	@Override
	public <K> Set<K> findForeignStoreKeySet(T entity, Field foreignStore, Class<K> keyClass) throws DataAccessException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void storeForeignStoreKeySet(T entity, Field foreignStore, Set<Object> keys) throws DataAccessException {
		throw new UnsupportedOperationException();
	}

}