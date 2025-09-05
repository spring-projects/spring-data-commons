/*
 * Copyright 2016-2025 the original author or authors.
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

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.Accessor;
import org.springframework.data.projection.MethodInterceptorFactory;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * {@link MethodInterceptorFactory} to create a {@link MethodInterceptor} that will
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Mikhael Sokolov
 * @soundtrack Jeff Coffin - Fruitcake (The Inside Of The Outside)
 * @since 1.13
 */
public class JsonProjectingMethodInterceptorFactory implements MethodInterceptorFactory {

	private final ParseContext context;

	/**
	 * Creates a new {@link JsonProjectingMethodInterceptorFactory} using the default {@link JsonProvider} and the given
	 * {@link MappingProvider}.
	 *
	 * @param mappingProvider must not be {@literal null}.
	 * @see Configuration#defaultConfiguration()
	 * @see Configuration#jsonProvider()
	 */
	public JsonProjectingMethodInterceptorFactory(MappingProvider mappingProvider) {
		this(Configuration.defaultConfiguration().jsonProvider(), mappingProvider);
	}

	/**
	 * Creates a new {@link JsonProjectingMethodInterceptorFactory} using the given {@link JsonProvider} and
	 * {@link MappingProvider}.
	 *
	 * @param jsonProvider must not be {@literal null}.
	 * @param mappingProvider must not be {@literal null}.
	 * @since 2.5.3
	 */
	public JsonProjectingMethodInterceptorFactory(JsonProvider jsonProvider, MappingProvider mappingProvider) {

		Assert.notNull(jsonProvider, "JsonProvider must not be null");
		Assert.notNull(mappingProvider, "MappingProvider must not be null");

		Configuration configuration = Configuration.builder()//
				.options(Option.ALWAYS_RETURN_LIST) //
				.jsonProvider(jsonProvider) //
				.mappingProvider(mappingProvider) //
				.build();

		this.context = JsonPath.using(configuration);
	}

	@Override
	public MethodInterceptor createMethodInterceptor(Object source, Class<?> targetType) {

		DocumentContext context = source instanceof InputStream ? this.context.parse((InputStream) source)
				: this.context.parse(source);

		return new InputMessageProjecting(context);
	}

	@Override
	public boolean supports(Object source, Class<?> targetType) {

		if (source instanceof InputStream || source instanceof JSONObject || source instanceof JSONArray) {
			return true;
		}

		return source instanceof Map && hasJsonPathAnnotation(targetType);
	}

	/**
	 * Returns whether the given type contains a method with a {@link org.springframework.data.web.JsonPath} annotation.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static boolean hasJsonPathAnnotation(Class<?> type) {

		for (Method method : type.getMethods()) {
			if (AnnotationUtils.findAnnotation(method, org.springframework.data.web.JsonPath.class) != null) {
				return true;
			}
		}

		return false;
	}

	private record InputMessageProjecting(DocumentContext context) implements MethodInterceptor {

		@Override
		public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();

			switch (method.getName()) {
				case "equals" -> {
					// Only consider equal when proxies are identical.
					return (invocation.getThis() == invocation.getArguments()[0]);
				}
				case "hashCode" -> {
					// Use hashCode of EntityManager proxy.
					return context.hashCode();
				}
				case "toString" -> {
					return context.jsonString();
				}
			}

			TypeInformation<?> returnType = TypeInformation.fromReturnTypeOf(method);
			ResolvableType type = ResolvableType.forMethodReturnType(method);
			boolean isCollectionResult = type.getRawClass() != null && Collection.class.isAssignableFrom(type.getRawClass());
			type = isCollectionResult ? type : ResolvableType.forClassWithGenerics(List.class, type);

			Iterable<String> jsonPaths = getJsonPaths(method);

			for (String jsonPath : jsonPaths) {

				try {

					if (returnType.getRequiredActualType().getType().isInterface()) {

						List<?> result = context.read(jsonPath);
						Object nested = result.isEmpty() ? null : result.get(0);

						return isCollectionResult && !(nested instanceof Collection) ? result : nested;
					}

					boolean definitePath = JsonPath.isPathDefinite(jsonPath);
					type = isCollectionResult && definitePath ? ResolvableType.forClassWithGenerics(List.class, type) : type;

					List<?> result = (List<?>) context.read(jsonPath, new ResolvableTypeRef(type));

					if (isCollectionResult && definitePath) {
						result = (List<?>) result.get(0);
					}

					return isCollectionResult ? result : result.isEmpty() ? null : result.get(0);

				} catch (PathNotFoundException o_O) {
					// continue with next path
				}
			}

			return null;
		}

		/**
		 * Returns the JSONPath expression to be used for the given method.
		 *
		 * @param method
		 * @return
		 */
		private static Collection<String> getJsonPaths(Method method) {

			org.springframework.data.web.JsonPath annotation = AnnotationUtils.findAnnotation(method,
					org.springframework.data.web.JsonPath.class);

			if (annotation != null) {
				return Arrays.asList(annotation.value());
			}

			return Collections.singletonList("$.".concat(new Accessor(method).getPropertyName()));
		}

		private static class ResolvableTypeRef extends TypeRef<Object> {

			private final ResolvableType type;

			ResolvableTypeRef(ResolvableType type) {
				this.type = type;
			}

			@Override
			public Type getType() {
				return type.getType();
			}
		}
	}
}
