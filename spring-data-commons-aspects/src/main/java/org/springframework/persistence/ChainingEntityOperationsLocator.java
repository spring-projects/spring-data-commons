package org.springframework.persistence;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.dao.DataAccessException;

/**
 * Chaining implementation of entity operations that automatically configures itself
 * from a Spring context if available.
 * 
 * @author Rod Johnson
 */
public class ChainingEntityOperationsLocator implements EntityOperationsLocator {
	
	private List<EntityOperations> entityOperationsList = new LinkedList<EntityOperations>();
	
	@Autowired
	public void init(ApplicationContext context) {
		Map<String, EntityOperations> beansOfType = context.getBeansOfType(EntityOperations.class);
		List<EntityOperations> l = new LinkedList<EntityOperations>();
		for (EntityOperations eo : beansOfType.values()) { 
				l.add(eo);
		}
		Collections.sort(l, new OrderComparator());
		for (EntityOperations eo : l) {
			add(eo);
		}
	}
	
	public void add(EntityOperations ef) {
		entityOperationsList.add(ef);
	}
	
	@Override
	public <T> EntityOperations<?,T> entityOperationsFor(Class<T> entityClass, RelatedEntity fs)
			throws DataAccessException {
		for (EntityOperations eo : entityOperationsList) {
			if (eo.supports(entityClass, fs)) {
				return eo;
			}
		}
		throw new UnknownEntityClassException(entityClass);
	}

}
