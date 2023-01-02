/*
 * Copyright 2011-2023 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * {@link TypeInformation} for a plain {@link Class}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @deprecated since 3.0 to go package protected at some point. Refer to {@link TypeInformation} only.
 */
@Deprecated(since = "3.0", forRemoval = true)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ClassTypeInformation<S> extends TypeDiscoverer<S> {

	private static final ConcurrentLruCache<ResolvableType, ClassTypeInformation<?>> cache = new ConcurrentLruCache<>(64,
			ClassTypeInformation::new);

	public static final ClassTypeInformation<Collection> COLLECTION;
	public static final ClassTypeInformation<List> LIST;
	public static final ClassTypeInformation<Set> SET;
	public static final ClassTypeInformation<Map> MAP;
	public static final ClassTypeInformation<Object> OBJECT;

	static {

		OBJECT = (ClassTypeInformation<Object>) cache.get(ResolvableType.forClass(Object.class));
		COLLECTION = (ClassTypeInformation<Collection>) cache.get(ResolvableType.forClass(Collection.class));
		LIST = (ClassTypeInformation<List>) cache.get(ResolvableType.forClass(List.class));
		SET = (ClassTypeInformation<Set>) cache.get(ResolvableType.forClass(Set.class));
		MAP = (ClassTypeInformation<Map>) cache.get(ResolvableType.forClass(Map.class));
	}

	private final Class<S> type;

	ClassTypeInformation(ResolvableType type) {
		super(type);
		this.type = (Class<S>) type.resolve(Object.class);
	}

	/**
	 * @param <S>
	 * @param type
	 * @return
	 * @deprecated since 3.0. Use {@link TypeInformation#of} instead.
	 */
	@Deprecated
	public static <S> ClassTypeInformation<S> from(Class<S> type) {
		return from(ResolvableType.forClass(type));
	}

	static <S> ClassTypeInformation<S> from(ResolvableType type) {

		Assert.notNull(type, "Type must not be null");

		return (ClassTypeInformation<S>) cache.get(type);
	}

	/**
	 * Warning: Does not fully resolve generic arguments.
	 *
	 * @param method
	 * @return
	 * @deprecated since 3.0. Use {@link TypeInformation#fromReturnTypeOf(Method)} instead.
	 */
	@Deprecated
	public static <S> TypeInformation<S> fromReturnTypeOf(Method method) {
		return (TypeInformation<S>) TypeInformation.of(ResolvableType.forMethodReturnType(method));
	}

	/**
	 * @param method
	 * @param actualType can be {@literal null}.
	 * @return
	 */
	static TypeInformation<?> fromReturnTypeOf(Method method, @Nullable Class<?> actualType) {

		var type = actualType == null ? ResolvableType.forMethodReturnType(method)
				: ResolvableType.forMethodReturnType(method, actualType);

		return TypeInformation.of(type);
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
}
