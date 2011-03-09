package org.springframework.persistence;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.dao.DataAccessException;

/**
 * Chaining implementation of ForeignStoreKeyManagerLocator that can be parameterized
 * from a Spring ApplicationContext.
 * 
 * @author Rod Johnson
 * 
 */
public class ChainingForeignStoreKeyManagerLocator implements ForeignStoreKeyManagerLocator {

	private List<ForeignStoreKeyManager> delegates = new LinkedList<ForeignStoreKeyManager>();

	public void add(ForeignStoreKeyManager fskm) {
		delegates.add(fskm);
	}

	@Autowired
	public void init(ApplicationContext context) {
		Map<String, ForeignStoreKeyManager> beansOfType = context.getBeansOfType(ForeignStoreKeyManager.class);
		List<ForeignStoreKeyManager> l = new LinkedList<ForeignStoreKeyManager>();
		for (ForeignStoreKeyManager fskm : beansOfType.values()) {
			l.add(fskm);
		}
		Collections.sort(l, new OrderComparator());
		for (ForeignStoreKeyManager fskm : l) {
			add(fskm);
		}
	}

	@Override
	public <T> ForeignStoreKeyManager<T> foreignStoreKeyManagerFor(Class<T> entityClass, Field f) throws DataAccessException {
		for (ForeignStoreKeyManager fskm : delegates) {
			if (fskm.isSupportedField(entityClass, f)) {
				return fskm;
			}
		}
		throw new IllegalArgumentException("No ForeignStoreKeyManager for " + entityClass + " on " + f);
	}

}
