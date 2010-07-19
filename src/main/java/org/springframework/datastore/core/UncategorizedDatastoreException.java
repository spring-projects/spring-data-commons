package org.springframework.datastore.core;

import org.springframework.dao.UncategorizedDataAccessException;

public class UncategorizedDatastoreException extends
		UncategorizedDataAccessException {

	public UncategorizedDatastoreException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
