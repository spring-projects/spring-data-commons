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

import java.util.Comparator;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.NullHandling;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.util.comparator.CompoundComparator;

/**
 * {@link SortAccessor} implementation capable of creating {@link SpelPropertyComperator}.
 * 
 * @author Christoph Strobl
 */
public enum SpelSortAccessor implements SortAccessor<Comparator<?>> {

	INSTNANCE;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Comparator<?> resolve(KeyValueQuery<?> query) {

		if (query == null || query.getSort() == null) {
			return null;
		}

		CompoundComparator compoundComperator = new CompoundComparator();
		for (Order order : query.getSort()) {

			SpelPropertyComperator<?> spelSort = new SpelPropertyComperator(order.getProperty());

			if (Direction.DESC.equals(order.getDirection())) {

				spelSort.desc();

				if (order.getNullHandling() != null && !NullHandling.NATIVE.equals(order.getNullHandling())) {
					spelSort = NullHandling.NULLS_FIRST.equals(order.getNullHandling()) ? spelSort.nullsFirst() : spelSort
							.nullsLast();
				}
			}
			compoundComperator.addComparator(spelSort);
		}

		return compoundComperator;
	}
}
