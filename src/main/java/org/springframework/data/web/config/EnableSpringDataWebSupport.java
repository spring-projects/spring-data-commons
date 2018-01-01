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
package org.springframework.data.web.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * Annotation to automatically register the following beans for usage with Spring MVC. Note that using this annotation
 * will require Spring 3.2.
 * <ul>
 * <li>{@link org.springframework.data.repository.support.DomainClassConverter} - to allow usage of domain types managed
 * by Spring Data repositories as controller method arguments bound with
 * {@link org.springframework.web.bind.annotation.PathVariable} or
 * {@link org.springframework.web.bind.annotation.RequestParam}.</li>
 * <li>{@link PageableHandlerMethodArgumentResolver} - to allow injection of
 * {@link org.springframework.data.domain.Pageable} instances into controller methods automatically created from request
 * parameters.</li>
 * <li>{@link org.springframework.data.web.SortHandlerMethodArgumentResolver} - to allow injection of
 * {@link org.springframework.data.domain.Sort} instances into controller methods automatically created from request
 * parameters.</li>
 * </ul>
 * If Spring HATEOAS is present on the classpath we will register the following beans:
 * <ul>
 * <li>{@link org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver} - instead of
 * {@link PageableHandlerMethodArgumentResolver}</li>
 * <li>{@link org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver} - instead of
 * {@link org.springframework.data.web.SortHandlerMethodArgumentResolver}</li>
 * <li>{@link org.springframework.data.web.PagedResourcesAssembler} - for injection into web components</li>
 * <li>{@link org.springframework.data.web.SortHandlerMethodArgumentResolver} - for injection of
 * {@link org.springframework.data.web.PagedResourcesAssembler} into controller methods</li>
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
@Import({ EnableSpringDataWebSupport.SpringDataWebConfigurationImportSelector.class,
		EnableSpringDataWebSupport.QuerydslActivator.class })
public @interface EnableSpringDataWebSupport {

	/**
	 * Import selector to import the appropriate configuration class depending on whether Spring HATEOAS is present on the
	 * classpath. We need to register the HATEOAS specific class first as apparently only the first class implementing
	 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport} gets callbacks invoked (see
	 * https://jira.springsource.org/browse/SPR-10565).
	 *
	 * @author Oliver Gierke
	 * @author Jens Schauder
	 */
	static class SpringDataWebConfigurationImportSelector implements ImportSelector, ResourceLoaderAware {

		private Optional<ClassLoader> resourceLoader = Optional.empty();

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
		 */
		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = Optional.of(resourceLoader).map(ResourceLoader::getClassLoader);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.annotation.ImportSelector#selectImports(org.springframework.core.type.AnnotationMetadata)
		 */
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {

			List<String> imports = new ArrayList<>();

			imports.add(ProjectingArgumentResolverRegistrar.class.getName());

			imports.add(resourceLoader//
					.filter(it -> ClassUtils.isPresent("org.springframework.hateoas.Link", it))//
					.map(it -> HateoasAwareSpringDataWebConfiguration.class.getName())//
					.orElseGet(() -> SpringDataWebConfiguration.class.getName()));

			resourceLoader//
					.filter(it -> ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", it))//
					.map(it -> SpringFactoriesLoader.loadFactoryNames(SpringDataJacksonModules.class, it))//
					.ifPresent(it -> imports.addAll(it));

			return imports.toArray(new String[imports.size()]);
		}
	}

	/**
	 * Import selector to register {@link QuerydslWebConfiguration} as configuration class if Querydsl is on the
	 * classpath.
	 *
	 * @author Oliver Gierke
	 * @soundtrack Anika Nilles - Chary Life
	 * @since 1.11
	 */
	static class QuerydslActivator implements ImportSelector {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.annotation.ImportSelector#selectImports(org.springframework.core.type.AnnotationMetadata)
		 */
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return QuerydslUtils.QUERY_DSL_PRESENT ? new String[] { QuerydslWebConfiguration.class.getName() }
					: new String[0];
		}
	}
}
