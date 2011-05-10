/*
 * Copyright 2008-2011 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.domain.Pageable;


/**
 * Annotation to set defaults when injecting a {@link Pageable} into a
 * controller method.
 *
 * @author Oliver Gierke
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PageableDefaults {

	/**
	 * The default-size the injected
	 * {@link org.springframework.data.domain.Pageable} should get if no
	 * corresponding parameter defined in request (default is 10).
	 */
	int value() default 10;


	/**
	 * The default-pagenumber the injected
	 * {@link org.synyx.hades.domain.Pageable} should get if no corresponding
	 * parameter defined in request (default is 0).
	 */
	int pageNumber() default 0;
}
