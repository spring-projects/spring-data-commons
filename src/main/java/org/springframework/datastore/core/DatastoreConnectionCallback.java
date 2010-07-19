package org.springframework.datastore.core;

import org.springframework.dao.DataAccessException;

public interface DatastoreConnectionCallback<C, T> {

	T doInConnection(C con) throws DataAccessException;
}
