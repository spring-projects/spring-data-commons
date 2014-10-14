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
package org.springframework.data.keyvalue.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.keyvalue.core.query.KeyValueQuery;

/**
 * @author Christoph Strobl
 * @since 1.10
 * @param <T>
 */
public class CompoundCriteriaAccessor<T> implements CriteriaAccessor<T> {

	List<CriteriaAccessor<?>> accessors = new ArrayList<CriteriaAccessor<?>>();

	public CompoundCriteriaAccessor(CriteriaAccessor<?>... accessors) {
		this.accessors.addAll(Arrays.asList(accessors));
	}

	@Override
	public T resolve(KeyValueQuery<?> query) {

		Object tmp = query.getCritieria();
		for (CriteriaAccessor<?> accessor : this.accessors) {
			try {
				tmp = accessor.resolve(new KeyValueQuery(tmp));
			} catch (Exception e) {
				// TODO: maybe check if another accessor is available and rethrow error if not
				// silently ignore transformation error and call next accessor
			}
		}
		return (T) tmp;
	}

}
