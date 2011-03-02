package org.springframework.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;

/**
 * Convenient base class for EntityOperations implementations 
 * that adds ordering support.
 * @author Rod Johnson
 *
 * @param <K>
 * @param <E>
 */
public abstract class OrderedEntityOperations<K, E> implements EntityOperations<K, E>, Ordered {

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
	 * Convenient default. Subclasses with non-Long key types can override this if they wish.
	 */
	@Override
	public Class<?> uniqueKeyType(Class<K> entityClass) throws DataAccessException {
		return Long.class;
	}
}