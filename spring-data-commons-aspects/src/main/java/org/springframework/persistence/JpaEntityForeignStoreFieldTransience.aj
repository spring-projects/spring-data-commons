package org.springframework.persistence;

import javax.persistence.Transient;
import javax.persistence.Entity;

/**
 * Aspect to annotate @ForeignStore fields as JPA @Transient to stop
 * JPA trying to manage them itself
 * @author Rod Johnson
 *
 */
public privileged aspect JpaEntityForeignStoreFieldTransience {
	
	declare @field : @RelatedEntity * (@Entity *).* : @Transient;


}
