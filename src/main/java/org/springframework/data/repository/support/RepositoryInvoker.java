/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.repository.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.MultiValueMap;

/**
 * API to invoke (CRUD) methods on Spring Data repository instances independently of the base interface they expose.
 * Clients should check the availability of the methods before invoking them by using the methods of
 * {@link RepositoryInvocationInformation}.
 * 
 * @author Oliver Gierke
 * @since 1.10
 */
public interface RepositoryInvoker extends RepositoryInvocationInformation {

	/**
	 * Invokes the method equivalent to {@link org.springframework.data.repository.CrudRepository#save(Object)} on the
	 * repository.
	 * 
	 * @param object
	 * @return the result of the invocation of the save method
	 * @throws IllegalStateException if the repository does not expose a save method.
	 */
	<T> T invokeSave(T object);

	/**
	 * Invokes the method equivalent to {@link org.springframework.data.repository.CrudRepository#findOne(Serializable)}.
	 * 
	 * @param id must not be {@literal null}.
	 * @return the entity with the given id.
	 * @throws IllegalStateException if the repository does not expose a find-one-method.
	 */
	<T> T invokeFindOne(Serializable id);

	/**
	 * Invokes the find-all method of the underlying repository using the method taking a {@link Pageable} as parameter if
	 * available (i.e. the equivalent to
	 * {@link org.springframework.data.repository.PagingAndSortingRepository#findAll(Pageable)}), using the method taking
	 * a {@link Sort} if available (i.e. the equivalent to
	 * {@link org.springframework.data.repository.PagingAndSortingRepository#findAll(Sort)} by extracting the {@link Sort}
	 * contained in the given {@link Pageable}) or the plain equivalent to
	 * {@link org.springframework.data.repository.CrudRepository#findAll()}.
	 * 
	 * @param pageable can be {@literal null}.
	 * @return the result of the invocation of the find-all method.
	 * @throws IllegalStateException if the repository does not expose a find-all-method.
	 */
	Iterable<Object> invokeFindAll(Pageable pageable);

	/**
	 * Invokes the find-all method of the underlying repository using the method taking a {@link Sort} as parameter if
	 * available (i.e. the equivalent to
	 * {@link org.springframework.data.repository.PagingAndSortingRepository#findAll(Sort)}) or the plain equivalent to
	 * {@link org.springframework.data.repository.CrudRepository#findAll()}.
	 * 
	 * @param pageable can be {@literal null}.
	 * @return the result of the invocation of the find-all method.
	 * @throws IllegalStateException if the repository does not expose a find-all-method.
	 */
	Iterable<Object> invokeFindAll(Sort sort);

	/**
	 * Invokes the method equivalent to {@link org.springframework.data.repository.CrudRepository#delete(Serializable)}.
	 * The given id is assumed to be of a type convertable into the actual identifier type of the backing repository.
	 * 
	 * @param id must not be {@literal null}.
	 * @throws {@link IllegalStateException} if the repository does not expose a delete-method.
	 */
	void invokeDelete(Serializable id);

	/**
	 * Invokes the query method backed by the given {@link Method} using the given parameters, {@link Pageable} and
	 * {@link Sort}.
	 * 
	 * @param method must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 * @param pageable must not be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @return the result of the invoked query method.
	 * @since 1.11
	 */
	Object invokeQueryMethod(Method method, MultiValueMap<String, ? extends Object> parameters, Pageable pageable,
			Sort sort);
}
