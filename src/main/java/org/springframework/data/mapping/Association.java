/*
 * Copyright 2011-2018 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Value object to capture {@link Association}s.
 *
 * @param <P> {@link PersistentProperty}s the association connects.
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Mark Paluch
 */
public class Association<P extends PersistentProperty<P>> {

	private final P inverse;
	private final @Nullable P obverse;

	/**
	 * Creates a new {@link Association} between the two given {@link PersistentProperty}s.
	 *
	 * @param inverse
	 * @param obverse
	 */
	public Association(P inverse, @Nullable P obverse) {
		this.inverse = inverse;
		this.obverse = obverse;
	}

	public P getInverse() {
		return inverse;
	}

	@Nullable
	public P getObverse() {
		return obverse;
	}
}
