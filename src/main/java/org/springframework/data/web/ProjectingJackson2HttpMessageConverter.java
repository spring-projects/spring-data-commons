/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.data.web;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * {@link HttpMessageConverter} implementation to enable projected JSON binding to interfaces annotated with
 * {@link ProjectedPayload}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @soundtrack Richard Spaven - Ice Is Nice (Spaven's 5ive)
 * @since 1.13
 */
public class ProjectingJackson2HttpMessageConverter extends MappingJackson2HttpMessageConverter
		implements BeanClassLoaderAware, BeanFactoryAware {

	private final SpelAwareProxyProjectionFactory projectionFactory;
	private final Map<Class<?>, Boolean> supportedTypesCache = new ConcurrentReferenceHashMap<>();

	/**
	 * Creates a new {@link ProjectingJackson2HttpMessageConverter} using a default {@link ObjectMapper}.
	 */
	public ProjectingJackson2HttpMessageConverter() {
		this.projectionFactory = initProjectionFactory(getObjectMapper());
	}

	/**
	 * Creates a new {@link ProjectingJackson2HttpMessageConverter} for the given {@link ObjectMapper}.
	 *
	 * @param mapper must not be {@literal null}.
	 */
	public ProjectingJackson2HttpMessageConverter(ObjectMapper mapper) {

		super(mapper);

		this.projectionFactory = initProjectionFactory(mapper);
	}

	/**
	 * Creates a new {@link SpelAwareProxyProjectionFactory} with the {@link JsonProjectingMethodInterceptorFactory}
	 * registered for the given {@link ObjectMapper}.
	 *
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	private static SpelAwareProxyProjectionFactory initProjectionFactory(ObjectMapper mapper) {

		Assert.notNull(mapper, "ObjectMapper must not be null!");

		SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();
		projectionFactory
				.registerMethodInvokerFactory(new JsonProjectingMethodInterceptorFactory(new JacksonMappingProvider(mapper)));

		return projectionFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		projectionFactory.setBeanClassLoader(classLoader);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		projectionFactory.setBeanFactory(beanFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter#canRead(java.lang.reflect.Type, java.lang.Class, org.springframework.http.MediaType)
	 */
	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {

		if (!canRead(mediaType)) {
			return false;
		}

		ResolvableType owner = contextClass == null ? null : ResolvableType.forClass(contextClass);
		Class<?> rawType = ResolvableType.forType(type, owner).resolve(Object.class);
		Boolean result = supportedTypesCache.get(rawType);

		if (result != null) {
			return result;
		}

		result = rawType.isInterface() && AnnotationUtils.findAnnotation(rawType, ProjectedPayload.class) != null;
		supportedTypesCache.put(rawType, result);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter#canWrite(java.lang.Class, org.springframework.http.MediaType)
	 */
	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter#read(java.lang.reflect.Type, java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@Override
	public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		return projectionFactory.createProjection(ResolvableType.forType(type).resolve(Object.class),
				inputMessage.getBody());
	}
}
