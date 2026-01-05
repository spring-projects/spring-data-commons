/*
 * Copyright 2025-present the original author or authors.
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

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.Assert;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.AbstractJsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingException;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter} implementation to enable projected JSON binding to
 * interfaces annotated with {@link ProjectedPayload}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @soundtrack Richard Spaven - Ice Is Nice (Spaven's 5ive)
 * @since 4.0
 */
public class ProjectingJacksonHttpMessageConverter extends JacksonJsonHttpMessageConverter
		implements BeanClassLoaderAware, BeanFactoryAware {

	private final SpelAwareProxyProjectionFactory projectionFactory;
	private final Map<Class<?>, Boolean> supportedTypesCache = new ConcurrentHashMap<>();

	/**
	 * Creates a new {@link ProjectingJacksonHttpMessageConverter} using a default {@link ObjectMapper}.
	 */
	public ProjectingJacksonHttpMessageConverter() {
		this.projectionFactory = initProjectionFactory(getMapper());
	}

	/**
	 * Creates a new {@link ProjectingJacksonHttpMessageConverter} for the given {@link ObjectMapper}.
	 *
	 * @param mapper must not be {@literal null}.
	 */
	public ProjectingJacksonHttpMessageConverter(JsonMapper mapper) {

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
		projectionFactory.registerMethodInvokerFactory(new JsonProjectingMethodInterceptorFactory(
				new JacksonJsonProvider(mapper), new JacksonMappingProvider(mapper)));

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
	protected boolean supports(Class<?> clazz) {

		if (clazz.isInterface()) {

			Boolean result = supportedTypesCache.get(clazz);

			if (result != null) {
				return result;
			}

			result = AnnotationUtils.findAnnotation(clazz, ProjectedPayload.class) != null;
			supportedTypesCache.put(clazz, result);

			return result;
		}

		return false;
	}

	@Override
	public boolean canRead(ResolvableType type, @Nullable MediaType mediaType) {

		if (!super.canRead(type, mediaType)) {
			return false;
		}

		Class<?> clazz = type.resolve();
		if (clazz == null) {
			return false;
		}

		return supports(clazz);
	}

	@Override
	public boolean canWrite(ResolvableType type, Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	@Override
	public Object read(ResolvableType type, HttpInputMessage inputMessage, @Nullable Map<String, Object> hints)
			throws IOException, HttpMessageNotReadableException {
		return projectionFactory.createProjection(type.resolve(Object.class), inputMessage.getBody());
	}

	record JacksonMappingProvider(ObjectMapper objectMapper) implements MappingProvider {

		@Override
		public <T> @Nullable T map(@Nullable Object source, Class<T> targetType, Configuration configuration) {
			if (source == null) {
				return null;
			}
			try {
				return objectMapper.convertValue(source, targetType);
			} catch (Exception e) {
				throw new MappingException(e);
			}

		}

		@Override
		public <T> @Nullable T map(@Nullable Object source, final TypeRef<T> targetType, Configuration configuration) {
			if (source == null) {
				return null;
			}

			tools.jackson.databind.JavaType type = objectMapper.getTypeFactory().constructType(targetType.getType());

			try {
				return objectMapper.convertValue(source, type);
			} catch (Exception e) {
				throw new MappingException(e);
			}

		}
	}

	static class JacksonJsonProvider extends AbstractJsonProvider {

		private static final ObjectMapper defaultObjectMapper = new ObjectMapper();
		private static final ObjectReader defaultObjectReader = defaultObjectMapper.reader().forType(Object.class);

		protected ObjectMapper objectMapper;
		protected ObjectReader objectReader;

		public ObjectMapper getObjectMapper() {
			return objectMapper;
		}

		/**
		 * Initialize the JacksonProvider with the default ObjectMapper and ObjectReader
		 */
		public JacksonJsonProvider() {
			this(defaultObjectMapper, defaultObjectReader);
		}

		/**
		 * Initialize the JacksonProvider with a custom ObjectMapper.
		 *
		 * @param objectMapper the ObjectMapper to use
		 */
		public JacksonJsonProvider(ObjectMapper objectMapper) {
			this(objectMapper, objectMapper.readerFor(Object.class));
		}

		/**
		 * Initialize the JacksonProvider with a custom ObjectMapper and ObjectReader.
		 *
		 * @param objectMapper the ObjectMapper to use
		 * @param objectReader the ObjectReader to use
		 */
		public JacksonJsonProvider(ObjectMapper objectMapper, tools.jackson.databind.ObjectReader objectReader) {
			this.objectMapper = objectMapper;
			this.objectReader = objectReader;
		}

		@Override
		public Object parse(String json) throws InvalidJsonException {
			return objectReader.readValue(json);
		}

		@Override
		public Object parse(InputStream jsonStream, String charset) throws InvalidJsonException {
			try {
				return objectReader.readValue(new InputStreamReader(jsonStream, charset));
			} catch (IOException e) {
				throw new InvalidJsonException(e);
			}
		}

		@Override
		public String toJson(Object obj) {
			StringWriter writer = new StringWriter();
			try {
				JsonGenerator generator = objectMapper.writer().createGenerator(writer);
				objectMapper.writeValue(generator, obj);
				writer.flush();
				writer.close();
				generator.close();
				return writer.getBuffer().toString();
			} catch (IOException e) {
				throw new InvalidJsonException(e);
			}
		}

		@Override
		public List<Object> createArray() {
			return new LinkedList<>();
		}

		@Override
		public Object createMap() {
			return new LinkedHashMap<String, Object>();
		}
	}

}
