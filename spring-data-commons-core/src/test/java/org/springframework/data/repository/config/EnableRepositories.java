/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.repository.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnableRepositories {

	String[] value() default {};

	String[] basePackages() default {};

	Class<?>[] basePackageClasses() default {};

	Filter[] includeFilters() default {};

	Filter[] excludeFilters() default {};

	Class<?> repositoryFactoryBeanClass() default RepositoryFactoryBeanSupport.class;

	String namedQueriesLocation() default "";

	String repositoryImplementationPostfix() default "Impl";
}
