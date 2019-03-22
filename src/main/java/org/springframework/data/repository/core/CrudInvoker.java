/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.repository.core;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;

/**
 * Interface for components that can invoke simple CRUD operations on repositories. Useful to be able to abstract being
 * backed by a {@link CrudRepository} implementation or a raw repository declaration with signature compatible methods
 * for {@link CrudRepository#findOne(Serializable)} and {@link CrudRepository#save(Object)}.
 * 
 * @author Oliver Gierke
 * @since 1.6
 */
public interface CrudInvoker<T> {

	/**
	 * Invokes the method equivalent to {@link CrudRepository#save(Object)}.
	 * 
	 * @param object must not be {@literal null}.
	 */
	T invokeSave(T object);

	/**
	 * Invokes the method equivalent to {@link CrudRepository#findOne(Serializable)}.
	 * 
	 * @param id must not be {@literal null}.
	 * @return
	 */
	T invokeFindOne(Serializable id);
}
