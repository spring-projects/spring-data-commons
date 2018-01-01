/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.repository.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object to represent {@link MethodParameters} to allow to easily find the ones with a given annotation.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.10
 */
class MethodParameters {

	private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
	private final List<MethodParameter> parameters;

	/**
	 * Creates a new {@link MethodParameters} from the given {@link Method}.
	 *
	 * @param method must not be {@literal null}.
	 */
	public MethodParameters(Method method) {
		this(method, Optional.empty());
	}

	/**
	 * Creates a new {@link MethodParameters} for the given {@link Method} and {@link AnnotationAttribute}. If the latter
	 * is given, method parameter names will be looked up from the annotation attribute if present.
	 *
	 * @param method must not be {@literal null}.
	 * @param namingAnnotation must not be {@literal null}.
	 */
	public MethodParameters(Method method, Optional<AnnotationAttribute> namingAnnotation) {

		Assert.notNull(method, "Method must not be null!");
		this.parameters = new ArrayList<>();

		for (int i = 0; i < method.getParameterCount(); i++) {

			MethodParameter parameter = new AnnotationNamingMethodParameter(method, i, namingAnnotation);
			parameter.initParameterNameDiscovery(discoverer);
			parameters.add(parameter);
		}
	}

	/**
	 * Returns all {@link MethodParameter}s.
	 *
	 * @return
	 */
	public List<MethodParameter> getParameters() {
		return parameters;
	}

	/**
	 * Returns the {@link MethodParameter} with the given name or {@literal null} if none found.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	public Optional<MethodParameter> getParameter(String name) {

		Assert.hasText(name, "Parameter name must not be null!");

		return getParameters().stream()//
				.filter(it -> name.equals(it.getParameterName())).findFirst();
	}

	/**
	 * Returns all parameters of the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 * @since 0.9
	 */
	public List<MethodParameter> getParametersOfType(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return getParameters().stream()//
				.filter(it -> it.getParameterType().equals(type))//
				.collect(Collectors.toList());
	}

	/**
	 * Returns all {@link MethodParameter}s annotated with the given annotation type.
	 *
	 * @param annotation must not be {@literal null}.
	 * @return
	 */
	public List<MethodParameter> getParametersWith(Class<? extends Annotation> annotation) {

		Assert.notNull(annotation, "Annotation must not be null!");

		return getParameters().stream()//
				.filter(it -> it.hasParameterAnnotation(annotation))//
				.collect(Collectors.toList());
	}

	/**
	 * Custom {@link MethodParameter} extension that will favor the name configured in the {@link AnnotationAttribute} if
	 * set over discovering it.
	 *
	 * @author Oliver Gierke
	 */
	private static class AnnotationNamingMethodParameter extends MethodParameter {

		private final Optional<AnnotationAttribute> attribute;
		private final Lazy<String> name;

		/**
		 * Creates a new {@link AnnotationNamingMethodParameter} for the given {@link Method}'s parameter with the given
		 * index.
		 *
		 * @param method must not be {@literal null}.
		 * @param parameterIndex
		 * @param attribute can be {@literal null}
		 */
		public AnnotationNamingMethodParameter(Method method, int parameterIndex, Optional<AnnotationAttribute> attribute) {

			super(method, parameterIndex);

			this.attribute = attribute;
			this.name = Lazy.of(() -> this.attribute.//
					flatMap(it -> it.getValueFrom(this).map(Object::toString)).//
					orElseGet(super::getParameterName));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.MethodParameter#getParameterName()
		 */
		@Nullable
		@Override
		public String getParameterName() {
			return name.orElse(null);
		}
	}
}
