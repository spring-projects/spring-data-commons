/*
 * Copyright 2013-2025 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Annotation to define the default {@link Sort} options to be used when injecting a {@link Sort} instance into a
 * controller handler method.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Mark Palich
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Repeatable(SortDefault.SortDefaults.class)
public @interface SortDefault {

	/**
	 * Alias for {@link #sort()} to make a declaration configuring fields only more concise.
	 *
	 * @return
	 */
	@AliasFor("sort")
	String[] value() default {};

	/**
	 * The properties to sort by default. If unset, no sorting will be applied at all.
	 *
	 * @return
	 */
	@AliasFor("value")
	String[] sort() default {};

	/**
	 * The direction to sort by. Defaults to {@link Direction#ASC}.
	 *
	 * @return
	 */
	Direction direction() default Direction.ASC;

	/**
	 * Specifies whether to apply case-sensitive sorting. Defaults to {@literal true}.
	 *
	 * @return
	 * @since 2.3
	 */
	boolean caseSensitive() default true;

	/**
	 * Wrapper annotation to allow declaring multiple {@link SortDefault} annotations on a method parameter.
	 *
	 * @since 1.6
	 * @author Oliver Gierke
	 */
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface SortDefaults {

		/**
		 * The individual {@link SortDefault} declarations to be sorted by.
		 *
		 * @return
		 */
		SortDefault[] value();
	}
}
