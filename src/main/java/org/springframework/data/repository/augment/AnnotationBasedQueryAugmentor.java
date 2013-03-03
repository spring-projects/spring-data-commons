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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.augment.QueryContext.QueryMode;
import org.springframework.data.repository.core.EntityMetadata;

/**
 * Base implementation of {@link QueryAugmentor} to lookup an annotation on the repository method invoked or at the
 * repository interface. It caches the lookups to avoid repeated reflection calls and hands the annotation found into
 * {@link #prepareQuery(QueryContext, Annotation)} and {@link #prepareUpdate(UpdateContext, Annotation)} methods. Opts
 * out of augmentation in case the annotation cannot be found on the method invoked or in the type.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
public abstract class AnnotationBasedQueryAugmentor<T extends Annotation, Q extends QueryContext<?>, N extends QueryContext<?>, U extends UpdateContext<?>>
		implements QueryAugmentor<Q, N, U> {

	private final Map<Method, T> cache = new HashMap<Method, T>();
	private final Class<T> annotationType;

	/**
	 * Creates a new {@link AnnotationBasedQueryAugmentor}.
	 */
	@SuppressWarnings("unchecked")
	public AnnotationBasedQueryAugmentor() {
		this.annotationType = (Class<T>) GenericTypeResolver.resolveTypeArguments(getClass(),
				AnnotationBasedQueryAugmentor.class)[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.QueryAugmentor#supports(org.springframework.data.repository.core.support.MethodMetadata, org.springframework.data.repository.core.support.QueryContext.Mode, org.springframework.data.repository.core.EntityMetadata)
	 */
	public boolean supports(MethodMetadata method, QueryMode queryMode, EntityMetadata<?> entityMetadata) {

		if (cache.containsKey(method)) {
			return cache.get(method) == null;
		}

		return findAndCacheAnnotation(method) != null;
	}

	/**
	 * Finds the annotation using the given {@link MethodMetadata} and caches it if found.
	 * 
	 * @param metadata must not be {@literal null}.
	 * @return
	 */
	private T findAndCacheAnnotation(MethodMetadata metadata) {

		Method method = metadata.getMethod();
		T expression = AnnotationUtils.findAnnotation(method, annotationType);

		if (expression != null) {
			cache.put(method, expression);
			return expression;
		}

		for (Class<?> type : metadata.getInvocationTargetType()) {

			expression = findAndCache(type, method);

			if (expression != null) {
				return expression;
			}
		}

		return findAndCache(method.getDeclaringClass(), method);
	}

	/**
	 * Tries to find the annotation on the given type and caches it if found.
	 * 
	 * @param type must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	private T findAndCache(Class<?> type, Method method) {

		T expression = AnnotationUtils.findAnnotation(type, annotationType);

		if (expression != null) {
			cache.put(method, expression);
			return expression;
		}

		return null;
	}

	public final N augmentNativeQuery(N context, MethodMetadata metadata) {
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.JpaQueryAugmentor#augmentQuery(javax.persistence.criteria.CriteriaQuery, org.springframework.data.repository.core.support.MethodMetadata)
	 */
	public final Q augmentQuery(Q context, MethodMetadata metadata) {

		Method method = metadata.getMethod();

		if (cache.containsKey(method)) {
			return prepareQuery(context, cache.get(method));
		}

		T expression = findAndCacheAnnotation(metadata);
		return expression == null ? context : prepareQuery(context, expression);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.QueryAugmentor#augmentUpdate(org.springframework.data.repository.core.support.UpdateContext, org.springframework.data.repository.core.support.MethodMetadata)
	 */
	public final U augmentUpdate(U update, MethodMetadata metadata) {

		Method method = metadata.getMethod();

		if (cache.containsKey(method)) {
			return prepareUpdate(update, cache.get(method));
		}

		T expression = findAndCacheAnnotation(metadata);
		return expression == null ? update : prepareUpdate(update, cache.get(method));
	}

	protected N prepareNativeQuery(N context, T expression) {
		return context;
	}

	/**
	 * Prepare the query contained in the given {@link QueryContext} using the given annotation. Default implementation
	 * returns the context as is.
	 * 
	 * @param context will never be {@literal null}.
	 * @param expression will never be {@literal null}.
	 * @return
	 */
	protected Q prepareQuery(Q context, T expression) {
		return context;
	}

	/**
	 * Prepare the update contained in the given {@link UpdateContext} using the given annotation. Default implementation
	 * returns the context as is.
	 * 
	 * @param context will never be {@literal null}.
	 * @param annotation will never be {@literal null}.
	 * @return
	 */
	protected U prepareUpdate(U context, T annotation) {
		return context;
	}
}
