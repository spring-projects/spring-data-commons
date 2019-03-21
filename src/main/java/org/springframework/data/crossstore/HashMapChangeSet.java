/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.crossstore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.ConversionService;

/**
 * Simple ChangeSet implementation backed by a HashMap.
 * 
 * @author Thomas Risberg
 * @author Rod Johnson
 */
public class HashMapChangeSet implements ChangeSet {

	private final Map<String, Object> values;

	public HashMapChangeSet(Map<String, Object> values) {
		this.values = values;
	}

	public HashMapChangeSet() {
		this(new HashMap<String, Object>());
	}

	public void set(String key, Object o) {
		values.put(key, o);
	}

	public String toString() {
		return "HashMapChangeSet: values=[" + values + "]";
	}

	public Map<String, Object> getValues() {
		return Collections.unmodifiableMap(values);
	}

	public Object removeProperty(String k) {
		return this.values.remove(k);
	}

	public <T> T get(String key, Class<T> requiredClass, ConversionService conversionService) {
		return conversionService.convert(values.get(key), requiredClass);
	}

}
