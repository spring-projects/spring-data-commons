package org.springframework.data.persistence;

public class ChangeSetConfiguration<T> {

	private ChangeSetPersister<T> changeSetPersister;

	private ChangeSetSynchronizer<ChangeSetBacked> changeSetManager;

	public ChangeSetPersister<T> getChangeSetPersister() {
		return changeSetPersister;
	}

	public void setChangeSetPersister(ChangeSetPersister<T> changeSetPersister) {
		this.changeSetPersister = changeSetPersister;
	}

	public ChangeSetSynchronizer<ChangeSetBacked> getChangeSetManager() {
		return changeSetManager;
	}

	public void setChangeSetManager(
			ChangeSetSynchronizer<ChangeSetBacked> changeSetManager) {
		this.changeSetManager = changeSetManager;
	}


}
