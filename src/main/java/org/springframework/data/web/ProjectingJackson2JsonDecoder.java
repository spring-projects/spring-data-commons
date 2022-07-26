/*
 * Copyright 2022-2022 the original author or authors.
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
package org.springframework.data.web;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link org.springframework.http.codec.HttpMessageDecoder} implementation to enable projected JSON binding to interfaces annotated with
 * {@link ProjectedPayload}.
 *
 * @author Matías Hermosilla
 * @since 3.0
 */
public class ProjectingJackson2JsonDecoder extends Jackson2JsonDecoder
		implements BeanClassLoaderAware, BeanFactoryAware {

	private final SpelAwareProxyProjectionFactory projectionFactory;
	private final Map<Class<?>, Boolean> supportedTypesCache = new ConcurrentReferenceHashMap<>();

	/**
	 * Creates a new {@link ProjectingJackson2JsonDecoder} using a default {@link ObjectMapper}.
	 */
	public ProjectingJackson2JsonDecoder() {
		this.projectionFactory = initProjectionFactory(getObjectMapper());
	}

	/**
	 * Creates a new {@link ProjectingJackson2JsonDecoder} for the given {@link ObjectMapper}.
	 *
	 * @param mapper must not be {@literal null}.
	 */
	public ProjectingJackson2JsonDecoder(ObjectMapper mapper) {

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

		Assert.notNull(mapper, "ObjectMapper must not be null");

		SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();
		projectionFactory.registerMethodInvokerFactory(
				new JsonProjectingMethodInterceptorFactory(new JacksonJsonProvider(mapper),
						new JacksonMappingProvider(mapper)));

		return projectionFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		projectionFactory.setBeanClassLoader(classLoader);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		projectionFactory.setBeanFactory(beanFactory);
	}

	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
		if (mapper == null) {
			return false;
		}
		JavaType javaType = mapper.constructType(elementType.getType());
		// Skip String: CharSequenceDecoder + "*/*" comes after
		if (CharSequence.class.isAssignableFrom(elementType.toClass()) || !supportsMimeType(mimeType)) {
			return false;
		}
		if (!logger.isDebugEnabled()) {
			return mapper.canDeserialize(javaType);
		} else {
			AtomicReference<Throwable> causeRef = new AtomicReference<>();
			if (mapper.canDeserialize(javaType, causeRef)) {
				Class<?> rawType = javaType.getRawClass();
				Boolean result = supportedTypesCache.get(rawType);

				if (result != null) {
					return result;
				}

				result = rawType.isInterface() && AnnotationUtils.findAnnotation(rawType, ProjectedPayload.class) != null;
				supportedTypesCache.put(rawType, result);

				return result;
			}
			logWarningIfNecessary(javaType, causeRef.get());
			return false;
		}
	}

}
