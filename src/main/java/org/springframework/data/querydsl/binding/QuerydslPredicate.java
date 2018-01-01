/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to customize the binding of HTTP request parameters to a Querydsl {@link com.mysema.query.types.Predicate}
 * in Spring MVC handler methods.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.11
 */
@Target({ ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface QuerydslPredicate {

	/**
	 * The root type to create the {@link com.mysema.query.types.Predicate}. Specify this explicitly if the type is not
	 * contained in the controller method's return type.
	 *
	 * @return
	 */
	Class<?> root() default Object.class;

	/**
	 * To customize the way individual properties' values should be bound to the predicate a
	 * {@link QuerydslBinderCustomizer} can be specified here. We'll try to obtain a Spring bean of this type but fall
	 * back to a plain instantiation if no bean is found in the current
	 * {@link org.springframework.beans.factory.BeanFactory}.
	 *
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends QuerydslBinderCustomizer> bindings() default QuerydslBinderCustomizer.class;
}
