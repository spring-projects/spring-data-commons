package org.springframework.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Persistence policies that can be attached to entities or relationship
 * fields.
 * @author rodjohnson
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface PersistencePolicy {
	
	enum LATENCY_SENSITIVITY { NONE, MEDIUM, HIGH };
	
	boolean largeObject() default false;
	
	boolean queryable() default true;
	
	boolean immutable() default false;
			
	boolean transactional() default true;
	
	boolean lossAcceptable() default false;
	
	// TODO freshness, or should this be handled separately in a caching annotation
	
	LATENCY_SENSITIVITY latencySensitivity() default LATENCY_SENSITIVITY.HIGH;

}
