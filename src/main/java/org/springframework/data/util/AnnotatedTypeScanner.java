/*
 * Copyright 2014-2023 the original author or authors.
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
package org.springframework.data.util;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Scanner to find types with annotations on the classpath.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Johannes Englmeier
 */
public class AnnotatedTypeScanner implements ResourceLoaderAware, EnvironmentAware {

	private final Iterable<Class<? extends Annotation>> annotationTypess;
	private final boolean considerInterfaces;

	private @Nullable ResourceLoader resourceLoader;
	private @Nullable Environment environment;

	private Consumer<ClassNotFoundException> classNotFoundAction = ex -> {
		throw new IllegalStateException(ex);
	};

	/**
	 * Creates a new {@link AnnotatedTypeScanner} for the given annotation types.
	 *
	 * @param annotationTypes the annotation types to scan for.
	 */
	@SafeVarargs
	public AnnotatedTypeScanner(Class<? extends Annotation>... annotationTypes) {
		this(true, annotationTypes);
	}

	/**
	 * Creates a new {@link AnnotatedTypeScanner} for the given annotation types.
	 *
	 * @param considerInterfaces whether to consider interfaces as well.
	 * @param annotationTypes the annotations to scan for.
	 */
	@SafeVarargs
	public AnnotatedTypeScanner(boolean considerInterfaces, Class<? extends Annotation>... annotationTypes) {
		this(considerInterfaces, Arrays.asList(annotationTypes));
	}

	/**
	 * @param considerInterfaces
	 * @param annotationTypes
	 * @since 3.0
	 */
	public AnnotatedTypeScanner(boolean considerInterfaces, Collection<Class<? extends Annotation>> annotationTypes) {

		this.considerInterfaces = considerInterfaces;
		this.annotationTypess = annotationTypes;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	void setClassNotFoundAction(Consumer<ClassNotFoundException> classNotFoundAction) {
		this.classNotFoundAction = classNotFoundAction;
	}

	public Set<Class<?>> findTypes(String... basePackages) {
		return findTypes(Arrays.asList(basePackages));
	}

	Set<Class<?>> findTypes(Iterable<String> basePackages, Collection<TypeFilter> filters) {

		ClassPathScanningCandidateComponentProvider provider = new InterfaceAwareScanner(considerInterfaces);

		if (resourceLoader != null) {
			provider.setResourceLoader(resourceLoader);
		}

		if (environment != null) {
			provider.setEnvironment(environment);
		}

		filters.forEach(provider::addIncludeFilter);

		Set<Class<?>> types = new HashSet<>();

		ResourceLoader loader = resourceLoader;
		ClassLoader classLoader = loader == null ? null : loader.getClassLoader();

		for (String basePackage : basePackages) {

			for (BeanDefinition definition : provider.findCandidateComponents(basePackage)) {

				String beanClassName = definition.getBeanClassName();

				if (beanClassName == null) {
					throw new IllegalStateException(
							String.format("Unable to obtain bean class name from bean definition %s!", definition));
				}

				try {
					types.add(ClassUtils.forName(beanClassName, classLoader));
				} catch (ClassNotFoundException o_O) {
					classNotFoundAction.accept(o_O);
				}
			}
		}

		return types;
	}

	public Set<Class<?>> findTypes(Iterable<String> basePackages) {
		return findTypes(basePackages, Streamable.of(annotationTypess).stream().map(annotation -> new AnnotationTypeFilter(annotation, true, considerInterfaces)).collect(Collectors.toSet()));
	}

	/**
	 * Custom extension of {@link ClassPathScanningCandidateComponentProvider} to make sure interfaces to not get dropped
	 * from scanning results.
	 *
	 * @author Oliver Gierke
	 */
	private static class InterfaceAwareScanner extends ClassPathScanningCandidateComponentProvider {

		private final boolean considerInterfaces;

		public InterfaceAwareScanner(boolean considerInterfaces) {
			super(false);
			this.considerInterfaces = considerInterfaces;
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			return super.isCandidateComponent(beanDefinition)
					|| considerInterfaces && beanDefinition.getMetadata().isInterface();
		}
	}
}
