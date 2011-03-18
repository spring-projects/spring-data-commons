package org.springframework.persistence.support;


/**
 * Try for a constructor taking a ChangeSet: failing that, try a no-arg
 * constructor and then setChangeSet().
 * 
 * @author Rod Johnson
 */
public class ChangeSetConstructorEntityInstantiator extends AbstractConstructorEntityInstantiator<ChangeSetBacked, ChangeSet>{
	
	protected void setState(ChangeSetBacked entity, ChangeSet cs) {
		entity.setChangeSet(cs);
	}

}
