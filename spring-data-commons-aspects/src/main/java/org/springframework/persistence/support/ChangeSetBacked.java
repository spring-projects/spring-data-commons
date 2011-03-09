package org.springframework.persistence.support;


/**
 * Interface introduced to objects exposing ChangeSet information
 * @author Rod Johnson
 * @author Thomas Risberg
 */
public interface ChangeSetBacked {
	
	ChangeSet getChangeSet();

}
