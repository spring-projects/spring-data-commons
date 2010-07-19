package org.springframework.datastore.core;

import java.util.List;

import org.springframework.data.core.DataMapper;
import org.springframework.data.core.QueryDefinition;

public abstract class AbstractDatastoreTemplate<C> {
	
	protected DatastoreConnectionFactory<C> datastoreConnectionFactory;
	
	public DatastoreConnectionFactory<C> getDatastoreConnectionFactory() {
		return datastoreConnectionFactory;
	}

	public void setDatastoreConnectionFactory(DatastoreConnectionFactory<C> datastoreConnectionFactory) {
		this.datastoreConnectionFactory = datastoreConnectionFactory;
	}

	public <T> T execute(DatastoreConnectionCallback<C, T> action) {
		try {
			return action.doInConnection(datastoreConnectionFactory.getConnection());
		}
		catch (Exception e) {
			throw new UncategorizedDatastoreException("Failure executing using datastore connection", e);
		}
	}

	public abstract <S, T> List<T> query(QueryDefinition query, DataMapper<S, T> mapper); 
	
}
