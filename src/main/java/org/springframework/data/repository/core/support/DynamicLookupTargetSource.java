/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.repository.core.support;

import java.util.function.Supplier;

import org.springframework.aop.TargetSource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link TargetSource}, that will re-obtain an instance using the configured supplier.
 *
 * @author Oliver Drotbohm
 * @since 3.4.0
 */
class DynamicLookupTargetSource<T> implements TargetSource {

	private final Class<T> type;
	private final Supplier<? extends T> supplier;

	/**
	 * Creates a new {@link DynamicLookupTargetSource} for the given type and {@link Supplier}.
	 *
	 * @param type must not be {@literal null}.
	 * @param supplier must not be {@literal null}.
	 */
	public DynamicLookupTargetSource(Class<T> type, Supplier<? extends T> supplier) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(supplier, "Supplier must not be null!");

		this.type = type;
		this.supplier = supplier;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.aop.TargetSource#isStatic()
	 */
	@Override
	public boolean isStatic() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.aop.TargetSource#getTarget()
	 */
	@Override
	@Nullable
	public Object getTarget() throws Exception {
		return supplier.get();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.aop.TargetSource#getTargetClass()
	 */
	@Override
	@NonNull
	public Class<?> getTargetClass() {
		return type;
	}
}
