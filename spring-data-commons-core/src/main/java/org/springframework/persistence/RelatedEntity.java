package org.springframework.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a field may be stored in a foreign store
 * and specifying the necessary guarantees. Conceptual rather than
 * implementation-specific.
 * @see ForeignStoreKeyManager
 * 
 * @author Rod Johnson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RelatedEntity {
	
	/**
	 * 
	 * Optional information as to how to compute or locate the key value.
	 * Some strategies may take this into account.
	 */
	String keyExpression() default "";
	
	/**
	 * Should we use the key of the present entity
	 * @return
	 */
	boolean sameKey() default false;
	
	/**
	 * Policies for persistence
	 * @return
	 */
	PersistencePolicy policy() default @PersistencePolicy();
	
	/**
	 * Name for the preferred data store. Merely a hint. May not be followed.
	 * @return
	 */
	String preferredStore() default "";
	
	/**
	 * Is asynchronous store OK?
	 * @return
	 */
	boolean asynchStore() default false;
	
	// TODO - indicates if an asynchronous write should begin
	// only after commit of a transaction
	boolean afterCommit() default false;
	
	/**
	 * Completion listener class. Only used if asynchStore is true.
	 * Must have a no-arg constructor.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	Class<? extends AsynchStoreCompletionListener> storeCompletionListenerClass() default AsynchStoreCompletionListener.NONE.class;
	
	String storeCompletionListenerBeanName() default "";

}
