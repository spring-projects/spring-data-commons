/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.mapping.callback;

/**
 * Entity callback before saving the entity. This interface allows for modifications to the actual entity that are
 * required prior to saving the entity.
 *
 * @author Mark Paluch
 * @see EntityCallback
 * @see SimpleEntityCallbacks
 */
@FunctionalInterface
public interface BeforeSaveCallback<T> extends EntityCallback<T> {

	/**
	 * Handle the entity callback. Modifications that result in creating a new instance should return the changed entity
	 * as method return value.
	 *
	 * @param object the entity for this callback.
	 * @return the resulting entity.
	 */
	T onBeforeSave(T object);
}
