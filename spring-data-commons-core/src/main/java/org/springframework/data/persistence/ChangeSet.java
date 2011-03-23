package org.springframework.data.persistence;

import java.util.Map;

import org.springframework.core.convert.ConversionService;

/**
 * Interface representing the set of changes in an entity.
 * 
 * @author Rod Johnson
 * @author Thomas Risberg
 *
 */
public interface ChangeSet {
		
	<T> T get(String key, Class<T> requiredClass, ConversionService cs);
	
	void set(String key, Object o);
	
	Map<String, Object> getValues();
	
	Object removeProperty(String k);

}
