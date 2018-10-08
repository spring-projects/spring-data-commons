/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.webflux.config;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically register the following beans for usage with Spring MVC.
 * <ul>
 * <li>{@link org.springframework.data.webflux.PageableHandlerMethodArgumentResolver} - to allow injection of
 * {@link org.springframework.data.domain.Pageable} instances into controller methods automatically created from request
 * parameters.</li>
 * <li>{@link org.springframework.data.webflux.SortHandlerMethodArgumentResolver} - to allow injection of
 * {@link org.springframework.data.domain.Sort} instances into controller methods automatically created from request
 * parameters.</li>
 * </ul>
 *
 * @author Eugene Utkin
 * @see org.springframework.data.webflux.config.PaginationWebFluxConfiguration
 * @since 1.8
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
@Import(EnableWebFluxPagination.PaginationWebFluxConfigurationImportSelector.class)
public @interface EnableWebFluxPagination {

    /**
     * Import selector to import the appropriate configuration class depending on whether Spring HATEOAS is present on the
     * classpath. We need to register the HATEOAS specific class first as apparently only the first class implementing
     * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport} gets callbacks invoked (see
     * https://jira.springsource.org/browse/SPR-10565).
     *
     * @author Eugene Utkin
     */
    class PaginationWebFluxConfigurationImportSelector implements ImportSelector {

        /*
         * (non-Javadoc)
         * @see org.springframework.context.annotation.ImportSelector#selectImports(org.springframework.core.type.AnnotationMetadata)
         */
        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            return new String[]{PaginationWebFluxConfiguration.class.getName()};
        }
    }
}
