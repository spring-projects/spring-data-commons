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
package org.springframework.data.mapping;

import org.springframework.data.mapping.AccessOptions.NullHandling;
import org.springframework.lang.Nullable;

/**
 * Extension of {@link PersistentPropertyAccessor} that is also able to obtain and set values for
 * {@link PersistentPropertyPath}s.
 *
 * @author Oliver Gierke
 * @since 2.2
 * @soundtrack Sophia Wahnschaffe - Leuchten (Live Session) https://www.youtube.com/watch?v=izZxWiPto5Q
 */
public interface PersistentPropertyPathAccessor<T> extends PersistentPropertyAccessor<T> {

	/**
	 * @author Oliver Gierke
	 */
	public enum NullValues {

	REJECT, EARLY_RETURN;

		public AccessOptions.NullHandling toNullHandling() {
			return REJECT == this ? NullHandling.REJECT : NullHandling.SKIP;
		}
	}

	/**
	 * Return the value pointed to by the given {@link PersistentPropertyPath}. If the given path is empty, the wrapped
	 * bean is returned.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	@Nullable
	@SuppressWarnings("deprecation")
	default Object getProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path) {
		return PersistentPropertyAccessor.super.getProperty(path);
	}

	/**
	 * Return the value pointed to by the given {@link PersistentPropertyPath} considering the given lookup options.
	 *
	 * @param path
	 * @param options
	 * @return
	 * @since 2.2
	 */
	@Nullable
	Object getProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path, NullValues values);

	/**
	 * Sets the given value for the {@link PersistentProperty} pointed to by the given {@link PersistentPropertyPath}. The
	 * lookup of intermediate values must not yield {@literal null}.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @param value can be {@literal null}.
	 * @see AccessOptions#DEFAULT
	 */
	void setProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path, @Nullable Object value);

	/**
	 * Sets the given value for the {@link PersistentProperty} pointed to by the given {@link PersistentPropertyPath}
	 * considering the given {@link AccessOptions}.
	 *
	 * @param path must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @param options must not be {@literal null}.
	 */
	void setProperty(PersistentPropertyPath<? extends PersistentProperty<?>> path, @Nullable Object value,
			AccessOptions options);
}
