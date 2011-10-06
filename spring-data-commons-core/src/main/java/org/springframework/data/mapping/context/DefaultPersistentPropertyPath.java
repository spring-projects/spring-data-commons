/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstraction of a path of {@link PersistentProperty}s.
 *
 * @author Oliver Gierke
 */
class DefaultPersistentPropertyPath<T extends PersistentProperty<T>> implements PersistentPropertyPath<T> {
	
	private final Iterable<T> properties;

	/**
	 * Creates a new {@link DefaultPersistentPropertyPath} for the given {@link PersistentProperty}s.
	 * 
	 * @param properties must not be {@literal null}.
	 */
	public DefaultPersistentPropertyPath(Iterable<T> properties) {
		Assert.notNull(properties);
		this.properties = properties;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toDotPath()
	 */
	public String toDotPath() {
		return toDotPath(new Converter<T, String>() {
			public String convert(T source) {
				return source.getName();
			}
		});
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toDotPath(org.springframework.core.convert.converter.Converter)
	 */
	public String toDotPath(Converter<? super T, String> converter) {
		
		List<String> result = new ArrayList<String>();
		
		for (T property : properties) {
			result.add(converter.convert(property));
		}
		
		return StringUtils.collectionToDelimitedString(result, ".");
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<T> iterator() {
		return properties.iterator();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		
		if (this == obj) {
			return true;
		}
		
		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}
		
		DefaultPersistentPropertyPath<?> that = (DefaultPersistentPropertyPath<?>) obj;
		
		return this.properties.equals(that.properties);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return properties.hashCode();
	}
}
