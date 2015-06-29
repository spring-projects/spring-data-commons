/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.repository.augment;

import org.springframework.util.Assert;

/**
 * Context to be handed around for update executions.
 * 
 * @since 1.11
 * @author Oliver Gierke
 */
public class UpdateContext<T> {

	/**
	 * The mode of the update execution.
	 * 
	 * @since 1.11
	 * @author Oliver Gierke
	 */
	public static enum UpdateMode {

		/**
		 * To be used for methods saving an object.
		 */
		SAVE,

		/**
		 * To bes used for methods deleting an object.
		 */
		DELETE,

		/**
		 * To be used when executing modifying queries.
		 */
		MODIFIYING_QUERY;

		/**
		 * Returns whether the {@link UpdateMode} is one of the given ones.
		 * 
		 * @param modes must not be {@literal null}.
		 * @return
		 */
		public boolean in(UpdateMode... modes) {

			for (UpdateMode updateMode : modes) {
				if (updateMode == this) {
					return true;
				}
			}

			return false;
		}
	}

	private final T entity;
	private final UpdateMode updateMode;

	/**
	 * Creates a new {@link UpdateContext} for the given entity and {@link UpdateContext}.
	 * 
	 * @param entity must not be {@literal null}.
	 * @param updateMode must not be {@literal null}.
	 */
	public UpdateContext(T entity, UpdateMode updateMode) {

		Assert.notNull(entity, "Entity must not be null!");
		Assert.notNull(updateMode, "QueryMode must not be null!");

		this.entity = entity;
		this.updateMode = updateMode;
	}

	/**
	 * Returns the entity to be updated.
	 * 
	 * @return will never be {@literal null}.
	 */
	public T getEntity() {
		return entity;
	}

	/**
	 * Returns the execution mode.
	 * 
	 * @return will never be {@literal null}.
	 */
	public UpdateMode getMode() {
		return updateMode;
	}
}
