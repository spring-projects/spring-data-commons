/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.web.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Annotation to automatically register the following beans for usage with Spring MVC.
 * <ul>
 * <li>{@link DomainClassConverter} - to allow usage of domain types managed by Spring Data repositories as controller
 * method arguments bound with {@link PathVariable} or {@link RequestParam}.</li>
 * <li>{@link PageableHandlerMethodArgumentResolver} - to allow injection of {@link Pageable} instances into controller
 * methods automatically created from request parameters.</li>
 * <li>{@link SortHandlerMethodArgumentResolver} - to allow injection of {@link Sort} instances into controller methods
 * automatically created from request parameters.</li>
 * </ul>
 * If Spring HATEOAS is present on the classpath we will additionall register the following beans:
 * <ul>
 * <li>{@link PagedResourcesAssembler} - for injection into web components</li>
 * <li>{@link PagedResourcesAssemblerArgumentResolver} - for injection of {@link PagedResourcesAssembler} into
 * controller methods</li>
 * <ul>
 * 
 * @since 1.6
 * @see SpringDataWebConfiguration
 * @see HateoasAwareSpringDataWebConfiguration
 * @author Oliver Gierke
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Inherited
@Import(EnableSpringDataWebSupport.SpringDataWebConfigurationImportSelector.class)
public @interface EnableSpringDataWebSupport {

	/**
	 * Import selector to import the appropriate configuration class depending on whether Spring HATEOAS is present on the
	 * classpath. We need to register the HATEOAS specific class first as apparently only the first class implementing
	 * {@link WebMvcConfigurationSupport} gets callbacks invoked (see https://jira.springsource.org/browse/SPR-10565).
	 * 
	 * @author Oliver Gierke
	 */
	class SpringDataWebConfigurationImportSelector implements ImportSelector {

		// Don't make final to allow test cases faking this to false
		private static boolean HATEOAS_PRESENT = ClassUtils.isPresent("org.springframework.hateoas.Link", null);

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.context.annotation.ImportSelector#selectImports(org.springframework.core.type.AnnotationMetadata)
		 */
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {

			List<String> configs = new ArrayList<String>();

			if (HATEOAS_PRESENT) {
				configs.add(HateoasAwareSpringDataWebConfiguration.class.getName());
			}

			configs.add(SpringDataWebConfiguration.class.getName());
			return configs.toArray(new String[configs.size()]);
		}
	}
}
