/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.web.querydsl;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.mysema.query.types.Path;

/**
 * {@link QuerydslBindingContext} holds parameters required for executing {@link QuerydslBinding} on a specific property
 * and {@link Path}.
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
class QuerydslBindingContext {

	private final TypeInformation<?> rootTypeInformation;
	private final ConversionService conversionService;
	private final QuerydslBindings bindings;

	/**
	 * @param rootTypeInformation Root type information. Must not be {@literal null}.
	 * @param bindings Path specific bindings. Defaulted to {@link QuerydslBindings} if {@literal null}.
	 * @param conversionService {@link ConversionService} to be used for converting request parameters into target type.
	 *          Defaulted to {@link DefaultConversionService} if {@literal null}.
	 */
	public QuerydslBindingContext(TypeInformation<?> rootTypeInformation, QuerydslBindings bindings,
			ConversionService conversionService) {

		Assert.notNull(rootTypeInformation, "TypeInformation must not be null!");

		this.conversionService = conversionService == null ? new DefaultConversionService() : conversionService;
		this.rootTypeInformation = rootTypeInformation;
		this.bindings = bindings == null ? new QuerydslBindings() : bindings;
	}

	/**
	 * {@link ConversionService} to be used for converting parameter values.
	 * 
	 * @return never {@literal null}.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Root type information
	 * 
	 * @return never {@literal null}.
	 */
	public TypeInformation<?> getTypeInformation() {
		return rootTypeInformation;
	}

	/**
	 * {@link QuerydslBindings} to be considered.
	 * 
	 * @return never {@literal null}.
	 */
	public QuerydslBindings getBindings() {
		return bindings;
	}

	/**
	 * Specific {@link QuerydslBinding} for {@link PropertyPath}.
	 * 
	 * @param path must not be {@literal null}.
	 * @return {@literal null} when no specific {@link QuerydslBinding} set for path.
	 * @see QuerydslBindings#getBindingForPath(PropertyPath)
	 */
	public QuerydslBinding<? extends Path<?>> getBindingForPath(PropertyPath path) {
		return bindings.getBindingForPath(path);
	}

	/**
	 * Specific {@link Path} for {@link PropertyPath}.
	 * 
	 * @param path
	 * @return
	 * @see QuerydslBindings#getPath(PropertyPath)
	 */
	public Path<?> getPathForPropertyPath(PropertyPath path) {
		return bindings.getPath(path);
	}

	/**
	 * Checks visibility of given path against {@link QuerydslBindings}.
	 * 
	 * @param path must not be {@literal null}.
	 * @return true when given path is visible by declaration.
	 * @see QuerydslBindings#isPathVisible(PropertyPath)
	 */
	public boolean isPathVisible(PropertyPath path) {
		return bindings.isPathVisible(path);
	}

	/**
	 * Get the actual raw type of the used root.
	 * 
	 * @return
	 * @see TypeInformation#getActualType()
	 */
	public Class<?> getTargetType() {
		return rootTypeInformation.getActualType().getType();
	}
}
