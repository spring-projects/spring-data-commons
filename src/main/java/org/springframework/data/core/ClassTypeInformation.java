/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.core;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * {@link TypeInformation} for a plain {@link Class}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Kamil Krzywański
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class ClassTypeInformation<S> extends TypeDiscoverer<S> {

	private static final ConcurrentLruCache<ResolvableTypeHolder, ClassTypeInformation<?>> cache = new ConcurrentLruCache<>(128,
			ClassTypeInformation::new);

	private static final ConcurrentLruCache<Class<?>, ResolvableType> resolvableTypeCache = new ConcurrentLruCache<>(128,
			ResolvableType::forClass);

	private final Class<S> type;

	ClassTypeInformation(ResolvableTypeHolder resolvableTypeHolder) {
		this(resolvableTypeHolder.type, resolvableTypeHolder.field);
	}

	ClassTypeInformation(Class<?> type) {
		this(ResolvableType.forType(type), null);
	}

	ClassTypeInformation(ResolvableType type, @Nullable Field field) {
		super(type, field);
		this.type = (Class<S>) type.resolve(Object.class);
	}

	/**
	 * @param <S>
	 * @param type
	 * @return
	 */
	public static <S> ClassTypeInformation<S> from(Class<S> type) {

		if (type == Object.class) {
			return (ClassTypeInformation<S>) TypeInformation.OBJECT;
		} else if (type == List.class) {
			return (ClassTypeInformation<S>) TypeInformation.LIST;
		} else if (type == Set.class) {
			return (ClassTypeInformation<S>) TypeInformation.SET;
		} else if (type == Map.class) {
			return (ClassTypeInformation<S>) TypeInformation.MAP;
		}

		return from(resolvableTypeCache.get(type));
	}

	static <S> ClassTypeInformation<S> from(ResolvableType type, @Nullable Field field) {

		Assert.notNull(type, "Type must not be null");

		if (type.getType() == Object.class) {
			return (ClassTypeInformation<S>) TypeInformation.OBJECT;
		} else if (type.getType() == List.class) {
			return (ClassTypeInformation<S>) TypeInformation.LIST;
		} else if (type.getType() == Set.class) {
			return (ClassTypeInformation<S>) TypeInformation.SET;
		} else if (type.getType() == Map.class) {
			return (ClassTypeInformation<S>) TypeInformation.MAP;
		}

		return (ClassTypeInformation<S>) cache.get(new ResolvableTypeHolder(type, field));
	}

	static <S> ClassTypeInformation<S> from(ResolvableType type) {
		return from(type, null);
	}

	@Override
	public Class<S> getType() {
		return type;
	}

	@Override
	public ClassTypeInformation<?> getRawTypeInformation() {
		return this;
	}

	@Override
	public boolean isAssignableFrom(TypeInformation<?> target) {
		return getType().isAssignableFrom(target.getType());
	}

	@Override
	public TypeInformation<? extends S> specialize(TypeInformation<?> type) {
		return (TypeInformation<? extends S>) type;
	}

	@Override
	public String toString() {
		return type.getName();
	}

	private record ResolvableTypeHolder(ResolvableType type, @Nullable Field field){}

}
