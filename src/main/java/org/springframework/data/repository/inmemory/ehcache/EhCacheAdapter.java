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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;

import org.springframework.beans.BeanUtils;
import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * @author Christoph Strobl
 */
public class EhCacheAdapter implements InMemoryAdapter {

	private CacheManager cacheManager;

	public EhCacheAdapter() {
		cacheManager = CacheManager.create();
	}

	@Override
	public Object put(Serializable id, Object item) {

		Element element = new Element(id, item);
		getCache(item).put(element);
		return item;
	}

	@Override
	public boolean contains(Serializable id, Class<?> type) {
		return get(id, type) != null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Serializable id, Class<T> type) {
		Element element = getCache(type).get(id);
		return (T) (element != null ? element.getObjectValue() : null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T delete(Serializable id, Class<T> type) {

		Element element = getCache(type).removeAndReturnElement(id);
		return (T) (element != null ? element.getObjectValue() : null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Collection<T> getAllOf(Class<T> type) {
		return (Collection<T>) getCache(type).getAll(getCache(type).getKeys());
	}

	@Override
	public void deleteAllOf(Class<?> type) {
		getCache(type).removeAll();
	}

	@Override
	public void clear() {
		cacheManager.clearAll();
	}

	protected Cache getCache(Object item) {

		Assert.notNull(item, "Item must not be 'null' for lookup.");
		return getCache(item.getClass());
	}

	protected Cache getCache(final Class<?> type) {

		Assert.notNull(type, "Type must not be 'null' for lookup.");
		Class<?> userType = ClassUtils.getUserClass(type);

		if (!cacheManager.cacheExists(userType.getName())) {

			CacheConfiguration cacheConfig = new CacheConfiguration(userType.getName(), 0);
			final Searchable s = new Searchable();

			// TODO: maybe use mappingcontex information at this point or register generic type using some spel expression
			// validator
			ReflectionUtils.doWithFields(userType, new FieldCallback() {

				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

					PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, field.getName());

					if (pd.getReadMethod() != null) {
						s.addSearchAttribute(new SearchAttribute().name(field.getName()).expression(
								"value." + pd.getReadMethod().getName() + "()"));
					}
				}
			});

			cacheConfig.addSearchable(s);
			cacheManager.addCache(new Cache(cacheConfig));
		}
		return cacheManager.getCache(userType.getName());
	}
}
