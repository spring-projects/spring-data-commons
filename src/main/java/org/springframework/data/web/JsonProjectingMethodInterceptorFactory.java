/*
 * Copyright 2016-2021 the original author or authors.
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
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.Accessor;
import org.springframework.data.projection.MethodInterceptorFactory;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * {@link MethodInterceptorFactory} to create a {@link MethodInterceptor} that will
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Jeff Coffin - Fruitcake (The Inside Of The Outside)
 * @since 1.13
 */
public class JsonProjectingMethodInterceptorFactory implements MethodInterceptorFactory {

	private final ParseContext context;

	/**
	 * Creates a new {@link JsonProjectingMethodInterceptorFactory} using the given {@link ObjectMapper}.
	 *
	 * @param mapper must not be {@literal null}.
	 */
	public JsonProjectingMethodInterceptorFactory(MappingProvider mappingProvider) {

		Assert.notNull(mappingProvider, "MappingProvider must not be null!");

		Configuration build = Configuration.builder()//
				.options(Option.ALWAYS_RETURN_LIST)//
				.mappingProvider(mappingProvider)//
				.build();

		this.context = JsonPath.using(build);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.MethodInterceptorFactory#createMethodInterceptor(java.lang.Object, java.lang.Class)
	 */
	@Override
	public MethodInterceptor createMethodInterceptor(Object source, Class<?> targetType) {

		DocumentContext context = InputStream.class.isInstance(source) ? this.context.parse((InputStream) source)
				: this.context.parse(source);

		return new InputMessageProjecting(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.MethodInterceptorFactory#supports(java.lang.Object, java.lang.Class)
	 */
	@Override
	public boolean supports(Object source, Class<?> targetType) {

		if (InputStream.class.isInstance(source) || JSONObject.class.isInstance(source)
				|| JSONArray.class.isInstance(source)) {
			return true;
		}

		return Map.class.isInstance(source) && hasJsonPathAnnotation(targetType);
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

	private static class InputMessageProjecting implements MethodInterceptor {

		private final DocumentContext context;

		public InputMessageProjecting(DocumentContext context) {
			this.context = context;
		}

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Nullable
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			TypeInformation<Object> returnType = ClassTypeInformation.fromReturnTypeOf(method);
			ResolvableType type = ResolvableType.forMethodReturnType(method);
			boolean isCollectionResult = Collection.class.isAssignableFrom(type.getRawClass());
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
					type = isCollectionResult && definitePath
							? ResolvableType.forClassWithGenerics(List.class, type)
							: type;

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

			/*
			 * (non-Javadoc)
			 * @see com.jayway.jsonpath.TypeRef#getType()
			 */
			@Override
			public Type getType() {
				return type.getType();
			}
		}
	}
}
