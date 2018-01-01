/*
 * Copyright 2011-2018 the original author or authors.
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
package org.springframework.data.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Indexed;

/**
 * Annotation to demarcate interfaces a repository proxy shall be created for. Annotating an interface with
 * {@link RepositoryDefinition} will cause the same behaviour as extending {@link Repository}.
 *
 * @see Repository
 * @author Oliver Gierke
 */
@Indexed
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RepositoryDefinition {

	/**
	 * The domain class the repository manages. Equivalent to the T type parameter in {@link Repository}.
	 *
	 * @see Repository
	 * @return
	 */
	Class<?> domainClass();

	/**
	 * The id class of the entity the repository manages. Equivalent to the ID type parameter in {@link Repository}.
	 *
	 * @see Repository
	 * @return
	 */
	Class<?> idClass();
}
