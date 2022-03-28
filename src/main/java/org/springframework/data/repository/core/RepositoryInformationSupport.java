/*
 * Copyright 2022 the original author or authors.
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

import static org.springframework.data.repository.util.ClassUtils.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Common base class for {@link RepositoryInformation} that delays resolution of {@link RepositoryMetadata} and the
 * repository base to the latest possible time.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
public abstract class RepositoryInformationSupport implements RepositoryInformation {

	private final Supplier<RepositoryMetadata> metadata;
	private final Supplier<Class<?>> repositoryBaseClass;

	public RepositoryInformationSupport(Supplier<RepositoryMetadata> metadata, Supplier<Class<?>> repositoryBaseClass) {

		Assert.notNull(metadata, "Repository metadata must not be null!");
		Assert.notNull(repositoryBaseClass, "Repository base class must not be null!");

		this.metadata = Lazy.of(metadata);
		this.repositoryBaseClass = Lazy.of(repositoryBaseClass);
	}

	@Override
	public Streamable<Method> getQueryMethods() {

		Set<Method> result = new HashSet<>();

		for (Method method : getRepositoryInterface().getMethods()) {
			method = ClassUtils.getMostSpecificMethod(method, getRepositoryInterface());
			if (isQueryMethodCandidate(method)) {
				result.add(method);
			}
		}

		return Streamable.of(Collections.unmodifiableSet(result));
	}

	private RepositoryMetadata getMetadata() {
		return metadata.get();
	}

	@Override
	public Class<?> getIdType() {
		return getMetadata().getIdType();
	}

	@Override
	public Class<?> getDomainType() {
		return getMetadata().getDomainType();
	}

	@Override
	public Class<?> getRepositoryInterface() {
		return getMetadata().getRepositoryInterface();
	}

	@Override
	public TypeInformation<?> getReturnType(Method method) {
		return getMetadata().getReturnType(method);
	}

	@Override
	public Class<?> getReturnedDomainClass(Method method) {
		return getMetadata().getReturnedDomainClass(method);
	}

	@Override
	public CrudMethods getCrudMethods() {
		return getMetadata().getCrudMethods();
	}

	@Override
	public boolean isPagingRepository() {
		return getMetadata().isPagingRepository();
	}

	@Override
	public Set<Class<?>> getAlternativeDomainTypes() {
		return getMetadata().getAlternativeDomainTypes();
	}

	@Override
	public boolean isReactiveRepository() {
		return getMetadata().isReactiveRepository();
	}

	@Override
	public Class<?> getRepositoryBaseClass() {
		return repositoryBaseClass.get();
	}

	@Override
	public boolean isQueryMethod(Method method) {
		return getQueryMethods().stream().anyMatch(it -> it.equals(method));
	}

	@Override
	public TypeInformation<?> getDomainTypeInformation() {
		return getMetadata().getDomainTypeInformation();
	}

	@Override
	public TypeInformation<?> getIdTypeInformation() {
		return getMetadata().getIdTypeInformation();
	}

	@Override
	public boolean hasCustomMethod() {

		Class<?> repositoryInterface = getRepositoryInterface();

		// No detection required if no typing interface was configured
		if (isGenericRepositoryInterface(repositoryInterface)) {
			return false;
		}

		for (Method method : repositoryInterface.getMethods()) {
			if (isCustomMethod(method) && !isBaseClassMethod(method)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether the given method contains a custom store specific query annotation annotated with
	 * {@link QueryAnnotation}. The method-hierarchy is also considered in the search for the annotation.
	 *
	 * @param method
	 * @return
	 */
	protected boolean isQueryAnnotationPresentOn(Method method) {

		return AnnotationUtils.findAnnotation(method, QueryAnnotation.class) != null;
	}

	/**
	 * Checks whether the given method is a query method candidate.
	 *
	 * @param method
	 * @return
	 */
	protected boolean isQueryMethodCandidate(Method method) {
		return !method.isBridge() && !method.isDefault() //
				&& !Modifier.isStatic(method.getModifiers()) //
				&& (isQueryAnnotationPresentOn(method) || !isCustomMethod(method) && !isBaseClassMethod(method));
	}
}
