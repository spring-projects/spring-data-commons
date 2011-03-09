package org.springframework.persistence;

import org.springframework.dao.UncategorizedDataAccessException;

public class UnknownEntityClassException extends
		UncategorizedDataAccessException {
	
	public UnknownEntityClassException(Class<?> entityClass) {
		super("Unknown entity class [" + entityClass.getName() + "]", null);
	}

}
