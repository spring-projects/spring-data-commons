/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.repository.util;

import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Traversable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.util.QueryExecutionConverters.WrapperType;
import org.springframework.lang.Nullable;

/**
 * Converter implementations to map from and to Vavr collections.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 2.0
 */
class VavrCollections {

	public enum ToJavaConverter implements Converter<Object, Object> {

		INSTANCE;

		public WrapperType getWrapperType() {
			return WrapperType.multiValue(io.vavr.collection.Traversable.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Nonnull
		@Override
		public Object convert(Object source) {

			if (source instanceof io.vavr.collection.Seq) {
				return ((io.vavr.collection.Seq<?>) source).asJava();
			}

			if (source instanceof io.vavr.collection.Map) {
				return ((io.vavr.collection.Map<?, ?>) source).toJavaMap();
			}

			if (source instanceof io.vavr.collection.Set) {
				return ((io.vavr.collection.Set<?>) source).toJavaSet();
			}

			throw new IllegalArgumentException("Unsupported Vavr collection " + source.getClass());
		}
	}

	public enum FromJavaConverter implements ConditionalGenericConverter {

		INSTANCE {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
			 */
			@Nonnull
			@Override
			public java.util.Set<ConvertiblePair> getConvertibleTypes() {
				return CONVERTIBLE_PAIRS;
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
			 */
			@Override
			public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

				// Prevent collections to be mapped to maps
				if (sourceType.isCollection() && io.vavr.collection.Map.class.isAssignableFrom(targetType.getType())) {
					return false;
				}

				// Prevent maps to be mapped to collections
				if (sourceType.isMap() && !(io.vavr.collection.Map.class.isAssignableFrom(targetType.getType())
						|| targetType.getType().equals(Traversable.class))) {
					return false;
				}

				return true;
			}

			/*
			* (non-Javadoc)
			* @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
			*/
			@Nullable
			@Override
			public Object convert(@Nullable Object source, TypeDescriptor sourceDescriptor, TypeDescriptor targetDescriptor) {

				Class<?> targetType = targetDescriptor.getType();

				if (io.vavr.collection.Seq.class.isAssignableFrom(targetType)) {
					return io.vavr.collection.List.ofAll((Iterable<?>) source);
				}

				if (io.vavr.collection.Set.class.isAssignableFrom(targetType)) {
					return LinkedHashSet.ofAll((Iterable<?>) source);
				}

				if (io.vavr.collection.Map.class.isAssignableFrom(targetType)) {
					return LinkedHashMap.ofAll((Map<?, ?>) source);
				}

				// No dedicated type asked for, probably Traversable.
				// Try to stay as close to the source value.

				if (source instanceof List) {
					return io.vavr.collection.List.ofAll((Iterable<?>) source);
				}

				if (source instanceof Set) {
					return LinkedHashSet.ofAll((Iterable<?>) source);
				}

				if (source instanceof Map) {
					return LinkedHashMap.ofAll((Map<?, ?>) source);
				}

				return source;
			}
		};

		private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS;

		static {

			Set<ConvertiblePair> pairs = new HashSet<>();
			pairs.add(new ConvertiblePair(Collection.class, io.vavr.collection.Traversable.class));
			pairs.add(new ConvertiblePair(Map.class, io.vavr.collection.Traversable.class));

			CONVERTIBLE_PAIRS = Collections.unmodifiableSet(pairs);
		}
	}
}
