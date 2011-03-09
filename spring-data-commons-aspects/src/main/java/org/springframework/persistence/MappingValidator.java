package org.springframework.persistence;

import java.lang.reflect.Field;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Interface to validate RelatedAnnotation annotation usage
 * and other mapping constructs.
 * 
 * @author Rod Johnson
 *
 */
public interface MappingValidator {
	
	void validateGet(Class<?> entityClass, Field f, RelatedEntity re) throws InvalidDataAccessApiUsageException;
	
	void validateSetTo(Class<?> entityClass, Field f, RelatedEntity re, Object newVal) throws InvalidDataAccessApiUsageException, IllegalArgumentException;

}
