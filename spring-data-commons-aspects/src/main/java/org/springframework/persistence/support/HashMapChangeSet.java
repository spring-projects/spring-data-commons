package org.springframework.persistence.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionService;

/**
 * Simple ChangeSet implementation backed by a HashMap.
 * @author Thomas Risberg
 * @author Rod Johnson
 */
public class HashMapChangeSet implements ChangeSet {
	
	private Map<String, Object> values;
		
	public HashMapChangeSet(Map<String,Object> values) {
		this.values = values;
	}
	
	public HashMapChangeSet() {
		this(new HashMap<String, Object>());
	}
	
	@Override
	public void set(String key, Object o) {
		values.put(key, o);
	}

	@Override
	public String toString() {
		return "HashMapChangeSet: values=[" + values + "]";
	}

	@Override
	public Map<String, Object> getValues() {
		return Collections.unmodifiableMap(values);
	}

	@Override
	public Object removeProperty(String k) {
		return this.values.remove(k);
	}

	@Override
	public <T> T get(String key, Class<T> requiredClass, ConversionService conversionService) {
		return conversionService.convert(values.get(key), requiredClass);
	}

}
