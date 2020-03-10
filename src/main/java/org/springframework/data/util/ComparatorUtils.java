/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.data.util;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static java.util.Comparator.*;
import static java.util.Optional.*;
import static org.springframework.beans.BeanUtils.*;

/**
 * Converter for {@link Sort} to make it applicable as Comparator based on reflection.
 *
 * @author Martin Konstiak
 */

@UtilityClass
public class ComparatorUtils {

	/**
	 * Converts {@link Sort} type to appropriate {@link Comparator} which can compare provided entities. Converts all
	 * provided orders to separate comparator and joins them into one by thenComparing operation. Null values have
	 * precedence in case of ascending direction.
	 *
	 * @param sort sort to convert
	 * @param <T> class to which the comparator can be applied
	 * @return generated comparator
	 */
	public static <T> Comparator<T> comparatorOf(Sort sort) {

		return Comparator.nullsFirst(sort.get().map(ComparatorUtils::comparatorOf).map(Comparator::nullsFirst)
				.reduce(Comparator::thenComparing).orElse(comparing(e -> 0)));
	}

	/**
	 * Converts {@link Sort.Order} type to appropriate {@link Comparator} which can compare provided entities. Null values
	 * have precedence in case of ascending direction.
	 *
	 * @param order order to convert
	 * @param <T> class to which the comparator can be applied
	 * @param <U> class of property specified in order
	 * @return generated comparator
	 */
	public static <T, U extends Comparable<? super U>> Comparator<T> comparatorOf(Sort.Order order) {

		Function<T, U> keyExtractor = keyExtractor(order.getProperty());
		Comparator<T> comparator = comparing(keyExtractor, nullSafeComparator());
		return order.isAscending() ? comparator : comparator.reversed();
	}

	private static <T, U> Function<T, U> keyExtractor(String propertiesPath) {

		return entity -> {
			Object innerValue = entity;
			for (String propertyName : parsePropertiesPath(propertiesPath)) {
				innerValue = getValue(innerValue, propertyName);

				if (innerValue == null) {
					return null;
				}
			}

			return (U) innerValue;
		};
	}

	private static List<String> parsePropertiesPath(String propertiesPath) {
		return Arrays.asList(StringUtils.delimitedListToStringArray(propertiesPath, "."));
	}

	private static <T extends Comparable<? super T>> Comparator<T> nullSafeComparator() {
		return Comparator.nullsFirst(Comparator.naturalOrder());
	}

	@Nullable
	private static Object getValue(Object entity, String propertyName) {

		return ofNullable(getPropertyDescriptor(entity.getClass(), propertyName))
				.map(descriptor -> getValue(descriptor, entity)).orElse(null);
	}

	private static <T> Object getValue(PropertyDescriptor descriptor, T entity) {

		try {
			return descriptor.getReadMethod().invoke(entity);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(
					String.format("Could not get value from specified property: %s", descriptor.getDisplayName()));
		}
	}
}
