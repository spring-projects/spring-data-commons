package org.springframework.data.persistence;


/**
 * Interface introduced to objects exposing ChangeSet information
 * @author Rod Johnson
 * @author Thomas Risberg
 */
public interface ChangeSetBacked {
	
	ChangeSet getChangeSet();

}
