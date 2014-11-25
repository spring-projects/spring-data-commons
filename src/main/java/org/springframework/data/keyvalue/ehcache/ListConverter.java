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

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Christoph Strobl
 * @param <S>
 * @param <T>
 */
public class ListConverter<S, T> implements Converter<Iterable<S>, List<T>> {

	private Converter<S, T> itemConverter;

	/**
	 * @param itemConverter The {@link Converter} to use for converting individual List items
	 */
	public ListConverter(Converter<S, T> itemConverter) {
		this.itemConverter = itemConverter;
	}

	public List<T> convert(Iterable<S> source) {
		if (source == null) {
			return null;
		}
		List<T> results = new ArrayList<T>();
		for (S result : source) {
			results.add(itemConverter.convert(result));
		}
		return results;
	}

}
