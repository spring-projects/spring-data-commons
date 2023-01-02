/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * An SPI to register custom collection types. Implementations need to be registered via
 * {@code META-INF/spring.factories}.
 *
 * @author Oliver Drotbohm
 * @since 2.7
 */
public interface CustomCollectionRegistrar {

	/**
	 * Returns whether the registrar is available, meaning whether it can be used at runtime. Primary use is for
	 * implementations that need to perform a classpath check to prevent the actual methods loading classes that might not
	 * be available.
	 *
	 * @return whether the registrar is available
	 */
	default boolean isAvailable() {
		return true;
	}

	/**
	 * Returns all types that are supposed to be considered maps. Primary requirement is key and value generics expressed
	 * in the first and second generics parameter of the type. Also, the types should be transformable into their
	 * Java-native equivalent using {@link #toJavaNativeCollection()}.
	 *
	 * @return will never be {@literal null}.
	 * @see #toJavaNativeCollection()
	 */
	Collection<Class<?>> getMapTypes();

	/**
	 * Returns all types that are supposed to be considered collections. Primary requirement is that their component types
	 * are expressed as first generics parameter. Also, the types should be transformable into their Java-native
	 * equivalent using {@link #toJavaNativeCollection()}.
	 *
	 * @return will never be {@literal null}.
	 * @see #toJavaNativeCollection()
	 */
	Collection<Class<?>> getCollectionTypes();

	/**
	 * Return all types that are considered valid return types for methods using pagination. These are usually collections
	 * with a stable order, like {@link List} but no {@link Set}s, as pagination usually involves sorting.
	 *
	 * @return will never be {@literal null}.
	 */
	default Collection<Class<?>> getAllowedPaginationReturnTypes() {
		return Collections.emptyList();
	}

	/**
	 * Register all converters to convert instances of the types returned by {@link #getCollectionTypes()} and
	 * {@link #getMapTypes()} from an to their Java-native counterparts.
	 *
	 * @param registry will never be {@literal null}.
	 */
	void registerConvertersIn(ConverterRegistry registry);

	/**
	 * Returns a {@link Function} to convert instances of their Java-native counterpart.
	 *
	 * @return must not be {@literal null}.
	 */
	Function<Object, Object> toJavaNativeCollection();
}
