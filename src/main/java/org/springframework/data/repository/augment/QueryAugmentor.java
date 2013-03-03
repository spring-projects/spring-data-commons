/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.data.repository.augment.QueryContext.QueryMode;
import org.springframework.data.repository.core.EntityMetadata;

/**
 * SPI to abstract components that want to augment queries executed by the repositories.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
public interface QueryAugmentor<Q extends QueryContext<?>, N extends QueryContext<?>, S extends UpdateContext<?>> {

	/**
	 * Determines whether the implementation is interested in augmentation at all. The implementations can expect to only
	 * get {@link #augmentQuery(QueryContext, MethodMetadata)} and {@link #augmentUpdate(UpdateContext, MethodMetadata)}
	 * after the client has called this method and the implementation returned {@literal true}.
	 * 
	 * @param method metadata about the repository method invoked, will never be {@literal null}.
	 * @param queryMode the query execution mode, will never be {@literal null}.
	 * @param entityMetadata metadata about the entity, will never be {@literal null}. Useful to create type specific
	 *          augmentors.
	 * @return
	 */
	boolean supports(MethodMetadata method, QueryMode queryMode, EntityMetadata<?> entityMetadata);

	N augmentNativeQuery(N query, MethodMetadata methodMetadata);

	/**
	 * Augments the query by either adding further constraints to it or entirely replacing it. Clients will use
	 * {@link QueryContext#getQuery()} proceeding with the query execution.
	 * 
	 * @param query the query context of the query about to be executed.
	 * @param methodMetadata metadata about the repository method being invoked.
	 * @return must not be {@literal null}.
	 */
	Q augmentQuery(Q query, MethodMetadata methodMetadata);

	/**
	 * Augments the update about to be executed. Implementations can prevent the original update from being executed by
	 * returning null.
	 * 
	 * @param update the update context of the update about to be executed
	 * @param methodMetadata metadata about the repository method being invoked.
	 * @return the update to continue to work with or {@literal null} in case the implementation already executed custom
	 *         update and wants to prevent the execution of the original update.
	 */
	S augmentUpdate(S update, MethodMetadata methodMetadata);
}
