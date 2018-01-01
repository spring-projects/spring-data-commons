/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.projection;

import org.springframework.aop.RawTargetAccess;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Extension of {@link org.springframework.aop.TargetClassAware} to be able to ignore the getter on JSON rendering.
 *
 * @author Oliver Gierke
 */
public interface TargetAware extends org.springframework.aop.TargetClassAware, RawTargetAccess {

	/**
	 * Returns the type of the proxy target.
	 *
	 * @return can be {@literal null}.
	 */
	@Nullable
	@JsonIgnore
	Class<?> getTargetClass();

	/**
	 * Returns the proxy target.
	 *
	 * @return will never be {@literal null}.
	 */
	@JsonIgnore
	Object getTarget();

	/**
	 * Re-declaration of Spring Framework 4.3's {@link DecoratingProxy#getDecoratedClass()} so that we can exclude it from
	 * Jackson serialization.
	 *
	 * @return
	 */
	@JsonIgnore
	Class<?> getDecoratedClass();
}
