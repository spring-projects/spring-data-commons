/*
 * Copyright 2011 the original author or authors.
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

import static org.springframework.data.repository.util.ClassUtils.*;
import static org.springframework.core.GenericTypeResolver.*;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link RepositoryInformation}.
 *
 * @author Oliver Gierke
 */
class DefaultRepositoryInformation implements RepositoryInformation {

	@SuppressWarnings("rawtypes")
	private static final TypeVariable<Class<Repository>>[] PARAMETERS = Repository.class.getTypeParameters();
	private static final String DOMAIN_TYPE_NAME = PARAMETERS[0].getName();
	private static final String ID_TYPE_NAME = PARAMETERS[1].getName();

	private final Map<Method, Method> methodCache = new ConcurrentHashMap<Method, Method>();

	private final RepositoryMetadata metadata;
	private final Class<?> repositoryBaseClass;
	private final Class<?> customImplementationClass;

	/**
	 * Creates a new {@link DefaultRepositoryMetadata} for the given repository interface and repository base class.
	 *
	 * @param metadata
	 * @param repositoryBaseClass
	 * @param customImplementationClass
	 */
	public DefaultRepositoryInformation(RepositoryMetadata metadata, Class<?> repositoryBaseClass, Class<?> customImplementationClass) {

		Assert.notNull(metadata);
		Assert.notNull(repositoryBaseClass);
		this.metadata = metadata;
		this.repositoryBaseClass = repositoryBaseClass;
		this.customImplementationClass = customImplementationClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getRepositoryInterface()
	 */
	public Class<?> getRepositoryInterface() {
		return metadata.getRepositoryInterface();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getDomainClass()
	 */
	public Class<?> getDomainClass() {
		return metadata.getDomainClass();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getIdClass()
	 */
	public Class<?> getIdClass() {
		return metadata.getIdClass();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInformation#getRepositoryBaseClass()
	 */
	public Class<?> getRepositoryBaseClass() {

		return this.repositoryBaseClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInformation#getTargetClassMethod(java.lang.reflect.Method)
	 */
	public Method getTargetClassMethod(Method method) {
		
		if (methodCache.containsKey(method)) {
			return methodCache.get(method);
		}
		
		Method result = getTargetClassMethod(method, customImplementationClass);
		
		if (!result.equals(method)) {
			methodCache.put(method, result);
			return result;
		}
		
		result = getTargetClassMethod(method, repositoryBaseClass);
		methodCache.put(method, result);
		return result;
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
	public Iterable<Method> getQueryMethods() {

		Set<Method> result = new HashSet<Method>();

		for (Method method : getRepositoryInterface().getMethods()) {
			if (!isCustomMethod(method) && !isBaseClassMethod(method)) {
				result.add(method);
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryInformation#isCustomMethod(java.lang.reflect.Method)
	 */
	public boolean isCustomMethod(Method method) {
		return isTargetClassMethod(method, customImplementationClass);
	}
	
	/**
	 * Returns whether the given method is a method covered by the base implementation.
	 *  
	 * @param method
	 * @return
	 */
	public boolean isBaseClassMethod(Method method) {
		return isTargetClassMethod(method, repositoryBaseClass);
	}

	/**
	 * Returns the given target class' method if the given method (declared in the repository interface) was also declared
	 * at the target class. Returns the given method if the given base class does not declare the method given.
	 * Takes generics into account.
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
	 * Checks the given method's parameters to match the ones of the given base class method. Matches generic arguments
	 * agains the ones bound in the given repository interface.
	 *
	 * @param method
	 * @param baseClassMethod
	 * @return
	 */
	private boolean parametersMatch(Method method, Method baseClassMethod) {

		Type[] genericTypes = baseClassMethod.getGenericParameterTypes();
		Class<?>[] types = baseClassMethod.getParameterTypes();

		for (int i = 0; i < genericTypes.length; i++) {

			Type type = genericTypes[i];
			MethodParameter parameter = new MethodParameter(method, i);
			Class<?> parameterType = resolveParameterType(parameter, metadata.getRepositoryInterface());

			if (type instanceof TypeVariable<?>) {
				String name = ((TypeVariable<?>) type).getName();
				if (!matchesGenericType(name, parameterType)) {
					return false;
				}
			} else {
				if (!types[i].equals(parameterType)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Checks whether the given parameter type matches the generic type of the given parameter. Thus when {@literal PK} is
	 * declared, the method ensures that given method parameter is the primary key type declared in the given repository
	 * interface e.g.
	 *
	 * @param name
	 * @param parameterType
	 * @return
	 */
	private boolean matchesGenericType(String name, Class<?> parameterType) {

		Class<?> entityType = getDomainClass();
		Class<?> idClass = getIdClass();

		if (ID_TYPE_NAME.equals(name) && parameterType.equals(idClass)) {
			return true;
		}

		if (DOMAIN_TYPE_NAME.equals(name) && parameterType.equals(entityType)) {
			return true;
		}

		return false;
	}
}
