package org.springframework.persistence.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.aspectj.lang.reflect.FieldSignature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aspect that saves field access in a ChangeSet
 * 
 * @author Rod Johnson
 * @author Thomas Risberg
 */
public abstract aspect AbstractDeferredUpdateMixinFields<ET extends Annotation> extends AbstractTypeAnnotatingMixinFields<ET, ChangeSetBacked> {
	
	//-------------------------------------------------------------------------
	// Configure aspect for whole system.
	// init() method can be invoked automatically if the aspect is a Spring
	// bean, or called in user code.
	//-------------------------------------------------------------------------
	// Aspect shared config
	
	private ChangeSetPersister<Object> changeSetPersister;
	
	private ChangeSetSynchronizer<ChangeSetBacked> changeSetManager;
	
	public void setChangeSetConfiguration(ChangeSetConfiguration<Object> changeSetConfiguration) {
		this.changeSetPersister = changeSetConfiguration.getChangeSetPersister();
		this.changeSetManager = changeSetConfiguration.getChangeSetManager();
	}
	
	//-------------------------------------------------------------------------
	// Advise user-defined constructors of ChangeSetBacked objects to create a new
	// backing ChangeSet
	//-------------------------------------------------------------------------
	pointcut arbitraryUserConstructorOfChangeSetBackedObject(ChangeSetBacked entity) : 
		execution((@ET ChangeSetBacked+).new(..)) &&
		!execution((@ET ChangeSetBacked+).new(ChangeSet)) &&
		this(entity);
	
	// Or could use cflow
	pointcut finderConstructorOfChangeSetBackedObject(ChangeSetBacked entity, ChangeSet cs) : 
		execution((@ET ChangeSetBacked+).new(ChangeSet)) &&
		this(entity) && 
		args(cs);
	
	
	before(ChangeSetBacked entity) : arbitraryUserConstructorOfChangeSetBackedObject(entity) {
		entity.itdChangeSetPersister = changeSetPersister;
		log.info("User-defined constructor called on ChangeSetBacked object of class " + entity.getClass());
		// Populate all properties
		ChangeSet changeSet = new HashMapChangeSet();
		changeSetManager.populateChangeSet(changeSet, entity);
		entity.setChangeSet(changeSet);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new InvalidDataAccessResourceUsageException("No transaction synchronization is active");
		}
		TransactionSynchronizationManager.registerSynchronization(new ChangedSetBackedTransactionSynchronization(changeSetPersister, entity));
	}
	
	before(ChangeSetBacked entity, ChangeSet changeSet) : finderConstructorOfChangeSetBackedObject(entity, changeSet) {
		entity.itdChangeSetPersister = changeSetPersister;
		changeSetManager.populateEntity(changeSet, entity);
		
		// Now leave an empty ChangeSet to listen only to future changes
		entity.setChangeSet(new HashMapChangeSet());
		
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new InvalidDataAccessResourceUsageException("No transaction synchronization is active");
		}
		TransactionSynchronizationManager.registerSynchronization(new ChangedSetBackedTransactionSynchronization(changeSetPersister, entity));
	}
	
	
	//-------------------------------------------------------------------------
	// ChangeSet-related mixins
	//-------------------------------------------------------------------------
	// Introduced field
	private ChangeSet ChangeSetBacked.changeSet;
	
	private ChangeSetPersister<?> ChangeSetBacked.itdChangeSetPersister;
	
	public void ChangeSetBacked.setChangeSet(ChangeSet cs) {
		this.changeSet = cs;
	}
	
	public ChangeSet ChangeSetBacked.getChangeSet() {
		return changeSet;
	}
	
	// Flush the entity state to the persistent store
	public void ChangeSetBacked.flush() {
		itdChangeSetPersister.persistState(this.getClass(), this.changeSet);
	}
	
	public Object ChangeSetBacked.getId() {
		return itdChangeSetPersister.getPersistentId(this.getClass(), this.changeSet);
	} 


	//-------------------------------------------------------------------------
	// Around advice for field get/set
	//-------------------------------------------------------------------------
	// Nothing to do on field get unless laziness desired

	Object around(ChangeSetBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
		Field f = ((FieldSignature) thisJoinPoint.getSignature()).getField();
		
		String propName = f.getName();//getRedisPropertyName(thisJoinPoint.getSignature());
		if (newVal instanceof Number) {
			log.info("SET " + f + " -> ChangeSet number value property [" + propName + "] with value=[" + newVal + "]");
			entity.getChangeSet().set(propName, (Number) newVal);
		}
		else if (newVal instanceof String) {
			log.info("SET " + f + " -> ChangeSet string value property [" + propName + "] with value=[" + newVal + "]");
			entity.getChangeSet().set(propName, (String) newVal);
		}
		else {
			log.info("Don't know how to SET " + f + " with value=[" + newVal + "]");
		}
		return proceed(entity, newVal);
	}

}