/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.util.Assert;

/**
 * Custom {@link ClassPathScanningCandidateComponentProvider} scanning for interfaces extending the given base
 * interface. Skips interfaces annotated with {@link NoRepositoryBean}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class RepositoryComponentProvider extends ClassPathScanningCandidateComponentProvider {

	private static final String METHOD_NOT_PUBLIC = "AnnotationConfigUtils.processCommonDefinitionAnnotations(…) is not public! Make sure you're using Spring 3.2.5 or better. The class was loaded from %s.";

	private boolean considerNestedRepositoryInterfaces;

	/**
	 * Creates a new {@link RepositoryComponentProvider} using the given {@link TypeFilter} to include components to be
	 * picked up.
	 * 
	 * @param includeFilters the {@link TypeFilter}s to select repository interfaces to consider, must not be
	 *          {@literal null}.
	 */
	public RepositoryComponentProvider(Iterable<? extends TypeFilter> includeFilters) {

		super(false);

		assertRequiredSpringVersionPresent();

		Assert.notNull(includeFilters);

		if (includeFilters.iterator().hasNext()) {
			for (TypeFilter filter : includeFilters) {
				addIncludeFilter(filter);
			}
		} else {
			super.addIncludeFilter(new InterfaceTypeFilter(Repository.class));
			super.addIncludeFilter(new AnnotationTypeFilter(RepositoryDefinition.class, true, true));
		}

		addExcludeFilter(new AnnotationTypeFilter(NoRepositoryBean.class));
	}

	/**
	 * Custom extension of {@link #addIncludeFilter(TypeFilter)} to extend the added {@link TypeFilter}. For the
	 * {@link TypeFilter} handed we'll have two filters registered: one additionally enforcing the
	 * {@link RepositoryDefinition} annotation, the other one forcing the extension of {@link Repository}.
	 * 
	 * @see ClassPathScanningCandidateComponentProvider#addIncludeFilter(TypeFilter)
	 */
	@Override
	public void addIncludeFilter(TypeFilter includeFilter) {

		List<TypeFilter> filterPlusInterface = new ArrayList<>(2);
		filterPlusInterface.add(includeFilter);
		filterPlusInterface.add(new InterfaceTypeFilter(Repository.class));

		super.addIncludeFilter(new AllTypeFilter(filterPlusInterface));

		List<TypeFilter> filterPlusAnnotation = new ArrayList<>(2);
		filterPlusAnnotation.add(includeFilter);
		filterPlusAnnotation.add(new AnnotationTypeFilter(RepositoryDefinition.class, true, true));

		super.addIncludeFilter(new AllTypeFilter(filterPlusAnnotation));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition)
	 */
	@Override
	protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {

		boolean isNonRepositoryInterface = !ClassUtils.isGenericRepositoryInterface(beanDefinition.getBeanClassName());
		boolean isTopLevelType = !beanDefinition.getMetadata().hasEnclosingClass();
		boolean isConsiderNestedRepositories = isConsiderNestedRepositoryInterfaces();

		return isNonRepositoryInterface && (isTopLevelType || isConsiderNestedRepositories);
	}

	/**
	 * Customizes the repository interface detection and triggers annotation detection on them.
	 */
	@Override
	public Set<BeanDefinition> findCandidateComponents(String basePackage) {

		Set<BeanDefinition> candidates = super.findCandidateComponents(basePackage);

		for (BeanDefinition candidate : candidates) {
			if (candidate instanceof AnnotatedBeanDefinition) {
				AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
			}
		}

		return candidates;
	}

	/**
	 * @return the considerNestedRepositoryInterfaces
	 */
	public boolean isConsiderNestedRepositoryInterfaces() {
		return considerNestedRepositoryInterfaces;
	}

	/**
	 * Controls whether nested inner-class {@link Repository} interface definitions should be considered for automatic
	 * discovery. This defaults to {@literal false}.
	 * 
	 * @param considerNestedRepositoryInterfaces
	 */
	public void setConsiderNestedRepositoryInterfaces(boolean considerNestedRepositoryInterfaces) {
		this.considerNestedRepositoryInterfaces = considerNestedRepositoryInterfaces;
	}

	/**
	 * Makes sure {@link AnnotationConfigUtils#processCommonDefinitionAnnotations(AnnotatedBeanDefinition) is public and
	 * indicates the offending JAR if not.
	 */
	private static void assertRequiredSpringVersionPresent() {

		try {
			AnnotationConfigUtils.class.getMethod("processCommonDefinitionAnnotations", AnnotatedBeanDefinition.class);
		} catch (NoSuchMethodException o_O) {
			throw new IllegalStateException(String.format(METHOD_NOT_PUBLIC, AnnotationConfigUtils.class
					.getProtectionDomain().getCodeSource().getLocation()), o_O);
		}
	}

	/**
	 * {@link org.springframework.core.type.filter.TypeFilter} that only matches interfaces. Thus setting this up makes
	 * only sense providing an interface type as {@code targetType}.
	 * 
	 * @author Oliver Gierke
	 */
	private static class InterfaceTypeFilter extends AssignableTypeFilter {

		/**
		 * Creates a new {@link InterfaceTypeFilter}.
		 * 
		 * @param targetType
		 */
		public InterfaceTypeFilter(Class<?> targetType) {
			super(targetType);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter#match(org.springframework.core.type.classreading.MetadataReader, org.springframework.core.type.classreading.MetadataReaderFactory)
		 */
		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {

			return metadataReader.getClassMetadata().isInterface() && super.match(metadataReader, metadataReaderFactory);
		}
	}

	/**
	 * Helper class to create a {@link TypeFilter} that matches if all the delegates match.
	 * 
	 * @author Oliver Gierke
	 */
	private static class AllTypeFilter implements TypeFilter {

		private final List<TypeFilter> delegates;

		/**
		 * Creates a new {@link AllTypeFilter} to match if all the given delegates match.
		 * 
		 * @param delegates must not be {@literal null}.
		 */
		public AllTypeFilter(List<TypeFilter> delegates) {

			Assert.notNull(delegates);
			this.delegates = delegates;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.type.filter.TypeFilter#match(org.springframework.core.type.classreading.MetadataReader, org.springframework.core.type.classreading.MetadataReaderFactory)
		 */
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {

			for (TypeFilter filter : delegates) {
				if (!filter.match(metadataReader, metadataReaderFactory)) {
					return false;
				}
			}

			return true;
		}
	}
}
