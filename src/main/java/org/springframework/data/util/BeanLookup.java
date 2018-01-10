/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.util;

import lombok.experimental.UtilityClass;

import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.util.Assert;

/**
 * Simple helper to allow lenient lookup of beans of a given type from a {@link ListableBeanFactory}. This is not user
 * facing API but a mere helper for Spring Data configuration code.
 * 
 * @author Oliver Gierke
 * @since 2.1
 * @soundtrack Dave Matthews Band - Bartender (DMB Live 25)
 */
@UtilityClass
public class BeanLookup {

	/**
	 * Returns a {@link Lazy} for the unique bean of the given type from the given {@link BeanFactory} (which needs to be
	 * a {@link ListableBeanFactory}). The lookup will produce a {@link NoUniqueBeanDefinitionException} in case multiple
	 * beans of the given type are available in the given {@link BeanFactory}.
	 * 
	 * @param type must not be {@literal null}.
	 * @param beanFactory the {@link BeanFactory} to lookup the bean from.
	 * @return a {@link Lazy} for the unique bean of the given type or the instance provided by the fallback in case no
	 *         bean of the given type can be found.
	 */
	public static <T> Lazy<T> lazyIfAvailable(Class<T> type, BeanFactory beanFactory) {

		Assert.notNull(type, "Type must not be null!");
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory);

		return Lazy.of(() -> lookupBean(type, (ListableBeanFactory) beanFactory));
	}

	/**
	 * Looks up the unique bean of the given type from the given {@link ListableBeanFactory}.
	 * 
	 * @param type must not be {@literal null}.
	 * @param beanFactory must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private static <T> T lookupBean(Class<T> type, ListableBeanFactory beanFactory) {

		Map<String, T> names = beanFactory.getBeansOfType(type, false, false);

		switch (names.size()) {

			case 0:
				return null;
			case 1:
				return names.values().iterator().next();
			default:
				throw new NoUniqueBeanDefinitionException(type, names.keySet());
		}
	}
}
