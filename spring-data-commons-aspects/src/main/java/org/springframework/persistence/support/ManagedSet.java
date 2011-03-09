package org.springframework.persistence.support;

import java.util.Set;

public interface ManagedSet extends Set {
	
	Set getKeySet();
	
	void addListener(ChangeListener l);
	
	boolean isDirty();
	
	interface ChangeListener {
		void onDirty();
	}
	
	// TODO move into managed collection
	
	// TODO insertions, deletions
	// after markSynchronized()
	
	// This may be wrong, shouldn't it give something back for a ChangeSet?
//	void retrieve(EntityOperationsLocator eol) throws DataAccessException;
//	
//	void persist(EntityOperationsLocator eol) throws DataAccessException;

}
