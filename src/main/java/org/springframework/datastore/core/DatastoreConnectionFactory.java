package org.springframework.datastore.core;

public interface DatastoreConnectionFactory<C> {
	C getConnection();
}
