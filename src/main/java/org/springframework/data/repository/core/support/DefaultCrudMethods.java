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
package org.springframework.data.repository.core.support;

import java.lang.reflect.Method;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Default implementation to discover CRUD methods based on a given {@link RepositoryInformation}. Will detect methods
 * exposed in {@link CrudRepository} but also hand crafted CRUD methods that are signature compatible with the ones on
 * {@link CrudRepository}.
 * 
 * @author Oliver Gierke
 * @since 1.6
 */
class DefaultCrudMethods implements CrudMethods {

	private final RepositoryInformation information;

	private Method findAllMethod;
	private boolean findAllHasPaging;

	private Method findOneMethod;
	private Method saveMethod;
	private Method deleteMethod;

	/**
	 * Creates a new {@link DefaultCrudMethods} using the given {@link RepositoryInformation}.
	 * 
	 * @param information must not be {@literal null}.
	 */
	public DefaultCrudMethods(RepositoryInformation information) {

		Assert.notNull(information, "RepositoryInformation must not be null!");
		this.information = information;

		for (Method method : ReflectionUtils.getAllDeclaredMethods(information.getRepositoryInterface())) {

			if (!information.isBaseClassMethod(method)) {
				continue;
			}

			if (method.getName().equals("findAll")) {
				findAllDetected(method);
				continue;
			}

			if (method.getName().equals("findOne")) {
				this.findOneMethod = method;
			}

			if (method.getName().equals("save")) {
				this.saveMethod = method;
			}

			if (method.getName().equals("delete")) {
				deleteDetected(method);
			}
		}
	}

	/**
	 * Checks whether the given method is a more usable find all method. Will prefer methods taking a {@link Pageable} and
	 * sort over a simple one.
	 * 
	 * @param method
	 */
	private void findAllDetected(Method method) {

		if (findAllMethod != null && findAllHasPaging) {
			return;
		}

		Class<?>[] parameterType = method.getParameterTypes();

		if (parameterType.length > 0) {

			if (parameterType[0].equals(Pageable.class)) {
				this.findAllMethod = method;
				this.findAllHasPaging = true;
				return;
			}

			if (parameterType[0].equals(Sort.class)) {
				this.findAllMethod = method;
			}

			return;
		}

		if (findAllMethod == null) {
			this.findAllMethod = method;
		}
	}

	/**
	 * Checks whether the given method is a more usable delete method. Will prefer delete-by-id methods over
	 * delete-by-instance ones.
	 * 
	 * @param method
	 */
	private void deleteDetected(Method method) {

		MethodParameter parameter = new MethodParameter(method, 0);
		Class<?> parameterType = GenericTypeResolver.resolveParameterType(parameter, information.getRepositoryInterface());

		if (information.getIdType().isAssignableFrom(parameterType)) {
			this.deleteMethod = method;
		}

		if (this.deleteMethod == null) {
			this.deleteMethod = method;
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.CrudMethods#getSaveMethod()
	 */
	@Override
	public Method getSaveMethod() {
		return saveMethod;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.CrudMethods#hasSaveMethod()
	 */
	@Override
	public boolean hasSaveMethod() {
		return saveMethod != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.CrudMethods#getFindAllMethod()
	 */
	@Override
	public Method getFindAllMethod() {
		return findAllMethod;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.CrudMethods#hasFindAllMethod()
	 */
	@Override
	public boolean hasFindAllMethod() {
		return findAllMethod != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.CrudMethods#getFindOneMethod()
	 */
	@Override
	public Method getFindOneMethod() {
		return findOneMethod;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.CrudMethods#hasFindOneMethod()
	 */
	@Override
	public boolean hasFindOneMethod() {
		return findOneMethod != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.CrudMethods#hasDelete()
	 */
	@Override
	public boolean hasDelete() {
		return this.deleteMethod != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.CrudMethods#getDeleteMethod()
	 */
	@Override
	public Method getDeleteMethod() {
		return this.deleteMethod;
	}
}
