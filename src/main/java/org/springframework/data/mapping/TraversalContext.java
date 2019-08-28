/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A context object for lookups of values for {@link PersistentPropertyPaths} via a {@link PersistentPropertyAccessor}.
 * It allows to register functions to post-process the objects returned for a particular property, so that the
 * subsequent traversal would rather use the processed object. This is especially helpful if you need to traverse paths
 * that contain {@link Collection} and {@link Map} elements that usually need indices and keys to reasonably traverse
 * nested properties. It also allows to register functions to pre-process objects to be set as value for a property.
 * Again this is helpful if you need to set properties with a path that contain {@link Collection} and {@link Map}
 * elements by allowing to set such a property to an element value which may be converted to an add operation on an
 * existing collection or creation of a new collection with an additional or replaced element.
 *
 * @author Oliver Drotbohm
 * @author Jens Schauder
 * @since 2.2
 * @soundtrack 8-Bit Misfits - Crash Into Me (Dave Matthews Band cover)
 */
public class TraversalContext {

	static private BiFunction NO_WRITE_HANDLER = (ov, nv) -> {
		throw new UnsupportedOperationException("No handler registered for writing");
	};

	private Map<PersistentProperty<?>, Function<Object, Object>> readHandlers = new HashMap<>();
	private Map<PersistentProperty<?>, BiFunction<Object, Object, Object>> writeHandlers = new HashMap<>();

	/**
	 * Registers a {@link Function} to post-process values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @param writeHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	public TraversalContext registerHandler(PersistentProperty<?> property, Function<Object, Object> readHandler,
			BiFunction<Object, Object, Object> writeHandler) {

		Assert.notNull(property, "Property must not be null!");
		Assert.notNull(readHandler, "Handler must not be null!");

		readHandlers.put(property, readHandler);
		writeHandlers.put(property, writeHandler);

		return this;
	}

	/**
	 * Registers a {@link Function} to handle {@link Collection} values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public TraversalContext registerCollectionHandler(PersistentProperty<?> property,
			Function<? super Collection, Object> readHandler) {
		return registerHandler(property, Collection.class, readHandler, NO_WRITE_HANDLER);
	}

	/**
	 * Registers a {@link Function} to handle {@link Collection} values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @param writeHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	public TraversalContext registerCollectionHandler(PersistentProperty<?> property,
			Function<? super Collection, Object> readHandler,
			BiFunction<? super Collection, Object, ? extends Collection> writeHandler) {
		return registerHandler(property, Collection.class, readHandler, writeHandler);
	}

	/**
	 * Registers a {@link Function} to handle {@link List} values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public TraversalContext registerListHandler(PersistentProperty<?> property,
			Function<? super List, Object> readHandler) {
		return registerHandler(property, List.class, readHandler, NO_WRITE_HANDLER);
	}

	/**
	 * Registers a {@link Function} to handle {@link List} values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @param writeHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	public TraversalContext registerListHandler(PersistentProperty<?> property,
			Function<? super List, Object> readHandler, BiFunction<? super List, Object, List> writeHandler) {
		return registerHandler(property, List.class, readHandler, writeHandler);
	}

	/**
	 * Registers a {@link Function} to handle {@link Set} values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public TraversalContext registerSetHandler(PersistentProperty<?> property,
			Function<? super Set, Object> readHandler) {
		return registerSetHandler(property, readHandler, NO_WRITE_HANDLER);
	}

	/**
	 * Registers a {@link Function} to handle {@link Set} values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @param writeHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	public TraversalContext registerSetHandler(PersistentProperty<?> property, Function<? super Set, Object> readHandler,
			BiFunction<? super Set, Object, Set> writeHandler) {
		return registerHandler(property, Set.class, readHandler, writeHandler);
	}

	/**
	 * Registers a {@link Function} to handle {@link Map} values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public TraversalContext registerMapHandler(PersistentProperty<?> property,
			Function<? super Map, Object> readHandler) {
		return registerMapHandler(property, readHandler, NO_WRITE_HANDLER);
	}

	/**
	 * Registers a {@link Function} to handle {@link Map} values for the given property.
	 *
	 * @param property must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @param writeHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public TraversalContext registerMapHandler(PersistentProperty<?> property, Function<? super Map, Object> readHandler,
			BiFunction<? super Map, Object, Map> writeHandler) {
		return registerHandler(property, Map.class, readHandler, writeHandler);
	}

	/**
	 * Registers the given {@link Function} to post-process values obtained for the given {@link PersistentProperty} for
	 * the given type.
	 *
	 * @param <T> the type of the value to handle.
	 * @param property must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	public <T> TraversalContext registerHandler(PersistentProperty<?> property, Class<T> type,
			Function<? super T, Object> readHandler) {
		return registerHandler(property, type, readHandler, NO_WRITE_HANDLER);
	}

	/**
	 * Registers the given {@link Function} to post-process values obtained for the given {@link PersistentProperty} for
	 * the given type.
	 *
	 * @param <T> the type of the value to handle.
	 * @param property must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param readHandler must not be {@literal null}.
	 * @param writeHandler must not be {@literal null}.
	 * @return this instance for use as a fluent interface.
	 */
	@SuppressWarnings("unchecked")
	public <T> TraversalContext registerHandler(PersistentProperty<?> property, Class<T> type,
			Function<? super T, Object> readHandler, BiFunction<? super T, Object, ? extends T> writeHandler) {

		Assert.isTrue(type.isAssignableFrom(property.getType()), () -> String
				.format("Cannot register a property handler for %s on a property of type %s!", type, property.getType()));

		Function<Object, T> caster = type::cast;

		BiFunction<Object, Object, Object> castWriteHandler = (ov, nv) -> writeHandler.apply((T) ov, nv);
		return registerHandler(property, caster.andThen(readHandler), castWriteHandler);
	}

	/**
	 * Post-processes the value obtained for the given {@link PersistentProperty} using the registered handler.
	 *
	 * @param property must not be {@literal null}.
	 * @param value can be {@literal null}.
	 * @return the post-processed value or the value itself if no handlers registered.
	 */
	@Nullable
	Object postProcess(PersistentProperty<?> property, @Nullable Object value) {

		Function<Object, Object> handler = readHandlers.get(property);

		return handler == null ? value : handler.apply(value);
	}

	public Object preProcess(PersistentProperty property, @Nullable Object originalValue, Object newValue) {

		BiFunction<Object, Object, Object> handler = writeHandlers.get(property);

		return handler == null ? newValue : handler.apply(originalValue, newValue);

	}
}
