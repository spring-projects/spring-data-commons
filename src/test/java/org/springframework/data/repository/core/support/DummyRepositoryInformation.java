/*
 * Copyright 2012-2021 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;

public final class DummyRepositoryInformation implements RepositoryInformation {

	private final RepositoryMetadata metadata;

	public DummyRepositoryInformation(Class<?> repositoryInterface) {
		this(new DefaultRepositoryMetadata(repositoryInterface));
	}

	public DummyRepositoryInformation(RepositoryMetadata metadata) {
		this.metadata = metadata;
	}

	public Class<?> getIdType() {
		return metadata.getIdType();
	}

	public Class<?> getDomainType() {
		return metadata.getDomainType();
	}

	public Class<?> getRepositoryInterface() {
		return metadata.getRepositoryInterface();
	}

	@Override
	public TypeInformation<?> getReturnType(Method method) {
		return metadata.getReturnType(method);
	}

	public Class<?> getReturnedDomainClass(Method method) {
		return getDomainType();
	}

	public Class<?> getRepositoryBaseClass() {
		return getRepositoryInterface();
	}

	public boolean hasCustomMethod() {
		return false;
	}

	public boolean isCustomMethod(Method method) {
		return false;
	}

	public boolean isQueryMethod(Method method) {
		return false;
	}

	public Streamable<Method> getQueryMethods() {
		return Streamable.empty();
	}

	public Method getTargetClassMethod(Method method) {
		return method;
	}

	public boolean isBaseClassMethod(Method method) {
		return true;
	}

	public CrudMethods getCrudMethods() {
		return new DefaultCrudMethods(this);
	}

	@Override
	public boolean isPagingRepository() {
		return false;
	}

	@Override
	public Set<Class<?>> getAlternativeDomainTypes() {
		return metadata.getAlternativeDomainTypes();
	}

	@Override
	public boolean isReactiveRepository() {
		return metadata.isReactiveRepository();
	}
}
