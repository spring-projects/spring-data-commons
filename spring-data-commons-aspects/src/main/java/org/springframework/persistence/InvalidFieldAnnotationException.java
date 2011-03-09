package org.springframework.persistence;

import java.lang.reflect.Field;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Exception thrown on an attempt to use a field with an invalid 
 * RelatedEntity annotation.
 * 
 * @author Rod Johnson
 */
public class InvalidFieldAnnotationException extends
		InvalidDataAccessApiUsageException {
	
	public InvalidFieldAnnotationException(Class<?> entityClass, Field f, String reason) {
		super("Field [" + f.getName() + "] has invalid RelatedEntity annotation: reason='" + reason + "'", null);
	}

}
