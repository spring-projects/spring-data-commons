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
package org.springframework.data.keyvalue.ehcache;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.expression.Criteria;

import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.keyvalue.core.QueryEngine;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * @author Christoph Strobl
 */
public class EhCacheKeyValueAdapter extends AbstractKeyValueAdapter {

	private CacheManager cacheManager;

	public EhCacheKeyValueAdapter() {
		this(CacheManager.create());
	}

	public EhCacheKeyValueAdapter(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public EhCacheKeyValueAdapter(
			QueryEngine<EhCacheKeyValueAdapter, Criteria, Map<Attribute<?>, Direction>> queryEngine, CacheManager cacheManager) {
		super(queryEngine);
		this.cacheManager = cacheManager;
	}

	@Override
	public Object put(Serializable id, Object item, Serializable keyspace) {

		Assert.notNull(id, "Id must not be 'null' for adding.");
		Assert.notNull(item, "Item must not be 'null' for adding.");

		Element element = new Element(id, item);
		getCache(keyspace, item.getClass()).put(element);
		return item;
	}

	@Override
	public boolean contains(Serializable id, Serializable keyspace) {
		return get(id, keyspace) != null;
	}

	@Override
	public Object get(Serializable id, Serializable keyspace) {

		Cache cache = getCache(keyspace);
		if (cache == null) {
			return null;
		}

		Element element = cache.get(id);
		return ElementConverter.INSTANCE.convert(element);
	}

	@Override
	public Object delete(Serializable id, Serializable keyspace) {

		Cache cache = getCache(keyspace);
		if (cache == null) {
			return null;
		}

		Element element = cache.removeAndReturnElement(id);
		return ElementConverter.INSTANCE.convert(element);
	}

	@Override
	public Collection<?> getAllOf(Serializable keyspace) {

		Cache cache = getCache(keyspace);
		if (cache == null) {
			return Collections.emptyList();
		}

		Collection<Element> values = cache.getAll(cache.getKeys()).values();
		return new ListConverter<Element, Object>(ElementConverter.INSTANCE).convert(values);
	}

	@Override
	public void deleteAllOf(Serializable keyspace) {

		Cache cache = getCache(keyspace);
		if (cache == null) {
			return;
		}

		cache.removeAll();
	}

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

	private enum ElementConverter implements Converter<Element, Object> {
		INSTANCE;

		@Override
		public Object convert(Element source) {

			if (source == null) {
				return null;
			}
			return source.getObjectValue();
		}
	}

	@Override
	public void destroy() throws Exception {
		this.cacheManager.shutdown();
	}

}
