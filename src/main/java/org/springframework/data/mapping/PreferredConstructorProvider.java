/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
// REVIEW: Would it be possible to have a specialized class that already contains directly the required information
// parameters in the constructor; are all attributes initialized by the constructor ..
// The parameter information might go directly into the EntityInstantiator
public interface PreferredConstructorProvider<T> {

	@Nullable
	<P extends PersistentProperty<P>> PreferredConstructor<T, P> getPreferredConstructor();

	// REVIEW: Why do we need a fallback/default? Shouldn't we gather all information during code generation?
	default <P extends PersistentProperty<P>> PreferredConstructor<T, P> getPreferredConstructorOrDefault(PreferredConstructor<T, P> fallback) {

		PreferredConstructor<T, P> preferredConstructor = getPreferredConstructor();
		return preferredConstructor != null ? preferredConstructor : fallback;
	}
}
