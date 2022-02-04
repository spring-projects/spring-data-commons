package org.springframework.data.repository.init;

import org.springframework.data.repository.CrudRepository;

public interface PersonRepository extends CrudRepository<Person, Long> {

	
	@SuppressWarnings("unchecked")
	@Override
	default Person save(Person entity) {
		
		return entity;
	}
	
}
