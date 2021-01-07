/*
 * Copyright 2020-2021 the original author or authors.
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
import java.util.function.Function;

import org.springframework.data.mapping.AccessOptions.SetOptions.SetNulls;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Access options when using {@link PersistentPropertyPathAccessor} to get and set properties. Allows defining how to
 * handle {@literal null} values, register custom transforming handlers when accessing collections and maps and
 * propagation settings for how to handle intermediate collection and map values when setting values.
 *
 * @author Oliver Drotbohm
 * @since 2.3
 */
public class AccessOptions {

	/**
	 * Returns the default {@link SetOptions} rejecting setting values when finding an intermediate property value to be
	 * {@literal null}.
	 *
	 * @return
	 */
	public static SetOptions defaultSetOptions() {
		return SetOptions.DEFAULT;
	}

	/**
	 * Returns the default {@link GetOptions} rejecting intermediate {@literal null} values when accessing property paths.
	 *
	 * @return
	 */
	public static GetOptions defaultGetOptions() {
		return GetOptions.DEFAULT;
	}

	/**
	 * Access options for getting values for property paths.
	 *
	 * @author Oliver Drotbohm
	 */
	public static class GetOptions {

		private static final GetOptions DEFAULT = new GetOptions(new HashMap<>(), GetNulls.REJECT);

		private final Map<PersistentProperty<?>, Function<Object, Object>> handlers;
		private final GetNulls nullValues;

		public GetOptions(Map<PersistentProperty<?>, Function<Object, Object>> handlers, GetNulls nullValues) {

			this.handlers = handlers;
			this.nullValues = nullValues;
		}

		public GetNulls getNullValues() {
			return this.nullValues;
		}

		public GetOptions withNullValues(GetNulls nullValues) {
			return this.nullValues == nullValues ? this : new GetOptions(this.handlers, nullValues);
		}

		/**
		 * How to handle null values during a {@link PersistentPropertyPath} traversal.
		 *
		 * @author Oliver Drotbohm
		 */
		public enum GetNulls {

			/**
			 * Reject the path lookup as a {@literal null} value cannot be traversed any further.
			 */
			REJECT,

			/**
			 * Returns {@literal null} as the entire path's traversal result.
			 */
			EARLY_RETURN;

			public SetOptions.SetNulls toNullHandling() {
				return REJECT == this ? SetNulls.REJECT : SetNulls.SKIP;
			}
		}

		/**
		 * Registers a {@link Function} to post-process values for the given property.
		 *
		 * @param property must not be {@literal null}.
		 * @param handler must not be {@literal null}.
		 * @return
		 */
		public GetOptions registerHandler(PersistentProperty<?> property, Function<Object, Object> handler) {

			Assert.notNull(property, "Property must not be null!");
			Assert.notNull(handler, "Handler must not be null!");

			Map<PersistentProperty<?>, Function<Object, Object>> newHandlers = new HashMap<>(handlers);
			newHandlers.put(property, handler);

			return new GetOptions(newHandlers, nullValues);
		}

		/**
		 * Registers a {@link Function} to handle {@link Collection} values for the given property.
		 *
		 * @param property must not be {@literal null}.
		 * @param handler must not be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public GetOptions registerCollectionHandler(PersistentProperty<?> property,
				Function<? super Collection<?>, Object> handler) {
			return registerHandler(property, Collection.class, (Function<Object, Object>) handler);
		}

		/**
		 * Registers a {@link Function} to handle {@link List} values for the given property.
		 *
		 * @param property must not be {@literal null}.
		 * @param handler must not be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public GetOptions registerListHandler(PersistentProperty<?> property, Function<? super List<?>, Object> handler) {
			return registerHandler(property, List.class, (Function<Object, Object>) handler);
		}

		/**
		 * Registers a {@link Function} to handle {@link Set} values for the given property.
		 *
		 * @param property must not be {@literal null}.
		 * @param handler must not be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public GetOptions registerSetHandler(PersistentProperty<?> property, Function<? super Set<?>, Object> handler) {
			return registerHandler(property, Set.class, (Function<Object, Object>) handler);
		}

		/**
		 * Registers a {@link Function} to handle {@link Map} values for the given property.
		 *
		 * @param property must not be {@literal null}.
		 * @param handler must not be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public GetOptions registerMapHandler(PersistentProperty<?> property, Function<? super Map<?, ?>, Object> handler) {
			return registerHandler(property, Map.class, (Function<Object, Object>) handler);
		}

		/**
		 * Registers the given {@link Function} to post-process values obtained for the given {@link PersistentProperty} for
		 * the given type.
		 *
		 * @param <T> the type of the value to handle.
		 * @param property must not be {@literal null}.
		 * @param type must not be {@literal null}.
		 * @param handler must not be {@literal null}.
		 * @return
		 */
		public <T> GetOptions registerHandler(PersistentProperty<?> property, Class<T> type,
				Function<? super T, Object> handler) {

			Assert.isTrue(type.isAssignableFrom(property.getType()), () -> String
					.format("Cannot register a property handler for %s on a property of type %s!", type, property.getType()));

			Function<Object, T> caster = it -> type.cast(it);

			return registerHandler(property, caster.andThen(handler));
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

			Function<Object, Object> handler = handlers.get(property);

			return handler == null ? value : handler.apply(value);
		}
	}

