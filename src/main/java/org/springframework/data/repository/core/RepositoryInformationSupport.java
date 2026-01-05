/*
 * Copyright 2022-present the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Contract;
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
	private final Supplier<DefaultQueryMethods> queryMethods;

	public RepositoryInformationSupport(Supplier<RepositoryMetadata> metadata, Supplier<Class<?>> repositoryBaseClass) {

		Assert.notNull(metadata, "Repository metadata must not be null");
		Assert.notNull(repositoryBaseClass, "Repository base class must not be null");

		this.metadata = Lazy.of(metadata);
		this.repositoryBaseClass = Lazy.of(repositoryBaseClass);
		this.queryMethods = Lazy.of(this::calculateQueryMethods);
	}

	@Override
	public List<Method> getQueryMethods() {
		return queryMethods.get().methods;
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
	public TypeInformation<?> getReturnedDomainTypeInformation(Method method) {
		return getMetadata().getReturnedDomainTypeInformation(method);
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
		return queryMethods.get().isQueryMethod(method);
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
		return queryMethods.get().hasCustomMethod;
	}

	@Override
	public boolean hasQueryMethods() {
		return queryMethods.get().hasQueryMethod;
	}

	/**
	 * Checks whether the given method contains a custom store specific query annotation annotated with
	 * {@link QueryAnnotation}. The method-hierarchy is also considered in the search for the annotation.
	 *
	 * @param method
	 * @return
	 */
	protected boolean isQueryAnnotationPresentOn(Method method) {

		Annotation[] annotations = method.getAnnotations();

		if (annotations.length == 0) {
			return false;
		}

		return MergedAnnotations.from(annotations).isPresent(QueryAnnotation.class);
	}

	/**
	 * Checks whether the given method is a query method candidate.
	 *
	 * @param method
	 * @return
	 */
	protected boolean isQueryMethodCandidate(Method method) {

		if (method.isBridge() || method.isDefault() || Modifier.isStatic(method.getModifiers())) {
			return false;
		}

		if (isCustomMethod(method)) {
			return false;
		}

		if (isQueryAnnotationPresentOn(method)) {
			return true;
		}

		if (isBaseClassMethod(method)) {
			return false;
		}

		return true;
	}

	protected RepositoryMetadata getMetadata() {
		return metadata.get();
	}

	private DefaultQueryMethods calculateQueryMethods() {

		Class<?> repositoryInterface = getRepositoryInterface();
		Method[] methods = repositoryInterface.getMethods();
		List<Method> queryMethods = new ArrayList<>(methods.length);
		for (Method method : methods) {

			Method msm = ClassUtils.getMostSpecificMethod(method, repositoryInterface);
			if (isQueryMethodCandidate(method)) {
				queryMethods.add(msm);
			}
		}

		return new DefaultQueryMethods(queryMethods, calculateHasCustomMethod(repositoryInterface, methods));
	}

	private boolean calculateHasCustomMethod(Class<?> repositoryInterface, Method[] methods) {

		// No detection required if no typing interface was configured
		if (isGenericRepositoryInterface(repositoryInterface)) {
			return false;
		}

		for (Method method : methods) {
			if (isCustomMethod(method) && !isBaseClassMethod(method)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns where the given type is the {@link Repository} interface.
	 *
	 * @param ifc
	 * @return
	 */
	private static boolean isGenericRepositoryInterface(Class<?> ifc) {
		return Repository.class.equals(ifc);
	}

	/**
	 * Returns whether the given type name is a repository interface name.
	 *
	 * @param interfaceName
	 * @return
	 */
	@Contract("null -> false")
	public static boolean isGenericRepositoryInterface(@Nullable String interfaceName) {
		return Repository.class.getName().equals(interfaceName);
	}

	/**
	 * Information about query methods to allow canonical computation and reuse of that information.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class DefaultQueryMethods {

		private final List<Method> methods;
		private final Set<Method> methodSet;
		private final boolean hasCustomMethod, hasQueryMethod;

		DefaultQueryMethods(List<Method> methods, boolean hasCustomMethod) {

			this.methods = Collections.unmodifiableList(methods);
			this.methodSet = new HashSet<>(methods);
			this.hasCustomMethod = hasCustomMethod;
			this.hasQueryMethod = !methods.isEmpty();
		}

		public boolean isQueryMethod(Method method) {
			return methodSet.contains(method);
		}
	}
}
