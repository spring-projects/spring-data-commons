package org.springframework.persistence.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.persistence.EntityOperations;
import org.springframework.persistence.EntityOperationsLocator;

public class DefaultManagedSet implements ManagedSet {
	
	public static DefaultManagedSet fromEntitySet(Set entitySet, EntityOperationsLocator eol) {
		DefaultManagedSet dms = new DefaultManagedSet(eol);
		if (entitySet != null) {
			for (Object entity : entitySet) {
				dms.add(entity);
			}
		}
		return dms;
	}
	
	public static DefaultManagedSet fromKeySet(Set keySet, Class<?> entityClass, EntityOperationsLocator eol) throws DataAccessException {
		DefaultManagedSet dms = new DefaultManagedSet(eol);
		if (keySet != null) {
			for (Object key : keySet) {
				dms.keySet.add(key);
				EntityOperations eo = eol.entityOperationsFor(entityClass, null);
				dms.entitySet.add(eo.findEntity(entityClass, key));
			}
		}
		return dms;
	}
	
	private final Set keySet = new HashSet();
	
	private final Set entitySet = new HashSet();
	
	private boolean dirty;
	
	private List<ChangeListener> listeners = new LinkedList<ChangeListener>();
	
	private final EntityOperationsLocator entityOperationsLocator;
	
	private DefaultManagedSet(EntityOperationsLocator eol) {
		this.entityOperationsLocator = eol;
	}
	
	@Override
	public void addListener(ChangeListener l) {
		this.listeners.add(l);
	}
	
	
	protected void publishEvent() {
		this.dirty = true;
		for (ChangeListener l : listeners) {
			l.onDirty();
		}
	}
	
	@Override
	public Set getKeySet() {
		return this.keySet;
	}
	
	@Override
	public boolean isDirty() {
		return this.dirty;	
	}


	@Override
	public boolean add(Object e) {
		if (entitySet.contains(e)) {
			return false;
		}
		EntityOperations eo = entityOperationsLocator.entityOperationsFor(e.getClass(), null);
		Object key = eo.findUniqueKey(e);
		if (key == null) {
			eo.makePersistent(null, e, null, null);
		}
		key = eo.findUniqueKey(e);
		keySet.add(key);
		entitySet.add(e);
		publishEvent();
		return true;
	}

	@Override
	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		this.entitySet.clear();
		this.keySet.clear();
		publishEvent();
	}

	@Override
	public boolean contains(Object o) {
		return entitySet.contains(o);
	}

	@Override
	public boolean containsAll(Collection c) {
		return entitySet.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return keySet.isEmpty();
	}

	@Override
	public Iterator iterator() {
		return entitySet.iterator();
	}

	@Override
	public boolean remove(Object e) {
		if (!entitySet.contains(e)) {
			return false;
		}
		EntityOperations eo = entityOperationsLocator.entityOperationsFor(e.getClass(), null);
		keySet.remove(eo.findUniqueKey(e));
		entitySet.remove(e);
		publishEvent();
		return true;
	}

	@Override
	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return keySet.size();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray(Object[] a) {
		throw new UnsupportedOperationException();
	}
		
}
