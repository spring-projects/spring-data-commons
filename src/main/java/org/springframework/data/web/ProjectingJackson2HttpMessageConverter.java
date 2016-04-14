/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * Htt
 * 
 * @author Oliver Gierke
 */
public class ProjectingJackson2HttpMessageConverter extends MappingJackson2HttpMessageConverter
		implements BeanClassLoaderAware, BeanFactoryAware {

	private final SpelAwareProxyProjectionFactory projectionFactory;

	public ProjectingJackson2HttpMessageConverter(ObjectMapper mapper) {

		super(mapper);

		this.projectionFactory = new SpelAwareProxyProjectionFactory();
		this.projectionFactory
				.registerMethodInvokerFactory(new JsonProjectingMethodInterceptorFactory(new JacksonMappingProvider(mapper)));
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
	public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {

		Class<?> rawType = ResolvableType.forType(type).getRawClass();

		return canRead(mediaType) && rawType.isInterface()
				&& AnnotationUtils.findAnnotation(rawType, Payload.class) != null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter#read(java.lang.reflect.Type, java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@Override
	public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		return projectionFactory.createProjection(ResolvableType.forType(type).getRawClass(), inputMessage.getBody());
	}
}
