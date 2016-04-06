/*
 * Copyright 2011-2016 the original author or authors.
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

import static org.springframework.core.GenericTypeResolver.*;
import static org.springframework.data.repository.util.ClassUtils.*;
import static org.springframework.util.ReflectionUtils.*;

import java.io.Serializable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link RepositoryInformation}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class DefaultRepositoryInformation implements RepositoryInformation {

	@SuppressWarnings("rawtypes") private static final TypeVariable<Class<Repository>>[] PARAMETERS = Repository.class
			.getTypeParameters();
	private static final String DOMAIN_TYPE_NAME = PARAMETERS[0].getName();
	private static final String ID_TYPE_NAME = PARAMETERS[1].getName();

	private final Map<Method, Method> methodCache = new ConcurrentHashMap<Method, Method>();

	private final RepositoryMetadata metadata;
	private final Class<?> repositoryBaseClass;
	private final Class<?> customImplementationClass;

	/**
	 * Creates a new {@link DefaultRepositoryMetadata} for the given repository interface and repository base class.
	 * 
	 * @param metadata must not be {@literal null}.
	 * @param repositoryBaseClass must not be {@literal null}.
	 * @param customImplementationClass
	 */
	public DefaultRepositoryInformation(RepositoryMetadata metadata, Class<?> repositoryBaseClass,
			Class<?> customImplementationClass) {

		Assert.notNull(metadata);
		Assert.notNull(repositoryBaseClass);

		this.metadata = metadata;
		this.repositoryBaseClass = repositoryBaseClass;
		this.customImplementationClass = customImplementationClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getDomainClass()
	 */
	@Override
	public Class<?> getDomainType() {
		return metadata.getDomainType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getIdClass()
	 */
	@Override
	public Class<? extends Serializable> getIdType() {
		return metadata.getIdType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInformation#getRepositoryBaseClass()
	 */
	@Override
	public Class<?> getRepositoryBaseClass() {
		return this.repositoryBaseClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInformation#getTargetClassMethod(java.lang.reflect.Method)
	 */
	@Override
	public Method getTargetClassMethod(Method method) {

		if (methodCache.containsKey(method)) {
			return methodCache.get(method);
		}

		Method result = getTargetClassMethod(method, customImplementationClass);

		if (!result.equals(method)) {
			return cacheAndReturn(method, result);
		}

		return cacheAndReturn(method, getTargetClassMethod(method, repositoryBaseClass));
	}

	private Method cacheAndReturn(Method key, Method value) {

		if (value != null) {
			makeAccessible(value);
		}

		methodCache.put(key, value);
		return value;
	}

	/**
	 * Returns whether the given method is considered to be a repository base class method.
	 * 
	 * @param method
	 * @return
	 */
	private boolean isTargetClassMethod(Method method, Class<?> targetType) {

		Assert.notNull(method);

		if (targetType == null) {
			return false;
		}

		if (method.getDeclaringClass().isAssignableFrom(targetType)) {
			return true;
		}

		return !method.equals(getTargetClassMethod(method, targetType));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInformation#getQueryMethods()
	 */
	@Override
	public Set<Method> getQueryMethods() {

		Set<Method> result = new HashSet<Method>();

		for (Method method : getRepositoryInterface().getMethods()) {
			method = ClassUtils.getMostSpecificMethod(method, getRepositoryInterface());
			if (isQueryMethodCandidate(method)) {
				result.add(method);
			}
		}

		return Collections.unmodifiableSet(result);
	}

	/**
	 * Checks whether the given method is a query method candidate.
	 * 
	 * @param method
	 * @return
	 */
	private boolean isQueryMethodCandidate(Method method) {
		return !method.isBridge() && !ReflectionUtils.isDefaultMethod(method) //
				&& (isQueryAnnotationPresentOn(method) || !isCustomMethod(method) && !isBaseClassMethod(method));
	}

	/**
	 * Checks whether the given method contains a custom store specific query annotation annotated with
	 * {@link QueryAnnotation}. The method-hierarchy is also considered in the search for the annotation.
	 * 
	 * @param method
	 * @return
	 */
	private boolean isQueryAnnotationPresentOn(Method method) {

		return AnnotationUtils.findAnnotation(method, QueryAnnotation.class) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInformation#isCustomMethod(java.lang.reflect.Method)
	 */
	@Override
	public boolean isCustomMethod(Method method) {
		return isTargetClassMethod(method, customImplementationClass);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryInformation#isQueryMethod(java.lang.reflect.Method)
	 */
	@Override
	public boolean isQueryMethod(Method method) {
		return getQueryMethods().contains(method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryInformation#isBaseClassMethod(java.lang.reflect.Method)
	 */
	@Override
	public boolean isBaseClassMethod(Method method) {

		Assert.notNull(method, "Method must not be null!");
		return isTargetClassMethod(method, repositoryBaseClass);
	}

	/**
	 * Returns the given target class' method if the given method (declared in the repository interface) was also declared
	 * at the target class. Returns the given method if the given base class does not declare the method given. Takes
	 * generics into account.
	 * 
	 * @param method must not be {@literal null}
	 * @param baseClass
	 * @return
	 */
	Method getTargetClassMethod(Method method, Class<?> baseClass) {

		if (baseClass == null) {
			return method;
		}

		for (Method baseClassMethod : baseClass.getMethods()) {

			// Wrong name
			if (!method.getName().equals(baseClassMethod.getName())) {
				continue;
			}

			// Wrong number of arguments
			if (!(method.getParameterTypes().length == baseClassMethod.getParameterTypes().length)) {
				continue;
			}

			// Check whether all parameters match
			if (!parametersMatch(method, baseClassMethod)) {
				continue;
			}

			return baseClassMethod;
		}

		return method;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInformation#hasCustomMethod()
	 */
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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getRepositoryInterface()
	 */
	@Override
	public Class<?> getRepositoryInterface() {
		return metadata.getRepositoryInterface();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getReturnedDomainClass(java.lang.reflect.Method)
	 */
	@Override
	public Class<?> getReturnedDomainClass(Method method) {
		return metadata.getReturnedDomainClass(method);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getCrudMethods()
	 */
	@Override
	public CrudMethods getCrudMethods() {
		return metadata.getCrudMethods();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#isPagingRepository()
	 */
	@Override
	public boolean isPagingRepository() {
		return metadata.isPagingRepository();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getAlternativeDomainTypes()
	 */
	@Override
	public Set<Class<?>> getAlternativeDomainTypes() {
		return metadata.getAlternativeDomainTypes();
	}

	/**
	 * Checks the given method's parameters to match the ones of the given base class method. Matches generic arguments
	 * against the ones bound in the given repository interface.
	 * 
	 * @param method
	 * @param baseClassMethod
	 * @return
	 */
	private boolean parametersMatch(Method method, Method baseClassMethod) {

		Class<?>[] methodParameterTypes = method.getParameterTypes();
		Type[] genericTypes = baseClassMethod.getGenericParameterTypes();
		Class<?>[] types = baseClassMethod.getParameterTypes();

		for (int i = 0; i < genericTypes.length; i++) {

			Type genericType = genericTypes[i];
			Class<?> type = types[i];
			MethodParameter parameter = new MethodParameter(method, i);
			Class<?> parameterType = resolveParameterType(parameter, metadata.getRepositoryInterface());

			if (genericType instanceof TypeVariable<?>) {

				if (!matchesGenericType((TypeVariable<?>) genericType, ResolvableType.forMethodParameter(parameter))) {
					return false;
				}

				continue;
			}

			if (!type.isAssignableFrom(parameterType) || !type.equals(methodParameterTypes[i])) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks whether the given parameter type matches the generic type of the given parameter. Thus when {@literal PK} is
	 * declared, the method ensures that given method parameter is the primary key type declared in the given repository
	 * interface e.g.
	 * 
	 * @param variable must not be {@literal null}.
	 * @param parameterType must not be {@literal null}.
	 * @return
	 */
	protected boolean matchesGenericType(TypeVariable<?> variable, ResolvableType parameterType) {

		GenericDeclaration declaration = variable.getGenericDeclaration();

		if (declaration instanceof Class) {

			ResolvableType entityType = ResolvableType.forClass(getDomainType());
			ResolvableType idClass = ResolvableType.forClass(getIdType());

			if (ID_TYPE_NAME.equals(variable.getName()) && parameterType.isAssignableFrom(idClass)) {
				return true;
			}

			Type boundType = variable.getBounds()[0];
			String referenceName = boundType instanceof TypeVariable ? boundType.toString() : variable.toString();

			return DOMAIN_TYPE_NAME.equals(referenceName) && parameterType.isAssignableFrom(entityType);
		}

		for (Type type : variable.getBounds()) {
			if (ResolvableType.forType(type).isAssignableFrom(parameterType)) {
				return true;
			}
		}

		return false;
	}
}
