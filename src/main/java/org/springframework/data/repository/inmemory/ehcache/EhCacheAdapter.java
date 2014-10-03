/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.inmemory.ehcache;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.data.util.ListConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * {@link InMemoryAdapter} implementation using {@link CacheManager}.
 * 
 * @author Christoph Strobl
 */
public class EhCacheAdapter implements InMemoryAdapter {

	private CacheManager cacheManager;

	public EhCacheAdapter() {
		this(CacheManager.create());
	}

	public EhCacheAdapter(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#put(java.io.Serializable, java.lang.Object, java.io.Serializable)
	 */
	@Override
	public Object put(Serializable id, Object item, Serializable collection) {

		Assert.notNull(id, "Id must not be 'null' for adding.");
		Assert.notNull(item, "Item must not be 'null' for adding.");

		Element element = new Element(id, item);
		getCache(collection, item.getClass()).put(element);
		return item;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#contains(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public boolean contains(Serializable id, Serializable collection) {
		return get(id, collection) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#get(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object get(Serializable id, Serializable collection) {

		Cache cache = getCache(collection);
		if (cache == null) {
			return null;
		}

		Element element = cache.get(id);
		return element != null ? element.getObjectValue() : null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#delete(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object delete(Serializable id, Serializable collection) {

		Cache cache = getCache(collection);
		if (cache == null) {
			return null;
		}

		Element element = cache.removeAndReturnElement(id);
		return element != null ? element.getObjectValue() : null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#getAllOf(java.io.Serializable)
	 */
	@Override
	public Collection<?> getAllOf(Serializable collection) {

		Cache cache = getCache(collection);
		if (cache == null) {
			return Collections.emptyList();
		}

		Collection<Element> values = cache.getAll(cache.getKeys()).values();
		return new ListConverter<Element, Object>(new ElementConverter()).convert(values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#deleteAllOf(java.io.Serializable)
	 */
	@Override
	public void deleteAllOf(Serializable collection) {

		Cache cache = getCache(collection);
		if (cache == null) {
			return;
		}

		getCache(collection).removeAll();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#clear()
	 */
	@Override
	public void clear() {
		cacheManager.clearAll();
	}

	protected Cache getCache(Serializable collection) {
		return getCache(collection, null);
	}

	protected Cache getCache(Serializable collection, final Class<?> type) {

		Assert.notNull(collection, "Collection must not be 'null' for lookup.");
		Assert.isInstanceOf(String.class, collection, "Collection identifier must be of type String.");

		Class<?> userType = ClassUtils.getUserClass(type);
		String collectionName = (String) collection;

		if (!cacheManager.cacheExists(collectionName)) {

			if (type == null) {
				return null;
			}

			CacheConfiguration cacheConfig = cacheManager.getConfiguration().getDefaultCacheConfiguration().clone();

			if (!cacheConfig.isSearchable()) {

				cacheConfig = new CacheConfiguration();
				cacheConfig.setMaxEntriesLocalHeap(0);
			}
			cacheConfig.setName(collectionName);
			final Searchable s = new Searchable();

			// TODO: maybe use mappingcontex information at this point or register generic type using some spel expression
			// validator
			ReflectionUtils.doWithFields(userType, new FieldCallback() {

				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

					PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, field.getName());

					if (pd != null && pd.getReadMethod() != null) {
						s.addSearchAttribute(new SearchAttribute().name(field.getName()).expression(
								"value." + pd.getReadMethod().getName() + "()"));
					}
				}
			});

			cacheConfig.addSearchable(s);
			cacheManager.addCache(new Cache(cacheConfig));
		}
		return cacheManager.getCache(collectionName);
	}

	private class ElementConverter implements Converter<Element, Object> {

		@Override
		public Object convert(Element source) {
			return source.getObjectValue();
		}
	}
}
