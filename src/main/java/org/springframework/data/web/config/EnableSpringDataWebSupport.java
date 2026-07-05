/*
 * Copyright 2013-present the original author or authors.
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
package org.springframework.data.web.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.util.ClassUtils;

/**
 * Annotation to automatically register the following beans for usage with Spring MVC. Note that using this annotation
 * will require Spring 3.2.
 * <ul>
 * <li>{@link org.springframework.data.repository.support.DomainClassConverter} - to allow usage of domain types managed
 * by Spring Data repositories as controller method arguments bound with
 * {@link org.springframework.web.bind.annotation.PathVariable} or
 * {@link org.springframework.web.bind.annotation.RequestParam}.</li>
 * <li>{@link org.springframework.data.web.PageableHandlerMethodArgumentResolver} - to allow injection of
 * {@link org.springframework.data.domain.Pageable} instances into controller methods automatically created from request
 * parameters.</li>
 * <li>{@link org.springframework.data.web.SortHandlerMethodArgumentResolver} - to allow injection of
 * {@link org.springframework.data.domain.Sort} instances into controller methods automatically created from request
 * parameters.</li>
 * </ul>
 * If Spring HATEOAS is present on the classpath we will register the following beans:
 * <ul>
 * <li>{@link org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver} - instead of
 * {@link org.springframework.data.web.PageableHandlerMethodArgumentResolver}</li>
 * <li>{@link org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver} - instead of
 * {@link org.springframework.data.web.SortHandlerMethodArgumentResolver}</li>
 * <li>{@link org.springframework.data.web.PagedResourcesAssembler} - for injection into web components</li>
 * <li>{@link org.springframework.data.web.SortHandlerMethodArgumentResolver} - for injection of
 * {@link org.springframework.data.web.PagedResourcesAssembler} into controller methods</li>
 * </ul>
 *
 * @since 1.6
 * @see SpringDataWebConfiguration
 * @see HateoasAwareSpringDataWebConfiguration
 * @author Oliver Gierke
 * @author Yanming Zhou
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Inherited
@Import({
		EnableSpringDataWebSupport.SpringDataWebConfigurationImportSelector.class,
		EnableSpringDataWebSupport.QuerydslActivator.class,
		EnableSpringDataWebSupport.SpringDataWebSettingsRegistrar.class
})
public @interface EnableSpringDataWebSupport {

	/**
	 * Configures how to render {@link org.springframework.data.domain.PageImpl} instances. Defaults to
	 * {@link PageSerializationMode#DIRECT} for backward compatibility reasons. Prefer explicitly setting this to
	 * {@link PageSerializationMode#VIA_DTO}, or manually convert {@link org.springframework.data.domain.PageImpl}
	 * instances before handing them out of a controller method, either by manually calling {@code new PagedModel<>(page)}
	 * or using Spring HATEOAS {@link org.springframework.hateoas.PagedModel} abstraction.
	 *
	 * @return will never be {@literal null}.
	 * @since 3.3
	 */
	PageSerializationMode pageSerializationMode() default PageSerializationMode.DIRECT;

	enum PageSerializationMode {

		/**
		 * {@link org.springframework.data.domain.PageImpl} instances will be rendered as is (discouraged, as there's no
		 * guarantee on the stability of the serialization result as we might need to change the type's API for unrelated
		 * reasons).
		 */
		DIRECT,

		/**
		 * Causes {@link org.springframework.data.domain.PageImpl} instances to be wrapped into
		 * {@link org.springframework.data.web.PagedModel} instances before rendering them as JSON to make sure the
		 * representation stays stable even if {@link org.springframework.data.domain.PageImpl} is changed.
		 */
		VIA_DTO;
	}

	/**
	 * Import selector to import the appropriate configuration class depending on whether Spring HATEOAS is present on the
	 * classpath. We need to register the HATEOAS specific class first as apparently only the first class implementing
	 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport} gets callbacks invoked (see
	 * https://jira.springsource.org/browse/SPR-10565).
	 *
	 * @author Oliver Gierke
	 * @author Jens Schauder
	 */
	class SpringDataWebConfigurationImportSelector implements ImportSelector, ResourceLoaderAware {

		private Optional<ClassLoader> resourceLoader = Optional.empty();

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = Optional.of(resourceLoader).map(ResourceLoader::getClassLoader);
		}

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
					.ifPresent(imports::addAll);

			resourceLoader//
					.filter(it -> ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", it))//
					.map(it -> SpringFactoriesLoader.loadFactoryNames(SpringDataJackson3Modules.class, it))//
					.ifPresent(imports::addAll);

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
	class QuerydslActivator implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return QuerydslUtils.QUERY_DSL_PRESENT ? new String[] { QuerydslWebConfiguration.class.getName() }
					: new String[0];
		}
	}

	/**
	 * Registers a bean definition for {@link SpringDataWebSettings} carrying the configuration values of
	 * {@link EnableSpringDataWebSupport}.
	 *
	 * @author Oliver Drotbohm
	 * @author Yanming Zhou
	 * @soundtrack Norah Jones - Chasing Pirates
	 * @since 3.3
	 */
	class SpringDataWebSettingsRegistrar implements ImportBeanDefinitionRegistrar {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.beans.factory.support.BeanNameGenerator)
		 */
		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
				BeanNameGenerator importBeanNameGenerator) {

			Map<String, Object> attributes = importingClassMetadata
					.getAnnotationAttributes(EnableSpringDataWebSupport.class.getName());

			if (attributes == null) {
				return;
			}

			Object pageSerializationMode = attributes.get("pageSerializationMode");

			if (pageSerializationMode == PageSerializationMode.DIRECT) {
				return;
			}

			AbstractBeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(SpringDataWebSettings.class)
					.addConstructorArgValue(pageSerializationMode)
					.getBeanDefinition();

			String beanName = importBeanNameGenerator.generateBeanName(definition, registry);

			registry.registerBeanDefinition(beanName, definition);
		}
	}
}
