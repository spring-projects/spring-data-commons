package org.springframework.data.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.support.ChangeSetBacked;
import org.springframework.data.support.ChangeSetPersister;
import org.springframework.transaction.support.TransactionSynchronization;

public class ChangeSetBackedTransactionSynchronization implements TransactionSynchronization {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private ChangeSetPersister<Object> changeSetPersister;

	private ChangeSetBacked entity;

	private int changeSetTxStatus = -1;

	public ChangeSetBackedTransactionSynchronization(ChangeSetPersister<Object> changeSetPersister, ChangeSetBacked entity) {
		this.changeSetPersister = changeSetPersister;
		this.entity = entity;
	}
	
	public void afterCommit() {
		log.debug("After Commit called for " + entity);
		changeSetPersister.persistState(entity.getClass(), entity.getChangeSet());
		changeSetTxStatus = 0;
	}

	public void afterCompletion(int status) {
		log.debug("After Completion called with status = " + status);
		if (changeSetTxStatus == 0) {
			if (status == STATUS_COMMITTED) {
				// this is good
				log.debug("ChangedSetBackedTransactionSynchronization completed successfully for " + this.entity);
			}
			else {
				// this could be bad - TODO: compensate
				log.error("ChangedSetBackedTransactionSynchronization failed for " + this.entity);
			}
		}
	}

	public void beforeCommit(boolean readOnly) {
	}

	public void beforeCompletion() {
	}

	public void flush() {
	}

	public void resume() {
		throw new IllegalStateException("ChangedSetBackedTransactionSynchronization does not support transaction suspension currently.");
	}

	public void suspend() {
		throw new IllegalStateException("ChangedSetBackedTransactionSynchronization does not support transaction suspension currently.");
	}
	
}
