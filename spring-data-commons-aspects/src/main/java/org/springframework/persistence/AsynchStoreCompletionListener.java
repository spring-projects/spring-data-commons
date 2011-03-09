package org.springframework.persistence;

import java.lang.reflect.Field;

/**
 * Listener interface for asynchronous storage operations.
 * Can be annotated with OnlyOnFailure as an optimization
 * if the listener is only interested in compensating transactions
 * in the event of write failure.
 * 
 * @author Rod Johnson
 *
 * @param <V> new value type
 */
public interface AsynchStoreCompletionListener<V> {
	
	/**
	 * Constant indicating no store completion action
	 */
	 class NONE implements AsynchStoreCompletionListener<Object> {
		public void onCompletion(AsynchStoreCompletionListener.StoreResult result, Object newValue, Field foreignStore) {}
	}
	
	enum StoreResult { 
		SUCCESS, 
		FAILURE, 
		INDETERMINATE 
	};	
	
	void onCompletion(StoreResult result, V newValue, Field foreignStore);	
	
}