	/**
	 * Access options for setting values for property paths.
	 *
	 * @author Oliver Drotbohm
	 */
	public static class SetOptions {

		public SetOptions(SetNulls nullHandling, Propagation collectionPropagation, Propagation mapPropagation) {
			this.nullHandling = nullHandling;
			this.collectionPropagation = collectionPropagation;
			this.mapPropagation = mapPropagation;
		}

		public SetOptions withNullHandling(SetNulls nullHandling) {
			return this.nullHandling == nullHandling ? this
					: new SetOptions(nullHandling, this.collectionPropagation, this.mapPropagation);
		}

		public SetOptions withCollectionPropagation(Propagation collectionPropagation) {
			return this.collectionPropagation == collectionPropagation ? this
					: new SetOptions(this.nullHandling, collectionPropagation, this.mapPropagation);
		}

		public SetOptions withMapPropagation(Propagation mapPropagation) {
			return this.mapPropagation == mapPropagation ? this
					: new SetOptions(this.nullHandling, this.collectionPropagation, mapPropagation);
		}

		public SetNulls getNullHandling() {
			return this.nullHandling;
		}

		/**
		 * How to handle intermediate {@literal null} values when setting
		 *
		 * @author Oliver Drotbohm
		 */
		public enum SetNulls {

			/**
			 * Reject {@literal null} values detected when traversing a path to eventually set the leaf property. This will
			 * cause a {@link MappingException} being thrown in that case.
			 */
			REJECT,

			/**
			 * Skip setting the value but log an info message to leave a trace why the value wasn't actually set.
			 */
			SKIP_AND_LOG,

			/**
			 * Silently skip the attempt to set the value.
			 */
			SKIP;
		}

		/**
		 * How to propagate setting values that cross collection and map properties.
		 *
		 * @author Oliver Drotbohm
		 */
		public enum Propagation {

			/**
			 * Skip the setting of values when encountering a collection or map value within the path to traverse.
			 */
			SKIP,

			/**
			 * Propagate the setting of values when encountering a collection or map value and set it on all collection or map
			 * members.
			 */
			PROPAGATE;
		}

		private static final SetOptions DEFAULT = new SetOptions();

		private final SetNulls nullHandling;
		private final Propagation collectionPropagation;
		private final Propagation mapPropagation;

		private SetOptions() {

			this.nullHandling = SetNulls.REJECT;
			this.collectionPropagation = Propagation.PROPAGATE;
			this.mapPropagation = Propagation.PROPAGATE;
		}

		/**
		 * Returns a new {@link AccessOptions} that will cause paths that contain {@literal null} values to be skipped when
		 * setting a property.
		 *
		 * @return
		 */
		public SetOptions skipNulls() {
			return withNullHandling(SetNulls.SKIP);
		}

		/**
		 * Returns a new {@link AccessOptions} that will cause paths that contain {@literal null} values to be skipped when
		 * setting a property but a log message produced in TRACE level.
		 *
		 * @return
		 */
		public SetOptions skipAndLogNulls() {
			return withNullHandling(SetNulls.SKIP_AND_LOG);
		}

		/**
		 * Returns a new {@link AccessOptions} that will cause paths that contain {@literal null} values to be skipped when
		 * setting a property.
		 *
		 * @return
		 */
		public SetOptions rejectNulls() {
			return withNullHandling(SetNulls.REJECT);
		}

		/**
		 * Shortcut to configure the same {@link Propagation} for both collection and map property path segments.
		 *
		 * @param propagation must not be {@literal null}.
		 * @return
		 */
		public SetOptions withCollectionAndMapPropagation(Propagation propagation) {

			Assert.notNull(propagation, "Propagation must not be null!");

			return withCollectionPropagation(propagation) //
					.withMapPropagation(propagation);
		}

		/**
		 * Returns whether the given property is supposed to be propagated, i.e. if values for it are supposed to be set at
		 * all.
		 *
		 * @param property can be {@literal null}.
		 * @return
		 */
		public boolean propagate(@Nullable PersistentProperty<?> property) {

			if (property == null) {
				return true;
			}

			if (property.isCollectionLike() && collectionPropagation.equals(Propagation.SKIP)) {
				return false;
			}

			if (property.isMap() && mapPropagation.equals(Propagation.SKIP)) {
				return false;
			}

			return true;
		}
	}
}
